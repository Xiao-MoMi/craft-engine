package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTextureAtlas;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTextureRegistry;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaTextureAnimation;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureAnimation;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.ClientPngReader;
import net.momirealms.craftengine.core.util.ImageUtils;
import net.momirealms.craftengine.core.util.MinecraftVersion;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class BedrockVanillaTextures {
    private static final JsonObject RULES = readRules("vanilla_textures.json").getAsJsonObject();
    private static final List<BedrockTextureConverter> CONVERTERS = List.of(
            new BedrockLayerTextures(),
            new BedrockLayoutTextures(),
            new BedrockBedTextures(),
            new BedrockAtlasTextures(),
            new BedrockUiTextures()
    );

    private BedrockVanillaTextures() {
    }

    static JsonElement readRules(String name) {
        try (InputStream input = BedrockVanillaTextures.class.getResourceAsStream("/bedrock/" + name)) {
            if (input == null) {
                throw new IOException("Missing bundled Bedrock texture rules " + name);
            }
            try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader);
            }
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static void write(JavaResourcePack pack, Path root, BedrockTextureRegistry textures) throws IOException {
        for (BedrockTextureConverter converter : CONVERTERS) {
            converter.convert(pack, root, textures);
        }
        for (Map.Entry<String, JsonElement> mapping : RULES.getAsJsonObject("textures").entrySet()) {
            String source = mapping.getKey() + ".png";
            if (!pack.hasCustomResource(resource(source))) {
                continue;
            }
            List<BedrockTextureRule.Target> targets = new ArrayList<>();
            for (JsonElement destination : mapping.getValue().getAsJsonArray()) {
                String path = destination.getAsString();
                if (!textures.contains(path)) {
                    targets.add(target(path));
                }
            }
            if (targets.isEmpty()) {
                continue;
            }
            BedrockTextureRule rule = new BedrockTextureRule(source, targets);
            try (InputStream input = pack.openCustomResource(resource(source))) {
                JsonObject metadata = pack.readResourceMetadata(resource(source));
                boolean animated = metadata != null && metadata.has("animation");
                byte[] bytes = input.readAllBytes();
                BedrockTextureAtlas.TextureData data = null;
                BufferedImage image = null;
                for (BedrockTextureRule.Target target : rule.targets()) {
                    if (!animated) {
                        if (textures.claim(target.path(), source)) {
                            JsonObject tga = RULES.getAsJsonObject("tga").getAsJsonObject(target.path());
                            if (tga != null) {
                                if (image == null) {
                                    image = readImage(new ByteArrayInputStream(bytes), source);
                                }
                                ImageUtils.writeTga(image, root.resolve(tga.get("path").getAsString()));
                                if (!tga.get("keep").getAsBoolean()) {
                                    continue;
                                }
                            }
                            Path file = root.resolve(target.path());
                            Files.createDirectories(file.getParent());
                            Files.write(file, bytes);
                        }
                    } else {
                        if (data == null) {
                            data = JavaTextureAnimation.read(readImage(new ByteArrayInputStream(bytes), source), metadata,
                                    pack.version().isAtOrAbove(MinecraftVersion.V1_21_11));
                        }
                        if (!writeKeyFrames(root, textures, source, target, data.animation())) {
                            writeFrames(root, textures, source, target, List.of(data), images -> images.getFirst());
                        }
                    }
                }
            } catch (IOException | IllegalArgumentException | ArithmeticException exception) {
                warn(source, exception);
            }
        }
    }

    static String resource(String texture) {
        return "assets/minecraft/textures/" + texture;
    }

    static BedrockTextureRule.Target target(String path) {
        String name = path.substring(0, path.lastIndexOf('.'));
        JsonElement entries = RULES.getAsJsonObject("tiles").get(name);
        List<BedrockTextureRule.Tile> tiles = new ArrayList<>();
        if (entries != null) {
            for (JsonElement entry : entries.getAsJsonArray()) {
                JsonObject tile = entry.getAsJsonObject();
                tiles.add(new BedrockTextureRule.Tile(tile.get("name").getAsString(), tile.get("index").getAsInt(),
                        tile.has("replicate") ? tile.get("replicate").getAsInt() : 1));
            }
        }
        return new BedrockTextureRule.Target(path, tiles);
    }

    static BedrockTextureAtlas.TextureData load(JavaResourcePack pack, String source) throws IOException {
        String path = resource(source);
        try (InputStream input = pack.openResource(path)) {
            if (input == null) {
                throw new IOException("Missing texture " + source);
            }
            return JavaTextureAnimation.read(readImage(input, source), pack.readResourceMetadata(path), pack.version().isAtOrAbove(MinecraftVersion.V1_21_11));
        }
    }

    private static BufferedImage readImage(InputStream input, String source) throws IOException {
        BufferedImage image = ClientPngReader.read(input);
        if (image == null) {
            throw new IOException("Invalid PNG " + source);
        }
        return image;
    }

    static void transform(JavaResourcePack pack, Path root, BedrockTextureRegistry textures, String output,
                          List<String> sources, Function<List<BufferedImage>, BufferedImage> transform) {
        boolean changed = false;
        for (String source : sources) {
            if (pack.hasCustomResource(resource(source))) {
                changed = true;
                break;
            }
        }
        if (!changed) {
            return;
        }
        try {
            List<BedrockTextureAtlas.TextureData> inputs = new ArrayList<>(sources.size());
            for (String source : sources) {
                inputs.add(load(pack, source));
            }
            writeFrames(root, textures, String.join(", ", sources), target(output), inputs, transform);
        } catch (IOException | IllegalArgumentException | ArithmeticException exception) {
            warn(output, exception);
        }
    }

    private static boolean writeKeyFrames(Path root, BedrockTextureRegistry textures, String owner,
                                          BedrockTextureRule.Target target, TextureAnimation animation) throws IOException {
        if (animation == null || target.tiles().isEmpty() || animation.width() != animation.height()) {
            return false;
        }
        int interval = animation.frames().getFirst().time();
        for (TextureAnimation.Frame frame : animation.frames()) {
            if (animation.interpolate() && frame.time() != interval) {
                return false;
            }
            interval = gcd(interval, frame.time());
        }
        // Keep timed key frames instead of expanding long interpolations into oversized textures.
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
        if (writeImage(root, textures, owner, target.path(), strip)) {
            List<Integer> sequence = new ArrayList<>(animation.duration() / interval);
            for (TextureAnimation.Frame frame : animation.frames()) {
                int repeats = frame.time() / interval;
                for (int repeat = 0; repeat < repeats; repeat++) {
                    sequence.add(frame.index());
                }
            }
            String path = target.path().substring(0, target.path().lastIndexOf('.'));
            for (BedrockTextureRule.Tile tile : target.tiles()) {
                textures.flipbook(tile.name(), tile.index(), path, interval, tile.replicate(), sequence, animation.interpolate());
            }
        }
        return true;
    }

    private static void writeFrames(Path root, BedrockTextureRegistry textures, String owner, BedrockTextureRule.Target target,
                                    List<BedrockTextureAtlas.TextureData> inputs, Function<List<BufferedImage>, BufferedImage> transform) throws IOException {
        List<BufferedImage> frames = new ArrayList<>(inputs.size());
        int duration = 1;
        int interval = 0;
        for (BedrockTextureAtlas.TextureData input : inputs) {
            frames.add(input.image());
            TextureAnimation animation = input.animation();
            if (animation != null) {
                duration = Math.multiplyExact(duration / gcd(duration, animation.duration()), animation.duration());
                interval = gcd(interval, animation.sampleInterval());
            }
        }
        BufferedImage first = transform.apply(frames);
        if (interval == 0) {
            writeImage(root, textures, owner, target.path(), first);
            return;
        }
        if (target.tiles().isEmpty() || first.getWidth() != first.getHeight()) {
            CraftEngine.instance().logger().warn("Exporting first frame of Bedrock texture " + target.path() + ": no compatible terrain flipbook target");
            writeImage(root, textures, owner, target.path(), first);
            return;
        }
        int count = duration / interval;
        BufferedImage strip = new BufferedImage(first.getWidth(), Math.multiplyExact(first.getHeight(), count), BufferedImage.TYPE_INT_ARGB);
        for (int index = 0; index < inputs.size(); index++) {
            TextureAnimation animation = inputs.get(index).animation();
            if (animation != null) {
                frames.set(index, new BufferedImage(animation.width(), animation.height(), BufferedImage.TYPE_INT_ARGB));
            }
        }
        Graphics2D graphics = strip.createGraphics();
        graphics.setComposite(AlphaComposite.Src);
        try {
            for (int frame = 0; frame < count; frame++) {
                for (int index = 0; index < inputs.size(); index++) {
                    TextureAnimation animation = inputs.get(index).animation();
                    if (animation != null) {
                        animation.render(frame * interval, 0xffffff, frames.get(index));
                    }
                }
                graphics.drawImage(transform.apply(frames), 0, frame * first.getHeight(), null);
            }
        } finally {
            graphics.dispose();
        }
        if (writeImage(root, textures, owner, target.path(), strip)) {
            String path = target.path().substring(0, target.path().lastIndexOf('.'));
            for (BedrockTextureRule.Tile tile : target.tiles()) {
                textures.flipbook(tile.name(), tile.index(), path, interval, tile.replicate());
            }
        }
    }

    static boolean writeImage(Path root, BedrockTextureRegistry textures, String owner, String output, BufferedImage image) throws IOException {
        if (!textures.claim(output, owner)) {
            return false;
        }
        JsonObject tga = RULES.getAsJsonObject("tga").getAsJsonObject(output);
        if (tga != null) {
            ImageUtils.writeTga(image, root.resolve(tga.get("path").getAsString()));
            if (!tga.get("keep").getAsBoolean()) {
                return true;
            }
        }
        ImageUtils.writePng(image, root.resolve(output));
        return true;
    }

    private static int gcd(int first, int second) {
        while (second != 0) {
            int remainder = first % second;
            first = second;
            second = remainder;
        }
        return first;
    }

    static void warn(String source, Exception exception) {
        CraftEngine.instance().logger().warn("Skipping Bedrock vanilla texture " + source + ": " + exception.getMessage());
    }
}
