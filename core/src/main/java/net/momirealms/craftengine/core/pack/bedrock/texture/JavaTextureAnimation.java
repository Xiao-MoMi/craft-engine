package net.momirealms.craftengine.core.pack.bedrock.texture;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class JavaTextureAnimation {

    private JavaTextureAnimation() {
    }

    public static TextureData read(BufferedImage image, @Nullable JsonObject metadata, boolean gpuInterpolation) throws IOException {
        if (metadata == null || !metadata.has("animation")) {
            return new TextureData(image, null);
        }
        JsonObject animation = metadata.getAsJsonObject("animation");
        int width;
        int height;
        if (animation.has("width")) {
            width = animation.get("width").getAsInt();
            height = GsonHelper.getAsInt(animation.get("height"), image.getHeight());
        } else if (animation.has("height")) {
            width = image.getWidth();
            height = animation.get("height").getAsInt();
        } else {
            width = Math.min(image.getWidth(), image.getHeight());
            height = width;
        }
        if (width <= 0 || height <= 0 || image.getWidth() % width != 0 || image.getHeight() % height != 0) {
            throw new IOException("Animation frame size " + width + "x" + height + " does not divide texture " + image.getWidth() + "x" + image.getHeight());
        }
        if (width == image.getWidth() && height == image.getHeight()) {
            return new TextureData(image, null);
        }
        int columns = image.getWidth() / width;
        int frameCount = columns * (image.getHeight() / height);

        int defaultTime = GsonHelper.getAsInt(animation.get("frametime"), 1);
        if (defaultTime <= 0) {
            throw new IOException("Animation frametime must be positive");
        }
        JsonArray frames = animation.getAsJsonArray("frames");
        List<TextureAnimation.Frame> sequence = new ObjectArrayList<>();
        if (frames == null) {
            for (int index = 0; index < frameCount; index++) {
                sequence.add(new TextureAnimation.Frame(index, defaultTime));
            }
        } else {
            for (JsonElement frame : frames) {
                int index;
                int time;
                if (frame instanceof JsonObject timed) {
                    index = timed.get("index").getAsInt();
                    time = GsonHelper.getAsInt(timed.get("time"), defaultTime);
                } else {
                    index = frame.getAsInt();
                    time = defaultTime;
                }
                if (index < 0 || index >= frameCount || time <= 0) {
                    continue;
                }
                sequence.add(new TextureAnimation.Frame(index, time));
            }
        }
        if (sequence.size() < 2) {
            // Java 不创建单条目动画，使用左上角静态帧。
            return new TextureData(image.getSubimage(0, 0, width, height), null);
        }
        int duration = 0;
        for (TextureAnimation.Frame frame : sequence) {
            duration = Math.addExact(duration, frame.time());
        }
        TextureAnimation result = new TextureAnimation(
                image,
                width,
                height,
                sequence,
                GsonHelper.getAsBoolean(animation.get("interpolate"), false),
                gpuInterpolation,
                duration
        );
        return new TextureData(result.firstFrame(), result);
    }
}
