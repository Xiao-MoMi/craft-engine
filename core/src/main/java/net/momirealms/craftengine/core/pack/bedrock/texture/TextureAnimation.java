package net.momirealms.craftengine.core.pack.bedrock.texture;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import it.unimi.dsi.fastutil.ints.Int2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
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

    public KeyFrames keyFrames() {
        Int2IntLinkedOpenHashMap indices = new Int2IntLinkedOpenHashMap();
        indices.defaultReturnValue(-1);
        for (Frame frame : this.frames) {
            indices.putIfAbsent(frame.index(), indices.size());
        }
        return new KeyFrames(indices);
    }

    public int uniformFrameTime() {
        int interval = this.frames.getFirst().time();
        for (Frame frame : this.frames) {
            if (frame.time() != interval) {
                return 0;
            }
        }
        return interval;
    }

    public final class KeyFrames {
        private final Int2IntLinkedOpenHashMap indices;

        private KeyFrames(Int2IntLinkedOpenHashMap indices) {
            this.indices = indices;
        }

        public int size() {
            return this.indices.size();
        }

        public BufferedImage strip(int color) {
            BufferedImage source = TextureAnimation.this.sheet;
            int width = TextureAnimation.this.width;
            int height = TextureAnimation.this.height;
            int columns = source.getWidth() / width;
            boolean originalOrder = columns == 1 && width == height && this.size() == source.getHeight() / height;
            if (originalOrder) {
                int expected = 0;
                for (IntIterator iterator = this.indices.keySet().iterator(); iterator.hasNext();) {
                    if (iterator.nextInt() != expected++) {
                        originalOrder = false;
                        break;
                    }
                }
            }
            if (originalOrder) {
                return color == 0xffffff ? source : ImageUtils.multiplyColor(source, color);
            }
            int size = Math.max(width, height);
            BufferedImage strip = new BufferedImage(size, Math.multiplyExact(size, this.size()), BufferedImage.TYPE_INT_ARGB);
            int[] pixels = new int[width * height];
            int destination = 0;
            for (IntIterator iterator = this.indices.keySet().iterator(); iterator.hasNext();) {
                int index = iterator.nextInt();
                source.getRGB(index % columns * width, index / columns * height, width, height, pixels, 0, width);
                strip.setRGB(0, destination++ * size, width, height, pixels, 0, width);
            }
            return color == 0xffffff ? strip : ImageUtils.multiplyColor(strip, color);
        }

        public JsonArray sequence(int interval) {
            JsonArray sequence = new JsonArray(TextureAnimation.this.duration / interval);
            for (Frame frame : TextureAnimation.this.frames) {
                JsonPrimitive index = new JsonPrimitive(this.indices.get(frame.index()));
                int repeats = frame.time() / interval;
                for (int i = 0; i < repeats; i++) {
                    sequence.add(index);
                }
            }
            return sequence;
        }
    }

    public Sampler sampler() {
        return new Sampler();
    }

    public final class Sampler {
        private int index;
        private int start;
        private final int[] nextPixels = TextureAnimation.this.interpolate ? new int[TextureAnimation.this.width * TextureAnimation.this.height] : null;

        private Sampler() {
        }

        public void render(int tick, int color, BufferedImage target) {
            int position = tick % TextureAnimation.this.duration;
            if (position < this.start) {
                this.start = 0;
                this.index = 0;
            }
            while (position - this.start >= TextureAnimation.this.frames.get(this.index).time()) {
                this.start += TextureAnimation.this.frames.get(this.index++).time();
            }
            TextureAnimation.this.render(this.index, position - this.start, color, target, this.nextPixels);
        }
    }

    public void forEachFrame(int color, FrameConsumer consumer) throws IOException {
        int interval = this.sampleInterval();
        BufferedImage image = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
        Sampler sampler = this.sampler();
        for (int tick = 0; tick < this.duration; tick += interval) {
            sampler.render(tick, color, image);
            consumer.accept(tick / interval, image);
        }
    }

    private void render(int frameIndex, int position, int color, BufferedImage target, @Nullable int[] nextPixels) {
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
