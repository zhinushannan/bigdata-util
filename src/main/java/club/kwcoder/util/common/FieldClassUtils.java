package club.kwcoder.util.common;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 获取实体类的属性名和类型的工具类，包括：
 * - 获取实体类所有、public、private、protected、default的  键为属性名的byte数组、值为属性的类型  的map集合
 * - 获取实体类所有、public、private、protected、default的  键为属性名字符串、值为属性的类型  的map集合
 *
 * @author zhinushannan
 */
public class FieldClassUtils {

    /**
     * 根据实体类类型获取 键为属性名的byte数组、值为属性的类型  的map集合
     * 范围：所有属性
     *
     * @param cls 实体类类型
     */
    public static Map<byte[], Class<?>> getAllFieldClassBytesKey(Class<?> cls) {
        return getFieldClass2Bytes(getFieldClass(cls, -1));
    }

    /**
     * 根据实体类类型获取 键为属性名的byte数组、值为属性的类型  的map集合
     * 范围：default属性
     *
     * @param cls 实体类类型
     */
    public static Map<byte[], Class<?>> getDefaultFieldClassBytesKey(Class<?> cls) {
        return getFieldClass2Bytes(getFieldClass(cls, 0));
    }

    /**
     * 根据实体类类型获取 键为属性名的byte数组、值为属性的类型  的map集合
     * 范围：public属性
     *
     * @param cls 实体类类型
     */
    public static Map<byte[], Class<?>> getPublicFieldClassBytesKey(Class<?> cls) {
        return getFieldClass2Bytes(getFieldClass(cls, 1));
    }

    /**
     * 根据实体类类型获取 键为属性名的byte数组、值为属性的类型  的map集合
     * 范围：private属性
     *
     * @param cls 实体类类型
     */
    public static Map<byte[], Class<?>> getPrivateFieldClassBytesKey(Class<?> cls) {
        return getFieldClass2Bytes(getFieldClass(cls, 2));
    }

    /**
     * 根据实体类类型获取 键为属性名的byte数组、值为属性的类型  的map集合
     * 范围：protected属性
     *
     * @param cls 实体类类型
     */
    public static Map<byte[], Class<?>> getProtectedFieldClassBytesKey(Class<?> cls) {
        return getFieldClass2Bytes(getFieldClass(cls, 4));
    }

    /**
     * 根据实体类类型获取 键为属性名的名称、值为属性的类型  的map集合
     * 范围：所有属性
     *
     * @param cls 实体类类型
     */
    public static Map<String, Class<?>> getAllFieldClass(Class<?> cls) {
        return getFieldClass(cls, -1);
    }

    /**
     * 根据实体类类型获取 键为属性名的名称、值为属性的类型  的map集合
     * 范围：default属性
     *
     * @param cls 实体类类型
     */
    public static Map<String, Class<?>> getDefaultFieldClass(Class<?> cls) {
        return getFieldClass(cls, 0);
    }

    /**
     * 根据实体类类型获取 键为属性名的名称、值为属性的类型  的map集合
     * 范围：public属性
     *
     * @param cls 实体类类型
     */
    public static Map<String, Class<?>> getPublicFieldClass(Class<?> cls) {
        return getFieldClass(cls, 1);
    }

    /**
     * 根据实体类类型获取 键为属性名的名称、值为属性的类型  的map集合
     * 范围：private属性
     *
     * @param cls 实体类类型
     */
    public static Map<String, Class<?>> getPrivateFieldClass(Class<?> cls) {
        return getFieldClass(cls, 2);
    }

    /**
     * 根据实体类类型获取 键为属性名的名称、值为属性的类型  的map集合
     * 范围：protected属性
     *
     * @param cls 实体类类型
     */
    public static Map<String, Class<?>> getProtectedFieldClass(Class<?> cls) {
        return getFieldClass(cls, 4);
    }

    /**
     * 根据类型和访问控制符获取对应的 键为属性名的名称、值为属性的类型  的map集合
     *
     * @param cls       实体类类型
     * @param modifiers 访问控制符
     */
    private static Map<String, Class<?>> getFieldClass(Class<?> cls, int modifiers) {
        Field[] declaredFields = cls.getDeclaredFields();
        Map<String, Class<?>> field2Class = new HashMap<>();
        for (Field declaredField : declaredFields) {
            if (modifiers == -1) {
                if (declaredField.getName().startsWith("this$")) {
                    continue;
                }
                field2Class.put(declaredField.getName(), declaredField.getType());
            } else if (declaredField.getModifiers() == modifiers) {
                field2Class.put(declaredField.getName(), declaredField.getType());
            }
        }
        return field2Class;
    }

    /**
     * 通过 键为属性名的名称、值为属性的类型  的map集合 转换为 键为属性名的byte数组、值为属性的类型  的map集合
     *
     * @param map 键为属性名的名称、值为属性的类型  的map集合
     */
    private static Map<byte[], Class<?>> getFieldClass2Bytes(Map<String, Class<?>> map) {
        Map<byte[], Class<?>> field2Class = new HashMap<>();
        for (Map.Entry<String, Class<?>> entry : map.entrySet()) {
            field2Class.put(entry.getKey().getBytes(StandardCharsets.UTF_8), entry.getValue());
        }
        return field2Class;
    }

}
