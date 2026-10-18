package net.momirealms.craftengine.core.pack.bedrock.texture;

import net.momirealms.craftengine.core.pack.bedrock.model.BedrockModelConverter;
import net.momirealms.craftengine.core.util.HashUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.PngOptimizer;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class BedrockAnimatedTextures {

    private BedrockAnimatedTextures() {
    }

    public static List<Track> write(BedrockModelConverter.Result model, Key resource, Path root) throws IOException {
        return write(model, resource, root, true);
    }

    public static List<Track> write(BedrockModelConverter.Result model, Key resource, Path root, boolean separateFrames) throws IOException {
        BedrockTextureAtlas atlas = model.atlas();
        if (atlas.animations().isEmpty()) {
            return List.of();
        }
        String name = HashUtils.sha1(resource.asString().getBytes(StandardCharsets.UTF_8));
        int size = Math.max(atlas.image().getWidth(), atlas.image().getHeight());
        List<Map.Entry<BedrockTextureAtlas.TextureId, BedrockTextureAtlas.AnimatedSprite>> sprites = new ArrayList<>(atlas.animations().entrySet());
        sprites.sort(Comparator.comparing(entry -> entry.getValue().material()));
        List<Track> tracks = new ArrayList<>(sprites.size());
        for (int index = 0; index < sprites.size(); index++) {
            Map.Entry<BedrockTextureAtlas.TextureId, BedrockTextureAtlas.AnimatedSprite> entry = sprites.get(index);
            BedrockTextureAtlas.AnimatedSprite sprite = entry.getValue();
            TextureAnimation animation = sprite.animation();
            BedrockTextureAtlas.Region region = atlas.regions().get(entry.getKey());
            int interval = animation.sampleInterval();
            int count = animation.duration() / interval;
            String id = name + '_' + index;
            String path = "textures/ce_anim/" + id;
            BufferedImage strip = new BufferedImage(size, Math.multiplyExact(size, count), BufferedImage.TYPE_INT_ARGB);
            int[] stripPixels = ((DataBufferInt) strip.getRaster().getDataBuffer()).getData();
            BufferedImage frame = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            int[] framePixels = ((DataBufferInt) frame.getRaster().getDataBuffer()).getData();
            BufferedImage sample = new BufferedImage(animation.width(), animation.height(), BufferedImage.TYPE_INT_ARGB);
            List<String> frames = separateFrames ? new ArrayList<>(count) : List.of();
            // Independent sprite tracks avoid multiplying frame counts when a model mixes different animation periods.
            for (int sampleIndex = 0; sampleIndex < count; sampleIndex++) {
                animation.render(sampleIndex * interval, sprite.color(), sample);
                BedrockTextureAtlas.copySprite(sample, framePixels, size, region);
                System.arraycopy(framePixels, 0, stripPixels, sampleIndex * framePixels.length, framePixels.length);
                if (separateFrames) {
                    String framePath = path + '/' + sampleIndex;
                    writePng(frame, root.resolve(framePath + ".png"));
                    frames.add(framePath);
                }
            }
            writePng(strip, root.resolve(path + ".png"));
            tracks.add(new Track(sprite.material(), "ce_anim_" + id, path, id, interval, size, List.copyOf(frames)));
        }
        return List.copyOf(tracks);
    }

    private static void writePng(BufferedImage image, Path path) throws IOException {
        Files.createDirectories(path.getParent());
        try (OutputStream output = Files.newOutputStream(path)) {
            new PngOptimizer(image, 0).write(output);
        }
    }

    public record Track(String material, String texture, String strip, String resource, int interval, int size, List<String> frames) {
    }
}
