package net.momirealms.craftengine.bedrock.geyser.text;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;
import net.momirealms.craftengine.bedrock.text.FontGlyph;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class GeyserGlyphs {

    private GeyserGlyphs() {
    }

    public static GeyserTextData read(List<Path> packs) throws IOException {
        Map<String, Map<Integer, Integer>> fonts = new HashMap<>();
        Map<String, Map<Integer, FontGlyph>> metrics = new HashMap<>();
        for (Path pack : packs) {
            try (ZipFile archive = new ZipFile(pack.toFile())) {
                ZipEntry file = archive.getEntry(BedrockPackLayout.GLYPHS);
                if (file == null) {
                    continue;
                }
                try (Reader reader = new InputStreamReader(archive.getInputStream(file), StandardCharsets.UTF_8)) {
                    readMappings(JsonParser.parseReader(reader).getAsJsonObject(), fonts, metrics);
                }
            }
        }
        return new GeyserTextData(fonts, metrics.isEmpty() ? null : new ContainerTitleLayout(metrics),
                GeyserTranslations.read(packs));
    }

    private static void readMappings(JsonObject root, Map<String, Map<Integer, Integer>> fonts,
                                     Map<String, Map<Integer, FontGlyph>> metrics) throws IOException {
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
                // 图集字形有一像素的左边距。即使分配到原码位，也要覆盖低优先级包中的替换。
                if (glyph.visible() && (glyph.character() != codepoint || glyph.bearing() == 1)) {
                    fonts.computeIfAbsent(font.getKey(), key -> new HashMap<>()).put(codepoint, glyph.character());
                }
            }
        }
    }
}
