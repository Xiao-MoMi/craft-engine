package net.momirealms.craftengine.bedrock.geyser.glyph;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.*;
import net.momirealms.craftengine.bedrock.text.FontGlyph;

import java.util.HashMap;
import java.util.Map;

public final class GeyserGlyphs {

    private GeyserGlyphs() {
    }

    public static GeyserGlyphData parse(JsonObject glyphs) {
        Map<String, Int2ObjectMap<FontGlyph>> metrics = new HashMap<>();
        if (glyphs != null) {
            readMetrics(glyphs, metrics);
        }
        return new GeyserGlyphData(replacements(metrics), metrics);
    }

    private static void readMetrics(JsonObject layouts, Map<String, Int2ObjectMap<FontGlyph>> metrics) {
        for (Map.Entry<String, JsonElement> font : layouts.entrySet()) {
            Int2ObjectMap<FontGlyph> glyphs = metrics.computeIfAbsent(font.getKey(), key -> new Int2ObjectOpenHashMap<>());
            for (Map.Entry<String, JsonElement> value : font.getValue().getAsJsonObject().entrySet()) {
                int codepoint = Integer.parseInt(value.getKey(), 16);
                JsonObject data = value.getValue().getAsJsonObject();
                JsonElement character = data.get("character");
                JsonElement renderedAdvance = data.get("rendered_advance");
                JsonElement bearing = data.get("bearing");
                FontGlyph glyph = new FontGlyph(
                        character == null ? codepoint : character.getAsInt(),
                        data.get("advance").getAsFloat(),
                        renderedAdvance == null ? -1 : renderedAdvance.getAsInt(),
                        bearing == null ? 0 : bearing.getAsInt()
                );
                glyphs.put(codepoint, glyph);
            }
        }
    }

    private static Map<String, Int2IntMap> replacements(Map<String, Int2ObjectMap<FontGlyph>> metrics) {
        Map<String, Int2IntMap> fonts = new HashMap<>();
        for (Map.Entry<String, Int2ObjectMap<FontGlyph>> font : metrics.entrySet()) {
            for (Int2ObjectMap.Entry<FontGlyph> entry : Int2ObjectMaps.fastIterable(font.getValue())) {
                int codepoint = entry.getIntKey();
                FontGlyph glyph = entry.getValue();
                if (glyph.character() != codepoint && (glyph.visible() || glyph.advance() < 0)) {
                    fonts.computeIfAbsent(font.getKey(), key -> new Int2IntOpenHashMap()).put(codepoint, glyph.character());
                }
            }
        }
        return fonts;
    }
}
