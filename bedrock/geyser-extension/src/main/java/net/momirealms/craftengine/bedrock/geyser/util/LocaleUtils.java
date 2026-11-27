package net.momirealms.craftengine.bedrock.geyser.util;

import java.util.Locale;

public final class LocaleUtils {

    private LocaleUtils() {
    }

    public static String normalizeLocale(String locale) {
        String normalized = locale.toLowerCase(Locale.ROOT);
        return normalized.equals("no_no") ? "nb_no" : normalized;
    }
}
