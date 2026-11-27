package net.momirealms.craftengine.core.pack.bedrock.font;

import com.google.gson.stream.JsonWriter;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;
import net.momirealms.craftengine.bedrock.text.FontData;
import net.momirealms.craftengine.bedrock.text.FontGlyph;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class BedrockFontGenerator {
    private static final int SPACER_PAGE = ContainerTitleLayout.SPACER_FIRST >>> 8;

    private BedrockFontGenerator() {
    }

    public static void write(JavaResourcePack resources, Path root) throws IOException {
        write(resources, root, root.resolve(BedrockPackLayout.FONTS));
    }

    public static void write(JavaResourcePack resources, Path root, Path fontFile) throws IOException {
        JavaFontResolver fonts = new JavaFontResolver(resources, Config.bedrockExcludedFonts());
        Map<Key, Set<Integer>> selected = GuiGlyphAssets.includeImages(fonts, Config.bedrockGuiOnlyGlyphs(), CraftEngine.instance().fontManager().bedrockUiImages());
        write(fonts, Config.bedrockFontVerticalOffsets().keySet(), selected, root, fontFile);
    }

    static void write(JavaFontResolver fonts, Set<Key> nativeFonts, Path root, Path fontFile) throws IOException {
        write(fonts, nativeFonts, Map.of(), root, fontFile);
    }

    static void write(JavaFontResolver fonts, Set<Key> nativeFonts, Map<Key, Set<Integer>> guiOnly, Path root, Path fontFile) throws IOException {
        // 所有字体共用的位图页和码位
        GlyphAtlas atlas = new GlyphAtlas();
        GuiGlyphAssets images = new GuiGlyphAssets(root);
        for (Key selected : guiOnly.keySet()) {
            if (!fonts.fonts().contains(selected)) {
                throw new IOException("Unknown GUI-only font " + selected);
            }
        }

        Map<String, FontData.Font> entries = new LinkedHashMap<>();
        for (Key font : fonts.fonts()) {
            Int2ObjectMap<Glyph> glyphs = fonts.resolve(font);
            boolean excluded = fonts.isExcluded(font);
            Set<Integer> selected = guiOnly.getOrDefault(font, Set.of());
            images.write(font, selected, glyphs);
            if (glyphs.isEmpty() && !excluded) {
                continue;
            }
            FontData.Rendering allocated = nativeFonts.contains(font) ? GlyphAtlas.nativeFont(glyphs) : atlas.allocateFont(font, glyphs, selected);
            BitSet unicode = new BitSet();
            Int2ObjectMaps.fastForEach(glyphs, entry -> {
                int code = entry.getIntKey();
                FontGlyph mapped = allocated.get(code);
                if (entry.getValue().unicode() && (mapped == null || (mapped.character(code) == code && mapped.bearing() == 0))) {
                    unicode.set(code);
                }
            });
            entries.put(font.asString(), FontData.Font.of(glyphs, Glyph::advance, Glyph::boldOffset, allocated, unicode, excluded));
        }
        Path directory = Files.createDirectories(root.resolve("font"));
        atlas.writePages(directory);
        copySpacerPage(directory);
        Files.createDirectories(fontFile.getParent());
        try (JsonWriter writer = new JsonWriter(Files.newBufferedWriter(fontFile))) {
            new FontData(entries, images.fonts()).write(writer);
        }
    }

    private static void copySpacerPage(Path directory) throws IOException {
        // 容器标题的正间距字形固定为 2–33 像素，所有资源包共用同一张 E2 页。
        String resource = "/bedrock/font/glyph_E2.png";
        try (InputStream input = BedrockFontGenerator.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Missing bundled spacer page " + resource);
            }
            Files.copy(input, GlyphPage.file(directory, SPACER_PAGE), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
