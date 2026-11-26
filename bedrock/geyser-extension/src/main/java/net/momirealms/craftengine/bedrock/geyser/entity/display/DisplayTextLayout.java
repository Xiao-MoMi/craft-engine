package net.momirealms.craftengine.bedrock.geyser.entity.display;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.momirealms.craftengine.bedrock.GuiTitlePart;
import net.momirealms.craftengine.bedrock.geyser.glyph.GeyserGlyphData;
import net.momirealms.craftengine.bedrock.geyser.glyph.GeyserGlyphSerializer;
import net.momirealms.craftengine.bedrock.geyser.util.GeyserTextParts;
import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;
import net.momirealms.craftengine.bedrock.text.FontData;
import net.momirealms.craftengine.bedrock.text.FontGlyph;

import java.util.ArrayList;
import java.util.List;

public record DisplayTextLayout(List<Line> lines, int width) {

    private static final String DEFAULT_FONT = "minecraft:default";
    private static final String JAVA_FORMATS = "0123456789abcdefklmnor";
    private static final int[] JAVA_COLORS = {
            0, 0x0000aa, 0x00aa00, 0x00aaaa, 0xaa0000, 0xaa00aa, 0xffaa00, 0xaaaaaa,
            0x555555, 0x5555ff, 0x55ff55, 0x55ffff, 0xff5555, 0xff55ff, 0xffff55, 0xffffff
    };

    record FormattedParts(List<GuiTitlePart> parts, boolean trailingFormat) {
    }

    static FormattedParts expandFormatting(List<GuiTitlePart> parts) {
        GuiTitlePart.Builder expanded = null;
        boolean trailingFormat = false;
        for (int i = 0; i < parts.size(); i++) {
            GuiTitlePart part = parts.get(i);
            String text = part.text();
            if (text.isEmpty()) {
                continue;
            }
            int marker = text.indexOf('§');
            if (marker < 0) {
                trailingFormat = false;
                if (expanded != null) {
                    expanded.addComponent(part);
                }
                continue;
            }
            if (expanded == null) {
                expanded = new GuiTitlePart.Builder(new ArrayList<>(parts.subList(0, i)));
            }
            String font = part.font();
            int color = part.color();
            int decorations = part.decorations();
            int start = 0;
            while (marker >= 0) {
                expanded.append(font, text.substring(start, marker), color, decorations);
                start = Math.min(text.length(), marker + 2);
                if (marker + 1 < text.length()) {
                    int format = JAVA_FORMATS.indexOf(Character.toLowerCase(text.charAt(marker + 1)));
                    if (format >= 0 && format < 16) {
                        color = JAVA_COLORS[format];
                        decorations = 0;
                    } else if (format >= 16 && format < 21) {
                        decorations |= 1 << (format - 16);
                    } else if (format == 21) {
                        font = DEFAULT_FONT;
                        color = 0xffffff;
                        decorations = 0;
                    }
                }
                marker = text.indexOf('§', start);
            }
            trailingFormat = start == text.length();
            expanded.append(font, text.substring(start), color, decorations);
        }
        return new FormattedParts(expanded == null ? parts : expanded.finish(), trailingFormat);
    }

    static DisplayTextLayout wrap(List<GuiTitlePart> source, GeyserGlyphData glyphs, int maxWidth, String locale) {
        FormattedParts formatted = expandFormatting(source);
        List<GuiTitlePart> parts = formatted.parts();
        if (parts.size() == 1) {
            GuiTitlePart part = parts.getFirst();
            if (part.font().equals(DEFAULT_FONT) && glyphs.verticalOffset(DEFAULT_FONT) == 0 && part.color() == 0xffffff && part.decorations() == 0) {
                FontData.Font font = glyphs.font(DEFAULT_FONT);
                if (isPlainText(part.text(), font.rendering())) {
                    return splitPlain(part.text(), maxWidth, font, formatted.trailingFormat());
                }
            }
        }
        StringBuilder text = new StringBuilder();
        int[] partStarts = new int[parts.size() + 1];
        for (int index = 0; index < parts.size(); index++) {
            text.append(parts.get(index).text());
            partStarts[index + 1] = text.length();
        }
        float[] advances = new float[text.length()];
        boolean plain = true;
        boolean needsPositioning = false;
        for (int partIndex = 0; partIndex < parts.size(); partIndex++) {
            GuiTitlePart part = parts.get(partIndex);
            FontData.Font font = glyphs.font(part.font());
            Int2ObjectMap<FontGlyph> rendering = font.rendering();
            boolean bold = (part.decorations() & 2) != 0;
            boolean shifted = glyphs.verticalOffset(part.font()) != 0;
            needsPositioning |= shifted;
            plain &= !shifted && part.font().equals(DEFAULT_FONT) && part.color() == 0xFFFFFF && part.decorations() == 0;
            for (int index = partStarts[partIndex]; index < partStarts[partIndex + 1];) {
                int codepoint = part.text().codePointAt(index - partStarts[partIndex]);
                if (codepoint >= Character.MIN_SURROGATE && codepoint <= Character.MAX_SURROGATE) {
                    codepoint = 0xfffd;
                    text.setCharAt(index, (char) codepoint);
                }
                if (codepoint != '\n') {
                    advances[index] = font.advance(codepoint, bold);
                    needsPositioning |= advances[index] < 0;
                    plain = plain && isPlainGlyph(codepoint, rendering);
                }
                index += Character.charCount(codepoint);
            }
        }

        boolean plainText = plain;
        boolean positionLines = needsPositioning;
        return split(text, (index, codepoint) -> advances[index], maxWidth, formatted.trailingFormat(), new LineText() {
            private int firstPart;

            @Override
            public Line slice(int start, int end, int width) {
                if (plainText) {
                    return new Line(text.substring(start, end), width);
                }
                while (this.firstPart < parts.size() && partStarts[this.firstPart + 1] <= start) {
                    this.firstPart++;
                }
                return serializeLine(parts, partStarts, this.firstPart, start, end, width, locale, glyphs, positionLines);
            }
        });
    }

    @FunctionalInterface
    private interface LineText {
        Line slice(int start, int end, int width);
    }

    private static boolean isPlainText(String text, Int2ObjectMap<FontGlyph> rendering) {
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character != '\n' && !isPlainGlyph(character, rendering)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPlainGlyph(int codepoint, Int2ObjectMap<FontGlyph> rendering) {
        if (codepoint < ' ' || codepoint > '~') {
            return false;
        }
        FontGlyph glyph = rendering.get(codepoint);
        return glyph == null || glyph.character() == codepoint;
    }

    @FunctionalInterface
    private interface Advance {
        float at(int index, int codepoint);
    }

    private static DisplayTextLayout splitPlain(String text, int maxWidth, FontData.Font font, boolean trailingFormat) {
        return split(text, (index, codepoint) -> font.advance(codepoint, false), maxWidth, trailingFormat, (start, end, width) -> new Line(text.substring(start, end), width));
    }

    private static DisplayTextLayout split(CharSequence text, Advance advances, int maxWidth, boolean trailingFormat, LineText contents) {
        List<Line> lines = new ArrayList<>();
        int maximum = 0;
        int start = 0;
        maxWidth = Math.max(1, maxWidth);
        while (start < text.length()) {
            int end = start;
            int next = text.length() + 1;
            int lastSpace = -1;
            float width = 0;
            float widthAtSpace = 0;
            boolean hasWidth = false;
            while (end < text.length()) {
                int codepoint = Character.codePointAt(text, end);
                if (codepoint == '\n') {
                    next = end + 1;
                    break;
                }
                if (codepoint == ' ') {
                    lastSpace = end;
                    widthAtSpace = width;
                }
                float advance = advances.at(end, codepoint);
                if (hasWidth && width + advance > maxWidth) {
                    if (lastSpace >= start) {
                        end = lastSpace;
                        width = widthAtSpace;
                        next = end + 1;
                    } else {
                        next = end;
                    }
                    break;
                }
                width += advance;
                hasWidth |= advance != 0;
                end += Character.charCount(codepoint);
            }
            int lineWidth = Math.max(0, (int) Math.ceil(width));
            lines.add(contents.slice(start, end, lineWidth));
            maximum = Math.max(maximum, lineWidth);
            start = next;
        }
        // 原版保留显式换行或未消费格式码产生的空尾行，自动换行吞掉的末尾空格不产生新行。
        if (start == text.length() && (trailingFormat || (!text.isEmpty() && text.charAt(start - 1) == '\n'))) {
            lines.add(new Line("", 0));
        }
        return new DisplayTextLayout(lines, maximum);
    }

    private static Line serializeLine(List<GuiTitlePart> parts,
                                      int[] starts,
                                      int first,
                                      int start,
                                      int end,
                                      int width,
                                      String locale,
                                      GeyserGlyphData fonts,
                                      boolean positionLines) {
        GuiTitlePart.Builder line = new GuiTitlePart.Builder(new ArrayList<>());
        for (int index = first; index < parts.size() && starts[index] < end; index++) {
            GuiTitlePart part = parts.get(index);
            String text = part.text().substring(Math.max(0, start - starts[index]), Math.min(part.text().length(), end - starts[index]));
            line.append(part.font(), text, part.color(), part.decorations());
        }
        List<GuiTitlePart> contents = line.finish();
        List<Piece> pieces = positionLines ? positionedPieces(contents, fonts, locale) : List.of();
        String text = GeyserTextParts.serializeParts(inlineSpacing(contents, fonts), locale);
        return new Line(text, width, Line.hasVisibleText(text), pieces);
    }

    static List<GuiTitlePart> inlineSpacing(List<GuiTitlePart> parts, GeyserGlyphData fonts) {
        GuiTitlePart.Builder output = null;
        double gap = 0;
        for (int index = 0; index < parts.size(); index++) {
            GuiTitlePart part = parts.get(index);
            FontData.Font font = fonts.font(part.font());
            String text = part.text();
            boolean bold = (part.decorations() & 2) != 0;
            int start = 0;
            for (int offset = 0; offset < text.length();) {
                int code = text.codePointAt(offset);
                int next = offset + Character.charCount(code);
                FontGlyph glyph = font.rendering().get(code);
                if (glyph != null && !glyph.visible()) {
                    if (output == null) {
                        output = new GuiTitlePart.Builder(new ArrayList<>(parts.subList(0, index)));
                    }
                    if (start < offset) {
                        appendInlineGap(output, gap);
                        gap = 0;
                        output.append(part.font(), text.substring(start, offset), part.color(), part.decorations());
                    }
                    gap += font.advance(code, bold);
                    start = next;
                }
                offset = next;
            }
            if (output != null && start < text.length()) {
                appendInlineGap(output, gap);
                gap = 0;
                output.append(part.font(), text.substring(start), part.color(), part.decorations());
            }
        }
        if (output == null) {
            return parts;
        }
        appendInlineGap(output, gap);
        return output.finish();
    }

    private static int appendInlineGap(GuiTitlePart.Builder output, double gap) {
        // 整行中两次可见绘制之间只能前进 0 或至少 2 像素，等距时向前取整。
        if (gap >= 1) {
            int width = Math.max(2, (int) Math.round(gap));
            StringBuilder text = new StringBuilder();
            ContainerTitleLayout.appendSpacers(text, width);
            output.append(GeyserGlyphSerializer.RESOLVED_FONT, text.toString(), 0xffffff, 0);
            return width;
        }
        return 0;
    }

    static List<Piece> positionedPieces(List<GuiTitlePart> parts, GeyserGlyphData fonts, String locale) {
        float width = 0;
        float gap = 0;
        boolean backtracks = false;
        boolean shifted = false;
        boolean onlyImages = true;
        for (GuiTitlePart part : parts) {
            FontData.Font font = fonts.font(part.font());
            boolean bold = (part.decorations() & 2) != 0;
            int offset = fonts.verticalOffset(part.font());
            for (int i = 0; i < part.text().length();) {
                int code = part.text().codePointAt(i);
                i += Character.charCount(code);
                FontGlyph glyph = font.rendering().get(code);
                float advance = font.advance(code, bold);
                width += advance;
                if (glyph != null && !glyph.visible()) {
                    gap += advance;
                } else {
                    shifted |= offset != 0;
                    backtracks |= gap < 0;
                    onlyImages &= glyph != null && glyph.bearing() != 0;
                    gap = 0;
                }
            }
        }
        if (!shifted && !backtracks && gap >= 0) {
            return List.of();
        }

        // Java 游标决定每段的起点，基岩字宽决定该段 DebugText 的中心。
        List<Piece> pieces = new ArrayList<>();
        GuiTitlePart.Builder run = new GuiTitlePart.Builder(new ArrayList<>());
        float cursor = -Math.max(0, width) * 0.5f;
        float start = cursor;
        float rendered = 0;
        int baseline = 0;
        gap = 0;
        for (GuiTitlePart part : parts) {
            FontData.Font font = fonts.font(part.font());
            int offset = fonts.verticalOffset(part.font());
            boolean bold = (part.decorations() & 2) != 0;
            String text = part.text();
            int slice = 0;
            for (int i = 0; i < text.length();) {
                int code = text.codePointAt(i);
                int next = i + Character.charCount(code);
                FontGlyph glyph = font.rendering().get(code);
                float advance = font.advance(code, bold);
                if (glyph != null && !glyph.visible()) {
                    run.append(part.font(), text.substring(slice, i), part.color(), part.decorations());
                    slice = next;
                    gap += advance;
                } else {
                    if (onlyImages || gap < 0 || offset != baseline) {
                        flushPiece(run, pieces, start, rendered, baseline, locale);
                        start = cursor;
                        rendered = 0;
                        baseline = offset;
                    } else if (gap > 0) {
                        rendered += appendInlineGap(run, gap);
                    }
                    gap = 0;
                    float glyphWidth = glyph != null && glyph.renderedAdvance() >= 0 ? glyph.renderedAdvance() + (bold ? 1 : 0) : advance;
                    rendered += glyphWidth;
                    if (onlyImages) {
                        run.append(part.font(), text.substring(i, next), part.color(), part.decorations());
                        slice = next;
                    }
                }
                cursor += advance;
                i = next;
            }
            run.append(part.font(), text.substring(slice), part.color(), part.decorations());
        }
        flushPiece(run, pieces, start, rendered, baseline, locale);
        return pieces;
    }

    private static void flushPiece(GuiTitlePart.Builder run, List<Piece> pieces, float start, float width, int baseline, String locale) {
        List<GuiTitlePart> contents = run.finish();
        if (!contents.isEmpty()) {
            String text = GeyserTextParts.serializeParts(contents, locale);
            pieces.add(new Piece(text, GeyserTextDisplayEntity.withoutTextColors(text), start + width * 0.5f, baseline));
            contents.clear();
        }
    }

    static DisplayTextLayout wrapLegacy(String text, int maxWidth, FontData.Font font) {
        if (text.indexOf('§') < 0) {
            return splitPlain(text, maxWidth, font, false);
        }
        StringBuilder visible = new StringBuilder();
        float[] advances = new float[text.length()];
        List<FormatChange> changes = new ArrayList<>();
        String format = "";
        String color = "";
        boolean formatChanged = false;
        boolean bold = false;
        boolean italic = false;
        boolean obfuscated = false;
        for (int index = 0; index < text.length();) {
            int rawIndex = index;
            int character = text.codePointAt(index);
            index += Character.charCount(character);
            if (character == '§') {
                formatChanged = true;
                if (index == text.length()) {
                    break;
                }
                char code = Character.toLowerCase(text.charAt(index++));
                switch (code) {
                    case 'k' -> obfuscated = true;
                    case 'l' -> bold = true;
                    case 'o' -> italic = true;
                    default -> {
                        color = code == 'r' ? "" : "§" + code;
                        if (code == 'r' || "0123456789abcdef".indexOf(code) >= 0) {
                            bold = false;
                            italic = false;
                            obfuscated = false;
                        }
                    }
                }
                format = color + (obfuscated ? "§k" : "") + (bold ? "§l" : "") + (italic ? "§o" : "");
                continue;
            }
            int offset = visible.length();
            if (formatChanged) {
                changes.add(new FormatChange(offset, rawIndex - offset, format));
                formatChanged = false;
            }
            advances[offset] = font.advance(character, bold);
            visible.appendCodePoint(character);
        }
        return split(visible, (index, codepoint) -> advances[index], maxWidth, formatChanged, new LineText() {
            private int nextChange;
            private int skipped;
            private String prefix = "";

            @Override
            public Line slice(int start, int end, int width) {
                int rawStart = this.rawOffset(start);
                String active = this.prefix;
                String line = text.substring(rawStart, this.rawOffset(end));
                return new Line(start == 0 || line.isEmpty() || active.isEmpty() ? line : active + line, width);
            }

            private int rawOffset(int visibleOffset) {
                // 紧邻断点的格式码属于下一行；末尾格式码随原始字符串保留。
                while (this.nextChange < changes.size() && changes.get(this.nextChange).visibleIndex() < visibleOffset) {
                    FormatChange change = changes.get(this.nextChange++);
                    this.skipped = change.skipped();
                    this.prefix = change.format();
                }
                return visibleOffset == visible.length() ? text.length() : visibleOffset + this.skipped;
            }
        });
    }

    private record FormatChange(int visibleIndex, int skipped, String format) {
    }

    record Piece(String text, String shadow, float center, int verticalOffset) {
    }

    record Line(String text, int width, boolean visible, List<Piece> pieces) {
        Line(String text, int width) {
            this(text, width, hasVisibleText(text), List.of());
        }

        private static boolean hasVisibleText(String text) {
            for (int index = 0; index < text.length();) {
                int character = text.codePointAt(index);
                index += Character.charCount(character);
                if (character == '§' && index < text.length()) {
                    index++;
                } else if (!Character.isWhitespace(character)) {
                    return true;
                }
            }
            return false;
        }
    }
}
