package tech.afrobank.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.afrobank.exception.IdempotencyException;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotencyManager {

    private final IdempotencyStorage idempotencyStorage;
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    /**
     * Valida e adquire a chave de idempotência atomicamente.
     */
    public void acquire(String key) {
        if (key == null || key.isBlank()) {
            return;
        }

        boolean acquired = idempotencyStorage.tryAcquire(key, DEFAULT_TTL);
        if (!acquired) {
            Optional<String> cached = idempotencyStorage.getResponse(key);
            if (cached.isPresent()) {
                throw new IdempotencyException("Operação já processada com sucesso anteriormente para a chave: " + key);
            }
            throw new IdempotencyException("Operação em processamento para a chave informada: " + key);
        }
    }

    /**
     * Registra o payload de resposta após o processamento da transação.
     */
    public void complete(String key, String responsePayload) {
        if (key != null && !key.isBlank()) {
            idempotencyStorage.saveResponse(key, responsePayload, DEFAULT_TTL);
        }
    }
}
