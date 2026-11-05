package net.momirealms.craftengine.bedrock.geyser.text;

import net.momirealms.craftengine.bedrock.geyser.proxy.text.MessageTranslatorProxy;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.MinecraftLocaleProxy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public final class GeyserTextManager {
    private GeyserTextData data;
    private Object originalSerializer;
    private Map<String, Map<String, String>> originalTranslations;

    public void init() {
        GeyserAdventureHelper.disableLegacyFormattingWarning();
    }

    public void load(GeyserTextData data, Path configDirectory) throws IOException {
        this.data = data;
        this.originalTranslations = MinecraftLocaleProxy.INSTANCE.localeMappings();
        GeyserTranslations.install(data, configDirectory);
        this.originalSerializer = MessageTranslatorProxy.INSTANCE.bedrockSerializer();
        MessageTranslatorProxy.INSTANCE.bedrockSerializer(new GeyserGlyphSerializer(this.originalSerializer, data.fonts()).wrap());
    }

    public GeyserTextData data() {
        return this.data;
    }

    public void disable() {
        if (this.originalSerializer != null) {
            MessageTranslatorProxy.INSTANCE.bedrockSerializer(this.originalSerializer);
        }
        if (this.originalTranslations != null) {
            MinecraftLocaleProxy.INSTANCE.localeMappings(this.originalTranslations);
        }
    }
}
