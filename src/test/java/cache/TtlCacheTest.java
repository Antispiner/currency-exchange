package cache;

import org.example.cache.TtlCache;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TtlCacheTest {

    @Test
    void testPutAndGet() {
        TtlCache cache = new TtlCache("testCache", Duration.ofSeconds(1));
        cache.put("key1", "value1");
        Cache.ValueWrapper value = cache.get("key1");

        assertNotNull(value);
        assertEquals("value1", value.get());
    }

    @Test
    void testExpiration() throws InterruptedException {
        TtlCache cache = new TtlCache("testCache", Duration.ofMillis(500));
        cache.put("key1", "value1");
        Thread.sleep(600);
        Cache.ValueWrapper value = cache.get("key1");

        assertNull(value);
    }

    @Test
    void testEvict() {
        TtlCache cache = new TtlCache("testCache", Duration.ofSeconds(1));
        cache.put("key1", "value1");
        cache.evict("key1");
        Cache.ValueWrapper value = cache.get("key1");

        assertNull(value);
    }

    @Test
    void testClear() {
        TtlCache cache = new TtlCache("testCache", Duration.ofSeconds(1));
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.clear();

        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));
    }

    @Test
    void testGetWithCallable() throws Exception {
        TtlCache cache = new TtlCache("testCache", Duration.ofSeconds(1));

        String value = cache.get("key1", () -> "value1");
        assertEquals("value1", value);

        Cache.ValueWrapper cachedValue = cache.get("key1");
        assertNotNull(cachedValue);
        assertEquals("value1", cachedValue.get());
    }
}
