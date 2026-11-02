package net.momirealms.craftengine.core.pack.bedrock.font;

import net.momirealms.craftengine.core.plugin.locale.TranslationManager;

record GlyphMetrics(int width, int advance, int cellSize) {

    static GlyphMetrics measure(Glyph glyph) {
        if (glyph.height() <= 0) {
            throw new UnsupportedOperationException(TranslationManager.message("resource_pack.bedrock.font.height"));
        }
        int advance = (int) glyph.advance();
        if (glyph.actualWidth() == 0 || advance <= 1) {
            throw new UnsupportedOperationException(TranslationManager.message("resource_pack.bedrock.font.spacing"));
        }

        // 基岩的基线在格子中心下方 3 像素。上、下半格分别要容得下 Java 字形基线两侧的像素。
        int width = advance - 1;
        long aboveCenter = (long) glyph.ascent() - GlyphPage.BASELINE_OFFSET;
        long belowCenter = (long) glyph.height() - glyph.ascent() + GlyphPage.BASELINE_OFFSET;
        long requiredSize = Math.max(width, 2 * Math.max(aboveCenter, belowCenter));
        if (requiredSize > 512) {
            throw new UnsupportedOperationException(TranslationManager.message("resource_pack.bedrock.font.size"));
        }
        int cellSize = 8;
        while (cellSize < requiredSize) {
            cellSize *= 2;
        }
        return new GlyphMetrics(width, advance, cellSize);
    }
}
