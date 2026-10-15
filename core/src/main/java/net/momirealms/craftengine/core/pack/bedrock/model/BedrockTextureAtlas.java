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
import java.util.Map;
import java.util.TreeSet;

public record BedrockTextureAtlas(BufferedImage image, Map<TextureId, Region> regions) {
    private static final int PADDING = 1;
    private static final Comparator<Key> KEY_ORDER = Comparator.comparing(Key::namespace).thenComparing(Key::value);
    private static final Comparator<TextureId> TEXTURE_ORDER = Comparator.comparing(TextureId::sprite, KEY_ORDER)
            .thenComparing(TextureId::atlas, Comparator.nullsFirst(KEY_ORDER));
    private static final Comparator<Sprite> SPRITE_ORDER = Comparator.comparingInt((Sprite sprite) -> sprite.image().getHeight()).reversed()
            .thenComparing(Comparator.comparingInt((Sprite sprite) -> sprite.image().getWidth()).reversed())
            .thenComparing(Sprite::id, TEXTURE_ORDER);

    public BedrockTextureAtlas {
        regions = Map.copyOf(regions);
    }

    public static BedrockTextureAtlas create(JavaModel model, TextureSource source) throws IOException {
        TreeSet<TextureId> textures = new TreeSet<>(TEXTURE_ORDER);
        for (JavaModel.Element element : model.elements()) {
            for (JavaModel.Face face : element.faces().values()) {
                textures.add(TextureId.of(face.texture()));
            }
        }
        List<Sprite> sprites = new ArrayList<>(textures.size());
        long area = 0;
        int widest = 1;
        for (TextureId texture : textures) {
            BufferedImage image = source.read(texture);
            if (image == null) {
                throw new IOException("Missing static sprite " + texture + " in model " + model.id());
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
                    new Region(0, 0, sprite.image().getWidth(), sprite.image().getHeight())));
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
            PngOptimizer.readPixels(sprite.image(), pixels, region.y() * width + region.x(), width);
            extrudeBorder(pixels, width, region);
        }
        return new BedrockTextureAtlas(atlas, regions);
    }

    public Region region(JavaModel.Texture texture) {
        Region region = this.regions.get(TextureId.of(texture));
        if (region == null) {
            throw new IllegalArgumentException("Sprite is not in the model atlas: " + texture);
        }
        return region;
    }

    private static void extrudeBorder(int[] pixels, int stride, Region region) {
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
    }

    public record TextureId(Key sprite, @Nullable Key atlas) {

        public static TextureId of(JavaModel.Texture texture) {
            return new TextureId(texture.sprite(), texture.atlas());
        }
    }

    public record Region(int x, int y, int width, int height) {
    }

    private record Sprite(TextureId id, BufferedImage image) {
    }
}
