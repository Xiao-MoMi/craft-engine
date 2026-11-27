package net.momirealms.craftengine.bedrock.geyser.glyph;

import net.momirealms.craftengine.bedrock.geyser.proxy.text.MessageTranslatorProxy;
import net.momirealms.craftengine.bedrock.geyser.util.GeyserAdventureHelper;

public final class GeyserGlyphManager {
    private GeyserGlyphData glyphs;
    private Object originalSerializer;
    private Object installedSerializer;

    public GeyserGlyphManager() {
        GeyserAdventureHelper.disableLegacyFormattingWarning();
    }

    // 替换掉 geyser 的旧版文本序列化器
    public void load(GeyserGlyphData glyphs) {
        this.disable();
        this.glyphs = glyphs;
        if (!glyphs.replacements().isEmpty()) {
            this.originalSerializer = MessageTranslatorProxy.INSTANCE.bedrockSerializer();
            this.installedSerializer = new GeyserGlyphSerializer(this.originalSerializer, glyphs.replacements()).wrap();
            MessageTranslatorProxy.INSTANCE.bedrockSerializer(this.installedSerializer);
        }
    }

    public GeyserGlyphData glyphs() {
        return this.glyphs;
    }

    public void disable() {
        if (this.installedSerializer != null && MessageTranslatorProxy.INSTANCE.bedrockSerializer() == this.installedSerializer) {
            MessageTranslatorProxy.INSTANCE.bedrockSerializer(this.originalSerializer);
        }
        this.installedSerializer = null;
        this.originalSerializer = null;
    }
}
