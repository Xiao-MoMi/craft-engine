package net.momirealms.craftengine.bedrock.text;

public record FontGlyph(int character, int renderedAdvance, int bearing) {
    public static final int NATIVE_FALLBACK = -2;
    public static final FontGlyph IDENTITY_FALLBACK = new FontGlyph(-2, NATIVE_FALLBACK, 0);

    public int character(int sourceCodepoint) {
        return this == IDENTITY_FALLBACK ? sourceCodepoint : this.character;
    }

    public boolean visible() {
        return this == IDENTITY_FALLBACK || this.character >= 0;
    }

    public boolean usesAtlas(int sourceCodepoint) {
        return this.character >= 0 && (this.bearing != 0 || this.character != sourceCodepoint);
    }
}
