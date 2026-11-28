package tech.afrobank.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tech.afrobank.application.port.out.IdempotencyPort;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class IdempotencyFilter extends OncePerRequestFilter {

    public static final String IDEMPOTENCY_HEADER = "X-Idempotency-Key";
    private static final Duration LOCK_TTL = Duration.ofSeconds(120);
    private static final Duration RESPONSE_TTL = Duration.ofHours(24);
    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final IdempotencyPort idempotencyPort;
    private final ObjectMapper objectMapper;

    public IdempotencyFilter(Optional<IdempotencyPort> idempotencyPort, ObjectMapper objectMapper) {
        this.idempotencyPort = idempotencyPort.orElse(null);
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String method = request.getMethod();
        String idempotencyKey = request.getHeader(IDEMPOTENCY_HEADER);

        // Apenas requisições mutantes com chave de idempotência e porta ativa são interceptadas
        if (idempotencyPort == null || !MUTATING_METHODS.contains(method) || idempotencyKey == null || idempotencyKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = idempotencyKey.trim();

        // Tenta adquirir a trava atômica de processamento
        boolean acquired = idempotencyPort.acquireLock(key, LOCK_TTL);

        if (!acquired) {
            // Verifica se a resposta já está concluída em cache
            Optional<String> cachedResponse = idempotencyPort.getResponse(key, String.class);
            if (cachedResponse.isPresent()) {
                response.setStatus(HttpStatus.OK.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write(cachedResponse.get());
                return;
            }

            // Operação em processamento concorrente por outra thread/nó -> 409 Conflict
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.CONFLICT,
                    "A transaction with idempotency key [" + key + "] is currently being processed."
            );
            problem.setTitle("Idempotency Conflict");
            problem.setType(URI.create("https://afrobank.tech/errors/idempotency-conflict"));

            response.setStatus(HttpStatus.CONFLICT.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getWriter(), problem);
            return;
        }

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(request, responseWrapper);

            int status = responseWrapper.getStatus();
            if (status >= 200 && status < 300) {
                // Requisição bem-sucedida: armazena a resposta em cache
                byte[] content = responseWrapper.getContentAsByteArray();
                if (content.length > 0) {
                    String responseBody = new String(content, StandardCharsets.UTF_8);
                    idempotencyPort.saveResponse(key, responseBody, RESPONSE_TTL);
                }
            } else {
                // Falha de validação ou erro: libera a trava para permitir nova tentativa
                idempotencyPort.releaseLock(key);
            }
            responseWrapper.copyBodyToResponse();
        } catch (Exception ex) {
            idempotencyPort.releaseLock(key);
            throw ex;
        }
    }
}
