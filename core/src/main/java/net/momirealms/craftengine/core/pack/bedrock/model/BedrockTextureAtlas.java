package net.momirealms.craftengine.core.pack.bedrock.model;

import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.PngOptimizer;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

public record BedrockTextureAtlas(BufferedImage image, Map<TextureId, Region> regions, Map<TextureId, AnimatedSprite> animations) {
    private static final int PADDING = 1;
    private static final Comparator<Key> KEY_ORDER = Comparator.comparing(Key::namespace).thenComparing(Key::value);
    private static final Comparator<TextureId> TEXTURE_ORDER = Comparator.comparing(TextureId::sprite, KEY_ORDER)
            .thenComparing(TextureId::atlas, Comparator.nullsFirst(KEY_ORDER)).thenComparingInt(TextureId::color);
    private static final Comparator<Sprite> SPRITE_ORDER = Comparator.comparingInt((Sprite sprite) -> sprite.image().getHeight()).reversed()
            .thenComparing(Comparator.comparingInt((Sprite sprite) -> sprite.image().getWidth()).reversed())
            .thenComparing(Sprite::id, TEXTURE_ORDER);

    public BedrockTextureAtlas {
        regions = Map.copyOf(regions);
        animations = Map.copyOf(animations);
    }

    public BedrockTextureAtlas(BufferedImage image, Map<TextureId, Region> regions) {
        this(image, regions, Map.of());
    }

    public static BedrockTextureAtlas create(JavaModel model, TextureSource source) throws IOException {
        return create(model, source, List.of());
    }

    public static BedrockTextureAtlas create(JavaModel model, TextureSource source, List<Integer> tints) throws IOException {
        TreeSet<TextureId> textures = new TreeSet<>(TEXTURE_ORDER);
        for (JavaModel.Element element : model.elements()) {
            for (JavaModel.Face face : element.faces().values()) {
                textures.add(TextureId.of(face, tints));
            }
        }
        List<Sprite> sprites = new ArrayList<>(textures.size());
        long area = 0;
        int widest = 1;
        Map<TextureId, TextureData> originals = new HashMap<>();
        Map<TextureId, AnimatedSprite> animations = new LinkedHashMap<>();
        for (TextureId texture : textures) {
            TextureId original = new TextureId(texture.sprite(), texture.atlas());
            TextureData data = originals.get(original);
            if (data == null) {
                data = source.load(original);
                if (data != null) {
                    originals.put(original, data);
                }
            }
            BufferedImage image = data == null ? null : data.image();
            if (image == null) {
                throw new IOException("Missing static sprite " + texture + " in model " + model.id());
            }
            if (texture.color() != 0xFFFFFF) {
                image = tint(image, texture.color());
            }
            if (data.animation() != null) {
                animations.put(texture, new AnimatedSprite("ce_anim_" + animations.size(), data.animation(), texture.color()));
            }
            sprites.add(new Sprite(texture, image));
            int width = Math.addExact(image.getWidth(), PADDING * 2);
            int height = Math.addExact(image.getHeight(), PADDING * 2);
            area = Math.addExact(area, (long) width * height);
            widest = Math.max(widest, width);
        }
        if (sprites.isEmpty()) {
            return new BedrockTextureAtlas(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), Map.of());
        }
        if (sprites.size() == 1) {
            Sprite sprite = sprites.getFirst();
            return new BedrockTextureAtlas(sprite.image(), Map.of(sprite.id(),
                    new Region(0, 0, sprite.image().getWidth(), sprite.image().getHeight())), animations);
        }
        // Stable shelf packing: texture identifiers break ties independently of map iteration order.
        sprites.sort(SPRITE_ORDER);
        int width = powerOfTwo(Math.max(widest, (int) Math.ceil(Math.sqrt(area))));
        Map<TextureId, Region> regions = new HashMap<>(sprites.size());
        int x = 0;
        int y = 0;
        int rowHeight = 0;
        for (Sprite sprite : sprites) {
            int spriteWidth = sprite.image().getWidth() + PADDING * 2;
            int spriteHeight = sprite.image().getHeight() + PADDING * 2;
            if (x > width - spriteWidth) {
                x = 0;
                y = Math.addExact(y, rowHeight);
                rowHeight = 0;
            }
            regions.put(sprite.id(), new Region(x + PADDING, y + PADDING, sprite.image().getWidth(), sprite.image().getHeight()));
            x += spriteWidth;
            rowHeight = Math.max(rowHeight, spriteHeight);
        }
        int height = powerOfTwo(Math.addExact(y, rowHeight));
        BufferedImage atlas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = ((DataBufferInt) atlas.getRaster().getDataBuffer()).getData();
        for (Sprite sprite : sprites) {
            Region region = regions.get(sprite.id());
            copySprite(sprite.image(), pixels, width, region);
        }
        return new BedrockTextureAtlas(atlas, regions, animations);
    }

    public Region region(JavaModel.Texture texture) {
        return this.region(TextureId.of(texture));
    }

    public Region region(JavaModel.Face face, List<Integer> tints) {
        return this.region(TextureId.of(face, tints));
    }

    private Region region(TextureId texture) {
        Region region = this.regions.get(texture);
        if (region == null) {
            throw new IllegalArgumentException("Sprite is not in the model atlas: " + texture);
        }
        return region;
    }

    static BufferedImage tint(BufferedImage source, int color) {
        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int[] pixels = ((DataBufferInt) result.getRaster().getDataBuffer()).getData();
        PngOptimizer.readPixels(source, pixels, 0, source.getWidth());
        for (int index = 0; index < pixels.length; index++) {
            int pixel = pixels[index];
            int red = ((pixel >>> 16) & 255) * ((color >>> 16) & 255) / 255;
            int green = ((pixel >>> 8) & 255) * ((color >>> 8) & 255) / 255;
            int blue = (pixel & 255) * (color & 255) / 255;
            pixels[index] = (pixel & 0xFF000000) | (red << 16) | (green << 8) | blue;
        }
        return result;
    }

    public static void copySprite(BufferedImage image, int[] pixels, int stride, Region region) {
        PngOptimizer.readPixels(image, pixels, region.y() * stride + region.x(), stride);
        if (region.x() == 0 || region.y() == 0) {
            return;
        }
        // Duplicate edge pixels, including corners, so filtering does not sample the adjacent sprite.
        for (int y = region.y(); y < region.y() + region.height(); y++) {
            int offset = y * stride + region.x();
            pixels[offset - 1] = pixels[offset];
            pixels[offset + region.width()] = pixels[offset + region.width() - 1];
        }
        int firstRow = region.y() * stride + region.x() - 1;
        int lastRow = (region.y() + region.height() - 1) * stride + region.x() - 1;
        System.arraycopy(pixels, firstRow, pixels, firstRow - stride, region.width() + PADDING * 2);
        System.arraycopy(pixels, lastRow, pixels, lastRow + stride, region.width() + PADDING * 2);
    }

    private static int powerOfTwo(int value) {
        if (value <= 0 || value > (1 << 30)) {
            throw new IllegalArgumentException("Model atlas dimension is too large: " + value);
        }
        return value == 1 ? 1 : Integer.highestOneBit(value - 1) << 1;
    }

    @FunctionalInterface
    public interface TextureSource {
        BufferedImage read(TextureId texture) throws IOException;

        default TextureData load(TextureId texture) throws IOException {
            return new TextureData(this.read(texture), null);
        }
    }

    public record TextureData(BufferedImage image, @Nullable TextureAnimation animation) {
    }

    public record AnimatedSprite(String material, TextureAnimation animation, int color) {
    }

    public record TextureId(Key sprite, @Nullable Key atlas, int color) {

        public TextureId(Key sprite, @Nullable Key atlas) {
            this(sprite, atlas, 0xFFFFFF);
        }

        public static TextureId of(JavaModel.Face face, List<Integer> tints) {
            int index = face.tintIndex();
            int color = index >= 0 && index < tints.size() ? tints.get(index) & 0xFFFFFF : 0xFFFFFF;
            return new TextureId(face.texture().sprite(), face.texture().atlas(), color);
        }

        public static TextureId of(JavaModel.Texture texture) {
            return new TextureId(texture.sprite(), texture.atlas());
        }
    }

    public record Region(int x, int y, int width, int height) {
    }

    private record Sprite(TextureId id, BufferedImage image) {
    }
}
