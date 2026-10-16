package net.momirealms.craftengine.bedrock;

import java.util.Map;
import java.util.regex.Pattern;

public record BedrockPackLanguages(Map<String, Map<String, String>> locales, Map<String, String> itemNames) {
    public static final String PATH = "craftengine/languages.json";
    public static final BedrockPackLanguages EMPTY = new BedrockPackLanguages(Map.of(), Map.of());
    private static final Pattern LOCALE = Pattern.compile("[a-z]{2,3}_[a-z0-9]{2,3}");

    public static boolean isLocale(String locale) {
        return LOCALE.matcher(locale).matches();
    }
}
