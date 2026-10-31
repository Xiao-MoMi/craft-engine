package net.momirealms.craftengine.bedrock.geyser.text;

import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GeyserTextData {
    public static final GeyserTextData EMPTY = new GeyserTextData(Map.of(), null, Map.of());
    private final Map<String, Map<Integer, Integer>> fonts;
    private final ContainerTitleLayout layout;
    private final Map<String, Map<String, String>> translations;
    private final Map<String, Language> languages = new ConcurrentHashMap<>();

    public GeyserTextData(Map<String, Map<Integer, Integer>> fonts,
                          ContainerTitleLayout layout,
                          Map<String, Map<String, String>> translations) {
        this.fonts = fonts;
        this.layout = layout;
        this.translations = translations;
    }

    public Map<String, Map<Integer, Integer>> fonts() {
        return this.fonts;
    }

    public ContainerTitleLayout layout() {
        return this.layout;
    }

    Map<String, String> translate(String locale, Map<String, String> base, Map<String, String> overrides) {
        Map<String, String> custom = this.translations.get(locale);
        if (base == null || custom == null) {
            return base;
        }
        Language language = this.languages.compute(locale, (key, previous) -> {
            if (previous != null && previous.base == base) {
                return previous;
            }
            Map<String, String> merged = new HashMap<>(base);
            merged.putAll(custom);
            if (overrides != null) {
                merged.putAll(overrides);
            }
            return new Language(base, merged);
        });
        return language.merged;
    }

    private record Language(Map<String, String> base, Map<String, String> merged) {
    }
}
