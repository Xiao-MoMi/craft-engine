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
                    boolean nativeText, // 保留原码点，交给基岩原生字体绘制
                    int actualWidth,
                    float boldOffset,
                    boolean unicode) {

    public Glyph(@Nullable BufferedImage image, int x, int y, int width, int sourceHeight, int height, int ascent, float spacing, boolean nativeText, int actualWidth, float boldOffset) {
        this(image, x, y, width, sourceHeight, height, ascent, spacing, nativeText, actualWidth, boldOffset, false);
    }

    public Glyph(@Nullable BufferedImage image, int x, int y, int width, int sourceHeight, int height, int ascent, float spacing, boolean nativeText, int actualWidth) {
        this(image, x, y, width, sourceHeight, height, ascent, spacing, nativeText, actualWidth, 1, false);
    }

    static Glyph unihex(int width) {
        return new Glyph(null, 0, 0, width, 16, 8, 7, width / 2 + 1, true, width, 0.5f, true);
    }

    public Glyph(@Nullable BufferedImage image, int x, int y, int width, int sourceHeight, int height, int ascent, float spacing, boolean nativeText) {
        this(image, x, y, width, sourceHeight, height, ascent, spacing, nativeText, false);
    }

    Glyph(@Nullable BufferedImage image, int x, int y, int width, int sourceHeight, int height, int ascent, float spacing, boolean nativeText, boolean unicode) {
        this(image, x, y, width, sourceHeight, height, ascent, spacing, nativeText,
                image == null ? 0 : measureWidth(image, x, y, width, sourceHeight), 1, unicode);
    }

    public Glyph(@Nullable BufferedImage image, int x, int y, int width, int sourceHeight, int height, int ascent, float spacing) {
        this(image, x, y, width, sourceHeight, height, ascent, spacing, false);
    }

    static Glyph space(float advance) {
        return new Glyph(null, 0, 0, 0, 0, 0, 0, advance);
    }

    Glyph forNativeRendering() {
        if (this.image == null) {
            return this;
        }
        boolean visible = !this.advanceOnly();
        if (this.nativeText && visible) {
            return this;
        }
        // 去掉位图后用 spacing 保存已测出的 Java advance，纯间距仍然不可见。
        return new Glyph(null, this.x, this.y, this.width, this.sourceHeight, this.height, this.ascent, this.advance(), visible, this.actualWidth, this.boldOffset, this.unicode);
    }

    public boolean advanceOnly() {
        if (this.image == null) {
            return !this.nativeText;
        }
        return this.height <= 0 || this.ascent < -2048 || this.actualWidth == 0;
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

    public float advance(boolean bold) {
        return this.advance() + (bold ? this.boldOffset : 0);
    }

    public float advance() {
        if (this.image == null) {
            return this.spacing;
        }
        return (int) (0.5 + this.actualWidth * ((float) this.height / this.sourceHeight)) + 1;
    }
}
