package net.momirealms.craftengine.core.util;

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
}
