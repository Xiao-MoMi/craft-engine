package net.momirealms.craftengine.bedrock.text;

public record FontGlyph(int character, int renderedAdvance, int bearing) {
    public static final int NATIVE_FALLBACK = -2;

    public boolean visible() {
        return this.character >= 0;
    }
}
