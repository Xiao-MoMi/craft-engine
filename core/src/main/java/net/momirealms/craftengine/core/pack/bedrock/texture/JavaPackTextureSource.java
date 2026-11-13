package net.momirealms.craftengine.core.pack.bedrock.texture;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.momirealms.craftengine.core.pack.atlas.Atlas;
import net.momirealms.craftengine.core.pack.atlas.UnstitchSource;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel.Texture;
import net.momirealms.craftengine.core.pack.mcmeta.PackVersion;
import net.momirealms.craftengine.core.pack.model.definition.tint.ConstantTint;
import net.momirealms.craftengine.core.pack.model.definition.tint.GrassTint;
import net.momirealms.craftengine.core.pack.model.definition.tint.SimpleDefaultTint;
import net.momirealms.craftengine.core.pack.model.definition.tint.Tint;
import net.momirealms.craftengine.core.util.ClientPngReader;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import net.momirealms.craftengine.core.util.ResourcePackUtils;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class JavaPackTextureSource {
    public static final Integer WHITE = 0xffffff;
    private static final Key ITEMS = Key.minecraft("items");
    private static final Key BLOCKS = Key.minecraft("blocks");
    private static final Key DYE = Key.minecraft("dye");
    private final JavaResourcePack pack;
    private final Map<Key, Atlas> atlases = new HashMap<>();
    private final Map<Key, Map<Texture, TextureData>> textures = new HashMap<>();
    private final Map<Key, Map<Texture, IOException>> textureFailures = new HashMap<>();
    private final Map<Key, BufferedImage> paletteImages = new HashMap<>();
    private final Map<Key, TextureData> headSkins = new HashMap<>();
    private final Map<Key, TextureData> standaloneTextures = new HashMap<>();
    private final Map<Key, Map<Key, Int2IntMap>> palettes = new HashMap<>();
    private BufferedImage grassColormap;

    public JavaPackTextureSource(JavaResourcePack pack) {
        this.pack = pack;
    }

    public Integer resolveTint(Tint tint) throws IOException {
        if (tint instanceof ConstantTint constant) {
            return rgb(constant.value());
        }
        Integer dye = this.dyeColor(tint);
        if (dye != null) {
            return dye;
        }
        if (tint instanceof GrassTint grass) {
            double temperature = grass.temperature();
            double downfall = grass.downfall();
            if (!(temperature >= 0 && temperature <= 1 && downfall >= 0 && downfall <= 1)) {
                throw new IOException("Grass tint temperature and downfall must be between 0 and 1");
            }
            if (this.grassColormap == null) {
                String path = "assets/minecraft/textures/colormap/grass.png";
                try (InputStream input = this.pack.openResource(path)) {
                    BufferedImage image = input == null ? null : ClientPngReader.read(input);
                    if (image == null || image.getWidth() != 256 || image.getHeight() != 256) {
                        throw new IOException("Expected a 256x256 grass colormap at " + path);
                    }
                    this.grassColormap = image;
                }
            }
            int x = (int) ((1 - temperature) * 255);
            int y = (int) ((1 - downfall * temperature) * 255);
            return this.grassColormap.getRGB(x, y) & 0xffffff;
        }
        return WHITE;
    }

    @Nullable
    public Integer dyeColor(Tint tint) {
        if (tint instanceof SimpleDefaultTint dye && DYE.equals(dye.type())) {
            return dye.defaultValue() == null ? WHITE : rgb(dye.defaultValue());
        }
        return null;
    }

    private static int rgb(Either<Integer, List<Float>> color) {
        return color.map(value -> value & 0xffffff, channels ->
                ((int) (channels.get(0) * 255) << 16) | ((int) (channels.get(1) * 255) << 8) | (int) (channels.get(2) * 255));
    }

    public TextureData read(Texture texture, TextureUsage usage) throws IOException {
        if (texture.standalone()) {
            return this.readStandalone(texture.sprite());
        }
        if (texture.skin()) {
            return this.readHeadSkin(texture.sprite());
        }
        boolean useItemAtlas = usage == TextureUsage.ITEM && this.pack.version().isAtOrAbove(MinecraftVersion.V1_21_11);
        if (useItemAtlas) {
            TextureData data = this.readFromAtlas(texture, ITEMS);
            if (data != null) {
                return data;
            }
        }
        TextureData data = this.readFromAtlas(texture, BLOCKS);
        if (data == null) {
            throw this.textureFailure(texture, usage, useItemAtlas);
        }
        return data;
    }

    private IOException textureFailure(Texture texture, TextureUsage usage, boolean useItemAtlas) {
        IOException itemFailure = useItemAtlas ? this.atlasFailure(texture, ITEMS) : null;
        IOException blockFailure = this.atlasFailure(texture, BLOCKS);
        String message = "No successfully loaded sprite " + texture.sprite() + " for " + usage;
        if (itemFailure != null) {
            message += "; " + itemFailure.getMessage();
        }
        if (blockFailure != null) {
            message += "; " + blockFailure.getMessage();
        }
        IOException failure = new IOException(message);
        if (itemFailure != null) {
            failure.addSuppressed(itemFailure);
        }
        if (blockFailure != null) {
            failure.addSuppressed(blockFailure);
        }
        return failure;
    }

    @Nullable
    private IOException atlasFailure(Texture texture, Key atlas) {
        Map<Texture, IOException> failures = this.textureFailures.get(atlas);
        return failures == null ? null : failures.get(texture);
    }

    private TextureData readStandalone(Key texture) throws IOException {
        TextureData cached = this.standaloneTextures.get(texture);
        if (cached != null) {
            return cached;
        }
        try (InputStream input = this.pack.openResource(ResourcePackUtils.texturePath(texture))) {
            BufferedImage image = input == null ? null : ClientPngReader.read(input);
            if (image == null) {
                throw new IOException("Missing standalone texture " + texture);
            }
            TextureData data = this.withVanillaReference(texture, this.pack.readResourceMetadata(ResourcePackUtils.texturePath(texture)), new TextureData(image, null));
            this.standaloneTextures.put(texture, data);
            return data;
        }
    }

    private TextureData withVanillaReference(Key resource, @Nullable JsonObject metadata, TextureData data) {
        String reference = BedrockVanillaTextureReferences.resolve(this.pack, resource, metadata);
        return reference == null ? data : new TextureData(data.image(), data.animation(), reference);
    }

    private TextureData readHeadSkin(Key skin) throws IOException {
        TextureData cached = this.headSkins.get(skin);
        if (cached != null) {
            return cached;
        }
        try (InputStream input = this.pack.openResource(ResourcePackUtils.texturePath(skin))) {
            BufferedImage image = input == null ? null : ClientPngReader.read(input);
            if (image == null || (image.getWidth() != image.getHeight() && image.getWidth() != 2 * image.getHeight())) {
                throw new UnsupportedOperationException("Missing or invalid player head texture: " + skin);
            }
            if (image.getWidth() != image.getHeight()) {
                BufferedImage square = new BufferedImage(image.getWidth(), image.getWidth(), BufferedImage.TYPE_INT_ARGB);
                square.setRGB(0, 0, image.getWidth(), image.getHeight(), image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()), 0, image.getWidth());
                image = square;
            }
            TextureData data = new TextureData(image, null);
            this.headSkins.put(skin, data);
            return data;
        }
    }

    @Nullable
    private TextureData readFromAtlas(Texture texture, Key atlasId) throws IOException {
        Map<Texture, TextureData> atlasTextures = this.textures.computeIfAbsent(atlasId, ignored -> new HashMap<>());
        if (atlasTextures.containsKey(texture)) {
            return atlasTextures.get(texture);
        }
        TextureData data;
        try {
            data = this.load(texture, atlasId, this.atlas(atlasId));
        } catch (IOException | RuntimeException e) {
            // 失败的 sprite 继续参与后续图集回退，所有候选失败时才汇总原因。
            IOException failure = new IOException("Failed to load sprite " + texture.sprite() + " in atlas " + atlasId + ": " + e.getMessage(), e);
            this.textureFailures.computeIfAbsent(atlasId, ignored -> new HashMap<>()).put(texture, failure);
            data = null;
        }
        atlasTextures.put(texture, data);
        return data;
    }

    @Nullable
    private TextureData load(Texture texture, Key atlasId, Atlas atlas) throws IOException {
        Key spriteId = texture.sprite();

        Atlas.TexturePalette palette = atlas.getTexturePalette(spriteId);
        if (palette != null) {
            try {
                return this.readPalette(palette);
            } catch (IOException | RuntimeException e) {
                throw new IOException("Failed to load palette texture " + ResourcePackUtils.texturePath(palette.resource()) + ": " + e.getMessage(), e);
            }
        }

        Atlas.TextureRegion region = atlas.getTextureRegion(spriteId);
        Key resourceId;
        if (region != null) {
            resourceId = region.resource();
        } else {
            resourceId = atlas.getSourceTexturePath(spriteId);
        }
        if (resourceId == null) {
            if (atlas.isDefined(spriteId)) {
                return null;
            }
            throw new IOException("Missing sprite " + spriteId + " in atlas " + atlasId);
        }

        try {
            return this.loadResource(resourceId, region);
        } catch (IOException | RuntimeException e) {
            throw new IOException("Failed to load texture " + ResourcePackUtils.texturePath(resourceId) + ": " + e.getMessage(), e);
        }
    }

    private TextureData loadResource(Key resourceId, @Nullable Atlas.TextureRegion region) throws IOException {
        String texturePath = ResourcePackUtils.texturePath(resourceId);
        JsonObject metadata;
        try {
            metadata = this.pack.readResourceMetadata(texturePath);
        } catch (IOException | RuntimeException e) {
            throw new IOException("Failed to read texture metadata " + texturePath + ".mcmeta: " + e.getMessage(), e);
        }
        BufferedImage image;
        try (InputStream input = this.pack.openResource(texturePath)) {
            if (input == null) {
                throw new IOException("Missing texture " + resourceId);
            }
            image = ClientPngReader.read(input);
            if (image == null) {
                throw new IOException("Invalid PNG texture " + resourceId);
            }
        }

        if (region == null) {
            TextureData data = JavaTextureAnimation.read(image, metadata, this.pack.version().isAtOrAbove(MinecraftVersion.V1_21_11));
            return this.withVanillaReference(resourceId, metadata, data);
        }

        // unstitch 使用整张图片切片，不应用源图片的动画元数据。
        UnstitchSource.Region rectangle = region.region();
        double scaleX = image.getWidth() / region.divisorX();
        double scaleY = image.getHeight() / region.divisorY();
        int x = (int) Math.floor(rectangle.x() * scaleX);
        int y = (int) Math.floor(rectangle.y() * scaleY);
        int width = (int) Math.floor(rectangle.width() * scaleX);
        int height = (int) Math.floor(rectangle.height() * scaleY);
        if (width <= 0 || height <= 0 || x < 0 || y < 0 || x > image.getWidth() - width || y > image.getHeight() - height) {
            throw new IOException("Invalid unstitch region for texture " + resourceId);
        }
        return new TextureData(image.getSubimage(x, y, width, height), null);
    }

    private Atlas atlas(Key id) throws IOException {
        Atlas atlas = this.atlases.get(id);
        if (atlas == null) {
            try {
                atlas = new Atlas(this.pack.readAtlas(id));
            } catch (IOException | RuntimeException e) {
                throw new IOException("Failed to load atlas assets/" + id.namespace() + "/atlases/" + id.value() + ".json: " + e.getMessage(), e);
            }
            this.atlases.put(id, atlas);
        }
        return atlas;
    }

    private TextureData readPalette(Atlas.TexturePalette palette) throws IOException {
        Map<Key, Int2IntMap> permutations = this.palettes.computeIfAbsent(palette.key(), key -> new HashMap<>());
        Int2IntMap colors = permutations.get(palette.palette());
        if (colors == null) {
            boolean modern = this.pack.version().packFormat().isAtOrAbove(PackVersion.PALETTE_TEXTURES_VERSION);
            BufferedImage keys = this.paletteImage(modern ? Key.of(palette.key().namespace(), "palettes/" + palette.key().value()) : palette.key());
            BufferedImage values = this.paletteImage(modern ? Key.of(palette.palette().namespace(), "palettes/" + palette.palette().value()) : palette.palette());
            int size = keys.getWidth() * keys.getHeight();
            if (size != values.getWidth() * values.getHeight()) {
                throw new IOException("Palette sizes differ: " + palette.key() + " and " + palette.palette());
            }
            colors = new Int2IntOpenHashMap(size);
            for (int i = 0; i < size; i++) {
                int key = keys.getRGB(i % keys.getWidth(), i / keys.getWidth());
                if ((key >>> 24) != 0) {
                    colors.put(key | 0xff000000, values.getRGB(i % values.getWidth(), i / values.getWidth()));
                }
            }
            permutations.put(palette.palette(), colors);
        }
        BufferedImage source = this.paletteImage(palette.resource());
        BufferedImage image = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        // Java 调色忽略键的透明度，输出透明度相乘；生成的 sprite 不读取源动画元数据。
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int color = source.getRGB(x, y);
                int alpha = color >>> 24;
                if (alpha != 0) {
                    int opaque = color | 0xff000000;
                    int mapped = colors.getOrDefault(opaque, opaque);
                    color = ((alpha * (mapped >>> 24) / 255) << 24) | (mapped & 0xffffff);
                }
                image.setRGB(x, y, color);
            }
        }
        return new TextureData(image, null);
    }

    private BufferedImage paletteImage(Key resource) throws IOException {
        BufferedImage image = this.paletteImages.get(resource);
        if (image == null) {
            try (InputStream input = this.pack.openResource(ResourcePackUtils.texturePath(resource))) {
                if (input == null || (image = ClientPngReader.read(input)) == null) {
                    throw new IOException("Missing or invalid palette texture " + resource);
                }
            } catch (IOException | RuntimeException e) {
                throw new IOException("Failed to load palette image " + ResourcePackUtils.texturePath(resource) + ": " + e.getMessage(), e);
            }
            this.paletteImages.put(resource, image);
        }
        return image;
    }
}
