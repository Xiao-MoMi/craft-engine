package net.momirealms.craftengine.core.pack.bedrock.font;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.momirealms.craftengine.bedrock.text.GuiGlyphImage;
import net.momirealms.craftengine.core.font.BitmapImage;
import net.momirealms.craftengine.core.font.Image;
import net.momirealms.craftengine.core.font.ReferenceImage;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.PngOptimizer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

final class GuiGlyphAssets {
    private final Path directory;
    private final Map<Glyph, String> textures = new HashMap<>();
    private final Map<String, Int2ObjectMap<String>> fonts = new LinkedHashMap<>();

    GuiGlyphAssets(Path root) {
        this.directory = root.resolve(GuiGlyphImage.DIRECTORY);
    }

    static Map<Key, Set<Integer>> includeImages(JavaFontResolver fonts, Map<Key, List<GuiGlyphSelector>> configured, Map<Key, Image> images) throws IOException {
        if (configured.isEmpty() && images.isEmpty()) {
            return Map.of();
        }
        Map<Key, Set<Integer>> selected = new HashMap<>();
        for (Map.Entry<Key, List<GuiGlyphSelector>> entry : configured.entrySet()) {
            selected.put(entry.getKey(), GuiGlyphSelector.resolve(fonts, entry.getKey(), entry.getValue()));
        }
        for (Map.Entry<Key, Image> entry : images.entrySet()) {
            if (!includeImage(fonts, selected, entry.getValue())) {
                TranslationManager.warn("resource_pack.bedrock.font.bitmap", entry.getKey().asString());
            }
        }
        return selected;
    }

    private static boolean includeImage(JavaFontResolver fonts, Map<Key, Set<Integer>> selected, Image image) throws IOException {
        ReferenceImage ref = image instanceof ReferenceImage reference ? reference : null;
        Image source = ref == null ? image : ref.image();
        if (!(source instanceof BitmapImage bitmap) || (ref != null && !bitmap.isValidCoordinate(ref.row(), ref.col()))) {
            return false;
        }
        Int2ObjectMap<Glyph> glyphs = fonts.resolve(bitmap.font());
        BufferedImage texture = fonts.loadedBitmap(Key.of(bitmap.file()));
        if (texture == null || glyphs.isEmpty()) {
            return false;
        }
        int firstRow = ref == null ? 0 : ref.row();
        int firstColumn = ref == null ? 0 : ref.col();
        int endRow = ref == null ? bitmap.rows() : firstRow + 1;
        int endColumn = ref == null ? bitmap.columns() : firstColumn + 1;
        int width = texture.getWidth() / bitmap.columns();
        int height = texture.getHeight() / bitmap.rows();
        Set<Integer> codes = selected.computeIfAbsent(bitmap.font(), key -> new TreeSet<>());
        boolean complete = true;
        // 外部 ZIP 可能在同一码点放了别的图片。只选取输入字体实际使用的纹理和网格单元。
        for (int row = firstRow; row < endRow; row++) {
            for (int column = firstColumn; column < endColumn; column++) {
                int code = bitmap.codepointAt(row, column);
                if (code == 0) {
                    continue;
                }
                Glyph glyph = glyphs.get(code);
                if (glyph == null || glyph.image() != texture || glyph.x() != column * width || glyph.y() != row * height
                        || glyph.width() != width || glyph.sourceHeight() != height) {
                    complete = false;
                    continue;
                }
                if (glyph.isGuiBitmap()) {
                    codes.add(code);
                }
            }
        }
        return complete;
    }

    void write(Key font, Set<Integer> selected, Int2ObjectMap<Glyph> glyphs) throws IOException {
        if (selected.isEmpty()) {
            return;
        }
        Files.createDirectories(this.directory);
        Int2ObjectMap<String> images = new Int2ObjectLinkedOpenHashMap<>();
        for (int code : selected) {
            Glyph glyph = glyphs.get(code);
            if (glyph == null || !glyph.isGuiBitmap()) {
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
