package net.momirealms.craftengine.core.pack.bedrock.font;

import net.momirealms.craftengine.core.util.PngOptimizer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * 一张基岩字体纹理页，包含 16 × 16 个相同尺寸的字格，从左到右逐行填充。
 * 页号和槽位号分别构成字符码位的高、低字节。例如 {@code glyph_E3.png} 的槽位
 * {@code 0x12} 位于第 2 行第 3 列，对应字符 {@code U+E312}。
 */
final class GlyphPage {
    private static final int CELLS_PER_ROW = 16;
    static final int CELLS_PER_PAGE = CELLS_PER_ROW * CELLS_PER_ROW;
    static final int BASELINE_OFFSET = 3;
    private static final int ALMOST_TRANSPARENT = 0x01000000;

    private final int index;
    private final int cellSize;
    private final int side;
    private final Glyph[] glyphs = new Glyph[CELLS_PER_PAGE];
    private final int[] widths = new int[CELLS_PER_PAGE];
    private int usedSlots;

    GlyphPage(int index, int cellSize) {
        this.index = index;
        this.cellSize = cellSize;
        this.side = cellSize * CELLS_PER_ROW;
    }

    int cellSize() {
        return this.cellSize;
    }

    int remainingSlots() {
        return CELLS_PER_PAGE - this.usedSlots;
    }

    void write(Path directory) throws IOException {
        int[] pixels = new int[this.side * this.side];
        for (int slot = 0; slot < this.usedSlots; slot++) {
            Glyph glyph = this.glyphs[slot];
            int width = this.widths[slot];
            int left = slot % CELLS_PER_ROW * this.cellSize + (this.cellSize - width) / 2;
            int top = slot / CELLS_PER_ROW * this.cellSize + this.cellSize / 2 + BASELINE_OFFSET - glyph.ascent();
            int markerY = top + drawGlyph(glyph, pixels, this.side, width, left, top);
            markEdge(pixels, markerY * this.side + left);
            markEdge(pixels, markerY * this.side + left + width - 1);
        }
        try (OutputStream output = Files.newOutputStream(file(directory, this.index))) {
            PngOptimizer.writeArgb(output, this.side, this.side, pixels);
        }
    }

    static Path file(Path directory, int index) {
        return directory.resolve("glyph_" + Integer.toHexString(index).toUpperCase(Locale.ROOT) + ".png");
    }

    int add(Glyph glyph, GlyphMetrics metrics) {
        int slot = this.usedSlots++;
        this.glyphs[slot] = glyph;
        this.widths[slot] = metrics.width();
        return this.index * CELLS_PER_PAGE + slot;
    }

    static int drawGlyph(Glyph glyph, int[] pixels, int stride, int width, int left, int top) {
        int firstInkRow = -1;
        float scale = (float) glyph.height() / glyph.sourceHeight();
        // 将目标像素的中心映回源字形，直接取该位置的像素颜色，缩放时保留像素边缘。
        for (int y = 0; y < glyph.height(); y++) {
            int sourceY = Math.min(glyph.sourceHeight() - 1, (int) ((y + 0.5) * glyph.sourceHeight() / glyph.height()));
            for (int x = 0; x < width; x++) {
                int sourceX = Math.min(glyph.width() - 1, (int) ((x + 0.5) / scale));
                int pixel = glyph.image().getRGB(glyph.x() + sourceX, glyph.y() + sourceY);
                if (firstInkRow < 0 && (pixel >>> 24) != 0) {
                    firstInkRow = y;
                }
                pixels[(top + y) * stride + left + x] = pixel;
            }
        }

        return Math.max(firstInkRow, 0);
    }

    private static void markEdge(int[] pixels, int index) {
        if ((pixels[index] >>> 24) == 0) {
            pixels[index] = ALMOST_TRANSPARENT;
        }
    }
}
