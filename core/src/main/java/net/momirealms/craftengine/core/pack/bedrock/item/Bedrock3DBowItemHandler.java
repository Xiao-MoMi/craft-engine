package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockElementGeometry;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockGeneratedGeometry;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockModelTransform;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureUsage;
import org.joml.Matrix4fc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource.WHITE;

public final class Bedrock3DBowItemHandler implements BedrockItemHandler {
    private final JavaModelResolver models;
    private final JavaPackTextureSource textures;

    public Bedrock3DBowItemHandler(JavaModelResolver models, JavaPackTextureSource textures) {
        this.models = models;
        this.textures = textures;
    }

    @Override
    public boolean write(ItemRegistrationPlan.Entry entry, BedrockItemGenerationContext context) throws IOException {
        List<ItemBindingRule> rules = entry.definition().rules();
        int idle = BedrockItemAttachables.findBowIdle(rules);
        if (idle == -1) {
            return false;
        }
        Map<TextureKey, BedrockElementGeometry.Material> bindings = new LinkedHashMap<>();
        List<Bedrock3DBowAttachables.Material> materials = new ArrayList<>();
        List<Bedrock3DBowAttachables.Stage> stages = new ArrayList<>(rules.size());
        ItemBindingRule idleRule = rules.get(idle);
        BedrockItemBlocks.DisplayBlock display;
        try {
            for (ItemBindingRule rule : rules) {
                Matrix4fc transformation = BedrockModelTransform.compose(rule.transformations());
                BedrockModelTransform.uniformScale(transformation);
                ItemRenderTarget.Model target = (ItemRenderTarget.Model) rule.target();
                JavaModel model = this.models.resolve(target.model());
                JsonArray cubes = this.geometry(model, target, bindings, materials);
                // 局部变换烘焙到几何，各阶段的手持 display 留给挂点骨骼应用。
                BedrockModelTransform.apply(cubes, transformation, 1);
                stages.add(new Bedrock3DBowAttachables.Stage(model, cubes, BedrockItemAttachables.bowCondition(rule)));
            }
            // 背包和掉落使用待机 display，手持几何不缩到方块范围。
            display = context.blocks().write(entry.bedrockId(), List.of(idleRule), this.models, this.textures, context.root());
        } catch (UnsupportedOperationException exception) {
            return false;
        }
        Bedrock3DBowAttachables.write(entry.bedrockId(), stages, materials,
                entry.source().clientBehavior().use().maximumDuration().value(), context.root());
        List<ItemBindingCondition> conditions = idleRule.conditions().stream()
                .filter(condition -> !BedrockItemAttachables.isUseCondition(condition)).toList();
        ItemBindingRule registration = new ItemBindingRule(conditions, idleRule.target(), idleRule.transformations(), idleRule.sourcePath());
        context.variants().put(entry.bedrockId(), List.of(new BedrockItemIcons.Variant(entry.bedrockId(), entry.icon(), registration, null)));
        context.displays().put(entry.bedrockId(), display);
        context.attachables().add(entry.bedrockId());
        return true;
    }

    private JsonArray geometry(JavaModel model, ItemRenderTarget.Model target, Map<TextureKey, BedrockElementGeometry.Material> bindings,
                               List<Bedrock3DBowAttachables.Material> materials) throws IOException {
        if (model.kind() == JavaModel.GeometryKind.GENERATED) {
            JsonArray cubes = new JsonArray();
            List<JavaModel.Texture> layers = this.models.resolveLayers(model);
            for (int layer = 0; layer < layers.size(); layer++) {
                int color = layer < target.tints().size() ? this.textures.resolveTint(target.tints().get(layer)) : WHITE;
                BedrockElementGeometry.Material material = this.material(new TextureKey(layers.get(layer), color), bindings, materials);
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
                    faces.put(face, this.material(new TextureKey(face.texture(), color), bindings, materials));
                }
            }
            return BedrockElementGeometry.convert(model, faces);
        }
        throw new UnsupportedOperationException("Unsupported bow model geometry: " + model.id());
    }

    private BedrockElementGeometry.Material material(TextureKey key, Map<TextureKey, BedrockElementGeometry.Material> bindings,
                                                     List<Bedrock3DBowAttachables.Material> materials) throws IOException {
        BedrockElementGeometry.Material material = bindings.get(key);
        if (material == null) {
            material = new BedrockElementGeometry.Material("layer" + materials.size(), 1, 1);
            materials.add(new Bedrock3DBowAttachables.Material(this.textures.read(key.texture(), TextureUsage.ITEM), key.color()));
            bindings.put(key, material);
        }
        return material;
    }

    private record TextureKey(JavaModel.Texture texture, int color) {
    }
}
