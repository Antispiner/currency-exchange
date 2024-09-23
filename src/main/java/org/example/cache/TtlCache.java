package org.example.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.lang.Nullable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class TtlCache implements Cache {

    private final String name;
    private final Duration ttl;
    private final ConcurrentHashMap<Object, CacheValue> store = new ConcurrentHashMap<>();

    public TtlCache(String name, Duration ttl) {
        this.name = name;
        this.ttl = ttl;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return store;
    }

    @Override
    @Nullable
    public ValueWrapper get(Object key) {
        CacheValue cacheValue = store.get(key);
        if (cacheValue != null && !cacheValue.isExpired()) {
            return new SimpleValueWrapper(cacheValue.getValue());
        } else {
            evict(key);
            return null;
        }
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, @Nullable Class<T> type) {
        ValueWrapper valueWrapper = get(key);
        if (valueWrapper != null) {
            Object value = valueWrapper.get();
            if (type == null || type.isInstance(value)) {
                return (T) value;
            } else {
                throw new IllegalStateException("Cached value is not of required type [" + type.getName() + "]: " + value);
            }
        } else {
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Callable<T> valueLoader) {
        try {
            while (true) {
                CacheValue cacheValue = store.get(key);
                if (cacheValue != null && !cacheValue.isExpired()) {
                    return (T) cacheValue.getValue();
                } else {
                    CacheValue newValue = new CacheValue(valueLoader.call(), LocalDateTime.now());
                    CacheValue existingValue = store.putIfAbsent(key, newValue);
                    if (existingValue == null) {
                        return (T) newValue.getValue();
                    } else if (!existingValue.isExpired()) {
                        return (T) existingValue.getValue();
                    } else {
                        store.replace(key, existingValue, newValue);
                    }
                }
            }
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }

    @Override
    public void put(Object key, @Nullable Object value) {
        if (value != null) {
            store.put(key, new CacheValue(value, LocalDateTime.now()));
        } else {
            evict(key);
        }
    }

    @Override
    @Nullable
    public ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
        if (value == null) {
            return get(key);
        }
        CacheValue newValue = new CacheValue(value, LocalDateTime.now());
        CacheValue existingValue = store.putIfAbsent(key, newValue);
        if (existingValue == null || existingValue.isExpired()) {
            return null;
        } else {
            return new SimpleValueWrapper(existingValue.getValue());
        }
    }

    @Override
    public void evict(Object key) {
        store.remove(key);
    }

    @Override
    public boolean evictIfPresent(Object key) {
        return store.remove(key) != null;
    }

    @Override
    public void clear() {
        store.clear();
    }

    @Override
    public boolean invalidate() {
        boolean wasNotEmpty = !store.isEmpty();
        store.clear();
        return wasNotEmpty;
    }

    private class CacheValue {
        private final Object value;
        private final LocalDateTime timestamp;

        public CacheValue(Object value, LocalDateTime timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }

        public boolean isExpired() {
            return timestamp.plus(ttl).isBefore(LocalDateTime.now());
        }

        public Object getValue() {
            return value;
        }
    }
}
