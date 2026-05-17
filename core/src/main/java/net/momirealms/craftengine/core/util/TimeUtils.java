package net.momirealms.craftengine.core.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeUtils {
    private TimeUtils() {}

    private static final Map<Character, Long> TIME_UNITS = new HashMap<>(Map.of(
            'w', 604800000L,
            'd', 86400000L,
            'h', 3600000L,
            'm', 60000L,
            's', 1000L,
            't', 50L
    ));

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([dhmsDwHMSW])", Pattern.CASE_INSENSITIVE);

    public static long parseToMillis(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Time string cannot be null or empty");
        }
        String trimmedStr = timeStr.trim();
        if (trimmedStr.matches("^\\d+$")) {
            return Long.parseLong(trimmedStr);
        }
        long totalMillis = 0;
        Matcher matcher = TIME_PATTERN.matcher(trimmedStr);
        int lastEnd = 0;
        while (matcher.find()) {
            if (matcher.start() != lastEnd) {
                throw new IllegalArgumentException("Invalid characters in time string: " +
                        trimmedStr.substring(lastEnd, matcher.start()));
            }
            lastEnd = matcher.end();
            long value = Long.parseLong(matcher.group(1));
            if (value < 0) {
                throw new IllegalArgumentException("Time value cannot be negative: " + value);
            }
            char unit = Character.toLowerCase(matcher.group(2).charAt(0));
            if (!TIME_UNITS.containsKey(unit)) {
                throw new IllegalArgumentException("Unknown time unit: " + unit);
            }
            try {
                totalMillis = Math.addExact(totalMillis, Math.multiplyExact(value, TIME_UNITS.get(unit)));
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("Time value too large, would overflow long: " + timeStr);
            }
        }

        if (lastEnd != trimmedStr.length()) {
            throw new IllegalArgumentException("Invalid time format at position " + lastEnd +
                    ": " + trimmedStr.substring(lastEnd));
        }
        if (totalMillis < 0) {
            throw new IllegalArgumentException("Resulting time cannot be negative");
        }
        return totalMillis;
    }
}
