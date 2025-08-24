package net.momirealms.craftengine.core.util;

import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

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

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getAsMapList(Object obj) {
        if (obj == null) return List.of();
        if (obj instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        } else if (obj instanceof Map<?, ?>) {
            return List.of((Map<String, Object>) obj);
        }
        throw new IllegalArgumentException("Expected MapList/Map, got: " + obj.getClass().getSimpleName());
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

    public static String[] getAsStringArray(Object o) {
        if (o instanceof List<?> list) {
            String[] array = new String[list.size()];
            for (int i = 0; i < array.length; i++) {
                array[i] = list.get(i).toString();
            }
            return array;
        } else if (o != null) {
            return new String[]{o.toString()};
        } else {
            return new String[0];
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> getAsList(Object o, Class<T> clazz) {
        if (o instanceof List<?> list) {
            if (list.isEmpty()) {
                return List.of();
            }
            if (clazz.isInstance(list.getFirst())) {
                return (List<T>) list;
            }
        }
        if (clazz.isInstance(o)) {
            return List.of((T) o);
        }
        return List.of();
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
            } else if (split.length == 3) {
                return QuaternionUtils.toQuaternionf((float) Math.toRadians(Float.parseFloat(split[2])), (float) Math.toRadians(Float.parseFloat(split[1])), (float) Math.toRadians(Float.parseFloat(split[0])));
            } else if (split.length == 1) {
                return QuaternionUtils.toQuaternionf(0, (float) -Math.toRadians(Float.parseFloat(split[0])), 0);
            } else {
                throw new LocalizedResourceConfigException("warning.config.type.quaternionf", stringFormat, option);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void deepMergeMaps(Map<String, Object> baseMap, Map<String, Object> mapToMerge) {
        for (Map.Entry<String, Object> entry : mapToMerge.entrySet()) {
            String key = entry.getKey();
            if (key.length() > 2 && key.charAt(0) == '$' && key.charAt(1) == '$') {
                Object value = entry.getValue();
                baseMap.put(key.substring(1), value);
            } else {
                Object value = entry.getValue();
                if (baseMap.containsKey(key)) {
                    Object existingValue = baseMap.get(key);
                    if (existingValue instanceof Map && value instanceof Map) {
                        Map<String, Object> existingMap = (Map<String, Object>) existingValue;
                        Map<String, Object> newMap = (Map<String, Object>) value;
                        deepMergeMaps(existingMap, newMap);
                    } else if (existingValue instanceof List && value instanceof List) {
                        List<Object> existingList = (List<Object>) existingValue;
                        List<Object> newList = (List<Object>) value;
                        existingList.addAll(newList);
                    } else {
                        baseMap.put(key, value);
                    }
                } else {
                    baseMap.put(key, value);
                }
            }
        }
    }

    public static <T> T requireNonNullIf(T o, boolean condition) {
        if (condition) {
            return Objects.requireNonNull(o);
        } else {
            return o;
        }
    }

    public static boolean matchRegex(String id, Set<String> ids, boolean regexMatch) {
        if (regexMatch) {
            for (String regex : ids) {
                if (id.matches(regex)) {
                    return true;
                }
            }
        } else {
            return ids.contains(id);
        }
        return false;
    }
}
