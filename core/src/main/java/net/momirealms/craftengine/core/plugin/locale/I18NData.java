package net.momirealms.craftengine.core.plugin.locale;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class I18NData {
    public final Map<String, String> translations = new HashMap<>();

    public void addTranslations(Map<String, Object> data) {
        data.forEach((key, value) -> translations.put(key, value.toString()));
    }

    public void addTranslation(String key, String value) {
        this.translations.put(key, value);
    }

    @Nullable
    public String translate(String key) {
        return this.translations.get(key);
    }
}