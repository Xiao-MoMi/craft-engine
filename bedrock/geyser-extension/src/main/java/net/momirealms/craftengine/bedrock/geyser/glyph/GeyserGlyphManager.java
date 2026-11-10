package net.momirealms.craftengine.bedrock.geyser.glyph;

import net.momirealms.craftengine.bedrock.geyser.proxy.text.MessageTranslatorProxy;
import net.momirealms.craftengine.bedrock.geyser.util.GeyserAdventureHelper;

public final class GeyserGlyphManager {
    private GeyserGlyphData glyphs;
    private Object originalSerializer;

    public GeyserGlyphManager() {
        GeyserAdventureHelper.disableLegacyFormattingWarning();
    }

    // 替换掉 geyser 的旧版文本序列化器
    public void load(GeyserGlyphData glyphs) {
        this.glyphs = glyphs;
        this.originalSerializer = MessageTranslatorProxy.INSTANCE.bedrockSerializer();
        MessageTranslatorProxy.INSTANCE.bedrockSerializer(new GeyserGlyphSerializer(this.originalSerializer, glyphs.fonts()).wrap());
    }

    public GeyserGlyphData glyphs() {
        return this.glyphs;
    }

    public void disable() {
        if (this.originalSerializer != null) {
            MessageTranslatorProxy.INSTANCE.bedrockSerializer(this.originalSerializer);
        }
    }
}