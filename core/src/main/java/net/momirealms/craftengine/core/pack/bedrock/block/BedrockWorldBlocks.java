package net.momirealms.craftengine.core.pack.bedrock.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockElementGeometry;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockModelTransform;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTerrainAtlas;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureUsage;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourcePackUtils;
import net.momirealms.craftengine.core.util.VectorUtils;
import net.momirealms.craftengine.core.world.collision.AABB;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.*;

public final class BedrockWorldBlocks {
    private final JavaPackTextureSource textures;
    private final JavaBlockModelResolver models;
    private final BedrockTerrainAtlas atlas = new BedrockTerrainAtlas();

    private BedrockWorldBlocks(JavaModelResources resources) {
        this.textures = resources.textures();
        this.models = new JavaBlockModelResolver(resources);
    }

    public static JsonArray write(JavaModelResources resources, List<WorldBlockSource> sources, Path root) throws IOException {
        return new BedrockWorldBlocks(resources).writeBlocks(sources, root);
    }

    private JsonArray writeBlocks(List<WorldBlockSource> sources, Path root) throws IOException {
        Map<String, List<WorldBlockSource>> groups = groupByVisualState(sources);
        this.atlas.read(root);
        Path blockFile = root.resolve("blocks.json");
        JsonObject blocks = Files.isRegularFile(blockFile) ? GsonHelper.readJsonObjectFromFile(blockFile) : new JsonObject();
        JsonArray entries = new JsonArray(groups.size());
        for (Map.Entry<String, List<WorldBlockSource>> group : groups.entrySet()) {
            try {
                entries.add(this.writeGroup(group.getKey(), group.getValue(), blocks, root));
            } catch (UnsupportedOperationException exception) {
                // 同一显示状态只能注册一套方块属性；跳过整组，避免其他状态误用首个状态的碰撞箱。
                for (WorldBlockSource source : group.getValue()) {
                    TranslationManager.warn("resource_pack.bedrock.block.skipped", source.id().asString(), Integer.toString(source.stateIndex()), exception.getLocalizedMessage());
                }
            }
        }
        if (!entries.isEmpty()) {
            blocks.add(FORMAT_VERSION, VectorUtils.toJson(1, 1, 0));
            GsonHelper.writeJsonFile(blocks, blockFile);
            this.atlas.finish(root);
        }
        return entries;
    }

    private static Map<String, List<WorldBlockSource>> groupByVisualState(List<WorldBlockSource> sources) {
        Map<String, List<WorldBlockSource>> groups = new LinkedHashMap<>();
        for (WorldBlockSource source : sources) {
            if (source.visual() == null) {
                TranslationManager.warn("resource_pack.bedrock.block.no_visual", source.id().asString(), Integer.toString(source.stateIndex()));
            } else {
                groups.computeIfAbsent(source.visual().getAsString(), ignored -> new ArrayList<>()).add(source);
            }
        }
        return groups;
    }

    private JsonObject writeGroup(String visualState, List<WorldBlockSource> sources, JsonObject blocks, Path root) throws IOException {
        JsonObject properties = sharedComponents(visualState, sources);
        WorldBlockSource first = sources.getFirst();
        String name = "craftengine/world/" + first.id().namespace() + '/' + first.id().value() + "/state_" + first.stateIndex();
        List<JavaBlockModelResolver.Part> parts = this.models.resolve(first.visual());

        JsonObject entry = this.writeGeometry(name, parts, sources, root);
        entry.addProperty("java_state", visualState);
        for (Map.Entry<String, JsonElement> property : properties.entrySet()) {
            entry.add(property.getKey(), property.getValue());
        }
        entry.add("server_states", serverStatesToJson(sources));

        JsonObject block = new JsonObject();
        block.addProperty("sound", "stone");
        blocks.add("geyser_custom:" + name, block);
        return entry;
    }

    private static JsonObject sharedComponents(String visualState, List<WorldBlockSource> sources) {
        WorldBlockSource first = sources.getFirst();
        JsonObject properties = components(first);
        for (WorldBlockSource source : sources) {
            if (source != first && !properties.equals(components(source))) {
                throw new UnsupportedOperationException(TranslationManager.message("resource_pack.bedrock.block.conflict", visualState));
            }
        }
        return properties;
    }

    private static JsonObject components(WorldBlockSource source) {
        if (source.collisionBoxes().size() > 16) {
            throw new UnsupportedOperationException(TranslationManager.message("resource_pack.bedrock.block.collision"));
        }
        JsonObject properties = new JsonObject();
        properties.addProperty("light_emission", source.lightEmission());
        properties.addProperty("light_dampening", source.lightDampening());
        float friction = 1 - source.friction();
        if (!Float.isFinite(friction) || friction < 0 || friction > 0.9f) {
            throw new UnsupportedOperationException(TranslationManager.message("resource_pack.bedrock.block.friction"));
        }
        properties.addProperty("friction", friction);
        JsonArray collision = new JsonArray();
        for (AABB box : source.collisionBoxes()) {
            collision.add(box(box, 1.5));
        }
        properties.add("collision_boxes", collision);
        properties.add("selection_box", source.selectionBox() == null ? null : box(source.selectionBox(), 1));
        return properties;
    }

    private static JsonObject box(AABB box, double maxHeight) {
        if (box.minX < 0 || box.minY < 0 || box.minZ < 0 || box.maxX > 1 || box.maxY > maxHeight || box.maxZ > 1) {
            throw new UnsupportedOperationException(TranslationManager.message("resource_pack.bedrock.block.bounds", Double.toString(maxHeight), box.toString()));
        }
        JsonObject result = new JsonObject();
        result.add("origin", VectorUtils.toJson((float) (8 - box.maxX * 16), (float) (box.minY * 16), (float) (box.minZ * 16 - 8)));
        result.add("size", VectorUtils.toJson((float) ((box.maxX - box.minX) * 16), (float) ((box.maxY - box.minY) * 16), (float) ((box.maxZ - box.minZ) * 16)));
        return result;
    }

    private static JsonArray serverStatesToJson(List<WorldBlockSource> sources) {
        JsonArray states = new JsonArray(sources.size());
        for (WorldBlockSource source : sources) {
            JsonObject state = new JsonObject();
            state.addProperty("id", source.serverStateId());
            state.addProperty("state", source.serverState());
            state.addProperty("block_id", source.id().asString());
            state.addProperty("hardness", source.hardness());
            states.add(state);
        }
        return states;
    }

    private JsonObject writeGeometry(String name, List<JavaBlockModelResolver.Part> parts, List<WorldBlockSource> sources, Path root) throws IOException {
        Map<MaterialKey, BedrockElementGeometry.Material> bindings = new LinkedHashMap<>();
        JsonArray cubes = this.buildCubes(parts, sources.getFirst().visual(), bindings);
        // 每个部件先应用自己的变换，再统一缩放整个方块以保留部件之间的相对位置。
        float scale = BedrockElementGeometry.fitWorldBlock(cubes);
        String id = "geometry." + ResourcePackUtils.bedrockResourceIdentifier(Key.ce(name.substring("craftengine/".length()))) + ".world";
        writeGeometryFile(root.resolve("models/blocks/" + name + ".geo.json"), id, cubes);
        if (scale < 1) {
            String percent = String.format(Locale.ROOT, "%.4f", scale * 100.0);
            for (WorldBlockSource source : sources) {
                TranslationManager.warn("resource_pack.bedrock.block.scaled", source.id().asString(), Integer.toString(source.stateIndex()), percent);
            }
        }

        JsonObject entry = new JsonObject();
        entry.addProperty(NAME, name);
        entry.addProperty(GEOMETRY, id);
        this.writeMaterials(entry, name, bindings, root);
        return entry;
    }

    private JsonArray buildCubes(List<JavaBlockModelResolver.Part> parts, BlockStateWrapper visual,
                                 Map<MaterialKey, BedrockElementGeometry.Material> bindings) throws IOException {
        Map<JavaModel.Face, BedrockElementGeometry.Material> faces = new LinkedHashMap<>();
        BlockTint stateTint = null;
        JsonArray cubes = new JsonArray();
        for (JavaBlockModelResolver.Part part : parts) {
            for (JavaModel.Element element : part.model().elements()) {
                for (JavaModel.Face face : element.faces().values()) {
                    if (face.tintIndex() >= 0 && stateTint == null) {
                        stateTint = BlockTint.forState(visual);
                    }
                    // 叶片或瓜茎的颜色源在索引 0；同一张贴图也可能用于没有染色的面。
                    MaterialKey key = new MaterialKey(face.texture(), face.tintIndex() == 0 ? stateTint : BlockTint.NONE);
                    BedrockElementGeometry.Material material = bindings.get(key);
                    if (material == null) {
                        TextureData data = this.textures.read(face.texture(), TextureUsage.BLOCK);
                        float size = Math.max(data.image().getWidth(), data.image().getHeight());
                        String slot = "layer" + bindings.size();
                        material = new BedrockElementGeometry.Material(slot, data.image().getWidth() / size, data.image().getHeight() / size);
                        bindings.put(key, material);
                    }
                    faces.put(face, material);
                }
            }
            JsonArray converted = BedrockElementGeometry.convert(part.model(), faces);
            BedrockModelTransform.apply(converted, part.transform(), 1);
            cubes.addAll(converted);
        }
        return cubes;
    }

    private void writeMaterials(JsonObject entry, String name, Map<MaterialKey, BedrockElementGeometry.Material> bindings, Path root) throws IOException {
        JsonObject materials = new JsonObject();
        JsonObject tintMethods = new JsonObject();
        for (Map.Entry<MaterialKey, BedrockElementGeometry.Material> binding : bindings.entrySet()) {
            MaterialKey key = binding.getKey();
            String slot = binding.getValue().name();
            String tile = name + '/' + slot;
            this.atlas.write(this.textures.read(key.texture(), TextureUsage.BLOCK), key.tint().color(), tile, root);
            materials.addProperty(slot, tile);
            if (key.tint().method() != null) {
                tintMethods.addProperty(slot, key.tint().method());
            }
        }
        entry.add(MATERIALS, materials);
        if (!tintMethods.isEmpty()) {
            entry.add(TINT_METHODS, tintMethods);
        }
    }

    private record MaterialKey(JavaModel.Texture texture, BlockTint tint) {
    }

    private static void writeGeometryFile(Path path, String id, JsonArray cubes) throws IOException {
        JsonObject description = new JsonObject();
        description.addProperty("identifier", id);
        description.addProperty("texture_width", 16);
        description.addProperty("texture_height", 16);
        JsonObject bone = new JsonObject();
        bone.addProperty("name", "root");
        bone.add("pivot", VectorUtils.toJson(0, 8, 0));
        bone.add("cubes", cubes);
        JsonArray bones = new JsonArray();
        bones.add(bone);
        JsonObject geometry = new JsonObject();
        geometry.add("description", description);
        geometry.add("bones", bones);
        JsonArray geometries = new JsonArray();
        geometries.add(geometry);
        JsonObject file = new JsonObject();
        file.addProperty(FORMAT_VERSION, "1.21.0");
        file.add("minecraft:geometry", geometries);
        Files.createDirectories(path.getParent());
        GsonHelper.writeJsonFile(file, path);
    }
}
