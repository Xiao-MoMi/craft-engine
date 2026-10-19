package net.momirealms.craftengine.core.pack.bedrock.texture;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
        // 没有 animation 配置时，整张图片作为静态贴图使用。
        if (metadata == null || !metadata.has("animation")) {
            return new TextureData(image, null);
        }
        JsonObject animation = metadata.getAsJsonObject("animation");
        int width;
        int height;
        // width/height 表示单帧尺寸；只指定一个时，另一边使用整张图片的对应尺寸。
        if (animation.has("width")) {
            width = animation.get("width").getAsInt();
            height = GsonHelper.getAsInt(animation.get("height"), image.getHeight());
        } else if (animation.has("height")) {
            width = image.getWidth();
            height = animation.get("height").getAsInt();
        } else {
            // 都未指定时，以图片短边作为正方形帧的边长。
            width = Math.min(image.getWidth(), image.getHeight());
            height = width;
        }
        // 帧必须能将整张图片划分成完整的网格。
        if (width <= 0 || height <= 0 || image.getWidth() % width != 0 || image.getHeight() % height != 0) {
            throw new IOException("Animation frame size " + width + "x" + height + " does not divide texture " + image.getWidth() + "x" + image.getHeight());
        }
        // 图片只有一格时无需生成动画，也不再读取播放序列。
        if (width == image.getWidth() && height == image.getHeight()) {
            return new TextureData(image, null);
        }
        // 帧编号从 0 开始，按从左到右、从上到下的顺序排列。
        int columns = image.getWidth() / width;
        int frameCount = columns * (image.getHeight() / height);
        JsonArray frames = animation.getAsJsonArray("frames");
        // 时长单位为游戏 tick；frametime 是未单独指定时长的帧所用的默认值。
        int defaultTime = GsonHelper.getAsInt(animation.get("frametime"), 1);
        if (defaultTime <= 0) {
            throw new IOException("Animation frametime must be positive");
        }
        List<TextureAnimation.Frame> sequence = new ArrayList<>();
        if (frames == null) {
            // 省略 frames 时顺序播放全部帧；显式配置空数组则不会走这个分支。
            for (int index = 0; index < frameCount; index++) {
                sequence.add(new TextureAnimation.Frame(index, defaultTime));
            }
        } else {
            // 保留配置中的播放顺序和重复编号；整数表示帧号，对象还可单独指定 time。
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
                // 跳过越界帧和无效时长，其余有效帧仍可组成动画。
                if (index < 0 || index >= frameCount || time <= 0) {
                    continue;
                }
                sequence.add(new TextureAnimation.Frame(index, time));
            }
        }
        // 有效条目不足两个时，使用图片左上角那一帧，而不是 frames 指定的帧。
        if (sequence.size() < 2) {
            return new TextureData(image.getSubimage(0, 0, width, height), null);
        }
        // 累加一轮播放的总时长，溢出时直接报错，避免生成错误的循环周期。
        int duration = 0;
        for (TextureAnimation.Frame frame : sequence) {
            duration = Math.addExact(duration, frame.time());
        }
        // 此处只记录插值设置；gpuInterpolation 选择插值计算规则，不是在这里启动 GPU 渲染。
        TextureAnimation result = new TextureAnimation(
                image,
                width,
                height,
                sequence,
                GsonHelper.getAsBoolean(animation.get("interpolate"), false),
                gpuInterpolation,
                duration
        );
        // 静态预览取播放序列的首帧，同时保留完整动画数据供后续转换使用。
        return new TextureData(result.firstFrame(), result);
    }
}
