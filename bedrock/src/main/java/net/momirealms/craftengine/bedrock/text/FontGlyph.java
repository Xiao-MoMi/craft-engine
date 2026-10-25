package net.momirealms.craftengine.bedrock.text;

public record FontGlyph(int character, float advance, int renderedAdvance, int bearing) {

    public boolean visible() {
        return this.character >= 0;
    }
}
