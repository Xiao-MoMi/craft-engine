package net.momirealms.craftengine.core.pack.bedrock.texture;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.util.ImageUtils;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.util.List;

public record TextureAnimation(BufferedImage sheet,
                               int width,
                               int height,
                               List<Frame> frames,
                               boolean interpolate,
                               boolean gpuInterpolation,
                               int duration) {

    public TextureAnimation {
        frames = List.copyOf(frames);
    }

    public BufferedImage firstFrame() {
        int index = this.frames.getFirst().index();
        int columns = this.sheet.getWidth() / this.width;
        return this.sheet.getSubimage(index % columns * this.width, index / columns * this.height, this.width, this.height);
    }

    public int sampleInterval() {
        if (this.interpolate) {
            return 1;
        }
        int interval = this.frames.getFirst().time();
        for (Frame frame : this.frames) {
            interval = MiscUtils.gcd(interval, frame.time());
        }
        return interval;
    }

    public BufferedImage keyFrameStrip(int color) {
        BufferedImage source = color == 0xffffff ? this.sheet : ImageUtils.multiplyColor(this.sheet, color);
        int columns = source.getWidth() / this.width;
        if (columns == 1 && this.width == this.height) {
            return source;
        }
        int count = columns * (source.getHeight() / this.height);
        int size = Math.max(this.width, this.height);
        BufferedImage strip = new BufferedImage(size, Math.multiplyExact(size, count), BufferedImage.TYPE_INT_ARGB);
        int[] pixels = new int[this.width * this.height];
        for (int index = 0; index < count; index++) {
            source.getRGB(index % columns * this.width, index / columns * this.height, this.width, this.height, pixels, 0, this.width);
            strip.setRGB(0, index * size, this.width, this.height, pixels, 0, this.width);
        }
        return strip;
    }

    public JsonArray frameSequence(int interval) {
        JsonArray sequence = new JsonArray(this.duration / interval);
        for (Frame frame : this.frames) {
            JsonPrimitive index = new JsonPrimitive(frame.index());
            int repeats = frame.time() / interval;
            for (int i = 0; i < repeats; i++) {
                sequence.add(index);
            }
        }
        return sequence;
    }

    public void forEachFrame(int color, FrameConsumer consumer) throws IOException {
        int interval = this.sampleInterval();
        BufferedImage image = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
        // 所有采样复用同一画布；消费者必须在回调内完成读取或复制。
        for (int tick = 0; tick < this.duration; tick += interval) {
            this.render(tick, color, image);
            consumer.accept(tick / interval, image);
        }
    }

    public void render(int tick, int color, BufferedImage target) {
        int position = tick % this.duration;
        int frameIndex = 0;
        while (position >= this.frames.get(frameIndex).time()) {
            position -= this.frames.get(frameIndex++).time();
        }
        Frame current = this.frames.get(frameIndex);
        Frame next = this.frames.get((frameIndex + 1) % this.frames.size());
        int columns = this.sheet.getWidth() / this.width;
        int currentX = current.index() % columns * this.width;
        int currentY = current.index() / columns * this.height;
        int nextX = next.index() % columns * this.width;
        int nextY = next.index() / columns * this.height;
        double weight = this.interpolate ? (double) position / current.time() : 0;
        if (this.gpuInterpolation) {
            weight = (int) ((float) weight * 1000) / 1000.0;
        }
        int[] pixels = ((DataBufferInt) target.getRaster().getDataBuffer()).getData();
        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                int pixel = this.sheet.getRGB(currentX + x, currentY + y);
                int nextPixel = weight == 0 ? pixel : this.sheet.getRGB(nextX + x, nextY + y);
                // 1.21.11 moved interpolation to a shader: quantized progress, RGBA mixing and UNORM rounding.
                int red = this.mix((pixel >>> 16) & 255, (nextPixel >>> 16) & 255, weight);
                int green = this.mix((pixel >>> 8) & 255, (nextPixel >>> 8) & 255, weight);
                int blue = this.mix(pixel & 255, nextPixel & 255, weight);
                int alpha = this.gpuInterpolation ? this.mix(pixel >>> 24, nextPixel >>> 24, weight) : pixel >>> 24;
                red = red * ((color >>> 16) & 255) / 255;
                green = green * ((color >>> 8) & 255) / 255;
                blue = blue * (color & 255) / 255;
                pixels[y * this.width + x] = (alpha << 24) | (red << 16) | (green << 8) | blue;
            }
        }
    }

    private int mix(int current, int next, double weight) {
        double value = current * (1 - weight) + next * weight;
        return this.gpuInterpolation ? (int) Math.round(value) : (int) value;
    }

    public record Frame(int index, int time) {
    }

    @FunctionalInterface
    public interface FrameConsumer {

        void accept(int index, BufferedImage image) throws IOException;
    }
}
