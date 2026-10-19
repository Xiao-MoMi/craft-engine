package net.momirealms.craftengine.core.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;

public final class GuiSpriteUtils {

    private GuiSpriteUtils() {
    }

    public static BufferedImage render(BufferedImage image, @Nullable JsonObject metadata, int width, int height) {
        JsonObject gui = metadata == null ? null : metadata.getAsJsonObject("gui");
        JsonObject scaling = gui == null ? null : gui.getAsJsonObject("scaling");
        String type = scaling == null ? "stretch" : scaling.get("type").getAsString();
        int sourceWidth = width;
        int sourceHeight = height;
        int left = 0;
        int right = 0;
        int top = 0;
        int bottom = 0;
        boolean stretchInner = false;
        if (!type.equals("stretch")) {
            if (!type.equals("tile") && !type.equals("nine_slice")) {
                throw new IllegalArgumentException("Unknown GUI scaling type " + type);
            }
            sourceWidth = scaling.get("width").getAsInt();
            sourceHeight = scaling.get("height").getAsInt();
            if (type.equals("nine_slice")) {
                JsonElement border = scaling.get("border");
                if (border.isJsonObject()) {
                    JsonObject borders = border.getAsJsonObject();
                    left = borders.get("left").getAsInt();
                    right = borders.get("right").getAsInt();
                    top = borders.get("top").getAsInt();
                    bottom = borders.get("bottom").getAsInt();
                } else {
                    left = right = top = bottom = border.getAsInt();
                }
                stretchInner = scaling.has("stretch_inner") && scaling.get("stretch_inner").getAsBoolean();
            }
        }
        if (sourceWidth <= 0 || sourceHeight <= 0 || left < 0 || right < 0 || top < 0 || bottom < 0
                || (long) left + right >= sourceWidth || (long) top + bottom >= sourceHeight) {
            throw new IllegalArgumentException("Invalid GUI scaling dimensions or borders");
        }

        // 逻辑坐标与图片像素分开处理，以整数像素密度保留高清资源，再按 Java 的规则采样。
        int scale = Math.max(1, (int) Math.ceil(Math.max((double) image.getWidth() / sourceWidth, (double) image.getHeight() / sourceHeight)));
        int outputWidth = Math.multiplyExact(width, scale);
        int outputHeight = Math.multiplyExact(height, scale);
        if (type.equals("stretch") && image.getWidth() == outputWidth && image.getHeight() == outputHeight) {
            return image;
        }
        int[] columns = coordinates(width, sourceWidth, image.getWidth(), scale, type, left, right, stretchInner);
        int[] rows = coordinates(height, sourceHeight, image.getHeight(), scale, type, top, bottom, stretchInner);
        BufferedImage result = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < outputHeight; y++) {
            for (int x = 0; x < outputWidth; x++) {
                result.setRGB(x, y, image.getRGB(columns[x], rows[y]));
            }
        }
        return result;
    }

    private static int[] coordinates(int targetSize,
                                     int sourceSize,
                                     int pixels,
                                     int scale,
                                     String type,
                                     int start,
                                     int end,
                                     boolean stretchInner) {
        int[] coordinates = new int[Math.multiplyExact(targetSize, scale)];
        // Java 在目标过小时会裁短两侧边框；尺寸不变的轴直接保留原坐标。
        start = Math.min(start, targetSize / 2);
        end = Math.min(end, targetSize / 2);
        for (int index = 0; index < coordinates.length; index++) {
            double position = (index + 0.5) / scale;
            double source;
            if (type.equals("stretch")) {
                source = position * sourceSize / targetSize;
            } else if (type.equals("tile")) {
                source = position % sourceSize;
            } else if (targetSize == sourceSize || position < start) {
                source = position;
            } else if (position >= targetSize - end) {
                source = sourceSize - (targetSize - position);
            } else {
                int innerSize = sourceSize - start - end;
                source = start + (stretchInner
                        ? (position - start) * innerSize / (targetSize - start - end)
                        : (position - start) % innerSize);
            }
            coordinates[index] = Math.min(pixels - 1, (int) (source * pixels / sourceSize));
        }
        return coordinates;
    }
}
