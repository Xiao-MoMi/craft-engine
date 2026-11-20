package net.momirealms.craftengine.bedrock.geyser.glyph;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;
import net.momirealms.craftengine.bedrock.text.FontGlyph;

import java.util.Map;

public final class GeyserGlyphData {
    public static final GeyserGlyphData EMPTY = new GeyserGlyphData(Map.of(), Map.of());
    private final Map<String, Int2IntMap> fonts;
    private final Map<String, Int2ObjectMap<FontGlyph>> metrics;
    private final ContainerTitleLayout layout;

    public GeyserGlyphData(Map<String, Int2IntMap> fonts, Map<String, Int2ObjectMap<FontGlyph>> metrics) {
        this.fonts = fonts;
        this.metrics = metrics;
        this.layout = metrics.isEmpty() ? null : new ContainerTitleLayout(metrics);
    }

    public Map<String, Int2IntMap> fonts() {
        return this.fonts;
    }

    public ContainerTitleLayout layout() {
        return this.layout;
    }

    public Map<String, Int2ObjectMap<FontGlyph>> metrics() {
        return this.metrics;
    }
}
