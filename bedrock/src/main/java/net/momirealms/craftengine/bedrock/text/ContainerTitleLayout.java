package net.momirealms.craftengine.bedrock.text;

import net.momirealms.craftengine.bedrock.GuiTitlePart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class ContainerTitleLayout {
    public static final int ORIGIN = 256;
    public static final String MARKER = "craftengine:layout/";
    public static final int SPACER_FIRST = 0xE200;
    public static final int MAX_SPACER = 33;
    private static final int SPACER_BYTES = 3;
    private static final int RESET_PREFIX_BYTES = 3;
    private final Map<String, Map<Integer, FontGlyph>> fonts;

    public ContainerTitleLayout(Map<String, Map<Integer, FontGlyph>> fonts) {
        this.fonts = fonts;
    }

    public List<List<Run>> layout(List<GuiTitlePart> parts) {
        // 没有需要映射的自定义字形时，交给原生标题渲染。
        if (!this.hasCustomGlyph(parts)) {
            return List.of();
        }
        List<Layer> layers = new ArrayList<>();
        int remainingBytes = ContainerTitleCodec.MAX_BYTES;
        // Java 光标允许负间距回退；每个基岩图层的光标只能通过间距字符向右移动。
        double javaCursor = ORIGIN;
        for (int partIndex = 0; partIndex < parts.size(); partIndex++) {
            GuiTitlePart part = parts.get(partIndex);
            Map<Integer, FontGlyph> font = this.fonts.get(part.font());
            int boldAdvance = (part.decorations() & 2) != 0 ? 1 : 0;
            for (int offset = 0; offset < part.text().length();) {
                int codepoint = part.text().codePointAt(offset);
                offset += Character.charCount(codepoint);
                FontGlyph glyph = font == null ? null : font.get(codepoint);
                if (glyph == null || codepoint == '\n' || codepoint == '§') {
                    throw new UnsupportedOperationException("Unknown title glyph metrics: " + part.font() + "/" + Integer.toHexString(codepoint));
                }
                if (!glyph.visible()) {
                    javaCursor += glyph.advance();
                    continue;
                }
                if (glyph.renderedAdvance() < 0) {
                    throw new UnsupportedOperationException("Unknown Bedrock title glyph width: " + part.font() + "/" + Integer.toHexString(codepoint));
                }
                // 用 bearing 修正落笔位置，使两端字形的可见部分对齐。
                double position = Math.rint(javaCursor) - glyph.bearing();
                int advance = glyph.renderedAdvance() + boldAdvance;
                double inkStart = position + glyph.bearing();
                double inkEnd = inkStart + advance - 1;
                int minimumLayer = minimumLayer(layers, inkStart, inkEnd);
                Layer selected = findLayer(layers, minimumLayer, position);
                if (selected == null) {
                    if (position < 0 || position == 1) {
                        throw new UnsupportedOperationException("Title exceeds left margin");
                    }
                    selected = new Layer();
                    layers.add(selected);
                    remainingBytes -= ContainerTitleCodec.ENTRY_BYTES;
                }
                int gap = (int) (position - selected.cursor);
                int spacerCount = Math.ceilDiv(gap, MAX_SPACER);
                // 每个可见字符至少占一字节；间距段的 §r 前缀和每个 E2 字符各占三个 UTF-8 字节。
                // 先扣除确定的开销再展开间距，格式化后的实际字节数由编码器校验。
                remainingBytes--;
                if (spacerCount > 0) {
                    remainingBytes -= RESET_PREFIX_BYTES + spacerCount * SPACER_BYTES;
                }
                if (remainingBytes < 0) {
                    throw new UnsupportedOperationException("Title exceeds the four-digit layout table");
                }
                if (gap > 0) {
                    selected.appendSpacing(gap);
                }
                selected.appendGlyph(partIndex, glyph.character());
                selected.addSpan(inkStart, inkEnd);
                selected.cursor = position + advance;
                javaCursor += glyph.advance() + boldAdvance;
            }
        }
        // 有自定义字形但没有可见内容时，保留一个空图层，区别于无需转换的空结果。
        if (layers.isEmpty()) {
            return List.of(List.of());
        }
        List<List<Run>> result = new ArrayList<>(layers.size());
        for (Layer layer : layers) {
            layer.flushRun();
            result.add(layer.runs);
        }
        return result;
    }

    private static int minimumLayer(List<Layer> layers, double inkStart, double inkEnd) {
        // 后绘制的字形必须位于所有相交字形之上，负间距回退时也保持原有遮盖顺序。
        // 从顶层向下查找，首次相交就是需要越过的最高图层。
        for (int i = layers.size() - 1; i >= 0; i--) {
            if (layers.get(i).intersects(inkStart, inkEnd)) {
                return i + 1;
            }
        }
        return 0;
    }

    private static Layer findLayer(List<Layer> layers, int minimumLayer, double position) {
        // 间距字符只能表示 2 到 MAX_SPACER 像素；无需移动或至少前进 2 像素才能复用图层。
        for (int i = minimumLayer; i < layers.size(); i++) {
            Layer layer = layers.get(i);
            double gap = position - layer.cursor;
            if (gap == 0 || gap >= 2) {
                return layer;
            }
        }
        return null;
    }

    private boolean hasCustomGlyph(List<GuiTitlePart> parts) {
        for (GuiTitlePart part : parts) {
            Map<Integer, FontGlyph> font = this.fonts.get(part.font());
            if (font == null) {
                continue;
            }
            for (int offset = 0; offset < part.text().length();) {
                int codepoint = part.text().codePointAt(offset);
                offset += Character.charCount(codepoint);
                FontGlyph glyph = font.get(codepoint);
                if (glyph != null && glyph.character() != codepoint) {
                    return true;
                }
            }
        }
        return false;
    }

    // part 是原始标题片段的索引，-1 表示不继承片段样式的间距文本。
    public record Run(int part, String text) {
    }

    private static final class Layer {
        private final List<Run> runs = new ArrayList<>();
        // 每两个元素保存一个区间的起止位置，避免为每个字形分配 Span 对象。
        private double[] spans = new double[16];
        private int spanSize;
        private double minimumInkStart = Double.POSITIVE_INFINITY;
        private double maximumInkEnd = Double.NEGATIVE_INFINITY;
        private final StringBuilder text = new StringBuilder();
        private int part;
        private double cursor;

        private void addSpan(double start, double end) {
            if (this.spanSize == this.spans.length) {
                this.spans = Arrays.copyOf(this.spans, this.spans.length * 2);
            }
            this.spans[this.spanSize++] = start;
            this.spans[this.spanSize++] = end;
            this.minimumInkStart = Math.min(this.minimumInkStart, start);
            this.maximumInkEnd = Math.max(this.maximumInkEnd, end);
        }

        private boolean intersects(double start, double end) {
            // 常规文本向右追加时，字形位于已有区间之外，无需逐个检查之前的字形。
            if (start >= this.maximumInkEnd || end <= this.minimumInkStart) {
                return false;
            }
            // 包围范围内仍可能有空隙，必须检查实际区间，不能仅凭整体范围判断重叠。
            for (int i = this.spanSize - 2; i >= 0; i -= 2) {
                if (start < this.spans[i + 1] && end > this.spans[i]) {
                    return true;
                }
            }
            return false;
        }

        private void appendSpacing(int gap) {
            this.selectPart(-1);
            while (gap > 0) {
                int step = Math.min(gap, MAX_SPACER);
                // 没有 1 像素间距字符，当前步少走 1 像素，让最后一步至少为 2 像素。
                if (gap - step == 1) {
                    step--;
                }
                this.text.appendCodePoint(SPACER_FIRST + step - 2);
                gap -= step;
            }
        }

        private void appendGlyph(int part, int codepoint) {
            this.selectPart(part);
            this.text.appendCodePoint(codepoint);
        }

        private void selectPart(int part) {
            // 同一片段直接累积，切换片段时才创建 Run，避免逐字符复制已有文本。
            if (this.part != part) {
                this.flushRun();
                this.part = part;
            }
        }

        private void flushRun() {
            if (!this.text.isEmpty()) {
                this.runs.add(new Run(this.part, this.text.toString()));
                this.text.setLength(0);
            }
        }
    }
}