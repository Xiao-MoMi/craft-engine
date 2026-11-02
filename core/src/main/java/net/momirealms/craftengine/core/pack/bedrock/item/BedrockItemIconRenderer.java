package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel.GeometryKind;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel.Texture;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel.Transform;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureUsage;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayPosition;
import net.momirealms.craftengine.core.util.ImageUtils;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource.WHITE;

public final class BedrockItemIconRenderer {
    private static final EnumSet<DisplayPosition> HANDHELD_POSITIONS = EnumSet.of(
            DisplayPosition.FIRSTPERSON_RIGHTHAND, DisplayPosition.FIRSTPERSON_LEFTHAND,
            DisplayPosition.THIRDPERSON_RIGHTHAND, DisplayPosition.THIRDPERSON_LEFTHAND);
    private final JavaModelResolver models;
    private final JavaPackTextureSource textures;
    private JavaModel vanillaIconModel;

    public BedrockItemIconRenderer(JavaModelResources resources) {
        this.models = resources.models();
        this.textures = resources.textures();
    }

    @Nullable
    public BufferedImage render(ItemRenderTarget.Model target) throws IOException {
        JavaModel model = this.models.resolve(target.model());
        List<Texture> textures = this.resolveIconLayers(model);
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

    @Nullable
    private List<Texture> resolveIconLayers(JavaModel model) throws IOException {
        if (model.kind() != GeometryKind.GENERATED) {
            return null;
        }
        JavaModel vanilla = this.vanillaIconModel();
        if (model.guiLight() != vanilla.guiLight()) {
            return null;
        }
        for (Map.Entry<DisplayPosition, Transform> entry : model.display().entrySet()) {
            if (!HANDHELD_POSITIONS.contains(entry.getKey()) && !entry.getValue().equals(vanilla.display().get(entry.getKey()))) {
                return null;
            }
        }
        List<Texture> layers = this.models.resolveLayers(model);
        for (Texture texture : layers) {
            if (texture.forceTranslucent()) {
                return null;
            }
        }
        return layers;
    }

    boolean needsHandheldAttachable(JavaModel model) throws IOException {
        JavaModel vanilla = this.vanillaIconModel();
        for (DisplayPosition position : HANDHELD_POSITIONS) {
            if (!model.display().getOrDefault(position, Transform.IDENTITY).equals(vanilla.display().get(position))) {
                return true;
            }
        }
        return false;
    }

    private JavaModel vanillaIconModel() throws IOException {
        if (this.vanillaIconModel == null) {
            this.vanillaIconModel = this.models.resolveVanilla(Key.minecraft("item/generated"));
        }
        return this.vanillaIconModel;
    }
}
