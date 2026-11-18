package net.momirealms.craftengine.core.pack.bedrock.item.visual;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.BedrockFormatVersions;
import net.momirealms.craftengine.core.pack.bedrock.BedrockResourceException;
import net.momirealms.craftengine.core.pack.bedrock.block.BedrockBlockDefinitions;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingRule;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRenderTarget;
import net.momirealms.craftengine.core.pack.bedrock.model.*;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel.Texture;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTerrainAtlas;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureUsage;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayPosition;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourcePackUtils;
import net.momirealms.craftengine.core.util.VectorUtils;
import org.joml.Matrix3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.FORMAT_VERSION;
import static net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource.WHITE;

public final class BedrockItemDisplayBlocks {
    private final BedrockTerrainAtlas atlas;
    private final BedrockBlockDefinitions blockDefinitions;
    private final Map<BedrockItemGeometry.Material, String> textureTiles = new HashMap<>();
    private final BedrockAffineFaces affineFaces = new BedrockAffineFaces();
    private final Map<GeometryKey, List<Geometry>> geometries = new HashMap<>();

    public BedrockItemDisplayBlocks(BedrockTerrainAtlas atlas, BedrockBlockDefinitions blockDefinitions) {
        this.atlas = atlas;
        this.blockDefinitions = blockDefinitions;
    }

    public DisplayBlock write(Key item, List<ItemBindingRule> models, JavaModelResolver resolver, JavaPackTextureSource textures, Path root) throws IOException {
        return this.write(item, models, resolver, textures, root, null);
    }

    public DisplayBlock write(Key item, List<ItemBindingRule> models, JavaModelResolver resolver, JavaPackTextureSource textures, Path root, DisplayPosition view) throws IOException {
        String name = item.namespace() + '/' + item.value();
        Map<TextureKey, BedrockElementGeometry.Material> bindings = new LinkedHashMap<>();
        Map<String, BedrockItemGeometry.Material> images = new LinkedHashMap<>();
        JsonArray cubes = new JsonArray();
        JavaModel displayModel = null;
        for (ItemBindingRule rule : models) {
            ItemRenderTarget.Model target = (ItemRenderTarget.Model) rule.target();
            JavaModel model = target.resolve(resolver);
            if (displayModel == null) {
                displayModel = model;
                if (view != null) {
                    JavaModel.Transform display = model.display().getOrDefault(view, JavaModel.Transform.IDENTITY);
                    Vector3f scale = display.scale();
                    if (scale.x <= 0 || scale.y <= 0 || scale.z <= 0) {
                        Map<DisplayPosition, JavaModel.Transform> displays = new EnumMap<>(DisplayPosition.class);
                        displays.putAll(model.display());
                        // 零缩放不能求逆；有符号缩放也不写入方块 display，统一烘焙到部件里。
                        displays.put(view, new JavaModel.Transform(display.rotation(), display.translation(), new Vector3f(1)));
                        displayModel = new JavaModel(model.id(), model.kind(), model.textures(), model.elements(), displays, model.guiLight());
                    }
                }
            }
            Matrix4fc transformation = BedrockModelTransform.compose(rule.transformations());
            if (view != null) {
                JavaModel.Transform reference = displayModel.display().getOrDefault(view, JavaModel.Transform.IDENTITY);
                JavaModel.Transform display = model.display().getOrDefault(view, JavaModel.Transform.IDENTITY);
                if (!reference.equals(display)) {
                    // 提取首部件的公共 display，其余部件只烘焙相对变换，便于不同场景复用几何。
                    transformation = new Matrix4f(BedrockModelTransform.relativeDisplay(reference, display)).mul(transformation);
                }
            }
            BedrockModelTransform.validate(transformation);
            JsonArray part;
            if (model.kind() == JavaModel.GeometryKind.GENERATED) {
                part = new JsonArray();
                List<Texture> layers = resolver.resolveLayers(model);
                for (int layer = 0; layer < layers.size(); layer++) {
                    int color = layer < target.tints().size() ? textures.resolveTint(target.tints().get(layer)) : WHITE;
                    BedrockElementGeometry.Material material = bindTexture(new TextureKey(layers.get(layer), color), bindings, images, textures);
                    TextureData data = textures.read(layers.get(layer), TextureUsage.ITEM);
                    int size = Math.max(data.image().getWidth(), data.image().getHeight());
                    BedrockGeneratedGeometry.addLayer(part, data, material.name(), layer, size, size);
                }
            } else if (model.kind() == JavaModel.GeometryKind.ELEMENTS) {
                Map<JavaModel.Face, BedrockElementGeometry.Material> faces = new LinkedHashMap<>();
                for (JavaModel.Element element : model.elements()) {
                    for (JavaModel.Face face : element.faces().values()) {
                        int color = face.tintIndex() >= 0 && face.tintIndex() < target.tints().size() ? textures.resolveTint(target.tints().get(face.tintIndex())) : WHITE;
                        faces.put(face, bindTexture(new TextureKey(face.texture(), color), bindings, images, textures));
                    }
                }
                part = BedrockElementGeometry.convert(model, faces);
            } else {
                throw new BedrockResourceException("resource_pack.bedrock.reason.geometry", model.id().asString());
            }
            cubes.addAll(this.affineFaces.transform(part, transformation, images));
        }
        // 合并后统一缩小，避免各子模型分别适配尺寸而改变相对位置。
        float geometryScale = BedrockElementGeometry.fit(cubes).scale();
        JsonObject transforms = displayModel == null ? new JsonObject() : this.displayTransforms(displayModel, cubes, geometryScale, view);
        Map<String, String> materials = new LinkedHashMap<>();
        int index = 0;
        for (Map.Entry<String, BedrockItemGeometry.Material> entry : images.entrySet()) {
            String tile = this.textureTiles.get(entry.getValue());
            if (tile == null) {
                tile = name + "/layer_" + index;
                this.atlas.write(entry.getValue().data(), entry.getValue().color(), tile, root);
                this.textureTiles.put(entry.getValue(), tile);
            }
            materials.put(entry.getKey(), tile);
            index++;
        }
        return this.writeGeometry(item, cubes, transforms, materials, root);
    }

    private static BedrockElementGeometry.Material bindTexture(TextureKey key,
                                                               Map<TextureKey, BedrockElementGeometry.Material> bindings,
                                                               Map<String, BedrockItemGeometry.Material> images,
                                                               JavaPackTextureSource textures) throws IOException {
        BedrockElementGeometry.Material material = bindings.get(key);
        if (material == null) {
            TextureData data = textures.read(key.texture(), TextureUsage.ITEM);
            int width = data.image().getWidth();
            int height = data.image().getHeight();
            float size = Math.max(width, height);
            material = new BedrockElementGeometry.Material("layer" + bindings.size(), width / size, height / size);
            bindings.put(key, material);
            images.put(material.name(), new BedrockItemGeometry.Material(data, key.color()));
        }
        return material;
    }

    private DisplayBlock writeGeometry(Key item, JsonArray cubes, JsonObject transforms, Map<String, String> materials, Path root) throws IOException {
        GeometryKey key = new GeometryKey(cubes, materials);
        List<Geometry> candidates = this.geometries.computeIfAbsent(key, ignored -> new ArrayList<>());
        for (Geometry candidate : candidates) {
            // 相同几何可补充尚未使用的场景姿态；已有场景姿态冲突时必须另建，避免覆盖旧物品的外观。
            if (compatibleTransforms(candidate.transforms(), transforms)) {
                boolean changed = false;
                for (Map.Entry<String, JsonElement> entry : transforms.entrySet()) {
                    if (!candidate.transforms().has(entry.getKey())) {
                        candidate.transforms().add(entry.getKey(), entry.getValue());
                        changed = true;
                    }
                }
                if (changed) {
                    this.writeGeometryFile(candidate.display(), cubes, candidate.transforms(), root);
                }
                return candidate.display();
            }
        }
        String name = item.namespace() + '/' + item.value();
        String identifier = "geometry." + ResourcePackUtils.bedrockResourceIdentifier(item) + ".block"; // 展示方块几何标识，与手持几何区分
        DisplayBlock display = new DisplayBlock(name, identifier, materials);
        this.writeGeometryFile(display, cubes, transforms, root);
        candidates.add(new Geometry(display, transforms));

        this.blockDefinitions.register(name, "wood");
        return display;
    }

    private static boolean compatibleTransforms(JsonObject first, JsonObject second) {
        for (Map.Entry<String, JsonElement> entry : second.entrySet()) {
            JsonElement existing = first.get(entry.getKey());
            if (existing != null && !existing.equals(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    private void writeGeometryFile(DisplayBlock display, JsonArray cubes, JsonObject transforms, Path root) throws IOException {
        // 几何描述
        JsonObject description = new JsonObject();
        description.addProperty("identifier", display.geometry()); // 供方块引用的几何 ID
        description.addProperty("texture_width", 16); // U 坐标尺度，不是实际贴图宽度
        description.addProperty("texture_height", 16); // V 坐标尺度，不是实际贴图高度

        // 承载全部方块面的根骨骼
        JsonObject bone = new JsonObject();
        bone.addProperty("name", "root"); // 根骨骼名称
        bone.add("pivot", VectorUtils.toJson(0, 8, 0)); // 骨骼旋转支点
        bone.add("cubes", cubes); // 挂载生成的几何元素
        JsonArray bones = new JsonArray(); // 骨骼列表
        bones.add(bone); // 加入根骨骼

        // 单个几何定义
        JsonObject geometry = new JsonObject();
        geometry.add("description", description); // 写入标识和 UV 尺度
        geometry.add("bones", bones); // 写入骨骼和几何元素
        geometry.add("item_display_transforms", transforms); // 背包、手持、掉落等显示变换

        // 文件内的几何定义列表
        JsonArray geometries = new JsonArray();
        geometries.add(geometry);

        // 几何文件根对象
        JsonObject file = new JsonObject();
        file.addProperty(FORMAT_VERSION, BedrockFormatVersions.GEOMETRY); // 几何格式版本
        file.add("minecraft:geometry", geometries); // 几何定义入口
        Path geometryPath = root.resolve("models/blocks/" + display.name() + ".geo.json"); // 几何文件输出路径
        Files.createDirectories(geometryPath.getParent()); // 创建模型目录
        GsonHelper.writeJsonFile(file, geometryPath); // 写出几何文件
    }

    private JsonObject displayTransforms(JavaModel model, JsonArray cubes, float geometryScale, DisplayPosition view) {
        JsonObject transforms = new JsonObject();
        for (DisplayPosition position : DisplayPosition.values()) {
            if (view == null ? position == DisplayPosition.ON_SHELF : position != view) {
                continue;
            }
            JavaModel.Transform transform = model.display().getOrDefault(position, JavaModel.Transform.IDENTITY);
            // 几何为满足基岩边界已整体缩小，这里抵消该缩放，恢复 Java display 期望的显示尺寸
            Vector3f scale = new Vector3f(transform.scale()).div(geometryScale);
            Vector3f rotation = transform.rotation();
            Vector3f translation = transform.translation();
            if (position == DisplayPosition.ON_SHELF) {
                // Java 置物架按最终整组包围盒垂直居中，且其外层缩放是基岩 fixed 路径的一半
                Vector3f radians = new Vector3f(rotation).mul((float) (Math.PI / 180));
                Matrix4f displayMatrix = new Matrix4f().translation(translation)
                        .rotateXYZ(radians.x, radians.y, radians.z).scale(scale).translate(-8, -8, -8);
                float centerY = 0;
                if (!cubes.isEmpty()) {
                    Vector3f minimum = new Vector3f(Float.POSITIVE_INFINITY);
                    Vector3f maximum = new Vector3f(Float.NEGATIVE_INFINITY);
                    BedrockElementGeometry.bounds(cubes, minimum, maximum, displayMatrix);
                    centerY = (minimum.y + maximum.y) * 0.5f;
                }
                // 几何已转换为基岩坐标；补偿置物架朝向时，要连同公共 display 一起变换
                rotation = BedrockModelTransform.rotationAngles(new Matrix3d().rotationY(Math.PI)
                        .scale(-1, 1, 1).rotateXYZ(radians.x, radians.y, radians.z).scale(-1, 1, 1));
                translation = new Vector3f(translation.x, translation.y - centerY, -translation.z).mul(0.5f);
                scale.mul(0.5f);
            }
            if (scale.x < 0 || scale.y < 0 || scale.z < 0 || scale.x > 4 || scale.y > 4 || scale.z > 4) {
                throw new BedrockResourceException("resource_pack.bedrock.reason.display_scale", position.name(), scale.toString());
            }
            JsonObject display = new JsonObject();
            display.add("rotation", VectorUtils.toJson(rotation));
            display.add("translation", VectorUtils.toJson(translation));
            display.add("scale", VectorUtils.toJson(scale));
            display.add("rotation_pivot", VectorUtils.toJson(0, 0, 0));
            display.add("scale_pivot", VectorUtils.toJson(0, 0, 0));
            if (position == DisplayPosition.GUI) {
                display.addProperty("fit_to_frame", false);
            }
            // 1.21.0 几何格式中置物架继承 fixed；独立外观可以使用该槽位，不会覆盖主物品的展示框姿态
            transforms.add(position == DisplayPosition.ON_SHELF ? "fixed" : position.name().toLowerCase(Locale.ROOT), display);
        }
        return transforms;
    }

    private record TextureKey(Texture texture, int color) {
    }

    private record GeometryKey(JsonArray cubes, Map<String, String> materials) {
    }

    private record Geometry(DisplayBlock display, JsonObject transforms) {
    }

    public record DisplayBlock(String name, String geometry, Map<String, String> materials) {
    }
}
