package tech.afrobank.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tech.afrobank.application.port.out.IdempotencyPort;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyFilterTest {

    @Mock
    private IdempotencyPort idempotencyPort;

    @Mock
    private FilterChain filterChain;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private IdempotencyFilter filter;

    @BeforeEach
    void setUp() {
        filter = new IdempotencyFilter(Optional.of(idempotencyPort), objectMapper);
    }

    @Test
    @DisplayName("Should bypass GET requests without idempotency check")
    void shouldBypassGetRequests() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/accounts/1/balance");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(idempotencyPort);
    }

    @Test
    @DisplayName("Should bypass POST requests without X-Idempotency-Key header")
    void shouldBypassPostWithoutHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/transfers");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(idempotencyPort);
    }

    @Test
    @DisplayName("Should acquire lock, execute request, and cache response on 2xx")
    void shouldProcessAndCacheOnSuccess() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/transfers");
        request.addHeader(IdempotencyFilter.IDEMPOTENCY_HEADER, "test-key-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(idempotencyPort.acquireLock(eq("test-key-1"), any(Duration.class))).thenReturn(true);

        doAnswer(invocation -> {
            HttpServletResponse resp = invocation.getArgument(1);
            resp.setStatus(201);
            resp.getWriter().write("{\"txId\":\"abc\"}");
            return null;
        }).when(filterChain).doFilter(eq(request), any());

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(201);
        verify(idempotencyPort).saveResponse(eq("test-key-1"), eq("{\"txId\":\"abc\"}"), any(Duration.class));
    }

    @Test
    @DisplayName("Should return 409 Conflict when lock is already held and no response is cached")
    void shouldReturn409ConflictWhenProcessing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/transfers");
        request.addHeader(IdempotencyFilter.IDEMPOTENCY_HEADER, "test-key-2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(idempotencyPort.acquireLock(eq("test-key-2"), any(Duration.class))).thenReturn(false);
        when(idempotencyPort.getResponse("test-key-2", String.class)).thenReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(response.getContentAsString()).contains("Idempotency Conflict");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Should replay cached response when available without re-executing chain")
    void shouldReplayCachedResponse() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/transfers");
        request.addHeader(IdempotencyFilter.IDEMPOTENCY_HEADER, "test-key-3");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(idempotencyPort.acquireLock(eq("test-key-3"), any(Duration.class))).thenReturn(false);
        when(idempotencyPort.getResponse("test-key-3", String.class)).thenReturn(Optional.of("{\"status\":\"replayed\"}"));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).isEqualTo("{\"status\":\"replayed\"}");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Should release lock when downstream throws exception")
    void shouldReleaseLockOnException() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/transfers");
        request.addHeader(IdempotencyFilter.IDEMPOTENCY_HEADER, "test-key-4");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(idempotencyPort.acquireLock(eq("test-key-4"), any(Duration.class))).thenReturn(true);
        doThrow(new RuntimeException("Database error")).when(filterChain).doFilter(eq(request), any());

        try {
            filter.doFilter(request, response, filterChain);
        } catch (RuntimeException ignored) {}

        verify(idempotencyPort).releaseLock("test-key-4");
    }
}
