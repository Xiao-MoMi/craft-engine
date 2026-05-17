package net.momirealms.craftengine.core.plugin.locale;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ServerLangData {
    private final Map<Locale, String> translations = new HashMap<>();
    private final String fallback;

    public ServerLangData(String fallback) {
        this.fallback = fallback;
    }

    public ServerLangData() {
        this.fallback = null;
    }

    public void addTranslation(final Locale locale, final String translation) {
        this.translations.putIfAbsent(locale, translation);
    }

    public String translate(final Locale locale) {
        String translation = this.translations.get(locale);
        if (translation == null) {
            translation = this.translations.get(Locale.of(locale.getLanguage()));
            if (translation == null) {
                translation = this.fallback;
            }
        }
        return translation;
    }
}
