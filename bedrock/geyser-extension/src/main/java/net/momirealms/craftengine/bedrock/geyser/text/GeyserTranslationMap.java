package net.momirealms.craftengine.bedrock.geyser.text;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

final class GeyserTranslationMap extends AbstractMap<String, Map<String, String>> {
    private final Map<String, Map<String, String>> loaded = new ConcurrentHashMap<>();
    private final Supplier<GeyserTextData> source;
    private final Map<String, Map<String, String>> overrides;

    GeyserTranslationMap(Map<String, Map<String, String>> loaded,
                         Supplier<GeyserTextData> source,
                         Map<String, Map<String, String>> overrides) {
        this.source = source;
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
        return locale instanceof String language ? this.source.get().translate(language, base, this.overrides.get(locale)) : base;
    }

    @Override
    public boolean containsKey(Object locale) {
        return this.loaded.containsKey(locale);
    }

    @Override
    public Set<Entry<String, Map<String, String>>> entrySet() {
        return this.loaded.entrySet();
    }
}
