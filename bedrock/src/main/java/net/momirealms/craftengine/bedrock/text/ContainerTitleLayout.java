package net.momirealms.craftengine.bedrock.text;

import net.momirealms.craftengine.bedrock.GuiTitlePart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ContainerTitleLayout {
    public static final int ORIGIN = 256;
    public static final String MARKER = "ce:layout/";
    public static final int SPACER_FIRST = 0xE200;
    public static final int MAX_SPACER = 33;
    private static final int SPACER_BYTES = 3;
    private static final int RESET_PREFIX_BYTES = 3;
    private final FontData fonts;

    public ContainerTitleLayout(FontData fonts) {
        this.fonts = fonts;
    }

    public List<List<Run>> layout(List<GuiTitlePart> parts) {
        return this.layout(parts, false);
    }

    public List<List<Run>> layout(List<GuiTitlePart> parts, boolean force) {
        // 没有需要映射的自定义字形时，交给原生标题渲染。
        if (!force && !this.requiresLayout(parts, Target.CONTAINER)) {
            return List.of();
        }
        return this.layoutWithWidth(parts).layers();
    }

    public MeasuredLayout layoutWithWidth(List<GuiTitlePart> parts) {
        return this.layoutWithWidth(parts, Target.CONTAINER);
    }

    public List<List<Run>> layoutTitle(List<GuiTitlePart> parts, boolean force) {
        if (parts.isEmpty() || (!force && !this.requiresLayout(parts, Target.TITLE))) {
            return List.of();
        }
        return this.layoutWithWidth(parts, Target.TITLE).layers();
    }

    public MeasuredLayout layoutBossBar(List<GuiTitlePart> parts, boolean leftAnchored) {
        Target target = leftAnchored ? Target.BOSS_BAR_LEFT : Target.BOSS_BAR;
        if (!this.requiresLayout(parts, target)) {
            return new MeasuredLayout(List.of(), 0);
        }
        return this.layoutWithWidth(parts, target);
    }

    private MeasuredLayout layoutWithWidth(List<GuiTitlePart> parts, Target target) {
        List<Layer> layers = new ArrayList<>();
        int remainingBytes = ContainerTitleCodec.MAX_BYTES;
        // Java 光标允许负间距回退；每个基岩图层的光标只能通过间距字符向右移动。
        double javaCursor = ORIGIN;
        double width = 0;
        for (int partIndex = 0; partIndex < parts.size(); partIndex++) {
            GuiTitlePart part = parts.get(partIndex);
            FontData.Font font = this.fonts.font(part.font());
            int boldAdvance = (part.decorations() & 2) != 0 ? 1 : 0;
            for (int offset = 0; offset < part.text().length();) {
                int codepoint = part.text().codePointAt(offset);
                offset += Character.charCount(codepoint);
                FontGlyph glyph = font.rendering().get(codepoint);
                if (glyph == null || codepoint == '\n' || codepoint == '§') {
                    throw new UnsupportedOperationException("Unknown title glyph metrics: " + part.font() + "/" + Integer.toHexString(codepoint));
                }
                if (!glyph.visible()) {
                    float advance = font.advance(codepoint, false);
                    width += advance;
                    javaCursor += advance;
                    continue;
                }
                boolean nativeFallback = target == Target.BOSS_BAR_LEFT && glyph.renderedAdvance() == FontGlyph.NATIVE_FALLBACK;
                if (nativeFallback) {
                    FontGlyph ordinary = this.fonts.font("minecraft:default").rendering().get(codepoint);
                    if (ordinary != null && ordinary.character() == codepoint && ordinary.renderedAdvance() >= 0) {
                        glyph = ordinary;
                    }
                }
                boolean unknownWidth = nativeFallback && glyph.renderedAdvance() < 0;
                if (glyph.renderedAdvance() < 0 && !unknownWidth) {
                    throw new UnsupportedOperationException("Unknown Bedrock title glyph width: " + part.font() + "/" + Integer.toHexString(codepoint));
                }
                // 用 bearing 修正落笔位置，使两端字形的可见部分对齐。
                double position = Math.rint(javaCursor) - glyph.bearing();
                int advance = unknownWidth ? 0 : glyph.renderedAdvance() + boldAdvance;
                double inkStart = position + glyph.bearing();
                // 实际原生字宽未知时，以当前字符结束本层。之后的字符仍按各自的 Java 光标定位。
                double inkEnd = unknownWidth ? Double.POSITIVE_INFINITY : inkStart + advance - 1;
                int minimumLayer = minimumLayer(layers, inkStart, inkEnd);
                Layer selected = unknownWidth ? null : findLayer(layers, minimumLayer, position);
                if (selected == null) {
                    if (position < 0 || position == 1) {
                        throw new UnsupportedOperationException("Title exceeds left margin");
                    }
                    selected = new Layer();
                    layers.add(selected);
                    remainingBytes -= ContainerTitleCodec.ENTRY_BYTES;
                }
                int gap = (int) (position - selected.cursor);
                // 每个可见字符至少占一字节；间距段的 §r 前缀和每个 E2 字符各占三个 UTF-8 字节。
                // 先扣除确定的开销再展开间距，格式化后的实际字节数由编码器校验。
                remainingBytes--;
                remainingBytes -= spacingBytes(gap);
                if (remainingBytes < 0) {
                    throw new UnsupportedOperationException("Title exceeds the four-digit layout table");
                }
                if (gap > 0) {
                    selected.appendSpacing(gap);
                }
                selected.appendGlyph(partIndex, glyph.character());
                // Geyser 会展开 BossBar 名称里的 %。将它留在图层末尾，由 HUD 补回原字符。
                selected.closed = unknownWidth || ((target == Target.BOSS_BAR || target == Target.BOSS_BAR_LEFT) && glyph.character() == '%');
                selected.addSpan(inkStart, inkEnd);
                selected.cursor = position + advance;
                float javaAdvance = nativeFallback ? font.advance(codepoint, boldAdvance != 0) : font.advance(codepoint, false) + boldAdvance;
                javaCursor += javaAdvance;
                width += javaAdvance;
            }
        }
        // 有自定义字形但没有可见内容时，保留一个空图层，区别于无需转换的空结果。
        int pixelWidth = (int) Math.ceil(width);
        if (target != Target.BOSS_BAR_LEFT) {
            pixelWidth = Math.max(0, pixelWidth);
        }
        if (layers.isEmpty()) {
            return new MeasuredLayout(List.of(List.of()), pixelWidth);
        }
        if (target == Target.TITLE || target == Target.BOSS_BAR) {
            // 给每层补到相同的宽度，让 HUD 用原生居中对齐叠在同一条基线上。
            double centeredWidth = 2 * ORIGIN + Math.ceil(width);
            for (Layer layer : layers) {
                double padding = centeredWidth - layer.cursor;
                if (padding < 0 || padding == 1 || padding > ContainerTitleCodec.MAX_BYTES / SPACER_BYTES * MAX_SPACER) {
                    throw new UnsupportedOperationException("Title cannot be centered within the layout margin");
                }
                int gap = (int) padding;
                remainingBytes -= spacingBytes(gap);
                if (remainingBytes < 0) {
                    throw new UnsupportedOperationException("Title exceeds the four-digit layout table");
                }
                if (gap > 0) {
                    layer.appendSpacing(gap);
                }
            }
        }
        List<List<Run>> result = new ArrayList<>(layers.size());
        for (Layer layer : layers) {
            layer.flushRun();
            result.add(layer.runs);
        }
        return new MeasuredLayout(result, pixelWidth);
    }

    public int width(List<GuiTitlePart> parts) {
        double width = 0;
        for (GuiTitlePart part : parts) {
            FontData.Font font = this.fonts.font(part.font());
            for (int offset = 0; offset < part.text().length();) {
                int codepoint = part.text().codePointAt(offset);
                offset += Character.charCount(codepoint);
                FontGlyph glyph = font.rendering().get(codepoint);
                if (glyph == null) {
                    throw new UnsupportedOperationException("Unknown glyph metrics: " + part.font() + "/" + Integer.toHexString(codepoint));
                }
                width += font.advance(codepoint, false) + (glyph.visible() && (part.decorations() & 2) != 0 ? 1 : 0);
            }
        }
        return Math.max(0, (int) Math.ceil(width));
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
            if (!layer.closed && (gap == 0 || gap >= 2)) {
                return layer;
            }
        }
        return null;
    }

    public static void appendSpacers(StringBuilder text, int gap) {
        while (gap > 0) {
            int step = Math.min(gap, MAX_SPACER);
            // 没有 1 像素间距字符，当前步少走 1 像素，让最后一步至少为 2 像素。
            if (gap - step == 1) {
                step--;
            }
            text.appendCodePoint(SPACER_FIRST + step - 2);
            gap -= step;
        }
    }

    private static int spacingBytes(int gap) {
        return gap == 0 ? 0 : RESET_PREFIX_BYTES + Math.ceilDiv(gap, MAX_SPACER) * SPACER_BYTES;
    }

    private boolean requiresLayout(List<GuiTitlePart> parts, Target target) {
        for (GuiTitlePart part : parts) {
            FontData.Font font = this.fonts.font(part.font());
            if (font.rendering().isEmpty()) {
                continue;
            }
            for (int offset = 0; offset < part.text().length();) {
                int codepoint = part.text().codePointAt(offset);
                offset += Character.charCount(codepoint);
                FontGlyph glyph = font.rendering().get(codepoint);
                if (glyph == null) {
                    continue;
                }
                boolean required = switch (target) {
                    case CONTAINER -> glyph.character() != codepoint;
                    case TITLE -> font.advance(codepoint, false) < 0;
                    case BOSS_BAR, BOSS_BAR_LEFT -> {
                        float advance = font.advance(codepoint, false);
                        yield advance < 0 || (!glyph.visible() && advance > 0)
                                || (target == Target.BOSS_BAR_LEFT && glyph.renderedAdvance() == FontGlyph.NATIVE_FALLBACK);
                    }
                };
                if (required) {
                    return true;
                }
            }
        }
        return false;
    }

    public record MeasuredLayout(List<List<Run>> layers, int width) {
    }

    // part 是原始标题片段的索引，-1 表示不继承片段样式的间距文本。
    public record Run(int part, String text) {
    }

    private enum Target {
        CONTAINER, TITLE, BOSS_BAR, BOSS_BAR_LEFT
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
        private boolean closed;

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
            appendSpacers(this.text, gap);
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
