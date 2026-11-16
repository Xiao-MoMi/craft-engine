package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaTextureAnimation;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureAnimation;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.ClientPngReader;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.ImageUtils;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourcePackUtils;
import org.jetbrains.annotations.Nullable;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class BedrockVanillaTextures {
    private static final Rules RULES = readRules();
    private static final List<BedrockTextureConverter> CONVERTERS = List.of(
            new BedrockLayerTextures(),
            new BedrockLayoutTextures(),
            new BedrockAtlasTextures(),
            new BedrockParticleTextures(),
            new BedrockUiTextures(),
            new BedrockMappedTextures(RULES.mappings())
    );

    private BedrockVanillaTextures() {
    }

    public static void write(JavaResourcePack resources, Path root) throws IOException {
        VanillaTextureContext context = new VanillaTextureContext(resources, root, new VanillaTextureRegistry());
        for (BedrockTextureConverter converter : CONVERTERS) {
            converter.convert(context);
        }
        context.textures().write(root);
    }

    private static Rules readRules() {
        JsonObject json = GsonHelper.readJsonFromResource("/bedrock/vanilla_textures.json").getAsJsonObject();
        Map<String, List<BedrockTextureRule.Tile>> tiles = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("tiles").entrySet()) {
            List<BedrockTextureRule.Tile> entries = new ArrayList<>(entry.getValue().getAsJsonArray().size());
            for (JsonElement element : entry.getValue().getAsJsonArray()) {
                JsonObject tile = element.getAsJsonObject();
                entries.add(new BedrockTextureRule.Tile(tile.get("name").getAsString(), tile.get("index").getAsInt(), tile.has("replicate") ? tile.get("replicate").getAsInt() : 1));
            }
            tiles.put(entry.getKey(), List.copyOf(entries));
        }
        Map<String, BedrockTextureRule.Tga> tga = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("tga").entrySet()) {
            JsonObject target = entry.getValue().getAsJsonObject();
            tga.put(entry.getKey(), new BedrockTextureRule.Tga(target.get("path").getAsString(), target.get("keep").getAsBoolean()));
        }
        Map<String, String> firstFrames = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("first_frames").entrySet()) {
            firstFrames.put(entry.getKey(), entry.getValue().getAsString());
        }
        List<BedrockTextureRule> mappings = new ArrayList<>(json.getAsJsonObject("textures").size());
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("textures").entrySet()) {
            List<BedrockTextureRule.Target> targets = new ArrayList<>(entry.getValue().getAsJsonArray().size());
            for (JsonElement destination : entry.getValue().getAsJsonArray()) {
                String path = destination.getAsString();
                String firstFrame = firstFrames.get(path);
                String atlasPath = firstFrame != null ? firstFrame : path;
                String name = atlasPath.substring(0, atlasPath.lastIndexOf('.'));
                targets.add(new BedrockTextureRule.Target(path, tiles.getOrDefault(name, List.of()), firstFrame));
            }
            mappings.add(new BedrockTextureRule(entry.getKey() + ".png", List.copyOf(targets)));
        }
        return new Rules(List.copyOf(mappings), Map.copyOf(tiles), Map.copyOf(tga), Map.copyOf(firstFrames));
    }

    static BedrockTextureRule.Target target(String path) {
        String firstFrame = RULES.firstFrames().get(path);
        String atlasPath = firstFrame != null ? firstFrame : path;
        String name = atlasPath.substring(0, atlasPath.lastIndexOf('.'));
        return new BedrockTextureRule.Target(path, RULES.tiles().getOrDefault(name, List.of()), firstFrame);
    }

    @Nullable
    static BedrockTextureRule.Tga tga(String path) {
        return RULES.tga().get(path);
    }

    static TextureData load(VanillaTextureContext context, String source) throws IOException {
        return loadResource(context, ResourcePackUtils.vanillaTexturePath(source));
    }

    static TextureData loadResource(VanillaTextureContext context, String path) throws IOException {
        try (InputStream input = context.pack().openResource(path)) {
            if (input == null) {
                throw new IOException("Missing texture " + path);
            }
            return JavaTextureAnimation.read(
                    ClientPngReader.read(input),
                    context.pack().readResourceMetadata(path),
                    context.pack().version().isAtOrAbove(MinecraftVersion.V1_21_11)
            );
        }
    }

    static void transform(VanillaTextureContext context,
                          String output,
                          List<String> sources,
                          Function<List<BufferedImage>, BufferedImage> transform) {
        transform(context, output, sources, false, transform);
    }

    static void transform(VanillaTextureContext context,
                          String output,
                          List<String> sources,
                          boolean changed,
                          Function<List<BufferedImage>, BufferedImage> transform) {
        if (!changed) {
            for (String source : sources) {
                if (context.changedTexture(source)) {
                    changed = true;
                    break;
                }
            }
            if (!changed) {
                return;
            }
        }
        try {
            List<TextureData> inputs = new ObjectArrayList<>(sources.size());
            for (String source : sources) {
                inputs.add(load(context, source));
            }
            writeFrames(context, String.join(", ", sources), target(output), inputs, transform);
        } catch (IOException | RuntimeException exception) {
            CraftEngine.instance().logger().warn("Skipping Bedrock vanilla texture " + output + ": " + exception.getMessage());
        }
    }

    static boolean writeKeyFrames(VanillaTextureContext context,
                                  String owner,
                                  BedrockTextureRule.Target target,
                                  TextureAnimation animation) throws IOException {
        if (animation == null || animation.interpolate() || target.tiles().isEmpty() || animation.width() != animation.height()) {
            return false;
        }
        int interval = animation.frames().getFirst().time();
        for (TextureAnimation.Frame frame : animation.frames()) {
            interval = MiscUtils.gcd(interval, frame.time());
        }
        // 通过帧索引和时长复用关键帧，避免为重复帧复制像素。
        BufferedImage strip = animation.sheet();
        int columns = strip.getWidth() / animation.width();
        if (columns != 1) {
            int count = columns * (strip.getHeight() / animation.height());
            strip = new BufferedImage(animation.width(), Math.multiplyExact(animation.height(), count), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = strip.createGraphics();
            graphics.setComposite(AlphaComposite.Src);
            try {
                for (int index = 0; index < count; index++) {
                    int x = index % columns * animation.width();
                    int y = index / columns * animation.height();
                    graphics.drawImage(animation.sheet(), 0, index * animation.height(), animation.width(), (index + 1) * animation.height(),
                            x, y, x + animation.width(), y + animation.height(), null);
                }
            } finally {
                graphics.dispose();
            }
        }
        String animationPath = target.firstFrame() == null ? target.path().replace(".png", "_anim.png") : target.path();
        String firstFramePath = target.firstFrame() == null ? target.path() : target.firstFrame();
        if (writeImage(context, owner, animationPath, strip)) {
            writeImage(context, owner, firstFramePath, animation.firstFrame());
            List<Integer> sequence = new ArrayList<>(animation.duration() / interval);
            for (TextureAnimation.Frame frame : animation.frames()) {
                int repeats = frame.time() / interval;
                for (int repeat = 0; repeat < repeats; repeat++) {
                    sequence.add(frame.index());
                }
            }
            String path = animationPath.substring(0, animationPath.lastIndexOf('.'));
            for (BedrockTextureRule.Tile tile : target.tiles()) {
                context.textures().flipbook(tile.name(), tile.index(), path, interval, tile.replicate(), sequence, animation.interpolate());
            }
        }
        return true;
    }

    static void writeFrames(VanillaTextureContext context,
                            String owner,
                            BedrockTextureRule.Target target,
                            List<TextureData> inputs,
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
            if (writeImage(context, owner, target.path(), first) && target.firstFrame() != null) {
                writeImage(context, owner, target.firstFrame(), first);
                String path = target.path().substring(0, target.path().lastIndexOf('.'));
                for (BedrockTextureRule.Tile tile : target.tiles()) {
                    context.textures().flipbook(tile.name(), tile.index(), path, 1, tile.replicate(), List.of(0), false);
                }
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
        String animationPath = target.firstFrame() == null ? target.path().replace(".png", "_anim.png") : target.path();
        String firstFramePath = target.firstFrame() == null ? target.path() : target.firstFrame();
        if (writeImage(context, owner, animationPath, strip)) {
            writeImage(context, owner, firstFramePath, first);
            // flipbook 引用不带扩展名的贴图路径，同一条带可供多个 terrain 条目使用。
            String path = animationPath.substring(0, animationPath.lastIndexOf('.'));
            for (BedrockTextureRule.Tile tile : target.tiles()) {
                context.textures().flipbook(tile.name(), tile.index(), path, interval, tile.replicate());
            }
        }
    }

    static boolean writeImage(VanillaTextureContext context, String owner, String output, BufferedImage image) throws IOException {
        if (!context.textures().claim(output, owner)) {
            return false;
        }
        BedrockTextureRule.Tga tga = tga(output);
        if (tga != null) {
            ImageUtils.writeTga(image, context.root().resolve(tga.path()));
            if (!tga.keep()) {
                return true;
            }
        }
        ImageUtils.writePng(image, context.root().resolve(output));
        return true;
    }

    private record Rules(List<BedrockTextureRule> mappings,
                         Map<String, List<BedrockTextureRule.Tile>> tiles,
                         Map<String, BedrockTextureRule.Tga> tga,
                         Map<String, String> firstFrames) {
    }
}
