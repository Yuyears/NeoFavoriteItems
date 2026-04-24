package mycraft.yuyears.neofavoriteitems.common.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 反射辅助工具类。
 *
 * <p>使用带 TTL 的缓存减少重复反射开销，同时为失败查找保留负缓存，避免热点路径反复探测。
 * 缓存可通过手动失效或 TTL 过期重新刷新，防止一次失败永久生效。</p>
 */
public final class ReflectionHelper {
    private static final long DEFAULT_CACHE_TTL_MILLIS = 5 * 60 * 1000L;

    private static final Map<String, CacheEntry<Field>> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, CacheEntry<Method>> METHOD_CACHE = new ConcurrentHashMap<>();

    private static volatile long cacheTtlMillis = DEFAULT_CACHE_TTL_MILLIS;

    private ReflectionHelper() {}

    public static Integer readIntField(Object target, String fieldName) {
        Field field = findField(target.getClass(), fieldName);
        if (field == null) {
            return null;
        }

        try {
            return field.getInt(target);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T readField(Object target, String fieldName, Class<T> type) {
        Field field = findField(target.getClass(), fieldName);
        if (field == null || !type.isAssignableFrom(field.getType())) {
            return null;
        }

        try {
            return (T) field.get(target);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    public static Integer invokeIntMethod(Object target, String methodName) {
        Method method = findMethod(target.getClass(), methodName);
        if (method == null) {
            return null;
        }

        try {
            return (Integer) method.invoke(target);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T invokeMethod(Object target, String methodName, Class<T> returnType) {
        Method method = findMethod(target.getClass(), methodName);
        if (method == null || !returnType.isAssignableFrom(method.getReturnType())) {
            return null;
        }

        try {
            return (T) method.invoke(target);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    public static void invalidateFieldCache(Class<?> clazz, String fieldName) {
        FIELD_CACHE.remove(fieldCacheKey(clazz, fieldName));
    }

    public static void invalidateMethodCache(Class<?> clazz, String methodName) {
        METHOD_CACHE.remove(methodCacheKey(clazz, methodName));
    }

    public static void clearExpiredCacheEntries() {
        long now = System.currentTimeMillis();
        FIELD_CACHE.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
        METHOD_CACHE.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    public static void clearCache() {
        FIELD_CACHE.clear();
        METHOD_CACHE.clear();
    }

    public static String getCacheStats() {
        return String.format(
            "Field cache: %d entries, Method cache: %d entries",
            FIELD_CACHE.size(),
            METHOD_CACHE.size()
        );
    }

    static void setCacheTtlMillisForTesting(long ttlMillis) {
        cacheTtlMillis = ttlMillis;
    }

    static int fieldCacheSizeForTesting() {
        return FIELD_CACHE.size();
    }

    static int methodCacheSizeForTesting() {
        return METHOD_CACHE.size();
    }

    private static Field findField(Class<?> clazz, String fieldName) {
        String cacheKey = fieldCacheKey(clazz, fieldName);
        CacheEntry<Field> cached = getOrRefresh(FIELD_CACHE, cacheKey, () -> locateField(clazz, fieldName));
        return cached == null ? null : cached.value();
    }

    private static Method findMethod(Class<?> clazz, String methodName) {
        String cacheKey = methodCacheKey(clazz, methodName);
        CacheEntry<Method> cached = getOrRefresh(METHOD_CACHE, cacheKey, () -> locateMethod(clazz, methodName));
        return cached == null ? null : cached.value();
    }

    private static <T> CacheEntry<T> getOrRefresh(
        Map<String, CacheEntry<T>> cache,
        String cacheKey,
        ReflectiveLookup<T> lookup
    ) {
        long now = System.currentTimeMillis();
        CacheEntry<T> cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired(now)) {
            return cached;
        }

        CacheEntry<T> refreshed = new CacheEntry<>(lookup.find(), now + cacheTtlMillis);
        cache.put(cacheKey, refreshed);
        return refreshed;
    }

    private static Field locateField(Class<?> clazz, String fieldName) {
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            try {
                Field field = currentClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }

    private static Method locateMethod(Class<?> clazz, String methodName) {
        try {
            Method method = clazz.getMethod(methodName);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static String fieldCacheKey(Class<?> clazz, String fieldName) {
        return clazz.getName() + "#" + fieldName;
    }

    private static String methodCacheKey(Class<?> clazz, String methodName) {
        return clazz.getName() + "#" + methodName + "()";
    }

    private record CacheEntry<T>(T value, long expiresAt) {
        private boolean isExpired(long now) {
            return now >= expiresAt;
        }
    }

    @FunctionalInterface
    private interface ReflectiveLookup<T> {
        T find();
    }
}
