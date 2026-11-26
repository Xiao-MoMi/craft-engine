package net.momirealms.craftengine.bedrock;

import java.util.List;
import java.util.Map;

public record GuiTitlePart(String font, String text, int color, int decorations) {

    public static final class Builder {
        private static final String LEGACY_FORMATS = "0123456789abcdefklmnor";
        private static final int[] LEGACY_COLORS = {
                0, 0x0000aa, 0x00aa00, 0x00aaaa, 0xaa0000, 0xaa00aa, 0xffaa00, 0xaaaaaa,
                0x555555, 0x5555ff, 0x55ff55, 0x55ffff, 0xff5555, 0xff55ff, 0xffff55, 0xffffff
        };
        private final List<GuiTitlePart> parts;
        private StringBuilder joined;

        public Builder(List<GuiTitlePart> parts) {
            this.parts = parts;
        }

        public void append(String font, String text, int color, int decorations) {
            if (text.isEmpty()) {
                return;
            }
            if (!this.parts.isEmpty()) {
                GuiTitlePart previous = this.parts.getLast();
                if (previous.sameStyle(font, color, decorations)) {
                    if (this.joined == null) {
                        this.joined = new StringBuilder(previous.text);
                    }
                    this.joined.append(text);
                    return;
                }
            }
            this.addComponent(new GuiTitlePart(font, text, color, decorations));
        }

        public void appendFormatted(String font, String text, int color, int decorations) {
            int start = 0;
            int currentColor = color;
            int currentDecorations = decorations;
            for (int marker = text.indexOf('§'); marker >= 0; marker = text.indexOf('§', start)) {
                this.append(font, text.substring(start, marker), currentColor, currentDecorations);
                start = marker + 1;
                if (start == text.length()) {
                    return;
                }
                int code = LEGACY_FORMATS.indexOf(Character.toLowerCase(text.charAt(start++)));
                if (code >= 0 && code < 16) {
                    currentColor = LEGACY_COLORS[code];
                    currentDecorations = 0;
                } else if (code >= 16 && code < 21) {
                    currentDecorations |= 1 << (code - 16);
                } else if (code == 21) {
                    currentColor = color;
                    currentDecorations = decorations;
                }
            }
            this.append(font, start == 0 ? text : text.substring(start), currentColor, currentDecorations);
        }

        public void addComponent(GuiTitlePart part) {
            this.finish();
            this.parts.add(part);
        }

        public List<GuiTitlePart> finish() {
            if (this.joined != null) {
                GuiTitlePart previous = this.parts.getLast();
                this.parts.set(this.parts.size() - 1, new GuiTitlePart(previous.font, this.joined.toString(), previous.color, previous.decorations));
                this.joined = null;
            }
            return this.parts;
        }
    }

    public static void append(List<GuiTitlePart> parts, String font, String text, int color, int decorations) {
        if (text.isEmpty()) {
            return;
        }
        if (!parts.isEmpty()) {
            GuiTitlePart previous = parts.getLast();
            if (previous.sameStyle(font, color, decorations)) {
                parts.set(parts.size() - 1, new GuiTitlePart(font, previous.text + text, color, decorations));
                return;
            }
        }
        parts.add(new GuiTitlePart(font, text, color, decorations));
    }

    private boolean sameStyle(String font, int color, int decorations) {
        return this.font.equals(font) && this.color == color && this.decorations == decorations;
    }

    public static int decorations(int inherited, Map<?, ?> states) {
        int result = inherited;
        for (Map.Entry<?, ?> entry : states.entrySet()) {
            int flag = switch (((Enum<?>) entry.getKey()).name()) {
                case "OBFUSCATED" -> 1;
                case "BOLD" -> 2;
                case "STRIKETHROUGH" -> 4;
                case "UNDERLINED" -> 8;
                case "ITALIC" -> 16;
                default -> 0;
            };
            switch (((Enum<?>) entry.getValue()).name()) {
                case "TRUE" -> result |= flag;
                case "FALSE" -> result &= ~flag;
                default -> { }
            }
        }
        return result;
    }
}
