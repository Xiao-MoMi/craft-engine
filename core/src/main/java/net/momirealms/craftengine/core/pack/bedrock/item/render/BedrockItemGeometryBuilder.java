package net.momirealms.craftengine.core.pack.bedrock.item.render;

import com.google.gson.JsonArray;
import net.momirealms.craftengine.bedrock.BedrockTintSource;
import net.momirealms.craftengine.core.pack.bedrock.BedrockResourceException;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingRule;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRenderTarget;
import net.momirealms.craftengine.core.pack.bedrock.model.*;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureUsage;
import net.momirealms.craftengine.core.pack.model.definition.tint.CustomModelDataTint;
import net.momirealms.craftengine.core.pack.model.definition.tint.Tint;
import org.joml.Matrix4fc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource.WHITE;

public final class BedrockItemGeometryBuilder {
    private final JavaModelResolver models;
    private final JavaPackTextureSource textures;
    private final TextureUsage textureUsage;
    private final boolean preserveComponentTints;
    private final boolean singleTintSource;
    private Integer tintSource;
    private final Map<TextureKey, BedrockElementGeometry.Material> bindings = new LinkedHashMap<>();
    private final List<BedrockItemGeometry.Material> materials = new ArrayList<>();

    public BedrockItemGeometryBuilder(JavaModelResolver models, JavaPackTextureSource textures) {
        this(models, textures, TextureUsage.ITEM);
    }

    public BedrockItemGeometryBuilder(JavaModelResolver models, JavaPackTextureSource textures, TextureUsage textureUsage) {
        this(models, textures, textureUsage, false);
    }

    public BedrockItemGeometryBuilder(JavaModelResolver models, JavaPackTextureSource textures, TextureUsage textureUsage, boolean preserveComponentTints) {
        this(models, textures, textureUsage, preserveComponentTints, true);
    }

    public BedrockItemGeometryBuilder(JavaModelResolver models, JavaPackTextureSource textures, TextureUsage textureUsage, boolean preserveComponentTints, boolean singleTintSource) {
        this.models = models;
        this.textures = textures;
        this.textureUsage = textureUsage;
        this.preserveComponentTints = preserveComponentTints;
        this.singleTintSource = singleTintSource;
    }

    public BedrockItemGeometry build(ItemBindingRule rule) throws IOException {
        return this.build(rule, false);
    }

    public BedrockItemGeometry build(ItemBindingRule rule, boolean preserveDye) throws IOException {
        return this.build(rule, preserveDye, ((ItemRenderTarget.Model) rule.target()).resolve(this.models));
    }

    public BedrockItemGeometry build(ItemBindingRule rule, boolean preserveDye, JavaModel model) throws IOException {
        Matrix4fc transformation = BedrockModelTransform.compose(rule.transformations());
        BedrockModelTransform.validate(transformation);
        ItemRenderTarget.Model target = (ItemRenderTarget.Model) rule.target();
        boolean bakeCubes = BedrockModelTransform.canBakeCubes(transformation);
        // 烘焙会绕各 cube 的中心计算并舍入坐标，保留原边缘面的中心才能逐顶点一致。
        boolean mergeEdges = !bakeCubes || BedrockModelTransform.isIdentity(transformation);
        JsonArray cubes = this.geometry(model, target, preserveDye, mergeEdges);
        // 等比变换沿用紧凑的 cubes；拉伸、反射、错切和零缩放保留给骨骼，避免改动 UV。
        if (bakeCubes) {
            BedrockModelTransform.apply(cubes, transformation, 1);
            transformation = BedrockModelTransform.compose(List.of());
        }
        return new BedrockItemGeometry(model, cubes, transformation);
    }

    public List<BedrockItemGeometry.Material> materials() {
        return this.materials;
    }

    public int tintSource() {
        return this.tintSource == null ? BedrockTintSource.NONE : this.tintSource;
    }

    private JsonArray geometry(JavaModel model, ItemRenderTarget.Model target, boolean preserveDye, boolean mergeEdges) throws IOException {
        if (model.kind() == JavaModel.GeometryKind.GENERATED) {
            JsonArray cubes = new JsonArray();
            List<JavaModel.Texture> layers = this.models.resolveLayers(model);
            for (int layer = 0; layer < layers.size(); layer++) {
                BedrockElementGeometry.Material material = this.material(layers.get(layer), target, layer, preserveDye);
                TextureData data = this.textures.read(layers.get(layer), this.textureUsage);
                BedrockGeneratedGeometry.addLayer(cubes, data, material.name(), layer, data.image().getWidth(), data.image().getHeight(), mergeEdges);
            }
            return cubes;
        }
        if (model.kind() == JavaModel.GeometryKind.ELEMENTS) {
            Map<JavaModel.Face, BedrockElementGeometry.Material> faces = new LinkedHashMap<>();
            for (JavaModel.Element element : model.elements()) {
                for (JavaModel.Face face : element.faces().values()) {
                    faces.put(face, this.material(face.texture(), target, face.tintIndex(), preserveDye));
                }
            }
            return BedrockElementGeometry.convert(model, faces);
        }
        throw new BedrockResourceException("resource_pack.bedrock.reason.geometry", model.id().asString());
    }

    private BedrockElementGeometry.Material material(JavaModel.Texture texture, ItemRenderTarget.Model target, int tintIndex, boolean preserveDye) throws IOException {
        Tint tint = tintIndex >= 0 && tintIndex < target.tints().size() ? target.tints().get(tintIndex) : null;
        int source = JavaPackTextureSource.tintSource(tint);
        if (preserveDye && this.preserveComponentTints && tint instanceof CustomModelDataTint custom && custom.index() < 0) {
            throw new BedrockResourceException("resource_pack.bedrock.reason.tint_source", "custom_model_data index " + custom.index());
        }
        boolean dynamic = preserveDye && (source == BedrockTintSource.DYED_COLOR || (this.preserveComponentTints && BedrockTintSource.isDynamic(source)));
        Integer dye = dynamic ? this.textures.resolveTint(tint) : null;
        if (dynamic && this.preserveComponentTints && this.singleTintSource) {
            // 展示实体共用一组 RGB 属性；只检查几何实际使用的染色源，防止把不同来源错误合并。
            if (this.tintSource != null && this.tintSource != source) {
                throw new BedrockResourceException("resource_pack.bedrock.reason.tint_source", "multiple dynamic color sources");
            }
            this.tintSource = source;
        }
        int color = tint != null && dye == null ? this.textures.resolveTint(tint) : WHITE;
        TextureKey key = new TextureKey(texture, color, dye);
        BedrockElementGeometry.Material material = this.bindings.get(key);
        if (material == null) {
            material = new BedrockElementGeometry.Material("layer" + this.materials.size(), 1, 1);
            this.materials.add(new BedrockItemGeometry.Material(this.textures.read(key.texture(), this.textureUsage), key.color(), key.dyeColor()));
            this.bindings.put(key, material);
        }
        return material;
    }

    private record TextureKey(JavaModel.Texture texture, int color, Integer dyeColor) {
    }
}
