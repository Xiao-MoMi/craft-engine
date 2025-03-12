package net.momirealms.craftengine.core.util;

public class FormatUtils {

    private FormatUtils() {}

    public static String miniMessageFont(String raw, String font) {
        return "<font:" + font + ">" + raw + "</font>";
    }

    public static String mineDownFont(String raw, String font) {
        return "[" + raw + "](font=" + font + ")";
    }
}
