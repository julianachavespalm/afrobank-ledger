package tech.afrobank.adapter.out.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisIdempotencyAdapterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RedisIdempotencyAdapter adapter;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        adapter = new RedisIdempotencyAdapter(redisTemplate, objectMapper);
    }

    @Test
    @DisplayName("Should successfully acquire lock with SET NX EX")
    void shouldAcquireLock() {
        when(valueOperations.setIfAbsent(eq("lock:idem:k1"), eq("PROCESSING"), any(Duration.class)))
                .thenReturn(Boolean.TRUE);

        boolean acquired = adapter.acquireLock("k1", Duration.ofSeconds(60));

        assertThat(acquired).isTrue();
        verify(valueOperations).setIfAbsent("lock:idem:k1", "PROCESSING", Duration.ofSeconds(60));
    }

    @Test
    @DisplayName("Should fail to acquire lock when key already exists")
    void shouldFailToAcquireExistingLock() {
        when(valueOperations.setIfAbsent(eq("lock:idem:k1"), eq("PROCESSING"), any(Duration.class)))
                .thenReturn(Boolean.FALSE);

        boolean acquired = adapter.acquireLock("k1", Duration.ofSeconds(60));

        assertThat(acquired).isFalse();
    }

    @Test
    @DisplayName("Should release lock by deleting Redis key")
    void shouldReleaseLock() {
        adapter.releaseLock("k1");

        verify(redisTemplate).delete("lock:idem:k1");
    }

    @Test
    @DisplayName("Should save response payload and set lock status to COMPLETED")
    void shouldSaveResponse() {
        adapter.saveResponse("k1", "{\"status\":\"ok\"}", Duration.ofHours(24));

        verify(valueOperations).set("resp:idem:k1", "{\"status\":\"ok\"}", Duration.ofHours(24));
        verify(valueOperations).set("lock:idem:k1", "COMPLETED", Duration.ofHours(24));
    }

    @Test
    @DisplayName("Should retrieve cached response if present")
    void shouldGetCachedResponse() {
        when(valueOperations.get("resp:idem:k1")).thenReturn("{\"status\":\"ok\"}");

        Optional<String> response = adapter.getResponse("k1");

        assertThat(response).isPresent().contains("{\"status\":\"ok\"}");
    }

    @Test
    @DisplayName("Should return empty when cached response does not exist")
    void shouldReturnEmptyWhenNotFound() {
        when(valueOperations.get("resp:idem:k1")).thenReturn(null);

        Optional<String> response = adapter.getResponse("k1");

        assertThat(response).isEmpty();
    }
}
