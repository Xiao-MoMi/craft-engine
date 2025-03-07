package net.momirealms.craftengine.core.plugin.locale;

import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.Plugin;
import net.momirealms.craftengine.core.util.Key;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ClientLangMangerImpl implements ClientLangManager {
    private final Plugin plugin;
    private final Map<String, I18NData> i18nData = new HashMap<>();

    public ClientLangMangerImpl(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void parseSection(Pack pack, Path path, Key id, Map<String, Object> section) {
        I18NData data = this.i18nData.computeIfAbsent(id.value(), k -> new I18NData());
        for (Map.Entry<String, Object> entry : section.entrySet()) {
            String key = entry.getKey();
            data.addTranslation(key, entry.getValue().toString());
        }
    }

    @Override
    public Map<String, I18NData> langData() {
        return Collections.unmodifiableMap(i18nData);
    }
}
