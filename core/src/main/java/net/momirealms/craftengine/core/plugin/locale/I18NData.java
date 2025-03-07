package net.momirealms.craftengine.core.plugin.locale;

import java.util.HashMap;
import java.util.Map;

public class I18NData {
    public final Map<String, String> translations = new HashMap<>();

    public void addTranslation(String key, String value) {
        this.translations.put(key, value);
    }

    public String translate(String key) {
        return this.translations.getOrDefault(key, key);
    }
}