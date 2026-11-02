package net.momirealms.craftengine.core.pack.bedrock.font;

import net.momirealms.craftengine.core.util.ImageUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
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
    private final BufferedImage image;
    private int usedSlots;

    GlyphPage(int index, int cellSize) {
        this.index = index;
        this.cellSize = cellSize;
        this.image = new BufferedImage(cellSize * CELLS_PER_ROW, cellSize * CELLS_PER_ROW, BufferedImage.TYPE_INT_ARGB);
    }

    int cellSize() {
        return this.cellSize;
    }

    int remainingSlots() {
        return CELLS_PER_PAGE - this.usedSlots;
    }

    void write(Path directory) throws IOException {
        ImageUtils.writePng(this.image, file(directory, this.index));
    }

    static Path file(Path directory, int index) {
        return directory.resolve("glyph_" + Integer.toHexString(index).toUpperCase(Locale.ROOT) + ".png");
    }

    int add(Glyph glyph, GlyphMetrics metrics) {
        int slot = this.usedSlots++;
        int cellX = slot % CELLS_PER_ROW * this.cellSize;
        int cellY = slot / CELLS_PER_ROW * this.cellSize;
        int left = cellX + (this.cellSize - metrics.width()) / 2;
        // 页内基线位于字格中心下方 3 像素。从基线向上移 Java 字形的 ascent，得到绘制起点。
        int top = cellY + this.cellSize / 2 + BASELINE_OFFSET - glyph.ascent();
        this.drawGlyph(glyph, metrics.width(), left, top);
        return this.index * CELLS_PER_PAGE + slot;
    }

    private void drawGlyph(Glyph glyph, int width, int left, int top) {
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
                this.image.setRGB(left + x, top + y, pixel);
            }
        }

        // 基岩会裁掉左右的全透明列。用最低非零透明度标出两端，保留预期字宽，同时保留已有的可见像素。
        int markerY = top + Math.max(firstInkRow, 0);
        this.markEdge(left, markerY);
        this.markEdge(left + width - 1, markerY);
    }

    private void markEdge(int x, int y) {
        if ((this.image.getRGB(x, y) >>> 24) == 0) {
            this.image.setRGB(x, y, ALMOST_TRANSPARENT);
        }
    }
}
