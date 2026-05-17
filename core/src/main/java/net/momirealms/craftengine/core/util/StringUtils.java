package net.momirealms.craftengine.core.util;

import java.nio.charset.StandardCharsets;

public final class StringUtils {
    private StringUtils() {}

    public static String[] splitByDot(String s) {
        if (s == null || s.isEmpty()) {
            return new String[0];
        }
        int dotCount = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '.') {
                dotCount++;
            }
        }
        String[] result = new String[dotCount + 1];
        int start = 0;
        int index = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '.') {
                result[index++] = s.substring(start, i);
                start = i + 1;
            }
        }
        result[index] = s.substring(start);
        return result;
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
