package net.momirealms.craftengine.core.util;

import java.util.Locale;
import java.util.StringJoiner;

public final class EnumUtils {
    private EnumUtils() {}

    public static String toString(Enum<?>[] enums) {
        StringJoiner joiner = new StringJoiner(", ");
        for (Enum<?> e : enums) {
            joiner.add(e.name());
        }
        return joiner.toString();
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
}
