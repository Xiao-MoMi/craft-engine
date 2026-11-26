package net.momirealms.craftengine.bedrock.text;

import net.momirealms.craftengine.bedrock.GuiTitlePart;
import net.momirealms.craftengine.bedrock.GuiTitlePart.Shadow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

public final class ContainerTitleLayout {
    public static final int ORIGIN = 256;
    public static final String MARKER = "ce:layout/";
    public static final int SPACER_FIRST = 0xE200;
    public static final int MAX_SPACER = 33;
    private static final int SPACER_BYTES = 3;
    private static final int RESET_PREFIX_BYTES = 3;
    private static final MeasuredLayout NO_LAYOUT = new MeasuredLayout(List.of(), 0);
    // Native Unicode label advances for U+0020..U+007E, measured in half pixels. Bold adds half a pixel.
    private static final byte[] UNICODE_ASCII_HALF_ADVANCES = {
            8, 3, 7, 8, 9, 9, 9, 3, 5, 5, 9, 9, 4, 8, 4, 8,
            8, 7, 8, 8, 8, 8, 8, 8, 8, 8, 4, 4, 7, 8, 7, 8,
            8, 8, 8, 8, 8, 8, 8, 8, 8, 7, 9, 8, 8, 8, 8, 8,
            8, 9, 8, 8, 9, 8, 9, 8, 8, 9, 8, 5, 8, 5, 8, 9,
            5, 8, 8, 8, 8, 8, 7, 8, 8, 7, 7, 8, 7, 9, 8, 8,
            8, 8, 8, 8, 7, 8, 8, 9, 8, 8, 8, 5, 3, 5, 9
    };
    private final FontData fonts;
    private final boolean hudFonts;

    public ContainerTitleLayout(FontData fonts) {
        this.fonts = fonts;
        this.hudFonts = fonts.fonts().values().stream().anyMatch(font -> font.hudUnicode != null);
    }

    public MeasuredLayout positioned(List<GuiTitlePart> parts, Map<String, Integer> offsets, boolean force) {
        return this.positioned(parts, offsets, force, true);
    }

    public MeasuredLayout positioned(List<GuiTitlePart> parts, Map<String, Integer> offsets, boolean force, boolean defaultShadow) {
        Shadow opposite = defaultShadow ? Shadow.OFF : Shadow.ON;
        if (parts.isEmpty() || (!force && !this.requiresLayout(parts, Target.POSITIONED, offsets, opposite))) {
            return NO_LAYOUT;
        }
        return this.layoutWithWidth(parts, Target.POSITIONED, offsets, opposite);
    }

    public MeasuredLayout layoutBossBar(List<GuiTitlePart> parts, Map<String, Integer> fontBaselines) {
        if (!this.requiresLayout(parts, Target.BOSS_BAR, fontBaselines, Shadow.OFF)) {
            return NO_LAYOUT;
        }
        return this.layoutWithWidth(parts, Target.BOSS_BAR, fontBaselines, Shadow.OFF);
    }

    private MeasuredLayout layoutWithWidth(List<GuiTitlePart> parts, Target target, Map<String, Integer> fontBaselines, Shadow opposite) {
        List<Layer> layers = new ArrayList<>();
        FontData.Font defaultFont = this.fonts.font("minecraft:default");
        int remainingBytes = ContainerTitleCodec.MAX_BYTES;
        // Java 光标允许负间距回退；每个基岩图层的光标只能通过间距字符向右移动。
        double javaCursor = ORIGIN;
        double width = 0;
        for (int partIndex = 0; partIndex < parts.size(); partIndex++) {
            GuiTitlePart part = parts.get(partIndex);
            int baseline = fontBaselines.getOrDefault(part.font(), 0);
            boolean shadowOverride = part.shadow() == opposite;
            FontData.Font font = this.fonts.font(part.font());
            boolean allowMissingGlyph = "minecraft:default".equals(part.font()) || fontBaselines.containsKey(part.font());
            int boldAdvance = (part.decorations() & 2) != 0 ? 1 : 0;
            for (int offset = 0; offset < part.text().length();) {
                int codepoint = part.text().codePointAt(offset);
                offset += Character.charCount(codepoint);
                FontGlyph glyph = font.rendering().get(codepoint);
                boolean unicode = this.hudFonts && (font.hudUnicode(codepoint) || (codepoint == ' ' && font.hudUnicode('A')));
                // The default unihex font supplies Java advances without exporting a Bedrock bitmap.
                if (glyph == null && (unicode || allowMissingGlyph)) {
                    glyph = new FontGlyph(codepoint, FontGlyph.NATIVE_FALLBACK, 0);
                }
                if (glyph == null || codepoint == '\n' || codepoint == '§') {
                    throw new UnsupportedOperationException("Unknown title glyph metrics: " + part.font() + "/" + Integer.toHexString(codepoint));
                }
                if (!glyph.visible()) {
                    float advance = font.advance(codepoint, false);
                    width += advance;
                    javaCursor += advance;
                    continue;
                }
                boolean nativeFallback = glyph.renderedAdvance() == FontGlyph.NATIVE_FALLBACK;
                if (nativeFallback && !unicode) {
                    FontGlyph ordinary = defaultFont.rendering().get(codepoint);
                    if (ordinary != null && ordinary.character() == codepoint && ordinary.renderedAdvance() >= 0) {
                        glyph = ordinary;
                    }
                }
                double renderedAdvance = unicode ? unicodeAdvance(codepoint, boldAdvance) : glyph.renderedAdvance() + boldAdvance;
                boolean unknownWidth = unicode ? renderedAdvance < 0 : nativeFallback && glyph.renderedAdvance() < 0;
                if (!unicode && glyph.renderedAdvance() < 0 && !unknownWidth) {
                    throw new UnsupportedOperationException("Unknown Bedrock title glyph width: " + part.font() + "/" + Integer.toHexString(codepoint));
                }
                // 用 bearing 修正落笔位置，使两端字形的可见部分对齐。
                double position = (this.hudFonts ? Math.rint(2 * javaCursor) * 0.5 : Math.rint(javaCursor)) - glyph.bearing();
                double advance = unknownWidth ? 0 : renderedAdvance;
                Face face = !this.hudFonts ? Face.LEGACY : (unicode ? Face.UNICODE : (glyph.bearing() != 0 || glyph.character() != codepoint ? Face.ATLAS : Face.DEFAULT));
                double inkStart = position + glyph.bearing();
                // 实际原生字宽未知时，以当前字符结束本层。之后的字符仍按各自的 Java 光标定位。
                double inkEnd = unknownWidth ? Double.POSITIVE_INFINITY : inkStart + advance - 1;
                Layer selected = unknownWidth ? null : findLayer(layers, inkStart, inkEnd, position, baseline, face, shadowOverride);
                if (selected == null) {
                    selected = new Layer(baseline, position, face, shadowOverride, inkStart, inkEnd);
                    layers.add(selected);
                    remainingBytes -= ContainerTitleCodec.ENTRY_BYTES;
                } else {
                    selected.addSpan(inkStart, inkEnd);
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
                selected.closed = unknownWidth || (target == Target.BOSS_BAR && glyph.character() == '%');
                selected.cursor = position + advance;
                float javaAdvance = this.hudFonts || nativeFallback ? font.advance(codepoint, boldAdvance != 0) : font.advance(codepoint, false) + boldAdvance;
                javaCursor += javaAdvance;
                width += javaAdvance;
            }
        }
        // 有自定义字形但没有可见内容时，保留一个空图层，区别于无需转换的空结果。
        int pixelWidth = (int) Math.ceil(width);
        if (layers.isEmpty()) {
            return new MeasuredLayout(List.of(List.of()), pixelWidth);
        }
        List<List<Run>> result = new ArrayList<>(layers.size());
        int[] baselines = fontBaselines.isEmpty() ? null : new int[layers.size()];
        int[] halfLeft = new int[layers.size()];
        BitSet unicodeLayers = new BitSet();
        BitSet shadowOverrides = null;
        for (Layer layer : layers) {
            layer.flushRun();
            halfLeft[result.size()] = (int) ((layer.start - ORIGIN) * 2);
            if (layer.face == Face.UNICODE) {
                unicodeLayers.set(result.size());
            }
            if (layer.shadowOverride) {
                if (shadowOverrides == null) {
                    shadowOverrides = new BitSet();
                }
                shadowOverrides.set(result.size());
            }
            if (baselines != null) {
                baselines[result.size()] = layer.baseline;
            }
            result.add(layer.runs);
        }
        return new MeasuredLayout(result, pixelWidth, baselines, halfLeft, unicodeLayers, shadowOverrides);
    }

    private static Layer findLayer(List<Layer> layers, double inkStart, double inkEnd, double position, int baseline, Face face, boolean shadowOverride) {
        // 从顶层向下找到最低的可用层。遇到相交字形就停止，保持负间距回退时的遮盖顺序。
        Layer candidate = null;
        // 原生文字标签混入私用区间距字符可能切换整层字体。
        boolean canPad = face == Face.LEGACY || face == Face.ATLAS;
        for (int i = layers.size() - 1; i >= 0; i--) {
            Layer layer = layers.get(i);
            if (layer.intersects(inkStart, inkEnd)) {
                break;
            }
            if (layer.closed || layer.face != face || layer.baseline != baseline || layer.shadowOverride != shadowOverride) {
                continue;
            }
            double gap = position - layer.cursor;
            if (gap == 0 || (canPad && gap >= 2 && gap == Math.rint(gap))) {
                candidate = layer;
            }
        }
        return candidate;
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

    private static double unicodeAdvance(int codepoint, int bold) {
        return codepoint >= ' ' && codepoint <= '~' ? (UNICODE_ASCII_HALF_ADVANCES[codepoint - ' '] + bold) * 0.5 : -1;
    }

    private boolean requiresLayout(List<GuiTitlePart> parts, Target target, Map<String, Integer> fontBaselines, Shadow opposite) {
        for (GuiTitlePart part : parts) {
            if (!part.text().isEmpty() && (part.shadow() == opposite || fontBaselines.containsKey(part.font()))) {
                return true;
            }
            FontData.Font font = this.fonts.font(part.font());
            if (!this.hudFonts && font.rendering().isEmpty()) {
                continue;
            }
            for (int offset = 0; offset < part.text().length();) {
                int codepoint = part.text().codePointAt(offset);
                offset += Character.charCount(codepoint);
                if (this.hudFonts && font.hudUnicode(codepoint)) {
                    return true;
                }
                FontGlyph glyph = font.rendering().get(codepoint);
                if (glyph == null) {
                    continue;
                }
                boolean required = switch (target) {
                    case POSITIONED -> glyph.character() != codepoint || glyph.renderedAdvance() == FontGlyph.NATIVE_FALLBACK;
                    case BOSS_BAR -> {
                        float advance = font.advance(codepoint, false);
                        yield (this.hudFonts && glyph.character() != codepoint) || advance < 0 || (!glyph.visible() && advance > 0)
                                || glyph.renderedAdvance() == FontGlyph.NATIVE_FALLBACK;
                    }
                };
                if (required) {
                    return true;
                }
            }
        }
        return false;
    }

    public record MeasuredLayout(List<List<Run>> layers, int width, int[] baselines, int[] halfLeft, BitSet unicodeLayers, BitSet shadowOverrides) {
        private MeasuredLayout(List<List<Run>> layers, int width) {
            this(layers, width, null, null, null, null);
        }

        public boolean unicode(int layer) {
            return this.unicodeLayers != null && this.unicodeLayers.get(layer);
        }

        public boolean shadowOverride(int layer) {
            return this.shadowOverrides != null && this.shadowOverrides.get(layer);
        }
    }

    // part 是原始标题片段的索引，-1 表示不继承片段样式的间距文本。
    public record Run(int part, String text) {
    }

    private enum Target {
        POSITIONED, BOSS_BAR
    }

    private enum Face {
        LEGACY, DEFAULT, UNICODE, ATLAS
    }

    private static final class Layer {
        private final int baseline;
        private final double start;
        private final Face face;
        private final boolean shadowOverride;
        private final List<Run> runs = new ArrayList<>(2);
        private double[] spans;
        private int spanSize;
        private double minimumInkStart;
        private double maximumInkEnd;
        private final StringBuilder text = new StringBuilder();
        private int part;
        private double cursor;
        private boolean closed;

        private Layer(int baseline, double start, Face face, boolean shadowOverride, double inkStart, double inkEnd) {
            this.baseline = baseline;
            this.start = start;
            this.face = face;
            this.shadowOverride = shadowOverride;
            this.cursor = start;
            this.minimumInkStart = inkStart;
            this.maximumInkEnd = inkEnd;
        }

        private void addSpan(double start, double end) {
            // 单字层的边界就是它的唯一区间；第二个字形到来时才需要记录各自的区间。
            if (this.spans == null) {
                this.spans = new double[16];
                this.spans[0] = this.minimumInkStart;
                this.spans[1] = this.maximumInkEnd;
                this.spanSize = 2;
            } else if (this.spanSize == this.spans.length) {
                this.spans = Arrays.copyOf(this.spans, this.spans.length * 2);
            }
            this.spans[this.spanSize++] = start;
            this.spans[this.spanSize++] = end;
            this.minimumInkStart = Math.min(this.minimumInkStart, start);
            this.maximumInkEnd = Math.max(this.maximumInkEnd, end);
        }

        private boolean intersects(double start, double end) {
            if (this.spans == null) {
                return start < this.maximumInkEnd && end > this.minimumInkStart;
            }
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
