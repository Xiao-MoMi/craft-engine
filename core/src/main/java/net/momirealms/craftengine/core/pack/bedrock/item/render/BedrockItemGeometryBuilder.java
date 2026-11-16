package net.momirealms.craftengine.core.pack.bedrock.item.render;

import com.google.gson.JsonArray;
import net.momirealms.craftengine.core.pack.bedrock.BedrockResourceException;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingRule;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRenderTarget;
import net.momirealms.craftengine.core.pack.bedrock.model.*;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureUsage;
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
    private final Map<TextureKey, BedrockElementGeometry.Material> bindings = new LinkedHashMap<>();
    private final List<BedrockItemGeometry.Material> materials = new ArrayList<>();

    public BedrockItemGeometryBuilder(JavaModelResolver models, JavaPackTextureSource textures) {
        this.models = models;
        this.textures = textures;
    }

    public BedrockItemGeometry build(ItemBindingRule rule) throws IOException {
        return this.build(rule, false);
    }

    public BedrockItemGeometry build(ItemBindingRule rule, boolean preserveDye) throws IOException {
        return this.build(rule, preserveDye, ((ItemRenderTarget.Model) rule.target()).resolve(this.models));
    }

    public BedrockItemGeometry build(ItemBindingRule rule, boolean preserveDye, JavaModel model) throws IOException {
        Matrix4fc transformation = BedrockModelTransform.compose(rule.transformations());
        BedrockModelTransform.uniformScale(transformation);
        ItemRenderTarget.Model target = (ItemRenderTarget.Model) rule.target();
        JsonArray cubes = this.geometry(model, target, preserveDye);
        // 将规则的局部变换烘焙进几何，模型的 display 留给各渲染出口应用。
        BedrockModelTransform.apply(cubes, transformation, 1);
        return new BedrockItemGeometry(model, cubes);
    }

    public List<BedrockItemGeometry.Material> materials() {
        return this.materials;
    }

    private JsonArray geometry(JavaModel model, ItemRenderTarget.Model target, boolean preserveDye) throws IOException {
        if (model.kind() == JavaModel.GeometryKind.GENERATED) {
            JsonArray cubes = new JsonArray();
            List<JavaModel.Texture> layers = this.models.resolveLayers(model);
            for (int layer = 0; layer < layers.size(); layer++) {
                BedrockElementGeometry.Material material = this.material(layers.get(layer), target, layer, preserveDye);
                TextureData data = this.textures.read(layers.get(layer), TextureUsage.ITEM);
                BedrockGeneratedGeometry.addLayer(cubes, data, material.name(), layer, data.image().getWidth(), data.image().getHeight());
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
        Integer dye = preserveDye && tint != null ? this.textures.dyeColor(tint) : null;
        int color = tint != null && dye == null ? this.textures.resolveTint(tint) : WHITE;
        TextureKey key = new TextureKey(texture, color, dye);
        BedrockElementGeometry.Material material = this.bindings.get(key);
        if (material == null) {
            material = new BedrockElementGeometry.Material("layer" + this.materials.size(), 1, 1);
            this.materials.add(new BedrockItemGeometry.Material(this.textures.read(key.texture(), TextureUsage.ITEM), key.color(), key.dyeColor()));
            this.bindings.put(key, material);
        }
        return material;
    }

    private record TextureKey(JavaModel.Texture texture, int color, Integer dyeColor) {
    }
}
