package tech.afrobank.idempotency;

import java.time.Duration;
import java.util.Optional;

public interface IdempotencyStorage {

    /**
     * Tenta adquirir a chave de idempotência de forma atômica com status PROCESSING.
     * Retorna true se a chave foi adquirida pela primeira vez; false se já existia.
     */
    boolean tryAcquire(String key, Duration ttl);

    /**
     * Registra o payload de resposta final associado à chave de idempotência com status COMPLETED.
     */
    void saveResponse(String key, String responseJson, Duration ttl);

    /**
     * Obtém o payload de resposta cacheado se a operação já estiver concluída.
     */
    Optional<String> getResponse(String key);
}
