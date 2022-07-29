package io.github.zhinushannan.constant;

import java.util.HashMap;
import java.util.Map;

public class ConvertConst {

    /**
     * 包装类型映射到基本类型
     */
    public static Map<Class<?>, Class<?>> wrapper2primitive;

    public static Map<Class<?>, Class<?>> primitive2wrapper;

    static {
        wrapper2primitive = new HashMap<>();
        wrapper2primitive.put(Short.class, short.class);
        wrapper2primitive.put(Integer.class, int.class);
        wrapper2primitive.put(Long.class, long.class);
        wrapper2primitive.put(Double.class, double.class);
        wrapper2primitive.put(Float.class, float.class);
        wrapper2primitive.put(Boolean.class, boolean.class);

        primitive2wrapper = new HashMap<>();
        primitive2wrapper.put(short.class, Short.class);
        primitive2wrapper.put(int.class, Integer.class);
        primitive2wrapper.put(long.class, Long.class);
        primitive2wrapper.put(double.class, Double.class);
        primitive2wrapper.put(float.class, Float.class);
        primitive2wrapper.put(boolean.class, Boolean.class);
    }

}
