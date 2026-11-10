package net.momirealms.craftengine.bedrock.geyser.glyph;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;
import net.momirealms.craftengine.bedrock.text.FontGlyph;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class GeyserGlyphs {

    private GeyserGlyphs() {
    }

    public static GeyserGlyphData parse(JsonObject glyphs) throws IOException {
        Map<String, Map<Integer, FontGlyph>> metrics = new HashMap<>();
        if (glyphs != null) {
            readMetrics(glyphs, metrics);
        }
        return new GeyserGlyphData(replacements(metrics), metrics.isEmpty() ? null : new ContainerTitleLayout(metrics));
    }

    private static void readMetrics(JsonObject root, Map<String, Map<Integer, FontGlyph>> metrics) throws IOException {
        if (!(root.get("metrics") instanceof JsonObject layouts)) {
            throw new IOException("Missing glyph metrics");
        }
        for (Map.Entry<String, JsonElement> font : layouts.entrySet()) {
            Map<Integer, FontGlyph> glyphs = metrics.computeIfAbsent(font.getKey(), key -> new HashMap<>());
            for (Map.Entry<String, JsonElement> value : font.getValue().getAsJsonObject().entrySet()) {
                int codepoint = Integer.parseInt(value.getKey(), 16);
                JsonObject data = value.getValue().getAsJsonObject();
                FontGlyph glyph = new FontGlyph(
                        data.get("character").getAsInt(),
                        data.get("advance").getAsFloat(),
                        data.has("rendered_advance") ? data.get("rendered_advance").getAsInt() : -1,
                        data.get("bearing").getAsInt()
                );
                glyphs.put(codepoint, glyph);
            }
        }
    }

    private static Map<String, Int2IntMap> replacements(Map<String, Map<Integer, FontGlyph>> metrics) {
        Map<String, Int2IntMap> fonts = new HashMap<>();
        metrics.forEach((font, glyphs) -> glyphs.forEach((codepoint, glyph) -> {
            if (glyph.character() != codepoint && (glyph.visible() || glyph.advance() < 0)) {
                fonts.computeIfAbsent(font, key -> new Int2IntOpenHashMap()).put(codepoint.intValue(), glyph.character());
            }
        }));
        return fonts;
    }
}