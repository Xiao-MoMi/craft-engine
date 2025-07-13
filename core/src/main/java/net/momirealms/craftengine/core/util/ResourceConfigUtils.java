package net.momirealms.craftengine.core.util;

import com.mojang.datafixers.util.Either;
import net.momirealms.craftengine.core.plugin.locale.LocalizedException;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ResourceConfigUtils {

    private ResourceConfigUtils() {}

    public static <T, O> T getOrDefault(@Nullable O raw, Function<O, T> function, T defaultValue) {
        return raw != null ? function.apply(raw) : defaultValue;
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

    public static Object get(Map<String, Object> arguments, String... keys) {
        for (String key : keys) {
            Object value = arguments.get(key);
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
                    return Integer.parseInt(s);
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
}
