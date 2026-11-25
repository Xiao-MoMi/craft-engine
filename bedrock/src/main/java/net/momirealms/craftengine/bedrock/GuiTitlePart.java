package net.momirealms.craftengine.bedrock;

import java.util.List;
import java.util.Map;

public record GuiTitlePart(String font, String text, int color, int decorations) {

    public static final class Builder {
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
