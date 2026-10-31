package net.momirealms.craftengine.bedrock.geyser.text;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.MessageTranslatorProxy;
import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;
import net.momirealms.craftengine.bedrock.text.FontGlyph;
import org.geysermc.geyser.api.extension.ExtensionLogger;

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

    public static ContainerTitleLayout install(List<Path> packs, ExtensionLogger logger) {
        Map<String, Map<Integer, Integer>> fonts = new HashMap<>();
        Map<String, Map<Integer, FontGlyph>> metrics = new HashMap<>();
        for (Path pack : packs) {
            try (ZipFile archive = new ZipFile(pack.toFile())) {
                ZipEntry file = archive.getEntry(BedrockPackLayout.GLYPHS);
                if (file == null) {
                    continue;
                }
                try (Reader reader = new InputStreamReader(archive.getInputStream(file), StandardCharsets.UTF_8)) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    if (root.get(BedrockPackLayout.FORMAT_VERSION).getAsInt() != 1) {
                        throw new IOException("Unsupported glyph mapping format");
                    }
                    if (root.has("metrics")) {
                        for (Map.Entry<String, JsonElement> font : root.getAsJsonObject("metrics").entrySet()) {
                            Map<Integer, FontGlyph> glyphs = metrics.computeIfAbsent(font.getKey(), key -> new HashMap<>());
                            for (Map.Entry<String, JsonElement> value : font.getValue().getAsJsonObject().entrySet()) {
                                JsonObject glyph = value.getValue().getAsJsonObject();
                                glyphs.put(
                                        Integer.parseInt(value.getKey(), 16),
                                        new FontGlyph(
                                                glyph.get("character").getAsInt(),
                                                glyph.get("advance").getAsFloat(),
                                                glyph.has("rendered_advance") ? glyph.get("rendered_advance").getAsInt() : -1,
                                                glyph.get("bearing").getAsInt()
                                        )
                                );
                            }
                        }
                    }
                    for (Map.Entry<String, JsonElement> font : root.getAsJsonObject("fonts").entrySet()) {
                        Map<Integer, Integer> glyphs = fonts.computeIfAbsent(font.getKey(), key -> new HashMap<>());
                        for (Map.Entry<String, JsonElement> glyph : font.getValue().getAsJsonObject().entrySet()) {
                            glyphs.put(Integer.parseInt(glyph.getKey(), 16), glyph.getValue().getAsInt());
                        }
                    }
                }
            } catch (IOException | RuntimeException exception) {
                logger.error("Failed to read glyph mappings from " + pack, exception);
            }
        }
        if (fonts.isEmpty()) {
            return metrics.isEmpty() ? null : new ContainerTitleLayout(metrics);
        }
        try {
            MessageTranslatorProxy translator = MessageTranslatorProxy.INSTANCE;
            translator.bedrockSerializer(new GeyserGlyphSerializer(translator.bedrockSerializer(), fonts).wrap());
            logger.info("Loaded image glyph mappings for " + fonts.size() + " fonts.");
        } catch (RuntimeException | LinkageError exception) {
            logger.error("Failed to install image glyph mappings", exception);
        }
        return metrics.isEmpty() ? null : new ContainerTitleLayout(metrics);
    }
}
