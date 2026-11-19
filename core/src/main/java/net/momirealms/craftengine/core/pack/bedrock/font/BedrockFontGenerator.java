package net.momirealms.craftengine.core.pack.bedrock.font;

import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 生成基岩字形图集，并将字符映射和容器标题的布局度量写入 {@code craftengine/glyphs.json}。
 * 根对象按字体 ID 索引，每个字体按 Java 字符码位的小写十六进制字符串索引
 * 普通文本从字形的 {@code character} 读取替换码位；容器标题还使用同一条记录里的布局度量。
 * <pre>{@code
 * {
 *   "test:ref": {
 *     "21": { "character": -1,    "advance": -2.0, "rendered_advance": 0 },
 *     "4e": {                     "advance":  9.0, "rendered_advance": 9 },
 *     "5a": { "character": 58112, "advance":  8.0, "rendered_advance": 8, "bearing": 1 }
 *   }
 * }
 * }</pre>
 * {@code character} 是基岩输出的码位，-1 表示只移动光标。{@code advance} 是 Java 布局的光标位移，
 * {@code rendered_advance} 是基岩绘制字符后的前进宽度，{@code bearing} 是从绘制起点到字形左缘的水平偏移。
 * 省略 {@code character} 时使用原码位，省略 {@code bearing} 时偏移为零
 *
 * <p>以上例中的普通字重标题 {@code Z!N} 为例：Z 映射到 U+E300，光标前进 8 像素；
 * ! 不绘制，光标回退 2 像素；N 沿用原生码位，从距起点 6 像素处开始绘制。</p>
 */
public final class BedrockFontGenerator {
    private static final int SPACER_PAGE = ContainerTitleLayout.SPACER_FIRST >>> 8;

    private BedrockFontGenerator() {
    }

    public static void write(JavaResourcePack resources, Path root) throws IOException {
        write(resources, root, root.resolve(BedrockPackLayout.GLYPHS));
    }

    public static void write(JavaResourcePack resources, Path root, Path glyphData) throws IOException {
        JavaFontResolver fonts = new JavaFontResolver(resources, Config.bedrockExcludedFonts());
        // 所有字体共用的位图页和码位
        GlyphAtlas atlas = new GlyphAtlas();

        JsonObject layouts = new JsonObject();
        for (Key font : fonts.fonts()) {
            addFont(font, fonts.resolve(font), atlas, layouts);
        }

        Path directory = Files.createDirectories(root.resolve("font"));
        atlas.writePages(directory);
        copySpacerPage(directory);

        if (!layouts.isEmpty()) {
            Files.createDirectories(glyphData.getParent());
            GsonHelper.writeJsonFile(layouts, glyphData);
        }
    }

    private static void addFont(Key font, Int2ObjectMap<Glyph> glyphs, GlyphAtlas atlas, JsonObject layouts) {
        Int2ObjectMap<GlyphAtlas.Allocation> allocations = atlas.allocateFont(font, glyphs);
        if (allocations == null) {
            return;
        }
        JsonObject layout = new JsonObject();
        for (Int2ObjectMap.Entry<GlyphAtlas.Allocation> entry : allocations.int2ObjectEntrySet()) {
            int codepoint = entry.getIntKey();
            GlyphAtlas.Allocation glyph = entry.getValue();
            layout.add(Integer.toHexString(codepoint), layoutGlyph(codepoint, glyph));
        }
        if (!layout.isEmpty()) {
            layouts.add(font.asString(), layout);
        }
    }

    private static JsonObject layoutGlyph(int codepoint, GlyphAtlas.Allocation allocation) {
        JsonObject glyph = new JsonObject();
        if (allocation.character() != codepoint) {
            glyph.addProperty("character", allocation.character());
        }
        glyph.addProperty("advance", allocation.advance());
        if (allocation.renderedAdvance() >= 0) {
            glyph.addProperty("rendered_advance", allocation.renderedAdvance());
        }
        if (allocation.bearing() != 0) {
            glyph.addProperty("bearing", allocation.bearing());
        }
        return glyph;
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
