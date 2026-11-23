package tech.afrobank.idempotency;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryIdempotencyStorage implements IdempotencyStorage {

    private record Entry(String value, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String key, Duration ttl) {
        Instant expiresAt = Instant.now().plus(ttl);
        Entry newEntry = new Entry("PROCESSING", expiresAt);

        Entry existing = store.putIfAbsent(key, newEntry);
        if (existing == null || existing.isExpired()) {
            store.put(key, newEntry);
            return true;
        }
        return false;
    }

    @Override
    public void saveResponse(String key, String responseJson, Duration ttl) {
        store.put(key, new Entry(responseJson, Instant.now().plus(ttl)));
    }

    @Override
    public Optional<String> getResponse(String key) {
        Entry entry = store.get(key);
        if (entry != null && !entry.isExpired() && !"PROCESSING".equals(entry.value())) {
            return Optional.of(entry.value());
        }
        return Optional.empty();
    }
}
