package net.momirealms.craftengine.core.util;

import com.mojang.datafixers.util.Either;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.locale.LocalizedException;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.collision.AABB;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ResourceConfigUtils {

    private ResourceConfigUtils() {}

    public static <T, O> T getOrDefault(@Nullable O raw, Function<O, T> function, T defaultValue) {
        return raw != null ? function.apply(raw) : defaultValue;
    }

    public static String getAsStringOrNull(@Nullable Object raw) {
        if (raw == null) {
            return null;
        }
        return raw.toString();
    }

    public static <E extends Enum<E>> E getAsEnum(Object o, Class<E> clazz, E defaultValue) {
        if (o == null) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(clazz, o.toString().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    public static <T> T requireNonNullOrThrow(T obj, String node) {
        if (obj == null)
            throw new LocalizedResourceConfigException(node);
        return obj;
    }

    public static <T> T requireNonNullOrThrow(T obj, Supplier<LocalizedException> exceptionSupplier) {
        if (obj == null)
            throw exceptionSupplier.get();
        return obj;
    }

    public static String requireNonEmptyStringOrThrow(Object obj, String node) {
        Object o = requireNonNullOrThrow(obj, node);
        String s = o.toString();
        if (s.isEmpty()) throw new LocalizedResourceConfigException(node);
        return s;
    }

    public static String requireNonEmptyStringOrThrow(Object obj, Supplier<LocalizedException> exceptionSupplier) {
        Object o = requireNonNullOrThrow(obj, exceptionSupplier);
        String s = o.toString();
        if (s.isEmpty()) throw exceptionSupplier.get();
        return s;
    }

    @SuppressWarnings("unchecked")
    public static <T> Either<T, List<T>> parseConfigAsEither(Object config, Function<Map<String, Object>, T> converter) {
        if (config instanceof Map<?,?>) {
            return Either.left(converter.apply((Map<String, Object>) config));
        } else if (config instanceof List<?> list) {
            return switch (list.size()) {
                case 0 -> Either.right(Collections.emptyList());
                case 1 -> Either.left(converter.apply((Map<String, Object>) list.get(0)));
                case 2 -> Either.right(List.of(converter.apply((Map<String, Object>) list.get(0)), converter.apply((Map<String, Object>) list.get(1))));
                default -> {
                    List<T> result = new ArrayList<>(list.size());
                    for (Object o : list) {
                        result.add(converter.apply((Map<String, Object>) o));
                    }
                    yield Either.right(result);
                }
            };
        } else {
            return Either.right(Collections.emptyList());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> parseConfigAsList(Object config, Function<Map<String, Object>, T> converter) {
        if (config instanceof Map<?,?>) {
            return List.of(converter.apply((Map<String, Object>) config));
        } else if (config instanceof List<?> list) {
            return switch (list.size()) {
                case 0 -> Collections.emptyList();
                case 1 -> List.of(converter.apply((Map<String, Object>) list.get(0)));
                case 2 -> List.of(converter.apply((Map<String, Object>) list.get(0)), converter.apply((Map<String, Object>) list.get(1)));
                default -> {
                    List<T> result = new ArrayList<>(list.size());
                    for (Object o : list) {
                        result.add(converter.apply((Map<String, Object>) o));
                    }
                    yield result;
                }
            };
        } else {
            return Collections.emptyList();
        }
    }

    @SafeVarargs
    public static <K, V> V get(Map<K, V> arguments, K... keys) {
        for (K key : keys) {
            V value = arguments.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public static int getAsInt(Object o, String option) {
        switch (o) {
            case null -> {
                return 0;
            }
            case Integer i -> {
                return i;
            }
            case Number number -> {
                return number.intValue();
            }
            case String s -> {
                try {
                    return Integer.parseInt(s.replace("_", ""));
                } catch (NumberFormatException e) {
                    throw new LocalizedResourceConfigException("warning.config.type.int", e, s, option);
                }
            }
            case Boolean b -> {
                return b ? 1 : 0;
            }
            default -> throw new LocalizedResourceConfigException("warning.config.type.int", o.toString(), option);
        }
    }

    public static double getAsDouble(Object o, String option) {
        switch (o) {
            case null -> {
                return 0.0;
            }
            case Double d -> {
                return d;
            }
            case Number n -> {
                return n.doubleValue();
            }
            case String s -> {
                try {
                    return Double.parseDouble(s);
                } catch (NumberFormatException e) {
                    throw new LocalizedResourceConfigException("warning.config.type.double", e, s, option);
                }
            }
            default -> {
                throw new LocalizedResourceConfigException("warning.config.type.double", o.toString(), option);
            }
        }
    }

    public static float getAsFloat(Object o, String option) {
        switch (o) {
            case null -> {
                return 0.0f;
            }
            case Float f -> {
                return f;
            }
            case String s -> {
                try {
                    return Float.parseFloat(s);
                } catch (NumberFormatException e) {
                    throw new LocalizedResourceConfigException("warning.config.type.float", e, s, option);
                }
            }
            case Number number -> {
                return number.floatValue();
            }
            default -> {
                throw new LocalizedResourceConfigException("warning.config.type.float", o.toString(), option);
            }
        }
    }

    public static boolean getAsBoolean(Object o, String option) {
        switch (o) {
            case null -> {
                return false;
            }
            case Boolean b -> {
                return b;
            }
            case Number n -> {
                if (n.byteValue() == 0) return false;
                if (n.byteValue() == 1) return true;
                throw new LocalizedResourceConfigException("warning.config.type.boolean", String.valueOf(n), option);
            }
            case String s -> {
                if (s.equalsIgnoreCase("true")) return true;
                if (s.equalsIgnoreCase("false")) return false;
                throw new LocalizedResourceConfigException("warning.config.type.boolean", s, option);
            }
            default -> {
                throw new LocalizedResourceConfigException("warning.config.type.boolean", o.toString(), option);
            }
        }
    }

    public static long getAsLong(Object o, String option) {
        switch (o) {
            case null -> {
                return 0;
            }
            case Long l -> {
                return l;
            }
            case Number number -> {
                return number.longValue();
            }
            case String s -> {
                try {
                    return Long.parseLong(s.replace("_", ""));
                } catch (NumberFormatException e) {
                    throw new LocalizedResourceConfigException("warning.config.type.long", e, s, option);
                }
            }
            default -> {
                throw new LocalizedResourceConfigException("warning.config.type.long", o.toString(), option);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getAsMap(Object obj, String option) {
        if (obj instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new LocalizedResourceConfigException("warning.config.type.map", String.valueOf(obj), option);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getAsMapOrNull(Object obj, String option) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new LocalizedResourceConfigException("warning.config.type.map", String.valueOf(obj), option);
    }

    public static Vector3f getAsVector3f(Object o, String option) {
        switch (o) {
            case null -> {
                return new Vector3f();
            }
            case List<?> list when list.size() == 3 -> {
                return new Vector3f(Float.parseFloat(list.get(0).toString()), Float.parseFloat(list.get(1).toString()), Float.parseFloat(list.get(2).toString()));
            }
            case Number number -> {
                return new Vector3f(number.floatValue());
            }
            default -> {
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

    public static Vec3d getAsVec3d(Object o, String option) {
        if (o == null) return new Vec3d(0, 0, 0);
        if (o instanceof List<?> list && list.size() == 3) {
            return new Vec3d(Double.parseDouble(list.get(0).toString()), Double.parseDouble(list.get(1).toString()), Double.parseDouble(list.get(2).toString()));
        } else {
            String stringFormat = o.toString();
            String[] split = stringFormat.split(",");
            if (split.length == 3) {
                return new Vec3d(Double.parseDouble(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]));
            } else if (split.length == 1) {
                double d = Double.parseDouble(split[0]);
                return new Vec3d(d, d, d);
            } else {
                throw new LocalizedResourceConfigException("warning.config.type.vec3d", stringFormat, option);
            }
        }
    }

    public static boolean runCatching(Path configPath, String node, Runnable runnable, Supplier<String> config) {
        try {
            runnable.run();
            return true;
        } catch (LocalizedException e) {
            printWarningRecursively(e, configPath, node);
            return false;
        } catch (Exception e) {
            String message = "Unexpected error loading file " + configPath + " - '" + node + "'.";
            if (config != null) {
                message += " Configuration details: " + config.get();
            }
            CraftEngine.instance().logger().warn(message, e);
            return false;
        }
    }

    private static void printWarningRecursively(LocalizedException e, Path path, String node) {
        for (Throwable t : e.getSuppressed()) {
            if (t instanceof LocalizedException suppressed) {
                printWarningRecursively(suppressed, path, node);
            }
        }
        if (e instanceof LocalizedResourceConfigException exception) {
            exception.setPath(path);
            exception.setNode(node);
        }
        TranslationManager.instance().log(e.node(), e.arguments());
    }

    public static AABB getAsAABB(Object o, String option) {
        switch (o) {
            case null -> throw new LocalizedResourceConfigException("warning.config.type.aabb", "null", option);
            case AABB aabb -> {
                return aabb;
            }
            case Number number -> {
                double min = -(number.doubleValue() / 2);
                double max = number.doubleValue() / 2;
                return new AABB(min, min, min, max, max, max);
            }
            default -> {
                double[] args;
                if (o instanceof List<?> list) {
                    args = new double[list.size()];
                    for (int i = 0; i < args.length; i++) {
                        if (list.get(i) instanceof Number number) {
                            args[i] = number.doubleValue();
                        } else {
                            try {
                                args[i] = Double.parseDouble(list.get(i).toString());
                            } catch (NumberFormatException e) {
                                throw new LocalizedResourceConfigException("warning.config.type.aabb", o.toString(), option);
                            }
                        }
                    }
                } else {
                    String[] split = o.toString().split(",");
                    args = new double[split.length];
                    for (int i = 0; i < args.length; i++) {
                        try {
                            args[i] = Double.parseDouble(split[i]);
                        } catch (NumberFormatException e) {
                            throw new LocalizedResourceConfigException("warning.config.type.aabb", o.toString(), option);
                        }
                    }
                }
                if (args.length == 1) {
                    return new AABB(-args[0] / 2, -args[0] / 2, -args[0] / 2, args[0] / 2, args[0] / 2, args[0] / 2);
                } else if (args.length == 2) {
                    return new AABB(-args[0] / 2, -args[1] / 2, -args[0] / 2, args[0] / 2, args[1] / 2, args[0] / 2);
                } else if (args.length == 3) {
                    return new AABB(-args[0] / 2, -args[1] / 2, -args[2] / 2, args[0] / 2, args[1] / 2, args[2] / 2);
                } else if (args.length == 6) {
                    return new AABB(args[0], args[1], args[2], args[3], args[4], args[5]);
                } else {
                    throw new LocalizedResourceConfigException("warning.config.type.aabb", o.toString(), option);
                }
            }
        }
    }
}
