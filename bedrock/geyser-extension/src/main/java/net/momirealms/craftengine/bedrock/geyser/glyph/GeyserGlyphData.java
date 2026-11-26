package net.momirealms.craftengine.bedrock.geyser.glyph;

import it.unimi.dsi.fastutil.ints.*;
import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;
import net.momirealms.craftengine.bedrock.text.FontData;
import net.momirealms.craftengine.bedrock.text.FontGlyph;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class GeyserGlyphData {
    public static final GeyserGlyphData EMPTY = new GeyserGlyphData();
    private final FontData data;
    private final Map<String, Integer> verticalOffsets;
    private final Map<String, Int2IntMap> replacements = new HashMap<>();
    @Nullable private final ContainerTitleLayout layout;

    private GeyserGlyphData() {
        this.data = FontData.EMPTY;
        this.verticalOffsets = Map.of();
        this.layout = null;
    }

    public GeyserGlyphData(FontData data, Map<String, Integer> verticalOffsets) {
        this.data = data;
        this.verticalOffsets = verticalOffsets;
        this.layout = new ContainerTitleLayout(data);
        for (Map.Entry<String, FontData.Font> font : data.fonts().entrySet()) {
            Int2ObjectMap<FontGlyph> glyphs = font.getValue().rendering();
            for (Int2ObjectMap.Entry<FontGlyph> entry : Int2ObjectMaps.fastIterable(glyphs)) {
                int code = entry.getIntKey();
                FontGlyph glyph = entry.getValue();
                if (glyph.character() != code && (glyph.visible() || font.getValue().advance(code, false) < 0)) {
                    this.replacements.computeIfAbsent(font.getKey(), key -> new Int2IntOpenHashMap()).put(code, glyph.character());
                }
            }
        }
    }

    public Map<String, Int2IntMap> replacements() {
        return this.replacements;
    }

    @Nullable
    public ContainerTitleLayout layout() {
        return this.layout;
    }

    public FontData.Font font(String id) {
        return this.data.font(id);
    }

    public int verticalOffset(String id) {
        return this.verticalOffsets.getOrDefault(id, 0);
    }
}
