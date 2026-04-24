package mycraft.yuyears.neofavoriteitems.common.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReflectionHelperTest {
    @AfterEach
    void tearDown() {
        ReflectionHelper.setCacheTtlMillisForTesting(5 * 60 * 1000L);
        ReflectionHelper.clearCache();
    }

    @Test
    void readsInheritedMembersAndCachesThem() {
        Child child = new Child();

        assertEquals(7, ReflectionHelper.readIntField(child, "value"));
        assertEquals("hello", ReflectionHelper.invokeMethod(child, "text", String.class));
        assertEquals(1, ReflectionHelper.fieldCacheSizeForTesting());
        assertEquals(1, ReflectionHelper.methodCacheSizeForTesting());
    }

    @Test
    void negativeCacheEntriesCanBeInvalidatedAndExpired() {
        Child child = new Child();

        assertNull(ReflectionHelper.readIntField(child, "missing"));
        assertEquals(1, ReflectionHelper.fieldCacheSizeForTesting());

        ReflectionHelper.invalidateFieldCache(Child.class, "missing");
        assertEquals(0, ReflectionHelper.fieldCacheSizeForTesting());

        ReflectionHelper.setCacheTtlMillisForTesting(0L);
        assertNull(ReflectionHelper.readIntField(child, "missing"));
        ReflectionHelper.clearExpiredCacheEntries();
        assertEquals(0, ReflectionHelper.fieldCacheSizeForTesting());
    }

    private static class Parent {
        @SuppressWarnings("unused")
        private final int value = 7;

        public String text() {
            return "hello";
        }
    }

    private static final class Child extends Parent {
    }
}
