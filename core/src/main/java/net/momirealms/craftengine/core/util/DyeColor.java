package net.momirealms.craftengine.core.util;

public enum DyeColor {
    WHITE(0, MapColor.WHITE, 16383998, 15790320, 16777215),
    ORANGE(1, MapColor.ORANGE, 16351261, 15435844, 16738335),
    MAGENTA(2, MapColor.MAGENTA, 13061821, 12801229, 16711935),
    LIGHT_BLUE(3, MapColor.LIGHT_BLUE, 3847130, 6719955, 10141901),
    YELLOW(4, MapColor.YELLOW, 16701501, 14602026, 16776960),
    LIME(5, MapColor.GREEN, 8439583, 4312372, 12582656),
    PINK(6, MapColor.PINK, 15961002, 14188952, 16738740),
    GRAY(7, MapColor.GRAY, 4673362, 4408131, 8421504),
    LIGHT_GRAY(8, MapColor.LIGHT_GRAY, 10329495, 11250603, 13882323),
    CYAN(9, MapColor.CYAN, 1481884, 2651799, 65535),
    PURPLE(10, MapColor.PURPLE, 8991416, 8073150, 10494192),
    BLUE(11, MapColor.BLUE, 3949738, 2437522, 255),
    BROWN(12, MapColor.BROWN, 8606770, 5320730, 9127187),
    GREEN(13, MapColor.GREEN, 6192150, 3887386, 65280),
    RED(14, MapColor.RED, 11546150, 11743532, 16711680),
    BLACK(15, MapColor.BLACK, 1908001, 1973019, 0);

    private final int id;
    private final MapColor mapColor;
    private final int textureDiffuseColor;
    private final int fireworkColor;
    private final int textColor;

    DyeColor(int id, MapColor mapColor, int textureDiffuseColor, int fireworkColor, int signColor) {
        this.id = id;
        this.mapColor = mapColor;
        this.textColor = Color.opaque(signColor);
        this.textureDiffuseColor = Color.opaque(textureDiffuseColor);
        this.fireworkColor = fireworkColor;
    }

    public int id() {
        return id;
    }

    public MapColor mapColor() {
        return this.mapColor;
    }

    public int textureDiffuseColor() {
        return this.textureDiffuseColor;
    }

    public int fireworkColor() {
        return this.fireworkColor;
    }

    public int textColor() {
        return this.textColor;
    }
}
