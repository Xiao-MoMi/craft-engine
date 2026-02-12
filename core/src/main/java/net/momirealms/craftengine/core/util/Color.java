package net.momirealms.craftengine.core.util;

import org.joml.Vector3f;

import java.util.Arrays;

public class Color {
    private static final int BIT_MASK = 0xff;
    private static final byte DEFAULT_ALPHA = (byte) 255;
    private final int color;

    public Color(int color) {
        this.color = color;
    }

    public Color(int r, int g, int b) {
        this(DEFAULT_ALPHA, r, g, b);
    }

    public Color(int a, int r, int g, int b) {
        this(toDecimal(a, r, g, b));
    }

    public int color() {
        return color;
    }

    public static int toDecimal(int a, int r, int g, int b) {
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static int toDecimal(int r, int g, int b) {
        return DEFAULT_ALPHA << 24 | r << 16 | g << 8 | b;
    }

    public static Color fromDecimal(int decimal) {
        return new Color(decimal);
    }

    public static Color fromVector3f(Vector3f vec) {
        return new Color(0 << 24 /*不可省略*/ | MiscUtils.floor(vec.x) << 16 | MiscUtils.floor(vec.y) << 8 | MiscUtils.floor(vec.z));
    }

    public static int opaque(int color) {
        return color | -16777216;
    }

    public static int transparent(int color) {
        return color & 16777215;
    }

    public static int alpha(int color) {
        return color >>> 24;
    }

    public static int red(int color) {
        return color >> 16 & BIT_MASK;
    }

    public static int green(int color) {
        return color >> 8 & BIT_MASK;
    }

    public static int blue(int color) {
        return color & BIT_MASK;
    }

    public static Color fromStrings(String[] strings) {
        if (strings.length == 3) {
            // rgb
            return fromDecimal(toDecimal(Integer.parseInt(strings[0]), Integer.parseInt(strings[1]), Integer.parseInt(strings[2])));
        } else if (strings.length == 4) {
            // argb
            return fromDecimal(toDecimal(Integer.parseInt(strings[0]), Integer.parseInt(strings[1]), Integer.parseInt(strings[2]), Integer.parseInt(strings[3])));
        } else {
            throw new IllegalArgumentException("Invalid color format: " + Arrays.toString(strings));
        }
    }

    public int a() {
        return alpha(color);
    }

    public int b() {
        return blue(color);
    }

    public int g() {
        return green(color);
    }

    public int r() {
        return red(color);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Color color1)) return false;
        return this.color == color1.color;
    }

    @Override
    public int hashCode() {
        return Math.abs(this.color);
    }
}
