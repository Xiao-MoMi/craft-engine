package net.momirealms.craftengine.core.pack.bedrock.font;

import com.google.gson.stream.JsonWriter;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.momirealms.craftengine.bedrock.text.FontData;
import net.momirealms.craftengine.bedrock.text.FontGlyph;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BedrockFontGenerator {
    private static final int SPACER_PAGE = ContainerTitleLayout.SPACER_FIRST >>> 8;

    private BedrockFontGenerator() {
    }

    public static void write(JavaResourcePack resources, Path root) throws IOException {
        write(resources, root, root.resolve(BedrockPackLayout.FONTS));
    }

    public static void write(JavaResourcePack resources, Path root, Path fontFile) throws IOException {
        JavaFontResolver fonts = new JavaFontResolver(resources, List.of());
        JavaFontResolver rendering = fonts.excluding(Config.bedrockExcludedFonts());
        write(fonts, rendering, root, fontFile);
    }

    static void write(JavaFontResolver fonts, JavaFontResolver rendering, Path root, Path fontFile) throws IOException {
        // 所有字体共用的位图页和码位
        GlyphAtlas atlas = new GlyphAtlas();

        Map<String, FontData.Font> entries = new LinkedHashMap<>();
        for (Key font : fonts.fonts()) {
            Int2ObjectMap<Glyph> glyphs = fonts.resolve(font);
            if (glyphs.isEmpty()) {
                continue;
            }
            Int2ObjectMap<FontGlyph> allocated = atlas.allocateFont(font, rendering == fonts ? glyphs : rendering.resolve(font));
            entries.put(font.asString(), FontData.Font.of(glyphs, Glyph::advance, Glyph::boldOffset, allocated == null ? Int2ObjectMaps.emptyMap() : allocated));
        }
        Path directory = Files.createDirectories(root.resolve("font"));
        atlas.writePages(directory);
        copySpacerPage(directory);
        Files.createDirectories(fontFile.getParent());
        try (JsonWriter writer = new JsonWriter(Files.newBufferedWriter(fontFile))) {
            new FontData(entries).write(writer);
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
