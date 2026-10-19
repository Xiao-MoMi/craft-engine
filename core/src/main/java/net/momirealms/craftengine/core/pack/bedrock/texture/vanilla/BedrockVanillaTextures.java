package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.pack.bedrock.BedrockConversionContext;
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

    public static void write(BedrockConversionContext context) throws IOException {
        for (BedrockTextureConverter converter : CONVERTERS) {
            converter.convert(context);
        }
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
        List<BedrockTextureRule> mappings = new ArrayList<>(json.getAsJsonObject("textures").size());
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("textures").entrySet()) {
            List<BedrockTextureRule.Target> targets = new ArrayList<>(entry.getValue().getAsJsonArray().size());
            for (JsonElement destination : entry.getValue().getAsJsonArray()) {
                String path = destination.getAsString();
                String name = path.substring(0, path.lastIndexOf('.'));
                targets.add(new BedrockTextureRule.Target(path, tiles.getOrDefault(name, List.of())));
            }
            mappings.add(new BedrockTextureRule(entry.getKey() + ".png", List.copyOf(targets)));
        }
        return new Rules(List.copyOf(mappings), Map.copyOf(tiles), Map.copyOf(tga));
    }

    static BedrockTextureRule.Target target(String path) {
        String name = path.substring(0, path.lastIndexOf('.'));
        return new BedrockTextureRule.Target(path, RULES.tiles().getOrDefault(name, List.of()));
    }

    @Nullable
    static BedrockTextureRule.Tga tga(String path) {
        return RULES.tga().get(path);
    }

    static TextureData load(BedrockConversionContext context, String source) throws IOException {
        return loadResource(context, ResourcePackUtils.vanillaTexturePath(source));
    }

    static TextureData loadResource(BedrockConversionContext context, String path) throws IOException {
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

    static void transform(BedrockConversionContext context,
                          String output,
                          List<String> sources,
                          Function<List<BufferedImage>, BufferedImage> transform) {
        transform(context, output, sources, false, transform);
    }

    static void transform(BedrockConversionContext context,
                          String output,
                          List<String> sources,
                          boolean changed,
                          Function<List<BufferedImage>, BufferedImage> transform) {
        if (!changed) {
            for (String source : sources) {
                if (context.pack().hasCustomResource(ResourcePackUtils.vanillaTexturePath(source))) {
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
        } catch (IOException | IllegalArgumentException | ArithmeticException exception) {
            CraftEngine.instance().logger().warn("Skipping Bedrock vanilla texture " + output + ": " + exception.getMessage());
        }
    }

    static boolean writeKeyFrames(BedrockConversionContext context,
                                          String owner,
                                          BedrockTextureRule.Target target,
                                          TextureAnimation animation) throws IOException {
        if (animation == null || target.tiles().isEmpty() || animation.width() != animation.height()) {
            return false;
        }
        int interval = animation.frames().getFirst().time();
        for (TextureAnimation.Frame frame : animation.frames()) {
            if (animation.interpolate() && frame.time() != interval) {
                return false;
            }
            interval = MiscUtils.gcd(interval, frame.time());
        }
        // Keep timed key frames instead of expanding long interpolations into oversized context.textures().
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
        if (writeImage(context, owner, target.path(), strip)) {
            List<Integer> sequence = new ArrayList<>(animation.duration() / interval);
            for (TextureAnimation.Frame frame : animation.frames()) {
                int repeats = frame.time() / interval;
                for (int repeat = 0; repeat < repeats; repeat++) {
                    sequence.add(frame.index());
                }
            }
            String path = target.path().substring(0, target.path().lastIndexOf('.'));
            for (BedrockTextureRule.Tile tile : target.tiles()) {
                context.textures().flipbook(tile.name(), tile.index(), path, interval, tile.replicate(), sequence, animation.interpolate());
            }
        }
        return true;
    }

    static void writeFrames(BedrockConversionContext context,
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
            writeImage(context, owner, target.path(), first);
            return;
        }
        // 当前动画输出需要 terrain flipbook 映射和正方形单帧，否则只导出首帧并提示。
        if (target.tiles().isEmpty() || first.getWidth() != first.getHeight()) {
            CraftEngine.instance().logger().warn("Exporting first frame of Bedrock texture " + target.path() + ": no compatible terrain flipbook target");
            writeImage(context, owner, target.path(), first);
            return;
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
        if (writeImage(context, owner, target.path(), strip)) {
            // flipbook 引用不带扩展名的贴图路径，同一条带可供多个 terrain 条目使用。
            String path = target.path().substring(0, target.path().lastIndexOf('.'));
            for (BedrockTextureRule.Tile tile : target.tiles()) {
                context.textures().flipbook(tile.name(), tile.index(), path, interval, tile.replicate());
            }
        }
    }

    static boolean writeImage(BedrockConversionContext context, String owner, String output, BufferedImage image) throws IOException {
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
                         Map<String, BedrockTextureRule.Tga> tga) {
    }
}
