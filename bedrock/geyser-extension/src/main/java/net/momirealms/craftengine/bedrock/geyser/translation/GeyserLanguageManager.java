package net.momirealms.craftengine.bedrock.geyser.translation;

import net.momirealms.craftengine.bedrock.geyser.proxy.text.MinecraftLocaleProxy;

import java.util.Map;

public final class GeyserLanguageManager {
    private Map<String, Map<String, String>> original;
    private GeyserTranslationMap installed;

    public void load(GeyserLanguageData languages) {
        Map<String, Map<String, String>> current = MinecraftLocaleProxy.INSTANCE.localeMappings();
        Map<String, Map<String, String>> base = current == this.installed ? this.original : current;
        GeyserTranslationMap replacement = new GeyserTranslationMap(base, languages);
        MinecraftLocaleProxy.INSTANCE.localeMappings(replacement);
        this.original = base;
        this.installed = replacement;
    }

    public void disable() {
        if (this.installed != null && MinecraftLocaleProxy.INSTANCE.localeMappings() == this.installed) {
            MinecraftLocaleProxy.INSTANCE.localeMappings(this.original);
        }
        this.installed = null;
        this.original = null;
    }
}