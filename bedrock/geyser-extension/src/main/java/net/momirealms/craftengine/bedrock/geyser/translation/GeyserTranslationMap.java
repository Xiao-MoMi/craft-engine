package net.momirealms.craftengine.bedrock.geyser.translation;

import java.util.HashMap;
import java.util.Map;

final class GeyserTranslationMap extends HashMap<String, Map<String, String>> {
    private final Map<String, Map<String, String>> original;
    private final Map<String, Map<String, String>> translations;

    GeyserTranslationMap(Map<String, Map<String, String>> original, GeyserLanguageData data) {
        this.original = original;
        this.translations = data.translations();
        for (Map.Entry<String, Map<String, String>> entry : original.entrySet()) {
            super.put(entry.getKey(), this.merge(entry.getKey(), entry.getValue()));
        }
    }

    @Override
    public Map<String, String> put(String locale, Map<String, String> language) {
        Map<String, String> merged = this.merge(locale, language);
        this.original.put(locale, language);
        return super.put(locale, merged);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Map<String, String>> languages) {
        for (Map.Entry<? extends String, ? extends Map<String, String>> entry : languages.entrySet()) {
            this.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Map<String, String> remove(Object locale) {
        this.original.remove(locale);
        return super.remove(locale);
    }

    @Override
    public void clear() {
        this.original.clear();
        super.clear();
    }

    private Map<String, String> merge(String locale, Map<String, String> language) {
        Map<String, String> custom = this.translations.get(locale);
        if (custom == null || custom.isEmpty()) {
            return language;
        }
        // Merge only when a language is registered, leaving reads on HashMap's normal lookup path.
        Map<String, String> merged = new HashMap<>(language);
        merged.putAll(custom);
        return merged;
    }
}