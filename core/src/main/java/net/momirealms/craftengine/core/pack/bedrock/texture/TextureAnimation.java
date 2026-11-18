package net.momirealms.craftengine.core.pack.bedrock.texture;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.util.ImageUtils;
import net.momirealms.craftengine.core.util.MiscUtils;
import org.jetbrains.annotations.Nullable;

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
        int[] nextPixels = this.interpolate ? new int[this.width * this.height] : null;
        // 所有采样复用同一画布；消费者必须在回调内完成读取或复制。
        for (int tick = 0; tick < this.duration; tick += interval) {
            this.render(tick, color, image, nextPixels);
            consumer.accept(tick / interval, image);
        }
    }

    public void render(int tick, int color, BufferedImage target) {
        this.render(tick, color, target, null);
    }

    private void render(int tick, int color, BufferedImage target, @Nullable int[] nextPixels) {
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
        double weight = this.interpolate ? (double) position / current.time() : 0;
        if (this.gpuInterpolation) {
            weight = (int) ((float) weight * 1000) / 1000.0;
        }
        int[] pixels = ((DataBufferInt) target.getRaster().getDataBuffer()).getData();
        this.sheet.getRGB(currentX, currentY, this.width, this.height, pixels, 0, this.width);
        if (weight == 0 && color == 0xffffff) {
            return;
        }
        if (weight == 0) {
            nextPixels = pixels;
        } else {
            int nextX = next.index() % columns * this.width;
            int nextY = next.index() / columns * this.height;
            nextPixels = this.sheet.getRGB(nextX, nextY, this.width, this.height, nextPixels, 0, this.width);
        }
        int redMultiplier = (color >>> 16) & 255;
        int greenMultiplier = (color >>> 8) & 255;
        int blueMultiplier = color & 255;
        int size = this.width * this.height;
        for (int i = 0; i < size; i++) {
            int pixel = pixels[i];
            int nextPixel = nextPixels[i];
            // 1.21.11 moved interpolation to a shader: quantized progress, RGBA mixing and UNORM rounding
            int red = this.mix((pixel >>> 16) & 255, (nextPixel >>> 16) & 255, weight) * redMultiplier / 255;
            int green = this.mix((pixel >>> 8) & 255, (nextPixel >>> 8) & 255, weight) * greenMultiplier / 255;
            int blue = this.mix(pixel & 255, nextPixel & 255, weight) * blueMultiplier / 255;
            int alpha = this.gpuInterpolation ? this.mix(pixel >>> 24, nextPixel >>> 24, weight) : pixel >>> 24;
            pixels[i] = (alpha << 24) | (red << 16) | (green << 8) | blue;
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
