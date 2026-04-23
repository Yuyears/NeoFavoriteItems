package mycraft.yuyears.neofavoriteitems.common.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 反射辅助工具类
 * 
 * <p>提供带缓存的反射操作，避免重复反射带来的性能开销。</p>
 * 
 * <p><b>使用场景</b>：</p>
 * <ul>
 *   <li>访问 Minecraft 内部字段（如 hoveredSlot、leftPos、topPos）</li>
 *   <li>调用可能不存在的方法（如 getContainerSlot、getKey）</li>
 *   <li>需要跨版本兼容的反射操作</li>
 * </ul>
 * 
 * <p><b>线程安全</b>：所有缓存操作都是线程安全的</p>
 * <p><b>性能</b>：首次访问有反射开销，后续访问直接从缓存获取</p>
 */
public final class ReflectionHelper {
    
    private static final Map<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();
    
    private ReflectionHelper() {
        // 防止实例化
    }
    
    /**
     * 从对象中读取 int 字段值（支持继承链查找）
     * 
     * @param target 目标对象
     * @param fieldName 字段名
     * @return 字段值，如果失败返回 null
     */
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
    
    /**
     * 从对象中读取指定类型的字段值（支持继承链查找）
     * 
     * @param target 目标对象
     * @param fieldName 字段名
     * @param type 期望的字段类型
     * @param <T> 字段类型
     * @return 字段值，如果失败返回 null
     */
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
    
    /**
     * 调用对象的 int 返回值方法
     * 
     * @param target 目标对象
     * @param methodName 方法名
     * @return 方法返回值，如果失败返回 null
     */
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
    
    /**
     * 调用对象的指定返回值类型的方法
     * 
     * @param target 目标对象
     * @param methodName 方法名
     * @param returnType 期望的返回类型
     * @param <T> 返回类型
     * @return 方法返回值，如果失败返回 null
     */
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
    
    /**
     * 查找字段（带缓存，支持继承链查找）
     * 
     * @param clazz 类
     * @param fieldName 字段名
     * @return 找到的字段，如果不存在返回 null
     */
    private static Field findField(Class<?> clazz, String fieldName) {
        String cacheKey = clazz.getName() + "#" + fieldName;
        
        return FIELD_CACHE.computeIfAbsent(cacheKey, key -> {
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
        });
    }
    
    /**
     * 查找无参方法（带缓存）
     * 
     * @param clazz 类
     * @param methodName 方法名
     * @return 找到的方法，如果不存在返回 null
     */
    private static Method findMethod(Class<?> clazz, String methodName) {
        String cacheKey = clazz.getName() + "#" + methodName + "()";
        
        return METHOD_CACHE.computeIfAbsent(cacheKey, key -> {
            try {
                Method method = clazz.getMethod(methodName);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException e) {
                return null;
            }
        });
    }
    
    /**
     * 清除所有缓存（用于测试或内存管理）
     */
    public static void clearCache() {
        FIELD_CACHE.clear();
        METHOD_CACHE.clear();
    }
    
    /**
     * 获取缓存统计信息
     * 
     * @return 格式化的统计字符串
     */
    public static String getCacheStats() {
        return String.format("Field cache: %d entries, Method cache: %d entries",
                FIELD_CACHE.size(), METHOD_CACHE.size());
    }
}
