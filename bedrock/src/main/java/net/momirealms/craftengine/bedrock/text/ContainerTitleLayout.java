package net.momirealms.craftengine.bedrock.text;

import net.momirealms.craftengine.bedrock.GuiTitlePart;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ContainerTitleLayout {
    public static final int ORIGIN = 256;
    public static final String MARKER = "craftengine:layout/";
    public static final int SPACER_FIRST = 0xE200;
    public static final int MAX_SPACER = 33;
    private final Map<String, Map<Integer, FontGlyph>> fonts;

    public ContainerTitleLayout(Map<String, Map<Integer, FontGlyph>> fonts) {
        this.fonts = fonts;
    }

    public List<List<Run>> layout(List<GuiTitlePart> parts) {
        if (!this.hasCustomGlyph(parts)) {
            return List.of();
        }
        List<Layer> layers = new ArrayList<>();
        double cursor = ORIGIN;
        for (int partIndex = 0; partIndex < parts.size(); partIndex++) {
            GuiTitlePart part = parts.get(partIndex);
            Map<Integer, FontGlyph> font = this.fonts.get(part.font());
            for (int offset = 0; offset < part.text().length();) {
                int codepoint = part.text().codePointAt(offset);
                offset += Character.charCount(codepoint);
                FontGlyph glyph = font == null ? null : font.get(codepoint);
                if (glyph == null || codepoint == '\n' || codepoint == '§') {
                    throw new UnsupportedOperationException("Unknown title glyph metrics: " + part.font() + "/" + Integer.toHexString(codepoint));
                }
                if (!glyph.visible()) {
                    cursor += glyph.advance();
                    continue;
                }
                if (glyph.renderedAdvance() < 0) {
                    throw new UnsupportedOperationException("Unknown Bedrock title glyph width: " + part.font() + "/" + Integer.toHexString(codepoint));
                }
                boolean bold = (part.decorations() & 2) != 0;
                double position = Math.rint(cursor) - glyph.bearing();
                int advance = glyph.renderedAdvance() + (bold ? 1 : 0);
                double inkStart = position + glyph.bearing();
                double inkEnd = inkStart + advance - 1;
                int minimumLayer = 0;
                // Later glyphs must remain above earlier intersecting glyphs, even when the pen moves backwards.
                for (int i = 0; i < layers.size(); i++) {
                    for (Span span : layers.get(i).spans) {
                        if (inkStart < span.end && inkEnd > span.start) {
                            minimumLayer = i + 1;
                        }
                    }
                }
                Layer selected = null;
                for (int i = minimumLayer; i < layers.size(); i++) {
                    Layer layer = layers.get(i);
                    double gap = position - layer.cursor;
                    if (gap == 0 || gap >= 2) {
                        selected = layer;
                        break;
                    }
                }
                if (selected == null) {
                    if (position < 0 || position == 1) {
                        throw new UnsupportedOperationException("Title exceeds left margin");
                    }
                    selected = new Layer();
                    layers.add(selected);
                }
                int gap = (int) (position - selected.cursor);
                if (gap > 0) {
                    StringBuilder spaces = new StringBuilder();
                    while (gap > 0) {
                        int step = Math.min(gap, MAX_SPACER);
                        if (gap - step == 1) {
                            step--;
                        }
                        spaces.appendCodePoint(SPACER_FIRST + step - 2);
                        gap -= step;
                    }
                    selected.append(-1, spaces.toString());
                }
                selected.append(partIndex, Character.toString(glyph.character()));
                selected.spans.add(new Span(inkStart, inkEnd));
                selected.cursor = position + advance;
                cursor += glyph.advance() + (bold ? 1 : 0);
            }
        }
        List<List<Run>> result = new ArrayList<>(layers.size());
        if (layers.isEmpty()) {
            return List.of(List.of());
        }
        for (Layer layer : layers) {
            result.add(layer.runs);
        }
        return result;
    }

    private boolean hasCustomGlyph(List<GuiTitlePart> parts) {
        for (GuiTitlePart part : parts) {
            Map<Integer, FontGlyph> font = this.fonts.get(part.font());
            if (font == null) {
                continue;
            }
            for (int offset = 0; offset < part.text().length();) {
                int codepoint = part.text().codePointAt(offset);
                offset += Character.charCount(codepoint);
                FontGlyph glyph = font.get(codepoint);
                if (glyph != null && glyph.character() != codepoint) {
                    return true;
                }
            }
        }
        return false;
    }

    public record Run(int part, String text) {
    }

    private record Span(double start, double end) {
    }

    private static final class Layer {
        private final List<Run> runs = new ArrayList<>();
        private final List<Span> spans = new ArrayList<>();
        private double cursor;

        private void append(int part, String text) {
            if (!this.runs.isEmpty() && this.runs.getLast().part() == part) {
                Run previous = this.runs.removeLast();
                this.runs.add(new Run(part, previous.text() + text));
            } else {
                this.runs.add(new Run(part, text));
            }
        }
    }
}
