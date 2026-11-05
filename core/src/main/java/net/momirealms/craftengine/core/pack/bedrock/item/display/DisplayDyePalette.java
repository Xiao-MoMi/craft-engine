package net.momirealms.craftengine.core.pack.bedrock.item.display;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongMap;

import java.util.Arrays;

final class DisplayDyePalette {
    private DisplayDyePalette() {
    }

    static Int2IntMap group(Int2LongMap frequencies, int maxLayers) {
        Int2IntMap palette = new Int2IntOpenHashMap(frequencies.size());
        int[] colors = frequencies.keySet().toIntArray();
        if (colors.length <= maxLayers) {
            for (int color : colors) {
                palette.put(color, color);
            }
            return palette;
        }

        Arrays.sort(colors);
        int seed = 0;
        for (int i = 1; i < colors.length; i++) {
            if (frequencies.get(colors[i]) > frequencies.get(colors[seed])) {
                seed = i;
            }
        }
        int[] distances = new int[colors.length];
        Arrays.fill(distances, Integer.MAX_VALUE);
        int[] groups = new int[colors.length];
        // 从最常见的底色开始，每次优先拆分与已有代表色相差大且覆盖像素多的颜色。
        for (int group = 0; group < maxLayers; group++) {
            int center = colors[seed];
            long largestError = -1;
            for (int i = 0; i < colors.length; i++) {
                int distance = distanceSquared(center, colors[i]);
                if (distance < distances[i]) {
                    groups[i] = group;
                    distances[i] = distance;
                }
                long error = frequencies.get(colors[i]) * distances[i];
                if (error > largestError) {
                    largestError = error;
                    seed = i;
                }
            }
        }
        // 每组采用按像素频次加权的平均 RGB，保留渐变的整体亮度。
        long[] weights = new long[maxLayers];
        long[] reds = new long[maxLayers];
        long[] greens = new long[maxLayers];
        long[] blues = new long[maxLayers];
        for (int i = 0; i < colors.length; i++) {
            int group = groups[i];
            int color = colors[i];
            long weight = frequencies.get(color);
            weights[group] += weight;
            reds[group] += (color >> 16) * weight;
            greens[group] += (color >> 8 & 255) * weight;
            blues[group] += (color & 255) * weight;
        }
        int[] shades = new int[maxLayers];
        for (int i = 0; i < shades.length; i++) {
            long weight = weights[i];
            int red = (int) ((reds[i] + weight / 2) / weight);
            int green = (int) ((greens[i] + weight / 2) / weight);
            int blue = (int) ((blues[i] + weight / 2) / weight);
            shades[i] = red << 16 | green << 8 | blue;
        }
        for (int i = 0; i < colors.length; i++) {
            palette.put(colors[i], shades[groups[i]]);
        }
        return palette;
    }

    private static int distanceSquared(int first, int second) {
        int red = (first >> 16) - (second >> 16);
        int green = (first >> 8 & 255) - (second >> 8 & 255);
        int blue = (first & 255) - (second & 255);
        return red * red + green * green + blue * blue;
    }
}
