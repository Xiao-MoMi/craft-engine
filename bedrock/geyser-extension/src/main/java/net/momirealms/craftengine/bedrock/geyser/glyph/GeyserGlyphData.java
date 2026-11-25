package net.momirealms.craftengine.bedrock.geyser.glyph;

import it.unimi.dsi.fastutil.ints.*;
import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;
import net.momirealms.craftengine.bedrock.text.FontData;
import net.momirealms.craftengine.bedrock.text.FontGlyph;

import java.util.HashMap;
import java.util.Map;

public final class GeyserGlyphData {
    public static final GeyserGlyphData EMPTY = new GeyserGlyphData(FontData.EMPTY);
    private final FontData data;
    private final Map<String, Int2IntMap> replacements = new HashMap<>();
    private final ContainerTitleLayout layout;

    public GeyserGlyphData(FontData data) {
        this.data = data;
        boolean hasRendering = false;
        for (Map.Entry<String, FontData.Font> font : data.fonts().entrySet()) {
            Int2ObjectMap<FontGlyph> glyphs = font.getValue().rendering();
            if (glyphs.isEmpty()) {
                continue;
            }
            hasRendering = true;
            for (Int2ObjectMap.Entry<FontGlyph> entry : Int2ObjectMaps.fastIterable(glyphs)) {
                int code = entry.getIntKey();
                FontGlyph glyph = entry.getValue();
                if (glyph.character() != code && (glyph.visible() || font.getValue().advance(code, false) < 0)) {
                    this.replacements.computeIfAbsent(font.getKey(), key -> new Int2IntOpenHashMap()).put(code, glyph.character());
                }
            }
        }
        this.layout = hasRendering ? new ContainerTitleLayout(data) : null;
    }

    public Map<String, Int2IntMap> replacements() {
        return this.replacements;
    }

    public ContainerTitleLayout layout() {
        return this.layout;
    }

    public FontData.Font font(String id) {
        return this.data.font(id);
    }
}
