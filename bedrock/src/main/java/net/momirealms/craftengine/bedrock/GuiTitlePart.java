package net.momirealms.craftengine.bedrock;

import java.util.List;
import java.util.Map;

public record GuiTitlePart(String font, String text, int color, int decorations) {

    public static void append(List<GuiTitlePart> parts, String font, String text, int color, int decorations) {
        if (text.isEmpty()) {
            return;
        }
        if (!parts.isEmpty()) {
            GuiTitlePart previous = parts.getLast();
            if (previous.font.equals(font) && previous.color == color && previous.decorations == decorations) {
                parts.set(parts.size() - 1, new GuiTitlePart(font, previous.text + text, color, decorations));
                return;
            }
        }
        parts.add(new GuiTitlePart(font, text, color, decorations));
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
