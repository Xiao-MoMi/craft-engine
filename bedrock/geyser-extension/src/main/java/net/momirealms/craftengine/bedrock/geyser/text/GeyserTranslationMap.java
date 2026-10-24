package net.momirealms.craftengine.bedrock.geyser.text;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class GeyserTranslationMap extends AbstractMap<String, Map<String, String>> {
    private final Map<String, Map<String, String>> loaded = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> translations;
    private final Map<String, Map<String, String>> overrides;

    GeyserTranslationMap(Map<String, Map<String, String>> loaded,
                         Map<String, Map<String, String>> translations,
                         Map<String, Map<String, String>> overrides) {
        this.translations = translations;
        this.overrides = overrides;
        this.putAll(loaded);
    }

    @Override
    public Map<String, String> put(String locale, Map<String, String> language) {
        Map<String, String> custom = this.translations.get(locale);
        if (custom == null) {
            return this.loaded.put(locale, language);
        }
        // 仅在 Geyser 加载语言后合并，避免提前插入条目导致原版词条跳过加载。
        Map<String, String> merged = new HashMap<>(language);
        merged.putAll(custom);
        Map<String, String> override = this.overrides.get(locale);
        if (override != null) {
            merged.putAll(override);
        }
        return this.loaded.put(locale, merged);
    }

    @Override
    public Map<String, String> get(Object locale) {
        return this.loaded.get(locale);
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
