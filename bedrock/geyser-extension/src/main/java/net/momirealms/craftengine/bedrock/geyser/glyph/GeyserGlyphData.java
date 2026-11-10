package net.momirealms.craftengine.bedrock.geyser.glyph;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;

import java.util.Map;

public final class GeyserGlyphData {
    public static final GeyserGlyphData EMPTY = new GeyserGlyphData(Map.of(), null);
    private final Map<String, Int2IntMap> fonts;
    private final ContainerTitleLayout layout;

    public GeyserGlyphData(Map<String, Int2IntMap> fonts, ContainerTitleLayout layout) {
        this.fonts = fonts;
        this.layout = layout;
    }

    public Map<String, Int2IntMap> fonts() {
        return this.fonts;
    }

    public ContainerTitleLayout layout() {
        return this.layout;
    }
}