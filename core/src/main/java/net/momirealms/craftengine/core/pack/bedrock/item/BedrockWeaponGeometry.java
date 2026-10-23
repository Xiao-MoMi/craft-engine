package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockElementGeometry;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockGeneratedGeometry;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureUsage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource.WHITE;

final class BedrockWeaponGeometry {
    private final JavaModelResolver models;
    private final JavaPackTextureSource textures;
    private final Map<TextureKey, BedrockElementGeometry.Material> bindings = new LinkedHashMap<>();
    private final List<Bedrock3DItemAttachables.Material> materials = new ArrayList<>();

    BedrockWeaponGeometry(JavaModelResolver models, JavaPackTextureSource textures) {
        this.models = models;
        this.textures = textures;
    }

    JsonArray geometry(JavaModel model, ItemRenderTarget.Model target) throws IOException {
        if (model.kind() == JavaModel.GeometryKind.GENERATED) {
            JsonArray cubes = new JsonArray();
            List<JavaModel.Texture> layers = this.models.resolveLayers(model);
            for (int layer = 0; layer < layers.size(); layer++) {
                int color = layer < target.tints().size() ? this.textures.resolveTint(target.tints().get(layer)) : WHITE;
                BedrockElementGeometry.Material material = this.material(new TextureKey(layers.get(layer), color));
                TextureData data = this.textures.read(layers.get(layer), TextureUsage.ITEM);
                BedrockGeneratedGeometry.addLayer(cubes, data, material.name(), layer, data.image().getWidth(), data.image().getHeight());
            }
            return cubes;
        }
        if (model.kind() == JavaModel.GeometryKind.ELEMENTS) {
            Map<JavaModel.Face, BedrockElementGeometry.Material> faces = new LinkedHashMap<>();
            for (JavaModel.Element element : model.elements()) {
                for (JavaModel.Face face : element.faces().values()) {
                    int color = face.tintIndex() >= 0 && face.tintIndex() < target.tints().size()
                            ? this.textures.resolveTint(target.tints().get(face.tintIndex())) : WHITE;
                    faces.put(face, this.material(new TextureKey(face.texture(), color)));
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
            this.materials.add(new Bedrock3DItemAttachables.Material(this.textures.read(key.texture(), TextureUsage.ITEM), key.color()));
            this.bindings.put(key, material);
        }
        return material;
    }

    List<Bedrock3DItemAttachables.Material> materials() {
        return this.materials;
    }

    private record TextureKey(JavaModel.Texture texture, int color) {
    }
}
