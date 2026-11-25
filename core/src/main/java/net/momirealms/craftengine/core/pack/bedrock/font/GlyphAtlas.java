package net.momirealms.craftengine.core.pack.bedrock.font;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.bedrock.text.FontGlyph;
import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

final class GlyphAtlas {
    private static final int FIRST_BITMAP_PAGE = 0xE3;
    private static final int LAST_BITMAP_PAGE = 0xF8;

    private final Map<Glyph, FontGlyph> allocations = new HashMap<>();
    private final Map<Glyph, GlyphMetrics> metrics = new HashMap<>();
    private final Set<Glyph> unsupported = new HashSet<>();
    private final List<GlyphPage> pages = new ArrayList<>();

    Int2ObjectMap<FontGlyph> allocateFont(Key font, Int2ObjectMap<Glyph> glyphs) {
        IntArrayList codepoints = new IntArrayList();
        Int2ObjectMaps.fastForEach(glyphs, entry -> {
            Glyph glyph = entry.getValue();
            if (!glyph.nativeText() || glyph.image() != null) {
                codepoints.add(entry.getIntKey());
            }
        });
        IntArrays.quickSort(codepoints.elements(), 0, codepoints.size());
        // 多个字符可能引用同一字形，估算本字体所需空间时只计一个槽位。
        Set<Glyph> pending = new LinkedHashSet<>();
        for (int i = 0; i < codepoints.size(); i++) {
            int codepoint = codepoints.getInt(i);
            Glyph glyph = glyphs.get(codepoint);
            // 原生字符和纯间距字符无需绘制；此前已分配码位的字形可以复用原槽位。
            if (glyph.nativeText() || glyph.advanceOnly() || this.allocations.containsKey(glyph) || this.unsupported.contains(glyph)) {
                continue;
            }
            // 测量得到的格子尺寸可供其他字体复用；失败也会记录，避免同一字形反复测量和告警。
            if (!this.metrics.containsKey(glyph)) {
                try {
                    this.metrics.put(glyph, GlyphMetrics.measure(glyph));
                } catch (UnsupportedOperationException exception) {
                    this.unsupported.add(glyph);
                    TranslationManager.warn("resource_pack.bedrock.font.glyph", font.asString(), Integer.toHexString(codepoint), exception.getLocalizedMessage());
                    continue;
                }
            }
            pending.add(glyph);
        }
        // 容量检查通过后才分配，保证容量不足的字体不消耗任何槽位。
        boolean allocate = this.hasRoomFor(pending);
        if (!allocate) {
            TranslationManager.warn("resource_pack.bedrock.font.capacity", font.asString());
        }
        Int2ObjectMap<FontGlyph> result = new Int2ObjectLinkedOpenHashMap<>(codepoints.size());
        for (int i = 0; i < codepoints.size(); i++) {
            int codepoint = codepoints.getInt(i);
            Glyph glyph = glyphs.get(codepoint);
            float advance = glyph.advance();
            if (glyph.nativeText()) {
                result.put(codepoint, new FontGlyph(codepoint, (int) advance, 0));
            } else if (glyph.advanceOnly()) {
                boolean ordinarySpace = codepoint == ' ' && advance == 4;
                result.put(codepoint, new FontGlyph(ordinarySpace ? ' ' : -1, ordinarySpace ? 4 : 0, 0));
            } else if (!allocate) {
                FontGlyph existing = this.allocations.get(glyph);
                result.put(codepoint, existing == null ? new FontGlyph(codepoint, FontGlyph.NATIVE_FALLBACK, 0) : existing);
            } else if (!this.unsupported.contains(glyph)) {
                result.put(codepoint, this.allocateBitmap(glyph));
            }
        }
        return result;
    }

    private boolean hasRoomFor(Set<Glyph> pending) {
        // 同一页的格子尺寸固定，只能复用相同尺寸的空槽。这里模拟分配，不修改实际页。
        Int2IntOpenHashMap freeSlots = new Int2IntOpenHashMap();
        for (GlyphPage page : this.pages) {
            freeSlots.addTo(page.cellSize(), page.remainingSlots());
        }
        int nextPage = FIRST_BITMAP_PAGE + this.pages.size();
        for (Glyph glyph : pending) {
            int cellSize = this.metrics.get(glyph).cellSize();
            int available = freeSlots.get(cellSize);
            if (available == 0) {
                if (nextPage > LAST_BITMAP_PAGE) {
                    return false;
                }
                nextPage++;
                available = GlyphPage.CELLS_PER_PAGE;
            }
            freeSlots.put(cellSize, available - 1);
        }
        return true;
    }

    private FontGlyph allocateBitmap(Glyph glyph) {
        FontGlyph existing = this.allocations.get(glyph);
        if (existing != null) {
            return existing;
        }
        GlyphMetrics metrics = this.metrics.get(glyph);
        GlyphPage page = this.pageWithFreeSlot(metrics.cellSize());
        FontGlyph allocation = new FontGlyph(page.add(glyph, metrics), metrics.advance(), 1);
        this.allocations.put(glyph, allocation);
        return allocation;
    }

    private GlyphPage pageWithFreeSlot(int cellSize) {
        for (GlyphPage page : this.pages) {
            if (page.cellSize() == cellSize && page.remainingSlots() > 0) {
                return page;
            }
        }
        GlyphPage page = new GlyphPage(FIRST_BITMAP_PAGE + this.pages.size(), cellSize);
        this.pages.add(page);
        return page;
    }

    void writePages(Path directory) throws IOException {
        for (GlyphPage page : this.pages) {
            page.write(directory);
        }
    }
}
