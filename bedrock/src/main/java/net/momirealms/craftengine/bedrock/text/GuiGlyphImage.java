package net.momirealms.craftengine.bedrock.text;

public final class GuiGlyphImage {
    public static final String DIRECTORY = "textures/ce_gui_glyphs";
    public static final int DIMENSION_BYTES = 5;
    public static final int ASCENT_BYTES = 5;
    public static final int ASCENT_BIAS = 50000;
    static final int IMAGE_RUN = -2;

    private GuiGlyphImage() {
    }

    public static String encode(int width, int height, int ascent, String texture) {
        if (width <= 0 || width > 9999 || height <= 0 || height > 9999 || ascent < -ASCENT_BIAS || ascent >= ASCENT_BIAS) {
            throw new IllegalArgumentException("GUI glyph dimensions or ascent outside the supported range");
        }
        StringBuilder value = new StringBuilder(DIMENSION_BYTES * 2 + ASCENT_BYTES + DIRECTORY.length() + 1 + texture.length());
        ContainerTitleCodec.appendNumber(value, width, 10000);
        ContainerTitleCodec.appendNumber(value, height, 10000);
        ContainerTitleCodec.appendNumber(value, ASCENT_BIAS + ascent, 10000);
        return value.append(DIRECTORY).append('/').append(texture).toString();
    }

    static void append(StringBuilder line, String descriptor, int scale) {
        if (scale == 1) {
            line.append(descriptor);
            return;
        }
        int width = Integer.parseInt(descriptor, 0, DIMENSION_BYTES, 10);
        int height = Integer.parseInt(descriptor, DIMENSION_BYTES, 2 * DIMENSION_BYTES, 10);
        ContainerTitleCodec.appendNumber(line, width * scale, 10000);
        ContainerTitleCodec.appendNumber(line, height * scale, 10000);
        line.append(descriptor, 2 * DIMENSION_BYTES, descriptor.length());
    }
}
