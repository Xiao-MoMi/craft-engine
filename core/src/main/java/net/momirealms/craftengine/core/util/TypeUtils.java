package net.momirealms.craftengine.core.util;

import java.util.Arrays;
import java.util.Locale;

public class TypeUtils {

    private TypeUtils() {}

    /**
     * Checks if the provided object is of the specified type.
     * If not, throws an IllegalArgumentException with a detailed message.
     *
     * @param object The object to check.
     * @param expectedType The expected class type.
     * @param <T> The type parameter for expectedType.
     * @return The object cast to the expected type if it matches.
     * @throws IllegalArgumentException if the object's type does not match the expected type.
     */
    public static <T> T checkType(Object object, Class<T> expectedType) {
        if (!expectedType.isInstance(object)) {
            throw new IllegalArgumentException("Expected type: " + expectedType.getName() +
                    ", but got: " + (object == null ? "null" : object.getClass().getName()));
        }
        return expectedType.cast(object);
    }

    public static Object castBasicTypes(String value, String type) {
        return switch (type.toLowerCase(Locale.ENGLISH)) {
            case "boolean" -> Boolean.parseBoolean(value);
            case "byte" -> Byte.parseByte(value);
            case "short" -> Short.parseShort(value);
            case "int", "integer" -> Integer.parseInt(value);
            case "long" -> Long.parseLong(value);
            case "float" -> Float.parseFloat(value);
            case "double" -> Double.parseDouble(value);
            case "char" -> value.charAt(0);
            case "string" -> value;
            case "intarray" -> {
                String[] split = splitArrayValue(value);
                yield Arrays.stream(split).mapToInt(Integer::parseInt).toArray();
            }
            case "bytearray" -> {
                String[] split = splitArrayValue(value);
                byte[] bytes = new byte[split.length];
                for (int i = 0; i < split.length; i++){
                    bytes[i] = Byte.parseByte(split[i]);
                }
                yield bytes;
            }
            default -> throw new IllegalStateException("Unsupported type: " + type.toLowerCase(Locale.ENGLISH));
        };
    }

    private static String[] splitArrayValue(String value) {
        return value.substring(value.indexOf('[') + 1, value.lastIndexOf(']'))
                .replaceAll("\\s", "")
                .split(",");
    }
}