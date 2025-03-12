package net.momirealms.craftengine.core.util;

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
        throw new IllegalArgumentException("Expected Map, got: " + obj.getClass().getSimpleName());
    }

    @SuppressWarnings("unchecked")
    public static List<Object> castToList(Object obj, boolean allowNull) {
        if (allowNull && obj == null) {
            return null;
        }
        if (obj instanceof List<?> list) {
            return (List<Object>) list;
        }
        throw new IllegalArgumentException("Expected List, got: " + obj.getClass().getSimpleName());
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

    public static List<Float> getAsFloatList(Object o) {
        List<Float> list = new ArrayList<>();
        if (o instanceof List<?>) {
            for (Object object : (List<?>) o) {
                if (object instanceof Number) {
                    list.add(((Number) object).floatValue());
                } else if (object instanceof String) {
                    try {
                        list.add(Float.parseFloat((String) object));
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("Cannot convert " + object + " to float");
                    }
                } else {
                    throw new RuntimeException("Cannot convert " + object + " to float");
                }
            }
        } else if (o instanceof Float) {
            list.add((Float) o);
        } else if (o instanceof String) {
            try {
                list.add(Float.parseFloat((String) o));
            } catch (NumberFormatException e) {
                throw new RuntimeException("Cannot convert " + o + " to float");
            }
        } else {
            throw new RuntimeException("Cannot convert " + o + " to float");
        }
        return list;
    }

    public static int getAsInt(Object o) {
        if (o instanceof Integer) {
            return (Integer) o;
        } else if (o instanceof String) {
            try {
                return Integer.parseInt((String) o);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Cannot convert " + o + " to int");
            }
        } else if (o instanceof Boolean) {
            return (Boolean) o ? 1 : 0;
        } else if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        throw new RuntimeException("Cannot convert " + o + " to int");
    }

    public static double getAsDouble(Object o) {
        if (o instanceof Double) {
            return (Double) o;
        } else if (o instanceof String) {
            try {
                return Double.parseDouble((String) o);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Cannot convert " + o + " to double");
            }
        } else if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }
        throw new RuntimeException("Cannot convert " + o + " to double");
    }

    public static float getAsFloat(Object o) {
        if (o instanceof Float) {
            return (Float) o;
        } else if (o instanceof String) {
            try {
                return Float.parseFloat((String) o);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Cannot convert " + o + " to float");
            }
        } else if (o instanceof Number) {
            return ((Number) o).floatValue();
        }
        throw new RuntimeException("Cannot convert " + o + " to float");
    }

    public static Vector3f getVector3f(Object o) {
        String stringFormat = o.toString();
        String[] split = stringFormat.split(",");
        if (split.length == 3) {
            return new Vector3f(Float.parseFloat(split[0]), Float.parseFloat(split[1]), Float.parseFloat(split[2]));
        } else if (split.length == 1) {
            return new Vector3f(Float.parseFloat(split[0]));
        } else {
            throw new RuntimeException("Cannot convert " + o + " to Vector3f");
        }
    }

    public static Quaternionf getQuaternionf(Object o) {
        String stringFormat = o.toString();
        String[] split = stringFormat.split(",");
        if (split.length == 4) {
            return new Quaternionf(Float.parseFloat(split[0]), Float.parseFloat(split[1]), Float.parseFloat(split[2]), Float.parseFloat(split[3]));
        } else if (split.length == 1) {
            return QuaternionUtils.toQuaternionf(0, Math.toRadians(Float.parseFloat(split[0])), 0);
        } else {
            throw new RuntimeException("Cannot convert " + o + " to Quaternionf");
        }
    }
}
