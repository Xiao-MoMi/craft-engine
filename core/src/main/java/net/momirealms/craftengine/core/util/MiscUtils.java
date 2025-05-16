package net.momirealms.craftengine.core.util;

import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MiscUtils {

    private MiscUtils() {}

    @SuppressWarnings("unchecked")
    public static Map<String, Object> castToMap(Object obj, boolean allowNull) {
        if (allowNull && obj == null) {
            return null;
        }
        if (obj instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new IllegalArgumentException("Expected Map, got: " + (obj == null ? null : obj.getClass().getSimpleName()));
    }

    public static List<String> getAsStringList(Object o) {
        List<String> list = new ArrayList<>();
        if (o instanceof List<?>) {
            for (Object object : (List<?>) o) {
                list.add(object.toString());
            }
        } else if (o instanceof String) {
            list.add((String) o);
        } else {
            if (o != null) {
                list.add(o.toString());
            }
        }
        return list;
    }

    public static Vector3f getAsVector3f(Object o, String option) {
        if (o == null) return new Vector3f();
        if (o instanceof List<?> list && list.size() == 3) {
            return new Vector3f(Float.parseFloat(list.get(0).toString()), Float.parseFloat(list.get(1).toString()), Float.parseFloat(list.get(2).toString()));
        } else {
            String stringFormat = o.toString();
            String[] split = stringFormat.split(",");
            if (split.length == 3) {
                return new Vector3f(Float.parseFloat(split[0]), Float.parseFloat(split[1]), Float.parseFloat(split[2]));
            } else if (split.length == 1) {
                return new Vector3f(Float.parseFloat(split[0]));
            } else {
                throw new LocalizedResourceConfigException("warning.config.type.vector3f", stringFormat, option);
            }
        }
    }

    public static Quaternionf getAsQuaternionf(Object o, String option) {
        if (o == null) return new Quaternionf();
        if (o instanceof List<?> list && list.size() == 4) {
            return new Quaternionf(Float.parseFloat(list.get(0).toString()), Float.parseFloat(list.get(1).toString()), Float.parseFloat(list.get(2).toString()), Float.parseFloat(list.get(3).toString()));
        } else {
            String stringFormat = o.toString();
            String[] split = stringFormat.split(",");
            if (split.length == 4) {
                return new Quaternionf(Float.parseFloat(split[0]), Float.parseFloat(split[1]), Float.parseFloat(split[2]), Float.parseFloat(split[3]));
            } else if (split.length == 1) {
                return QuaternionUtils.toQuaternionf(0, Math.toRadians(Float.parseFloat(split[0])), 0);
            } else {
                throw new LocalizedResourceConfigException("warning.config.type.quaternionf", stringFormat, option);
            }
        }
    }
}
