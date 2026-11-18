package net.momirealms.craftengine.core.util;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

public final class StringUtils {
    private StringUtils() {}

    public static boolean isAsciiIdentifier(String value) {
        int length = value.length();
        if (length == 0) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            char c = value.charAt(i);
            boolean allowed = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-';
            if (!allowed) {
                return false;
            }
        }
        return true;
    }

    public static String capitalize(String str, Locale locale) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        String[] words = str.split("[_ ]", -1);
        StringBuilder builder = new StringBuilder(str.length());
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                builder.append(' ');
            }
            String word = words[i];
            if (!word.isEmpty()) {
                builder.append(String.valueOf(word.charAt(0)).toUpperCase(locale));
                if (word.length() > 1) {
                    builder.append(word.substring(1));
                }
            }
        }
        return builder.toString();
    }

    public static String[] splitByDot(String s) {
        return split(s, '.');
    }

    public static String[] split(String s, char delimiter) {
        if (s == null || s.isEmpty()) {
            return new String[0];
        }
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == delimiter) {
                count++;
            }
        }
        String[] result = new String[count + 1];
        int start = 0;
        int index = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == delimiter) {
                result[index++] = s.substring(start, i);
                start = i + 1;
            }
        }
        result[index] = s.substring(start);
        return result;
    }

    public static String[] split(String s, char delimiter, int limit) {
        if (limit <= 0) {
            return split(s, delimiter);
        }
        if (s == null || s.isEmpty()) {
            return new String[0];
        }
        String[] result = new String[limit];
        int start = 0;
        int index = 0;
        for (int i = 0; i < s.length() && index < limit - 1; i++) {
            if (s.charAt(i) == delimiter) {
                result[index++] = s.substring(start, i);
                start = i + 1;
            }
        }
        result[index] = s.substring(start);
        return index == limit - 1 ? result : Arrays.copyOf(result, index + 1);
    }

    public static String toLowerCase(String str) {
        if (str == null) {
            return null;
        }
        char[] chars = str.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c >= 'A' && c <= 'Z') {
                chars[i] = (char) (c + 32);
            }
        }
        return new String(chars);
    }

    public static String fromBytes(byte[] bytes, int index) {
        byte[] decodedBytes = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            decodedBytes[i] = (byte) (bytes[i] ^ ((byte) index));
        }
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }

    public static String normalizeString(String str) {
        if (str == null) {
            return null;
        }
        char[] chars = str.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == ':') {
                chars[i] = '_';
            } else {
                if (c >= 'A' && c <= 'Z') {
                    chars[i] = (char) (c + 32);
                }
            }
        }
        return new String(chars);
    }

    public static String normalizeSettingsType(String key) {
        if (key == null) return null;
        int len = key.length();
        if (len == 0) return key;

        // 提前扫描确定是否需要处理
        boolean hasHash = false;
        boolean hasDash = false;
        int hashPos = -1;

        for (int i = 0; i < len; i++) {
            char c = key.charAt(i);
            if (c == '#') {
                hasHash = true;
                hashPos = i;
                break;
            } else if (c == '-') {
                hasDash = true;
            }
        }

        // 情况1：无需任何处理
        if (!hasHash && !hasDash) {
            return key;
        }

        // 情况2：只有替换，没有截断
        if (!hasHash) {
            char[] chars = key.toCharArray();
            for (int i = 0; i < len; i++) {
                if (chars[i] == '-') {
                    chars[i] = '_';
                }
            }
            return new String(chars);
        }

        // 情况3：需要截断（可能有替换）
        int newLen = hashPos;
        char[] result = new char[newLen];

        // 只需要复制到 hashPos 位置
        for (int i = 0; i < newLen; i++) {
            char c = key.charAt(i);
            result[i] = (c == '-') ? '_' : c;
        }

        return new String(result);
    }
}
