package net.momirealms.craftengine.core.pack.bedrock.font;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.momirealms.craftengine.bedrock.text.GuiGlyphImage;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.CollectingConfigReader;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.PngOptimizer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class GuiGlyphAssets {
    private final Path directory;
    private final Map<Glyph, String> textures = new HashMap<>();
    private final Map<String, Int2ObjectMap<String>> fonts = new LinkedHashMap<>();

    GuiGlyphAssets(Path root) {
        this.directory = root.resolve(GuiGlyphImage.DIRECTORY);
    }

    public static Map<Key, Set<Integer>> selection(CollectingConfigReader config, String path) {
        Map<Key, Set<Integer>> selected = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : config.getSection(path).entrySet()) {
            String source = path + "." + entry.getKey();
            Key font = config.attempt(source, () -> ConfigValue.of(source, entry.getKey()).getAsIdentifier(), null);
            List<Integer> codes = config.getList(ConfigValue.of(source, entry.getValue()), value -> {
                String text = CollectingConfigReader.string(value);
                if (text.startsWith("U+") && text.length() >= 3 && text.length() <= 8) {
                    int code = Integer.parseUnsignedInt(text.substring(2), 16);
                    if (Character.isValidCodePoint(code) && (code < Character.MIN_SURROGATE || code > Character.MAX_SURROGATE)) {
                        return code;
                    }
                }
                throw new IllegalArgumentException("Expected a Unicode scalar such as U+E001 in " + value.path());
            });
            if (font != null && !codes.isEmpty()) {
                selected.put(font, Collections.unmodifiableSet(new TreeSet<>(codes)));
            }
        }
        return Map.copyOf(selected);
    }

    void write(Key font, Set<Integer> selected, Int2ObjectMap<Glyph> glyphs) throws IOException {
        if (selected.isEmpty()) {
            return;
        }
        Files.createDirectories(this.directory);
        Int2ObjectMap<String> images = new Int2ObjectLinkedOpenHashMap<>();
        for (int code : selected) {
            Glyph glyph = glyphs.get(code);
            if (glyph == null || glyph.image() == null || glyph.advanceOnly()) {
                throw new IOException("GUI-only glyph " + font + "/U+" + Integer.toHexString(code) + " needs a visible bitmap");
            }
            String token = this.textures.get(glyph);
            if (token == null) {
                int width = (int) glyph.advance() - 1;
                String name = Integer.toString(this.textures.size(), 36);
                token = GuiGlyphImage.encode(width, glyph.height(), glyph.ascent(), name);
                int[] pixels = new int[Math.multiplyExact(width, glyph.height())];
                GlyphPage.drawGlyph(glyph, pixels, width, width, 0, 0);
                try (OutputStream output = Files.newOutputStream(this.directory.resolve(name + ".png"))) {
                    PngOptimizer.writeArgb(output, width, glyph.height(), pixels);
                }
                this.textures.put(glyph, token);
            }
            images.put(code, token);
        }
        this.fonts.put(font.asString(), images);
    }

    Map<String, Int2ObjectMap<String>> fonts() {
        return this.fonts;
    }
}
