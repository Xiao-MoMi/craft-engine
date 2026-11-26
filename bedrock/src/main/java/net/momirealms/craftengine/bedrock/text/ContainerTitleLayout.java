package net.momirealms.craftengine.bedrock.text;

import net.momirealms.craftengine.bedrock.GuiTitlePart;

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

    public boolean supportsHudFonts() {
        return this.hudFonts;
    }

    public List<List<Run>> layout(List<GuiTitlePart> parts) {
        return this.layout(parts, false);
    }

    public List<List<Run>> layout(List<GuiTitlePart> parts, boolean force) {
        // 没有需要映射的自定义字形时，交给原生标题渲染。
        if (!force && !this.requiresLayout(parts, Target.CONTAINER, Map.of())) {
            return List.of();
        }
        return this.layoutWithWidth(parts).layers();
    }

    public MeasuredLayout layoutWithWidth(List<GuiTitlePart> parts) {
        return this.layoutWithWidth(parts, Target.CONTAINER);
    }

    public List<List<Run>> layoutTitle(List<GuiTitlePart> parts, boolean force) {
        if (parts.isEmpty() || (!force && !this.requiresLayout(parts, Target.TITLE, Map.of()))) {
            return List.of();
        }
        return this.layoutWithWidth(parts, Target.TITLE).layers();
    }

    public MeasuredLayout layoutBossBar(List<GuiTitlePart> parts, boolean leftAnchored) {
        return this.layoutBossBar(parts, leftAnchored, Map.of());
    }

    public MeasuredLayout positioned(List<GuiTitlePart> parts, Map<String, Integer> offsets, boolean force) {
        if (parts.isEmpty() || (!force && !this.requiresLayout(parts, Target.POSITIONED, offsets))) {
            return NO_LAYOUT;
        }
        return this.layoutWithWidth(parts, Target.POSITIONED, offsets);
    }

    public MeasuredLayout layoutBossBar(List<GuiTitlePart> parts, boolean leftAnchored, Map<String, Integer> fontBaselines) {
        Target target = leftAnchored ? Target.BOSS_BAR_LEFT : Target.BOSS_BAR;
        if (!this.requiresLayout(parts, target, fontBaselines)) {
            return NO_LAYOUT;
        }
        return this.layoutWithWidth(parts, target, fontBaselines);
    }

    private MeasuredLayout layoutWithWidth(List<GuiTitlePart> parts, Target target) {
        return this.layoutWithWidth(parts, target, Map.of());
    }

    private MeasuredLayout layoutWithWidth(List<GuiTitlePart> parts, Target target, Map<String, Integer> fontBaselines) {
        List<Layer> layers = new ArrayList<>();
        boolean nativeFallbackAllowed = target == Target.POSITIONED || target == Target.BOSS_BAR_LEFT;
        boolean faces = this.hudFonts && nativeFallbackAllowed;
        boolean absolute = target == Target.POSITIONED || faces;
        FontData.Font defaultFont = this.fonts.font("minecraft:default");
        int remainingBytes = ContainerTitleCodec.MAX_BYTES;
        // Java 光标允许负间距回退；每个基岩图层的光标只能通过间距字符向右移动。
        double javaCursor = ORIGIN;
        double width = 0;
        for (int partIndex = 0; partIndex < parts.size(); partIndex++) {
            GuiTitlePart part = parts.get(partIndex);
            int baseline = fontBaselines.getOrDefault(part.font(), 0);
            FontData.Font font = this.fonts.font(part.font());
            int boldAdvance = (part.decorations() & 2) != 0 ? 1 : 0;
            for (int offset = 0; offset < part.text().length();) {
                int codepoint = part.text().codePointAt(offset);
                offset += Character.charCount(codepoint);
                FontGlyph glyph = font.rendering().get(codepoint);
                boolean unicode = faces && (font.hudUnicode(codepoint) || (codepoint == ' ' && font.hudUnicode('A')));
                // The default unihex font supplies Java advances without exporting a Bedrock bitmap.
                if (glyph == null && (unicode || (target == Target.POSITIONED && ("minecraft:default".equals(part.font()) || fontBaselines.containsKey(part.font()))))) {
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
                boolean nativeFallback = nativeFallbackAllowed && glyph.renderedAdvance() == FontGlyph.NATIVE_FALLBACK;
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
                double position = (faces ? Math.rint(2 * javaCursor) * 0.5 : Math.rint(javaCursor)) - glyph.bearing();
                double advance = unknownWidth ? 0 : renderedAdvance;
                Face face = !faces ? Face.LEGACY : (unicode ? Face.UNICODE : (glyph.bearing() != 0 || glyph.character() != codepoint ? Face.ATLAS : Face.DEFAULT));
                double inkStart = position + glyph.bearing();
                // 实际原生字宽未知时，以当前字符结束本层。之后的字符仍按各自的 Java 光标定位。
                double inkEnd = unknownWidth ? Double.POSITIVE_INFINITY : inkStart + advance - 1;
                Layer selected = unknownWidth ? null : findLayer(layers, inkStart, inkEnd, position, baseline, face);
                if (selected == null) {
                    if (!absolute && (position < 0 || position == 1)) {
                        throw new UnsupportedOperationException("Title exceeds left margin");
                    }
                    selected = new Layer(baseline, absolute ? position : 0, face, inkStart, inkEnd);
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
                selected.closed = unknownWidth || ((target == Target.BOSS_BAR || target == Target.BOSS_BAR_LEFT) && glyph.character() == '%');
                selected.cursor = position + advance;
                float javaAdvance = faces || nativeFallback ? font.advance(codepoint, boldAdvance != 0) : font.advance(codepoint, false) + boldAdvance;
                javaCursor += javaAdvance;
                width += javaAdvance;
            }
        }
        // 有自定义字形但没有可见内容时，保留一个空图层，区别于无需转换的空结果。
        int pixelWidth = (int) Math.ceil(width);
        if (target != Target.BOSS_BAR_LEFT && target != Target.POSITIONED) {
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
        int[] baselines = fontBaselines.isEmpty() ? null : new int[layers.size()];
        int[] halfLeft = absolute ? new int[layers.size()] : null;
        BitSet unicodeLayers = new BitSet();
        for (Layer layer : layers) {
            layer.flushRun();
            if (halfLeft != null) {
                halfLeft[result.size()] = (int) ((layer.start - ORIGIN) * 2);
            }
            if (layer.face == Face.UNICODE) {
                unicodeLayers.set(result.size());
            }
            if (baselines != null) {
                baselines[result.size()] = layer.baseline;
            }
            result.add(layer.runs);
        }
        return new MeasuredLayout(result, pixelWidth, baselines, halfLeft, unicodeLayers);
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

    private static Layer findLayer(List<Layer> layers, double inkStart, double inkEnd, double position, int baseline, Face face) {
        // 从顶层向下找到最低的可用层。遇到相交字形就停止，保持负间距回退时的遮盖顺序。
        Layer candidate = null;
        // 原生文字标签混入私用区间距字符可能切换整层字体。
        boolean canPad = face == Face.LEGACY || face == Face.ATLAS;
        for (int i = layers.size() - 1; i >= 0; i--) {
            Layer layer = layers.get(i);
            if (layer.intersects(inkStart, inkEnd)) {
                break;
            }
            if (layer.closed || layer.face != face || layer.baseline != baseline) {
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

    private boolean requiresLayout(List<GuiTitlePart> parts, Target target, Map<String, Integer> fontBaselines) {
        boolean faces = this.hudFonts && (target == Target.POSITIONED || target == Target.BOSS_BAR_LEFT);
        for (GuiTitlePart part : parts) {
            if (!fontBaselines.isEmpty() && !part.text().isEmpty() && fontBaselines.containsKey(part.font())) {
                return true;
            }
            FontData.Font font = this.fonts.font(part.font());
            if (!faces && font.rendering().isEmpty()) {
                continue;
            }
            for (int offset = 0; offset < part.text().length();) {
                int codepoint = part.text().codePointAt(offset);
                offset += Character.charCount(codepoint);
                if (faces && font.hudUnicode(codepoint)) {
                    return true;
                }
                FontGlyph glyph = font.rendering().get(codepoint);
                if (glyph == null) {
                    continue;
                }
                boolean required = switch (target) {
                    case POSITIONED -> glyph.character() != codepoint || glyph.renderedAdvance() == FontGlyph.NATIVE_FALLBACK;
                    case CONTAINER -> glyph.character() != codepoint;
                    case TITLE -> font.advance(codepoint, false) < 0;
                    case BOSS_BAR, BOSS_BAR_LEFT -> {
                        float advance = font.advance(codepoint, false);
                        yield (faces && glyph.character() != codepoint) || advance < 0 || (!glyph.visible() && advance > 0)
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

    public record MeasuredLayout(List<List<Run>> layers, int width, int[] baselines, int[] halfLeft, BitSet unicodeLayers) {
        public MeasuredLayout(List<List<Run>> layers, int width, int[] baselines, int[] left) {
            this(layers, width, baselines, left == null ? null : Arrays.stream(left).map(pixel -> pixel * 2).toArray(), null);
        }

        public int[] left() {
            return this.halfLeft == null ? null : Arrays.stream(this.halfLeft).map(value -> Math.floorDiv(value, 2)).toArray();
        }

        public boolean unicode(int layer) {
            return this.unicodeLayers != null && this.unicodeLayers.get(layer);
        }

        public MeasuredLayout(List<List<Run>> layers, int width, int[] baselines) {
            this(layers, width, baselines, null, null);
        }

        public MeasuredLayout(List<List<Run>> layers, int width) {
            this(layers, width, null);
        }
    }

    // part 是原始标题片段的索引，-1 表示不继承片段样式的间距文本。
    public record Run(int part, String text) {
    }

    private enum Target {
        CONTAINER, TITLE, BOSS_BAR, BOSS_BAR_LEFT, POSITIONED
    }

    private enum Face {
        LEGACY, DEFAULT, UNICODE, ATLAS
    }

    private static final class Layer {
        private final int baseline;
        private final double start;
        private final Face face;
        private final List<Run> runs = new ArrayList<>(2);
        private double[] spans;
        private int spanSize;
        private double minimumInkStart;
        private double maximumInkEnd;
        private final StringBuilder text = new StringBuilder();
        private int part;
        private double cursor;
        private boolean closed;

        private Layer(int baseline, double start, Face face, double inkStart, double inkEnd) {
            this.baseline = baseline;
            this.start = start;
            this.face = face;
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
