package net.momirealms.craftengine.core.pack.bedrock.texture;

import net.momirealms.craftengine.core.pack.atlas.Atlas;
import net.momirealms.craftengine.core.pack.atlas.UnstitchSource;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.util.ClientPngReader;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MinecraftVersion;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class JavaPackTextureSource implements TextureSource {
    private static final Key BLOCKS = Key.minecraft("blocks");
    private static final Key ITEMS = Key.minecraft("items");
    private final JavaResourcePack pack;
    private final Map<Key, Atlas> atlases = new HashMap<>();

    public JavaPackTextureSource(JavaResourcePack pack) {
        this.pack = pack;
    }

    @Override
    public BufferedImage read(TextureId texture) throws IOException {
        return this.load(texture).image();
    }

    @Override
    public TextureData load(TextureId texture) throws IOException {
        Key atlasId = texture.atlas();
        if (atlasId == null) {
            atlasId = this.pack.version().isAtOrAbove(MinecraftVersion.V1_21_11) ? ITEMS : BLOCKS;
        }
        Atlas atlas = this.atlas(atlasId);
        // Java MaterialBaker tries the item atlas before the block atlas for implicit materials.
        if (texture.atlas() == null && atlasId.equals(ITEMS) && !atlas.isDefined(texture.sprite())) {
            atlasId = BLOCKS;
            atlas = this.atlas(atlasId);
        }
        Key resource = atlas.getSourceTexturePath(texture.sprite());
        Atlas.TextureRegion region = atlas.getTextureRegion(texture.sprite());
        if (region != null) {
            resource = region.resource();
        }
        if (resource == null) {
            throw new IOException("Sprite " + texture.sprite() + " in " + atlasId + " is missing or requires an unsupported generated atlas source");
        }
        String path = "assets/" + resource.namespace() + "/textures/" + resource.value() + ".png";
        try (InputStream input = this.pack.openResource(path)) {
            if (input == null) {
                throw new IOException("Missing texture " + resource + " for sprite " + texture.sprite());
            }
            BufferedImage image = ClientPngReader.read(input);
            if (image == null) {
                throw new IOException("Invalid PNG texture " + resource);
            }
            if (region != null) {
                UnstitchSource.Region rectangle = region.region();
                double scaleX = image.getWidth() / region.divisorX();
                double scaleY = image.getHeight() / region.divisorY();
                int x = (int) Math.floor(rectangle.x() * scaleX);
                int y = (int) Math.floor(rectangle.y() * scaleY);
                int width = (int) Math.floor(rectangle.width() * scaleX);
                int height = (int) Math.floor(rectangle.height() * scaleY);
                if (width <= 0 || height <= 0 || x < 0 || y < 0 || x > image.getWidth() - width || y > image.getHeight() - height) {
                    throw new IOException("Invalid unstitch region for sprite " + texture.sprite());
                }
                return new TextureData(image.getSubimage(x, y, width, height), null);
            }
            return JavaTextureAnimation.read(image, this.pack.readResourceMetadata(path), this.pack.version().isAtOrAbove(MinecraftVersion.V1_21_11));
        }
    }

    // 按需加载 atlas
    private Atlas atlas(Key id) throws IOException {
        Atlas atlas = this.atlases.get(id);
        if (atlas != null) {
            return atlas;
        }
        atlas = new Atlas(this.pack.readAtlas(id));
        this.atlases.put(id, atlas);
        return atlas;
    }
}
