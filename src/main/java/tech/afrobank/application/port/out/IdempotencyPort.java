package tech.afrobank.application.port.out;

import java.time.Duration;
import java.util.Optional;

public interface IdempotencyPort {

    boolean acquireLock(String key, Duration ttl);

    void releaseLock(String key);

    <T> void saveResponse(String key, T response, Duration ttl);

    <T> Optional<T> getResponse(String key, Class<T> responseType);
}
