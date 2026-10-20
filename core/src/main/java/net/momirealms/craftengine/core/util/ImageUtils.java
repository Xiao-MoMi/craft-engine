package net.momirealms.craftengine.core.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ImageUtils {

    private ImageUtils() {
    }

    public static BufferedImage resize(BufferedImage image, int width, int height) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        graphics.setComposite(AlphaComposite.Src);
        graphics.drawImage(image, 0, 0, width, height, null);
        graphics.dispose();
        return result;
    }

    public static BufferedImage crop(BufferedImage image, float x, float y, float width, float height) {
        BufferedImage result = new BufferedImage((int) width, (int) height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        graphics.setComposite(AlphaComposite.Src);
        graphics.drawImage(image, 0, 0, (int) width, (int) height, (int) x, (int) y, (int) (x + width), (int) (y + height), null);
        graphics.dispose();
        return result;
    }

    public static BufferedImage flip(BufferedImage image, boolean horizontal, boolean vertical) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result.setRGB(horizontal ? width - x - 1 : x, vertical ? height - y - 1 : y, image.getRGB(x, y));
            }
        }
        return result;
    }

    public static BufferedImage rotate(BufferedImage image, int angle) {
        int quarters = Math.floorMod(angle, 360) / 90;
        int width = image.getWidth();
        int height = image.getHeight();
        boolean swap = quarters % 2 != 0;
        BufferedImage result = new BufferedImage(swap ? height : width, swap ? width : height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);
                switch (quarters) {
                    case 1 -> result.setRGB(y, width - 1 - x, pixel);
                    case 2 -> result.setRGB(width - 1 - x, height - 1 - y, pixel);
                    case 3 -> result.setRGB(height - 1 - y, x, pixel);
                    default -> result.setRGB(x, y, pixel);
                }
            }
        }
        return result;
    }

    public static BufferedImage tint(BufferedImage image, int color) {
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                int grey = Math.round(0.2126f * ((pixel >>> 16) & 255) + 0.7152f * ((pixel >>> 8) & 255) + 0.0722f * (pixel & 255));
                int red = Math.round(grey * ((color >>> 16) & 255) / 255f);
                int green = Math.round(grey * ((color >>> 8) & 255) / 255f);
                int blue = Math.round(grey * (color & 255) / 255f);
                result.setRGB(x, y, (pixel & 0xff000000) | (red << 16) | (green << 8) | blue);
            }
        }
        return result;
    }

    public static void writePng(BufferedImage image, Path path) throws IOException {
        Files.createDirectories(path.getParent());
        try (OutputStream output = Files.newOutputStream(path)) {
            new PngOptimizer(image, 0).write(output);
        }
    }

    public static void writeTga(BufferedImage image, Path path) throws IOException {
        int width = image.getWidth();
        int height = image.getHeight();
        if (width > 65535 || height > 65535) {
            throw new IOException("TGA dimensions exceed 65535");
        }
        Files.createDirectories(path.getParent());
        try (OutputStream output = Files.newOutputStream(path)) {
            byte[] header = new byte[18];
            header[2] = 2;
            header[12] = (byte) width;
            header[13] = (byte) (width >>> 8);
            header[14] = (byte) height;
            header[15] = (byte) (height >>> 8);
            header[16] = 32;
            header[17] = 0x28;
            output.write(header);
            byte[] row = new byte[width * 4];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = image.getRGB(x, y);
                    int offset = x * 4;
                    row[offset] = (byte) pixel;
                    row[offset + 1] = (byte) (pixel >>> 8);
                    row[offset + 2] = (byte) (pixel >>> 16);
                    row[offset + 3] = (byte) (pixel >>> 24);
                }
                output.write(row);
            }
        }
    }
}
