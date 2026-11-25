package net.momirealms.craftengine.bedrock.geyser.entity.display;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.momirealms.craftengine.bedrock.GuiTitlePart;
import net.momirealms.craftengine.bedrock.geyser.util.GeyserTextParts;
import net.momirealms.craftengine.bedrock.geyser.glyph.GeyserGlyphData;
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
        List<GuiTitlePart> expanded = null;
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
                    expanded.add(part);
                }
                continue;
            }
            if (expanded == null) {
                expanded = new ArrayList<>(parts.subList(0, i));
            }
            String font = part.font();
            int color = part.color();
            int decorations = part.decorations();
            int start = 0;
            while (marker >= 0) {
                GuiTitlePart.append(expanded, font, text.substring(start, marker), color, decorations);
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
            GuiTitlePart.append(expanded, font, text.substring(start), color, decorations);
        }
        return new FormattedParts(expanded == null ? parts : expanded, trailingFormat);
    }

    static DisplayTextLayout wrap(List<GuiTitlePart> source, GeyserGlyphData glyphs, int maxWidth, String locale) {
        FormattedParts formatted = expandFormatting(source);
        List<GuiTitlePart> parts = formatted.parts();
        StringBuilder text = new StringBuilder();
        int[] partStarts = new int[parts.size() + 1];
        for (int index = 0; index < parts.size(); index++) {
            text.append(parts.get(index).text());
            partStarts[index + 1] = text.length();
        }
        float[] advances = new float[text.length()];
        boolean plain = true;
        for (int partIndex = 0; partIndex < parts.size(); partIndex++) {
            GuiTitlePart part = parts.get(partIndex);
            FontData.Font font = glyphs.font(part.font());
            Int2ObjectMap<FontGlyph> rendering = font.rendering();
            boolean bold = (part.decorations() & 2) != 0;
            plain &= part.font().equals(DEFAULT_FONT) && part.color() == 0xFFFFFF && part.decorations() == 0;
            for (int index = partStarts[partIndex]; index < partStarts[partIndex + 1];) {
                int codepoint = part.text().codePointAt(index - partStarts[partIndex]);
                if (codepoint >= Character.MIN_SURROGATE && codepoint <= Character.MAX_SURROGATE) {
                    codepoint = 0xfffd;
                    text.setCharAt(index, (char) codepoint);
                }
                if (codepoint != '\n') {
                    FontGlyph glyph = rendering.get(codepoint);
                    advances[index] = font.advance(codepoint, bold);
                    plain &= codepoint >= ' ' && codepoint <= '~' && (glyph == null || glyph.character() == codepoint);
                }
                index += Character.charCount(codepoint);
            }
        }

        boolean plainText = plain;
        return split(text, advances, maxWidth, formatted.trailingFormat(), new LineText() {
            private int firstPart;

            @Override
            public String slice(int start, int end) {
                if (plainText) {
                    return text.substring(start, end);
                }
                while (this.firstPart < parts.size() && partStarts[this.firstPart + 1] <= start) {
                    this.firstPart++;
                }
                return serializeLine(parts, partStarts, this.firstPart, start, end, locale);
            }
        });
    }

    @FunctionalInterface
    private interface LineText {
        String slice(int start, int end);
    }

    private static DisplayTextLayout split(StringBuilder text, float[] advances, int maxWidth, boolean trailingFormat, LineText contents) {
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
                int codepoint = text.codePointAt(end);
                if (codepoint == '\n') {
                    next = end + 1;
                    break;
                }
                if (codepoint == ' ') {
                    lastSpace = end;
                    widthAtSpace = width;
                }
                float advance = advances[end];
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
            lines.add(new Line(contents.slice(start, end), lineWidth));
            maximum = Math.max(maximum, lineWidth);
            start = next;
        }
        // 原版保留显式换行或未消费格式码产生的空尾行，自动换行吞掉的末尾空格不产生新行。
        if (start == text.length() && (trailingFormat || (!text.isEmpty() && text.charAt(start - 1) == '\n'))) {
            lines.add(new Line("", 0));
        }
        return new DisplayTextLayout(lines, maximum);
    }

    private static String serializeLine(List<GuiTitlePart> parts, int[] starts, int first, int start, int end, String locale) {
        List<GuiTitlePart> line = new ArrayList<>();
        for (int index = first; index < parts.size() && starts[index] < end; index++) {
            GuiTitlePart part = parts.get(index);
            String text = part.text().substring(Math.max(0, start - starts[index]), Math.min(part.text().length(), end - starts[index]));
            GuiTitlePart.append(line, part.font(), text, part.color(), part.decorations());
        }
        return GeyserTextParts.serializeParts(line, locale);
    }

    static DisplayTextLayout wrapLegacy(String text, int maxWidth, FontData.Font font) {
        StringBuilder visible = new StringBuilder();
        float[] advances = new float[text.length()];
        int[] rawOffsets = new int[text.length() + 1];
        String[] prefixes = new String[text.length() + 1];
        String format = "";
        String color = "";
        int rawStart = 0;
        String prefix = "";
        boolean bold = false;
        boolean italic = false;
        boolean obfuscated = false;
        for (int index = 0; index < text.length();) {
            int character = text.codePointAt(index);
            index += Character.charCount(character);
            if (character == '§') {
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
            rawOffsets[offset] = rawStart;
            prefixes[offset] = prefix;
            advances[offset] = font.advance(character, bold);
            visible.appendCodePoint(character);
            rawStart = index;
            prefix = format;
        }
        rawOffsets[visible.length()] = text.length();
        prefixes[visible.length()] = format;
        return split(visible, advances, maxWidth, rawStart < text.length(), (start, end) -> {
            String line = text.substring(rawOffsets[start], rawOffsets[end]);
            return start == 0 || line.isEmpty() ? line : prefixes[start] + line;
        });
    }

    record Line(String text, int width, boolean visible) {
        Line(String text, int width) {
            this(text, width, hasVisibleText(text));
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
