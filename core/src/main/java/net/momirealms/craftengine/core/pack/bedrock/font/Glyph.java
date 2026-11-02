package net.momirealms.craftengine.core.pack.bedrock.font;

import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;

public record Glyph(@Nullable BufferedImage image,
                    int x,
                    int y,
                    int width,
                    int sourceHeight,
                    int height,
                    int ascent,
                    float spacing,
                    boolean nativeText, // 未覆写的原版字形，仅导出度量，保留字符交给基岩原生字体绘制
                    int actualWidth) {

    public Glyph(@Nullable BufferedImage image, int x, int y, int width, int sourceHeight, int height, int ascent, float spacing, boolean nativeText) {
        this(image, x, y, width, sourceHeight, height, ascent, spacing, nativeText,
                image == null ? 0 : measureWidth(image, x, y, width, sourceHeight));
    }

    public Glyph(@Nullable BufferedImage image, int x, int y, int width, int sourceHeight, int height, int ascent, float spacing) {
        this(image, x, y, width, sourceHeight, height, ascent, spacing, false);
    }

    static Glyph space(float advance) {
        return new Glyph(null, 0, 0, 0, 0, 0, 0, advance);
    }

    public boolean advanceOnly() {
        return this.image == null || this.height <= 0 || this.ascent < -2048 || this.actualWidth == 0;
    }

    private static int measureWidth(BufferedImage image, int x, int y, int width, int sourceHeight) {
        for (int column = width - 1; column >= 0; column--) {
            for (int row = 0; row < sourceHeight; row++) {
                if ((image.getRGB(x + column, y + row) >>> 24) != 0) {
                    return column + 1;
                }
            }
        }
        return 0;
    }

    public float advance() {
        if (this.image == null) {
            return this.spacing;
        }
        return (int) (0.5 + this.actualWidth * ((float) this.height / this.sourceHeight)) + 1;
    }
}
