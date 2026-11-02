package net.momirealms.craftengine.core.pack.bedrock.font;

import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrays;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 生成基岩字形图集，并将字符映射和容器标题的布局度量写入 {@code craftengine/glyphs.json}。
 * {@code metrics} 先按字体 ID，再按 Java 字符码位的小写十六进制字符串索引。
 * 普通文本从字形的 {@code character} 读取替换码位；容器标题还使用同一条记录里的布局度量。
 * <pre>{@code
 * {
 *   "metrics": {
 *     "test:ref": {
 *       "21": { "character": -1,    "advance": -2.0, "rendered_advance": 0, "bearing": 0 },
 *       "4e": { "character": 78,    "advance":  9.0, "rendered_advance": 9, "bearing": 0 },
 *       "5a": { "character": 58112, "advance":  8.0, "rendered_advance": 8, "bearing": 1 }
 *     }
 *   }
 * }
 * }</pre>
 * {@code character} 是基岩输出的码位，-1 表示只移动光标。{@code advance} 是 Java 布局的光标位移，
 * {@code rendered_advance} 是基岩绘制字符后的前进宽度，{@code bearing} 是从绘制起点到字形左缘的水平偏移。
 *
 * <p>以上例中的普通字重标题 {@code Z!N} 为例：Z 映射到 U+E300，光标前进 8 像素；
 * ! 不绘制，光标回退 2 像素；N 沿用原生码位，从距起点 6 像素处开始绘制。</p>
 */
public final class BedrockFontGenerator {
    private static final int SPACER_PAGE = ContainerTitleLayout.SPACER_FIRST >>> 8;

    private BedrockFontGenerator() {
    }

    public static void write(JavaResourcePack resources, Path root) throws IOException {
        JavaFontResolver fonts = new JavaFontResolver(resources);
        // 所有字体共用的位图页和码位
        GlyphAtlas atlas = new GlyphAtlas();

        JsonObject layouts = new JsonObject();
        for (Key font : fonts.fonts()) {
            addFont(font, fonts.resolve(font), atlas, layouts);
        }

        Path directory = Files.createDirectories(root.resolve("font"));
        atlas.writePages(directory);
        copySpacerPage(directory);

        // 写入扩展所需数据
        JsonObject result = new JsonObject();
        result.add("metrics", layouts);
        GsonHelper.writeJsonFile(result, root.resolve(BedrockPackLayout.GLYPHS));
    }

    private static void addFont(Key font, Int2ObjectMap<Glyph> glyphs, GlyphAtlas atlas, JsonObject layouts) {
        int[] codepoints = glyphs.keySet().toIntArray();
        IntArrays.quickSort(codepoints);

        // 先检查整套字体所需的槽位。容量不足时整套跳过，以免生成一半映射后才用完码位。
        if (!atlas.canFitFont(font, codepoints, glyphs)) {
            TranslationManager.warn("resource_pack.bedrock.font.capacity", font.asString());
            return;
        }

        JsonObject layout = new JsonObject();
        for (int codepoint : codepoints) {
            Glyph glyph = glyphs.get(codepoint);
            if (glyph == null) {
                continue;
            }
            String key = Integer.toHexString(codepoint);
            if (glyph.nativeText()) {
                // 基岩原生字符沿用原码位，只记录标题布局所需的字宽。
                float advance = glyph.advance();
                layout.add(key, layoutGlyph(codepoint, advance, (int) advance, 0));
            } else if (glyph.advanceOnly()) {
                // 普通空格可直接显示；其余间距只移动布局光标，-1 表示不输出字符。
                float advance = glyph.advance();
                boolean ordinarySpace = codepoint == ' ' && advance == 4;
                layout.add(key, layoutGlyph(ordinarySpace ? ' ' : -1, advance, ordinarySpace ? 4 : 0, 0));
            } else {
                GlyphMetrics metrics = atlas.metricsFor(glyph);
                if (metrics != null) {
                    int target = atlas.codepointFor(glyph, metrics);
                    layout.add(key, layoutGlyph(target, glyph.advance(), metrics.advance(), 1));
                }
            }
        }
        if (!layout.isEmpty()) {
            layouts.add(font.asString(), layout);
        }
    }

    private static JsonObject layoutGlyph(int character, float advance, int renderedAdvance, int bearing) {
        JsonObject glyph = new JsonObject();
        glyph.addProperty("character", character);
        glyph.addProperty("advance", advance);
        if (renderedAdvance >= 0) {
            glyph.addProperty("rendered_advance", renderedAdvance);
        }
        glyph.addProperty("bearing", bearing);
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
