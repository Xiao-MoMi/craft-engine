package net.momirealms.craftengine.bedrock.geyser.translation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.geyser.util.LocaleUtils;

import java.util.HashMap;
import java.util.Map;

public final class GeyserTranslations {

    private GeyserTranslations() {
    }

    public static GeyserLanguageData parse(JsonObject json) {
        if (json == null) {
            return GeyserLanguageData.EMPTY;
        }
        Map<String, Map<String, String>> translations = new HashMap<>();
        for (Map.Entry<String, JsonElement> language : json.entrySet()) {
            translations.computeIfAbsent(LocaleUtils.normalizeLocale(language.getKey()), locale -> new HashMap<>()).putAll(readLanguage(language.getValue().getAsJsonObject()));
        }
        return new GeyserLanguageData(translations);
    }

    private static Map<String, String> readLanguage(JsonObject json) {
        Map<String, String> translations = new HashMap<>(json.size());
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            translations.put(entry.getKey(), entry.getValue().getAsString());
        }
        return translations;
    }
}