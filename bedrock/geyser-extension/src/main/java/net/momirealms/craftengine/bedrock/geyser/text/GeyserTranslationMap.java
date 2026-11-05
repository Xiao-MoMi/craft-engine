package net.momirealms.craftengine.bedrock.geyser.text;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class GeyserTranslationMap extends AbstractMap<String, Map<String, String>> {
    private final Map<String, Map<String, String>> loaded = new ConcurrentHashMap<>();
    private final GeyserTextData data;
    private final Map<String, Map<String, String>> overrides;

    GeyserTranslationMap(Map<String, Map<String, String>> loaded,
                         GeyserTextData data,
                         Map<String, Map<String, String>> overrides) {
        this.data = data;
        this.overrides = overrides;
        this.putAll(loaded);
    }

    @Override
    public Map<String, String> put(String locale, Map<String, String> language) {
        return this.loaded.put(locale, language);
    }

    @Override
    public Map<String, String> get(Object locale) {
        Map<String, String> base = this.loaded.get(locale);
        return locale instanceof String language ? this.data.translate(language, base, this.overrides.get(locale)) : base;
    }

    @Override
    public boolean containsKey(Object locale) {
        return this.loaded.containsKey(locale);
    }

    @NotNull
    @Override
    public Set<Entry<String, Map<String, String>>> entrySet() {
        return this.loaded.entrySet();
    }
}
