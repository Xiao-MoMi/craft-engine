package net.momirealms.craftengine.bedrock.text;

public record FontGlyph(int character, int renderedAdvance, int bearing) {

    public boolean visible() {
        return this.character >= 0;
    }
}
