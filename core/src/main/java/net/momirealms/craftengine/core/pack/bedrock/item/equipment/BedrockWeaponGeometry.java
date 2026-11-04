package net.momirealms.craftengine.core.pack.bedrock.item.equipment;

import com.google.gson.JsonArray;
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

public final class BedrockWeaponGeometry {
    private final JavaModelResolver models;
    private final JavaPackTextureSource textures;
    private final Map<TextureKey, BedrockElementGeometry.Material> bindings = new LinkedHashMap<>();
    private final List<Bedrock3DItemAttachables.Material> materials = new ArrayList<>();

    public BedrockWeaponGeometry(JavaModelResolver models, JavaPackTextureSource textures) {
        this.models = models;
        this.textures = textures;
    }

    public Bedrock3DItemAttachables.Stage createStage(ItemBindingRule rule, String condition) throws IOException {
        Matrix4fc transformation = BedrockModelTransform.compose(rule.transformations());
        BedrockModelTransform.uniformScale(transformation);
        ItemRenderTarget.Model target = (ItemRenderTarget.Model) rule.target();
        JavaModel model = target.resolve(this.models);
        JsonArray cubes = this.geometry(model, target, false);
        // 将规则的局部变换烘焙进几何，模型的手持 display 由挂点骨骼应用。
        BedrockModelTransform.apply(cubes, transformation, 1);
        return new Bedrock3DItemAttachables.Stage(model, cubes, condition);
    }

    public JsonArray dyedGeometry(JavaModel model, ItemRenderTarget.Model target) throws IOException {
        return this.geometry(model, target, true);
    }

    private JsonArray geometry(JavaModel model, ItemRenderTarget.Model target, boolean preserveDye) throws IOException {
        if (model.kind() == JavaModel.GeometryKind.GENERATED) {
            JsonArray cubes = new JsonArray();
            List<JavaModel.Texture> layers = this.models.resolveLayers(model);
            for (int layer = 0; layer < layers.size(); layer++) {
                int color = layer < target.tints().size() ? this.textures.resolveTint(target.tints().get(layer)) : WHITE;
                BedrockElementGeometry.Material material = this.material(new TextureKey(layers.get(layer), color, null));
                TextureData data = this.textures.read(layers.get(layer), TextureUsage.ITEM);
                BedrockGeneratedGeometry.addLayer(cubes, data, material.name(), layer, data.image().getWidth(), data.image().getHeight());
            }
            return cubes;
        }
        if (model.kind() == JavaModel.GeometryKind.ELEMENTS) {
            Map<JavaModel.Face, BedrockElementGeometry.Material> faces = new LinkedHashMap<>();
            for (JavaModel.Element element : model.elements()) {
                for (JavaModel.Face face : element.faces().values()) {
                    Tint tint = face.tintIndex() >= 0 && face.tintIndex() < target.tints().size() ? target.tints().get(face.tintIndex()) : null;
                    Integer dye = preserveDye && tint != null ? this.textures.dyeColor(tint) : null;
                    int color = tint != null && dye == null ? this.textures.resolveTint(tint) : WHITE;
                    faces.put(face, this.material(new TextureKey(face.texture(), color, dye)));
                }
            }
            return BedrockElementGeometry.convert(model, faces);
        }
        throw new UnsupportedOperationException("Unsupported item model geometry: " + model.id());
    }

    private BedrockElementGeometry.Material material(TextureKey key) throws IOException {
        BedrockElementGeometry.Material material = this.bindings.get(key);
        if (material == null) {
            material = new BedrockElementGeometry.Material("layer" + this.materials.size(), 1, 1);
            this.materials.add(new Bedrock3DItemAttachables.Material(this.textures.read(key.texture(), TextureUsage.ITEM), key.color(), key.dyeColor()));
            this.bindings.put(key, material);
        }
        return material;
    }

    public List<Bedrock3DItemAttachables.Material> materials() {
        return this.materials;
    }

    private record TextureKey(JavaModel.Texture texture, int color, Integer dyeColor) {
    }
}
