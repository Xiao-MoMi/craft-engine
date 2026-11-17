package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import com.google.gson.JsonArray;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureAnimation;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.util.ClientPngReader;
import net.momirealms.craftengine.core.util.ImageUtils;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

final class VanillaTextureWriter {
    private final Path root;
    private final VanillaTextureRegistry textures;

    VanillaTextureWriter(Path root, VanillaTextureRegistry textures) {
        this.root = root;
        this.textures = textures;
    }

    boolean contains(String output) {
        return this.textures.contains(output);
    }

    void copyMappedTextures(String owner, List<BedrockTextureRule.Target> targets, int firstTarget, byte[] png) throws IOException {
        BufferedImage image = null;
        for (int index = firstTarget; index < targets.size(); index++) {
            BedrockTextureRule.Target target = targets.get(index);
            if (this.contains(target.path())) {
                continue;
            }
            this.textures.claim(target.path(), owner);
            BedrockTextureRule.Tga tga = VanillaTextureMappings.INSTANCE.tga(target.path());
            if (tga != null) {
                if (image == null) {
                    image = ClientPngReader.read(new ByteArrayInputStream(png));
                }
                ImageUtils.writeTga(image, this.root.resolve(tga.path()));
                if (!tga.keep()) {
                    continue;
                }
            }

            this.copyPng(target.path(), png);
            if (target.firstFrame() != null) {
                if (this.textures.claim(target.firstFrame(), owner)) {
                    this.copyPng(target.firstFrame(), png);
                }
                this.registerStaticFlipbook(target);
            }
        }
    }

    private void copyPng(String output, byte[] png) throws IOException {
        Path path = this.root.resolve(output);
        Files.createDirectories(path.getParent());
        Files.write(path, png);
    }

    void writeMappedTexture(String owner, BedrockTextureRule.Target target, TextureData data) throws IOException {
        if (!this.writeKeyFrames(owner, target, data)) {
            this.writeFrames(owner, target, List.of(data), List::getFirst);
        }
    }

    private boolean writeKeyFrames(String owner, BedrockTextureRule.Target target, TextureData data) throws IOException {
        TextureAnimation animation = data.animation();
        if (animation == null || animation.interpolate() || target.tiles().isEmpty() || animation.width() != animation.height()) {
            return false;
        }
        int interval = animation.sampleInterval();
        // 通过帧索引和时长复用关键帧，避免为重复帧复制像素。
        BufferedImage strip = animation.keyFrameStrip(0xffffff);
        if (this.writeAnimationImages(owner, target, strip, data.image())) {
            this.registerFlipbook(target, target.animationPath(), interval, animation.frameSequence(interval));
        }
        return true;
    }

    void writeFrames(String owner, BedrockTextureRule.Target target, List<TextureData> inputs,
                     Function<List<BufferedImage>, BufferedImage> transform) throws IOException {
        // 先用各输入的静态图片或动画首帧合成，供静态输出和动画尺寸计算使用。
        List<BufferedImage> frames = new ObjectArrayList<>(inputs.size());
        int duration = 1;
        int interval = 0;
        for (TextureData input : inputs) {
            frames.add(input.image());
            TextureAnimation animation = input.animation();
            if (animation != null) {
                // 总周期取最小公倍数，确保循环结束时所有输入动画同时回到起点。
                duration = Math.multiplyExact(duration / MiscUtils.gcd(duration, animation.duration()), animation.duration());
                // 采样间隔取最大公约数，覆盖每个输入的采样时刻；单位均为 tick。
                interval = MiscUtils.gcd(interval, animation.sampleInterval());
            }
        }
        BufferedImage first = transform.apply(frames);
        // interval 仍为 0 表示没有动画输入，只需写出一次合成结果。
        if (interval == 0) {
            if (this.writeImage(owner, target.path(), first) && target.firstFrame() != null) {
                this.writeImage(owner, target.firstFrame(), first);
                this.registerStaticFlipbook(target);
            }
            return;
        }
        // 没有等价的动画目标时不写入覆盖资源，保留客户端原有效果。
        if (target.tiles().isEmpty() || first.getWidth() != first.getHeight()) {
            throw new IOException("No compatible terrain flipbook target for animated texture " + target.path());
        }
        // 将一个完整周期按固定间隔采样，每张合成帧从上到下排成竖直条带。
        int count = duration / interval;
        BufferedImage strip = new BufferedImage(first.getWidth(), Math.multiplyExact(first.getHeight(), count), BufferedImage.TYPE_INT_ARGB);
        // 动画输入分配可重复写入的帧缓冲区；静态输入始终复用原图。
        for (int index = 0; index < inputs.size(); index++) {
            TextureAnimation animation = inputs.get(index).animation();
            if (animation != null) {
                frames.set(index, new BufferedImage(animation.width(), animation.height(), BufferedImage.TYPE_INT_ARGB));
            }
        }
        Graphics2D graphics = strip.createGraphics();
        // 直接复制合成帧的 RGBA，保留透明像素。
        graphics.setComposite(AlphaComposite.Src);
        try {
            for (int frame = 0; frame < count; frame++) {
                // 所有输入按同一个 tick 取样，各动画内部按自己的周期循环并处理插值。
                for (int index = 0; index < inputs.size(); index++) {
                    TextureAnimation animation = inputs.get(index).animation();
                    if (animation != null) {
                        // 白色乘色不改变原色；具体染色、叠图或裁剪由 transform 完成。
                        animation.render(frame * interval, 0xffffff, frames.get(index));
                    }
                }
                graphics.drawImage(transform.apply(frames), 0, frame * first.getHeight(), null);
            }
        } finally {
            graphics.dispose();
        }
        // 写出成功后才注册播放规则；目标已被其他规则占用时，不覆盖其动画配置。
        if (this.writeAnimationImages(owner, target, strip, first)) {
            // flipbook 引用不带扩展名的贴图路径，同一条带可供多个 terrain 条目使用。
            this.registerFlipbook(target, target.animationPath(), interval, null);
        }
    }

    boolean writeImage(String owner, String output, BufferedImage image) throws IOException {
        if (!this.textures.claim(output, owner)) {
            return false;
        }
        BedrockTextureRule.Tga tga = VanillaTextureMappings.INSTANCE.tga(output);
        if (tga != null) {
            ImageUtils.writeTga(image, this.root.resolve(tga.path()));
            if (!tga.keep()) {
                return true;
            }
        }
        ImageUtils.writePng(image, this.root.resolve(output));
        return true;
    }

    private boolean writeAnimationImages(String owner, BedrockTextureRule.Target target, BufferedImage strip, BufferedImage firstFrame) throws IOException {
        if (!this.writeImage(owner, target.animationPath(), strip)) {
            return false;
        }
        this.writeImage(owner, target.firstFramePath(), firstFrame);
        return true;
    }

    private void registerStaticFlipbook(BedrockTextureRule.Target target) {
        JsonArray frames = new JsonArray(1);
        frames.add(0);
        this.registerFlipbook(target, target.path(), 1, frames);
    }

    private void registerFlipbook(BedrockTextureRule.Target target, String file, int interval, JsonArray frames) {
        String path = file.substring(0, file.lastIndexOf('.'));
        for (BedrockTextureRule.Tile tile : target.tiles()) {
            this.textures.flipbook(tile.name(), tile.index(), path, interval, tile.replicate(), frames, false);
        }
    }
}
