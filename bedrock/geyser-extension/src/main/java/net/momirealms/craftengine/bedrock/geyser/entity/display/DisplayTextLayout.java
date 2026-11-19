package net.momirealms.craftengine.bedrock.geyser.entity.display;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.momirealms.craftengine.bedrock.GuiTitlePart;
import net.momirealms.craftengine.bedrock.geyser.util.GeyserTextParts;
import net.momirealms.craftengine.bedrock.text.FontGlyph;
import org.geysermc.geyser.util.SignUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

record DisplayTextLayout(List<Line> lines, int width) {

    private static final String DEFAULT_FONT = "minecraft:default";

    static DisplayTextLayout wrap(List<GuiTitlePart> parts, Map<String, Map<Integer, FontGlyph>> metrics, int maxWidth, String locale) {
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
            Map<Integer, FontGlyph> font = metrics.get(part.font());
            boolean bold = (part.decorations() & 2) != 0;
            plain &= part.font().equals(DEFAULT_FONT) && part.color() == 0xFFFFFF && part.decorations() == 0;
            for (int index = partStarts[partIndex]; index < partStarts[partIndex + 1];) {
                int codepoint = text.codePointAt(index);
                if (codepoint != '\n') {
                    FontGlyph glyph = font instanceof Int2ObjectMap<FontGlyph> fast ? fast.get(codepoint) : (font == null ? null : font.get(codepoint));
                    advances[index] = (glyph == null ? nativeWidth(codepoint) : glyph.advance()) + (bold && (glyph == null || glyph.visible()) ? 1 : 0);
                    plain &= codepoint >= ' ' && codepoint <= '~' && (glyph == null || glyph.character() == codepoint);
                }
                index += Character.charCount(codepoint);
            }
        }

        List<Line> lines = new ArrayList<>();
        int firstPart = 0;
        int maximum = 0;
        int start = 0;
        while (start <= text.length()) {
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
            while (firstPart < parts.size() && partStarts[firstPart + 1] <= start) {
                firstPart++;
            }
            String line = plain ? text.substring(start, end) : serializeLine(parts, partStarts, firstPart, start, end, locale);
            lines.add(new Line(line, lineWidth));
            maximum = Math.max(maximum, lineWidth);
            start = next;
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

    private static int nativeWidth(int codepoint) {
        return Character.isBmpCodePoint(codepoint) ? SignUtils.getCharacterWidth((char) codepoint) : 6;
    }

    static DisplayTextLayout wrapLegacy(String text, int maxWidth) {
        List<Line> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        StringBuilder format = new StringBuilder();
        int width = 0;
        int maximum = 0;
        boolean bold = false;
        for (int index = 0; index < text.length();) {
            int character = text.codePointAt(index);
            index += Character.charCount(character);
            if (character == '§' && index < text.length() && Character.isBmpCodePoint(text.codePointAt(index))) {
                char code = text.charAt(index++);
                if (code == 'r' || "0123456789abcdef".indexOf(code) >= 0) {
                    format.setLength(0);
                    bold = false;
                }
                bold |= code == 'l';
                format.append('§').append(code);
                line.append('§').append(code);
                continue;
            }
            int advance = nativeWidth(character) + (bold ? 1 : 0);
            if (character == '\n' || (width > 0 && width + advance > maxWidth)) {
                lines.add(new Line(line.toString(), width));
                maximum = Math.max(maximum, width);
                line.setLength(0);
                line.append(format);
                width = 0;
            }
            if (character != '\n') {
                line.appendCodePoint(character);
                width += advance;
            }
        }
        lines.add(new Line(line.toString(), width));
        return new DisplayTextLayout(lines, Math.max(maximum, width));
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
