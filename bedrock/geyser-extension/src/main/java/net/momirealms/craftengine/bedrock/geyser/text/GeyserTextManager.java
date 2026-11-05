package net.momirealms.craftengine.bedrock.geyser.text;

import net.momirealms.craftengine.bedrock.geyser.pack.GeyserPackManager;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.MessageTranslatorProxy;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.MinecraftLocaleProxy;
import org.geysermc.geyser.session.GeyserSession;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public final class GeyserTextManager {
    private GeyserPackManager packManager;
    private Object originalSerializer;
    private Map<String, Map<String, String>> originalTranslations;

    public void init() {
        GeyserAdventureHelper.disableLegacyFormattingWarning();
    }

    public void load(GeyserPackManager packManager, Path configDirectory) throws IOException {
        this.packManager = packManager;
        this.originalTranslations = MinecraftLocaleProxy.INSTANCE.localeMappings();
        GeyserTranslations.install(this::data, configDirectory);
        this.originalSerializer = MessageTranslatorProxy.INSTANCE.bedrockSerializer();
        MessageTranslatorProxy.INSTANCE.bedrockSerializer(new GeyserGlyphSerializer(this.originalSerializer, () -> this.data().fonts()).wrap());
    }

    public GeyserTextData data() {
        return this.packManager.text(GeyserTextContext.session());
    }

    public void installContext() {
        GeyserTextContext.install();
    }

    public void initSession(GeyserSession session) {
        GeyserTextContext.install(session);
    }

    public void uninstallContext() {
        GeyserTextContext.uninstall();
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
