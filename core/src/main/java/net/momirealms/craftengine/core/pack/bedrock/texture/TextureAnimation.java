package net.momirealms.craftengine.core.pack.bedrock.texture;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
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
            int time = frame.time();
            while (time != 0) {
                int remainder = interval % time;
                interval = time;
                time = remainder;
            }
        }
        return interval;
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
}
