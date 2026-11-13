package net.momirealms.craftengine.bedrock.geyser.entity.display;

import org.geysermc.geyser.util.SignUtils;

import java.util.ArrayList;
import java.util.List;

record DisplayTextLayout(List<Line> lines, int width) {

    static DisplayTextLayout wrap(String text, int maxWidth) {
        List<Line> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        StringBuilder format = new StringBuilder();
        int width = 0;
        int maximum = 0;
        boolean bold = false;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character == '§' && index + 1 < text.length()) {
                char code = text.charAt(++index);
                if (code == 'r' || "0123456789abcdef".indexOf(code) >= 0) {
                    format.setLength(0);
                    bold = false;
                }
                bold |= code == 'l';
                format.append('§').append(code);
                line.append('§').append(code);
                continue;
            }
            int advance = SignUtils.getCharacterWidth(character) + (bold ? 1 : 0);
            if (character == '\n' || (width > 0 && width + advance > maxWidth)) {
                lines.add(new Line(line.toString(), width));
                maximum = Math.max(maximum, width);
                line.setLength(0);
                line.append(format);
                width = 0;
            }
            if (character != '\n') {
                line.append(character);
                width += advance;
            }
        }
        lines.add(new Line(line.toString(), width));
        return new DisplayTextLayout(lines, Math.max(maximum, width));
    }

    record Line(String text, int width) {
    }
}
