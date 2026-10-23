package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockElementGeometry;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockGeneratedGeometry;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockModelTransform;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel.Texture;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureAnimation;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureUsage;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayPosition;
import net.momirealms.craftengine.core.util.*;
import org.joml.Vector3f;
import org.joml.Matrix4fc;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.*;
import static net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource.WHITE;

public final class BedrockItemBlocks {
    private final JsonObject terrain = new JsonObject();
    private final JsonArray flipbooks = new JsonArray();
    private final JsonObject blocks = new JsonObject();

    public DisplayBlock write(Key item, List<ItemBindingRule> models, JavaModelResolver resolver, JavaPackTextureSource textures, Path root) throws IOException {
        String name = "ce_" + HashUtils.sha1(item.asString().getBytes(StandardCharsets.UTF_8));
        Map<TextureKey, BedrockElementGeometry.Material> bindings = new LinkedHashMap<>();
        List<TextureData> images = new ArrayList<>();
        JsonArray cubes = new JsonArray();
        JavaModel displayModel = null;
        for (ItemBindingRule rule : models) {
            ItemRenderTarget.Model target = (ItemRenderTarget.Model) rule.target();
            JavaModel model = resolver.resolve(target.model());
            if (displayModel == null) {
                displayModel = model;
            }
            Matrix4fc transformation = BedrockModelTransform.compose(rule.transformations());
            BedrockModelTransform.uniformScale(transformation);
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
                throw new UnsupportedOperationException("Unsupported model geometry: " + model.id());
            }
            BedrockModelTransform.apply(part, transformation, 1);
            cubes.addAll(part);
        }
        // 合并后统一缩小，避免各子模型分别适配尺寸而改变相对位置。
        float geometryScale = BedrockElementGeometry.fit(cubes).scale();
        JsonObject transforms = displayModel == null ? new JsonObject() : this.displayTransforms(displayModel, geometryScale);
        Map<String, String> materials = new LinkedHashMap<>();
        int index = 0;
        for (Map.Entry<TextureKey, BedrockElementGeometry.Material> entry : bindings.entrySet()) {
            String tile = name + '_' + index;
            this.writeTexture(images.get(index++), entry.getKey().color(), tile, root);
            materials.put(entry.getValue().name(), tile);
        }
        return this.writeGeometry(name, cubes, transforms, materials, root);
    }

    private static BedrockElementGeometry.Material bindTexture(TextureKey key, Map<TextureKey, BedrockElementGeometry.Material> bindings,
                                                               List<TextureData> images, JavaPackTextureSource textures) throws IOException {
        BedrockElementGeometry.Material material = bindings.get(key);
        if (material == null) {
            TextureData data = textures.read(key.texture(), TextureUsage.ITEM);
            int width = data.image().getWidth();
            int height = data.image().getHeight();
            float size = Math.max(width, height);
            material = new BedrockElementGeometry.Material("layer" + bindings.size(), width / size, height / size);
            bindings.put(key, material);
            images.add(data);
        }
        return material;
    }

    private void writeTexture(TextureData data, int color, String tile, Path root) throws IOException {
        String path = "textures/" + tile;
        TextureAnimation animation = data.animation();
        int width = data.image().getWidth();
        int height = data.image().getHeight();
        int size = Math.max(width, height);
        BufferedImage first;
        if (animation == null) {
            first = color == 0xffffff ? data.image() : ImageUtils.multiplyColor(data.image(), color);
        } else {
            int interval = animation.sampleInterval(); // 所有帧时长的最大公约数
            int count = animation.duration() / interval;
            BufferedImage strip = new BufferedImage(size, Math.multiplyExact(size, count), BufferedImage.TYPE_INT_ARGB); // 最终动画长条
            BufferedImage frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB); // 单帧临时画布
            int[] pixels = new int[width * height];
            // 每层独立播放；插值预烘焙到 tick，避免不同时长的帧在 Bedrock 中变速。
            for (int sample = 0; sample < count; sample++) {
                animation.render(sample * interval, color, frame);
                frame.getRGB(0, 0, width, height, pixels, 0, width);
                strip.setRGB(0, sample * size, width, height, pixels, 0, width);
            }
            ImageUtils.writePng(strip, root.resolve(path + "_anim.png"));
            JsonObject flipbook = new JsonObject();
            flipbook.addProperty("atlas_tile", tile);
            flipbook.addProperty("flipbook_texture", path + "_anim");
            flipbook.addProperty("ticks_per_frame", interval);
            flipbook.addProperty("blend_frames", false);
            this.flipbooks.add(flipbook);
            first = strip.getSubimage(0, 0, size, size);
        }
        if (first.getWidth() != size || first.getHeight() != size) {
            BufferedImage padded = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            padded.setRGB(0, 0, width, height, first.getRGB(0, 0, width, height, null, 0, width), 0, width);
            first = padded;
        }
        ImageUtils.writePng(first, root.resolve(path + ".png"));
        JsonObject texture = new JsonObject();
        texture.addProperty("textures", path);
        this.terrain.add(tile, texture);
    }

    private DisplayBlock writeGeometry(String name, JsonArray cubes, JsonObject transforms, Map<String, String> materials, Path root) throws IOException {
        String identifier = "geometry." + name; // 几何资源标识

        // 几何描述
        JsonObject description = new JsonObject();
        description.addProperty("identifier", identifier); // 供方块引用的几何 ID
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
        file.addProperty(FORMAT_VERSION, "1.21.0"); // 几何格式版本
        file.add("minecraft:geometry", geometries); // 几何定义入口
        Path geometryPath = root.resolve("models/blocks/" + name + ".geo.json"); // 几何文件输出路径
        Files.createDirectories(geometryPath.getParent()); // 创建模型目录
        GsonHelper.writeJsonFile(file, geometryPath); // 写出几何文件

        // blocks.json 中的方块配置
        JsonObject block = new JsonObject();
        block.addProperty("sound", "wood"); // 使用木质方块音效
        this.blocks.add("geyser_custom:" + name, block); // 匹配 Geyser 注册的方块标识
        return new DisplayBlock(name, identifier, materials);
    }

    private JsonObject displayTransforms(JavaModel model, float geometryScale) {
        JsonObject transforms = new JsonObject();
        for (DisplayPosition position : DisplayPosition.values()) {
            if (position == DisplayPosition.ON_SHELF) {
                continue;
            }
            JavaModel.Transform transform = model.display().getOrDefault(position, JavaModel.Transform.IDENTITY);
            Vector3f scale = new Vector3f(transform.scale()).div(geometryScale);
            if (scale.x < 0 || scale.y < 0 || scale.z < 0 || scale.x > 4 || scale.y > 4 || scale.z > 4) {
                throw new UnsupportedOperationException("Display scale exceeds Bedrock [0, 4] in " + position + ": " + scale);
            }
            JsonObject display = new JsonObject();
            display.add("rotation", VectorUtils.toJson(transform.rotation()));
            display.add("translation", VectorUtils.toJson(transform.translation()));
            display.add("scale", VectorUtils.toJson(scale));
            display.add("rotation_pivot", VectorUtils.toJson(0, 0, 0));
            display.add("scale_pivot", VectorUtils.toJson(0, 0, 0));
            if (position == DisplayPosition.GUI) {
                display.addProperty("fit_to_frame", false);
            }
            transforms.add(position.name().toLowerCase(Locale.ROOT), display);
        }
        return transforms;
    }

    public void finish(Path root) throws IOException {
        if (this.blocks.isEmpty()) {
            return;
        }
        JsonObject atlas = new JsonObject();
        atlas.addProperty("resource_pack_name", "craftengine");
        atlas.addProperty("texture_name", "atlas.terrain");
        atlas.addProperty("padding", 8);
        atlas.addProperty("num_mip_levels", 4);
        atlas.add("texture_data", this.terrain);
        GsonHelper.writeJsonFile(atlas, root.resolve(TERRAIN_TEXTURE_ATLAS));
        GsonHelper.writeJsonFile(this.flipbooks, root.resolve(FLIPBOOK_TEXTURES));
        JsonArray version = new JsonArray();
        version.add(1);
        version.add(1);
        version.add(0);
        this.blocks.add(FORMAT_VERSION, version);
        GsonHelper.writeJsonFile(this.blocks, root.resolve("blocks.json"));
    }

    private record TextureKey(Texture texture, int color) {
    }

    public record DisplayBlock(String name, String geometry, Map<String, String> materials) {
    }
}
