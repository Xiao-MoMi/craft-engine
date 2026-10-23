package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel.Texture;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureUsage;
import net.momirealms.craftengine.core.util.ImageUtils;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource.WHITE;

public final class BedrockItemIconRenderer {
    private final JavaModelResolver models;
    private final JavaPackTextureSource textures;

    public BedrockItemIconRenderer(JavaModelResolver models, JavaPackTextureSource textures) {
        this.models = models;
        this.textures = textures;
    }

    @Nullable
    public BufferedImage render(ItemRenderTarget.Model target) throws IOException {
        JavaModel model = this.models.resolve(target.model());
        List<Texture> textures = this.models.resolveIconLayers(model);
        if (textures == null || textures.isEmpty()) {
            return null;
        }
        List<BufferedImage> layers = new ArrayList<>(textures.size());
        for (int index = 0; index < textures.size(); index++) {
            Integer color = index < target.tints().size() ? this.textures.resolveTint(target.tints().get(index)) : WHITE;
            BufferedImage image = this.textures.readStatic(textures.get(index), TextureUsage.ITEM);
            if (image == null) {
                return null;
            }
            // 先按各层 tint 染色再合成，未染色的 overlay 保持原色。
            layers.add(color == 0xffffff ? image : ImageUtils.multiplyColor(image, color));
        }
        return ImageUtils.composite(layers);
    }
}
