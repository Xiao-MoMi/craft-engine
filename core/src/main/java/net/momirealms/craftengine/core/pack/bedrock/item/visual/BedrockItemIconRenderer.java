package net.momirealms.craftengine.core.pack.bedrock.item.visual;

import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRenderTarget;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel.GeometryKind;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel.Texture;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel.Transform;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTexture;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureUsage;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayPosition;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource.WHITE;

public final class BedrockItemIconRenderer {
    private static final EnumSet<DisplayPosition> HANDHELD_POSITIONS = EnumSet.of(
            DisplayPosition.FIRSTPERSON_RIGHTHAND,
            DisplayPosition.FIRSTPERSON_LEFTHAND,
            DisplayPosition.THIRDPERSON_RIGHTHAND,
            DisplayPosition.THIRDPERSON_LEFTHAND
    );
    private final JavaModelResolver models;
    private final JavaPackTextureSource textures;
    private final Map<JavaModel, List<TextureData>> iconLayers = new IdentityHashMap<>();
    private JavaModel vanillaIconModel;

    public BedrockItemIconRenderer(JavaModelResources resources) {
        this.models = resources.models();
        this.textures = resources.textures();
    }

    @Nullable
    public BedrockTexture render(ItemRenderTarget.Model target) throws IOException {
        List<TextureData> textures = this.analyze(target);
        if (textures == null) {
            return null;
        }
        if (textures.size() == 1) {
            return this.renderLayer(textures.getFirst(), target, 0);
        }
        List<BedrockTexture> layers = new ArrayList<>(textures.size());
        for (int index = 0; index < textures.size(); index++) {
            // 先按各层 tint 染色再合成，未染色的 overlay 保持原色。
            layers.add(this.renderLayer(textures.get(index), target, index));
        }
        return BedrockTexture.composite(layers);
    }

    private BedrockTexture renderLayer(TextureData texture, ItemRenderTarget.Model target, int index) throws IOException {
        int color = index < target.tints().size() ? this.textures.resolveTint(target.tints().get(index)) : WHITE;
        return texture.staticTexture(color);
    }

    public boolean canRender(ItemRenderTarget.Model target) throws IOException {
        return this.analyze(target) != null;
    }

    @Nullable
    private List<TextureData> analyze(ItemRenderTarget.Model target) throws IOException {
        JavaModel model = target.resolve(this.models);
        if (this.iconLayers.containsKey(model)) {
            return this.iconLayers.get(model);
        }
        List<TextureData> layers = this.resolveIconLayers(model);
        this.iconLayers.put(model, layers);
        return layers;
    }

    @Nullable
    private List<TextureData> resolveIconLayers(JavaModel model) throws IOException {
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
        if (layers.isEmpty()) {
            return null;
        }
        for (Texture texture : layers) {
            if (texture.forceTranslucent()) {
                return null;
            }
        }
        List<TextureData> data = new ArrayList<>(layers.size());
        for (Texture texture : layers) {
            TextureData layer = this.textures.read(texture, TextureUsage.ITEM);
            if (layer.animation() != null) {
                return null;
            }
            data.add(layer);
        }
        return data;
    }

    public boolean needsHandheldAttachable(JavaModel model) throws IOException {
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
