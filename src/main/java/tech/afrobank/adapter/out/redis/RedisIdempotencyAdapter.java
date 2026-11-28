package tech.afrobank.adapter.out.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tech.afrobank.application.port.out.IdempotencyPort;
import tech.afrobank.idempotency.IdempotencyStorage;

import java.time.Duration;
import java.util.Optional;

@Component
@Primary
public class RedisIdempotencyAdapter implements IdempotencyPort, IdempotencyStorage {

    private static final Logger log = LoggerFactory.getLogger(RedisIdempotencyAdapter.class);

    public static final String LOCK_PREFIX = "lock:idem:";
    public static final String RESPONSE_PREFIX = "resp:idem:";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_COMPLETED = "COMPLETED";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisIdempotencyAdapter(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean acquireLock(String key, Duration ttl) {
        if (key == null || key.isBlank()) {
            return false;
        }
        String lockKey = LOCK_PREFIX + key.trim();
        Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, STATUS_PROCESSING, ttl);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void releaseLock(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        String lockKey = LOCK_PREFIX + key.trim();
        redisTemplate.delete(lockKey);
    }

    @Override
    public <T> void saveResponse(String key, T response, Duration ttl) {
        if (key == null || key.isBlank() || response == null) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(response);
            saveResponse(key, json, ttl);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize idempotency response for key [{}]: {}", key, ex.getMessage());
            throw new RuntimeException("Could not serialize idempotency response", ex);
        }
    }

    @Override
    public <T> Optional<T> getResponse(String key, Class<T> responseType) {
        return getResponse(key).flatMap(json -> {
            try {
                return Optional.of(objectMapper.readValue(json, responseType));
            } catch (JsonProcessingException ex) {
                log.error("Failed to deserialize cached response for key [{}]: {}", key, ex.getMessage());
                return Optional.empty();
            }
        });
    }

    // Compatibilidade com IdempotencyStorage legada
    @Override
    public boolean tryAcquire(String key, Duration ttl) {
        return acquireLock(key, ttl);
    }

    @Override
    public void saveResponse(String key, String responseJson, Duration ttl) {
        if (key == null || key.isBlank() || responseJson == null) {
            return;
        }
        String trimmedKey = key.trim();
        String respKey = RESPONSE_PREFIX + trimmedKey;
        String lockKey = LOCK_PREFIX + trimmedKey;

        redisTemplate.opsForValue().set(respKey, responseJson, ttl);
        redisTemplate.opsForValue().set(lockKey, STATUS_COMPLETED, ttl);
    }

    @Override
    public Optional<String> getResponse(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        String respKey = RESPONSE_PREFIX + key.trim();
        String json = redisTemplate.opsForValue().get(respKey);
        return Optional.ofNullable(json);
    }
}
