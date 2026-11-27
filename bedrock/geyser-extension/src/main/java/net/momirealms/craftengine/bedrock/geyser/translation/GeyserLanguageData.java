package net.momirealms.craftengine.bedrock.geyser.translation;

import java.util.Map;

public final class GeyserLanguageData {
    public static final GeyserLanguageData EMPTY = new GeyserLanguageData(Map.of());
    private final Map<String, Map<String, String>> translations;

    public GeyserLanguageData(Map<String, Map<String, String>> translations) {
        this.translations = translations;
    }

    public Map<String, Map<String, String>> translations() {
        return this.translations;
    }
}