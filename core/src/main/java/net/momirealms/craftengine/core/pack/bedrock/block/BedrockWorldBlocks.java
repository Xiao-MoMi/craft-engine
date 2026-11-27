package net.momirealms.craftengine.core.pack.bedrock.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.pack.bedrock.BedrockFormatVersions;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockElementGeometry;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockModelTransform;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.pack.bedrock.sound.BedrockSounds;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTerrainAtlas;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureUsage;
import net.momirealms.craftengine.core.plugin.CraftEngine;
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
    private final BedrockTerrainAtlas atlas;

    private BedrockWorldBlocks(JavaModelResources resources, BedrockTerrainAtlas atlas) {
        this.atlas = atlas;
        this.textures = resources.textures();
        this.models = resources.blockModels();
    }

    public static JsonArray write(JavaModelResources resources, List<WorldBlockSource> sources, Path root, BedrockTerrainAtlas atlas, BedrockBlockDefinitions blockDefinitions) throws IOException {
        return write(resources, sources, root, atlas, blockDefinitions, Map.of());
    }

    public static JsonArray write(JavaModelResources resources, List<WorldBlockSource> sources, Path root, BedrockTerrainAtlas atlas, BedrockBlockDefinitions blockDefinitions, Map<Key, Key> replacements) throws IOException {
        BedrockSounds sounds = new BedrockSounds(resources.pack(), replacements);
        JsonArray blocks = new BedrockWorldBlocks(resources, atlas).writeBlocks(sources, root, blockDefinitions, sounds);
        sounds.write(root, new JsonObject());
        return blocks;
    }

    public static Result write(JavaModelResources resources, WorldBlockPlan plan, Path root, BedrockTerrainAtlas atlas,
                               BedrockBlockDefinitions definitions, BedrockSounds sounds) throws IOException {
        BedrockWorldBlocks writer = new BedrockWorldBlocks(resources, atlas);
        JsonArray customBlocks = writer.writeBlocks(plan.customBlocks(), root, definitions, sounds);
        return new Result(customBlocks, writer.writeVanillaOverrides(plan.vanillaOverrides(), root, definitions, sounds));
    }

    private JsonArray writeVanillaOverrides(List<WorldBlockPlan.VanillaOverride> overrides, Path root,
                                            BedrockBlockDefinitions definitions, BedrockSounds sounds) throws IOException {
        JsonArray entries = new JsonArray(overrides.size());
        for (WorldBlockPlan.VanillaOverride override : overrides) {
            try {
                entries.add(this.writeVanillaState(override, root, definitions, sounds));
            } catch (UnsupportedOperationException exception) {
                CraftEngine.instance().logger().warn("Skipped Bedrock vanilla model for " + override.state().getAsString() + ": " + exception.getLocalizedMessage());
            }
        }
        return entries;
    }

    private JsonObject writeVanillaState(WorldBlockPlan.VanillaOverride override, Path root,
                                         BedrockBlockDefinitions definitions, BedrockSounds sounds) throws IOException {
        BlockStateWrapper state = override.state();
        Key block = state.ownerId();
        WorldBlockProperties blockProperties = override.properties().components();
        JsonObject properties = components(blockProperties);
        List<JavaBlockModelResolver.Part> parts = this.models.resolveVanillaOverride(override.model());
        BedrockSounds.Group sound = sounds.resolve(override.properties().sounds(), block);
        String name = PATH_PREFIX + "vanilla/" + block.namespace() + '/' + block.value() + "/state_" + state.registryId();
        GeneratedGeometry geometry = this.writeGeometry(name, parts, state, root);
        if (geometry.scale() < 1) {
            warnScaled(block, state.registryId(), scalePercent(geometry.scale()));
        }
        JsonObject entry = geometry.entry();
        entry.addProperty("java_state", state.getAsString());
        entry.addProperty("hardness", blockProperties.hardness());
        for (Map.Entry<String, JsonElement> property : properties.entrySet()) {
            entry.add(property.getKey(), property.getValue());
        }
        definitions.register(name, sounds.register(sound));
        return entry;
    }

    private JsonArray writeBlocks(List<WorldBlockSource> sources, Path root, BedrockBlockDefinitions blockDefinitions, BedrockSounds sounds) throws IOException {
        Map<String, List<WorldBlockSource>> groups = groupByVisualState(sources);
        JsonArray entries = new JsonArray(groups.size());
        for (Map.Entry<String, List<WorldBlockSource>> group : groups.entrySet()) {
            try {
                entries.add(this.writeGroup(group.getKey(), group.getValue(), blockDefinitions, sounds, root));
            } catch (UnsupportedOperationException exception) {
                // 同一显示状态只能注册一套方块属性；跳过整组，避免其他状态误用首个状态的碰撞箱。
                for (WorldBlockSource source : group.getValue()) {
                    TranslationManager.warn("resource_pack.bedrock.block.skipped", source.id().asString(), Integer.toString(source.stateIndex()), exception.getLocalizedMessage());
                }
            }
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

    private JsonObject writeGroup(String visualState, List<WorldBlockSource> sources, BedrockBlockDefinitions blockDefinitions, BedrockSounds sounds, Path root) throws IOException {
        JsonObject properties = sharedComponents(visualState, sources);
        WorldBlockSource first = sources.getFirst();
        BedrockSounds.Group group = sounds.resolve(first.sounds(), first.id());
        for (int i = 1; i < sources.size(); i++) {
            WorldBlockSource source = sources.get(i);
            BedrockSounds.Group other = sounds.resolve(source.sounds(), source.id());
            if (!group.equals(other)) {
                throw new UnsupportedOperationException(TranslationManager.message("resource_pack.bedrock.block.sound_conflict",
                        visualState, first.id().asString(), group.toString(), source.id().asString(), other.toString()));
            }
        }
        String name = PATH_PREFIX + "world/" + first.id().namespace() + '/' + first.id().value() + "/state_" + first.stateIndex();
        List<JavaBlockModelResolver.Part> parts = this.models.resolve(first.visual());

        GeneratedGeometry geometry = this.writeGeometry(name, parts, first.visual(), root);
        if (geometry.scale() < 1) {
            String percent = scalePercent(geometry.scale());
            for (WorldBlockSource source : sources) {
                warnScaled(source.id(), source.stateIndex(), percent);
            }
        }
        JsonObject entry = geometry.entry();
        entry.addProperty("java_state", visualState);
        for (Map.Entry<String, JsonElement> property : properties.entrySet()) {
            entry.add(property.getKey(), property.getValue());
        }
        entry.add("server_states", serverStatesToJson(sources));

        blockDefinitions.register(name, sounds.register(group));
        return entry;
    }

    private static JsonObject sharedComponents(String visualState, List<WorldBlockSource> sources) {
        WorldBlockSource first = sources.getFirst();
        JsonObject properties = components(first.properties());
        for (WorldBlockSource source : sources) {
            if (source != first && !properties.equals(components(source.properties()))) {
                throw new UnsupportedOperationException(TranslationManager.message("resource_pack.bedrock.block.conflict", visualState));
            }
        }
        return properties;
    }

    private static JsonObject components(WorldBlockProperties source) {
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
            states.add(state);
        }
        return states;
    }

    private GeneratedGeometry writeGeometry(String name, List<JavaBlockModelResolver.Part> parts, BlockStateWrapper visual, Path root) throws IOException {
        Map<MaterialKey, BedrockElementGeometry.Material> bindings = new LinkedHashMap<>();
        JsonArray cubes = this.buildCubes(parts, visual, bindings);
        // 每个部件先应用自己的变换，再统一缩放整个方块以保留部件之间的相对位置。
        float scale = BedrockElementGeometry.fitWorldBlock(cubes);
        String id = "geometry." + ResourcePackUtils.bedrockResourceIdentifier(Key.of(NAMESPACE, name.substring(PATH_PREFIX.length()))) + ".world";
        writeGeometryFile(root.resolve("models/blocks/" + name + ".geo.json"), id, cubes);
        JsonObject entry = new JsonObject();
        entry.addProperty(NAME, name);
        entry.addProperty(GEOMETRY, id);
        this.writeMaterials(entry, name, bindings, root);
        return new GeneratedGeometry(entry, scale);
    }

    private static String scalePercent(float scale) {
        return String.format(Locale.ROOT, "%.4f", scale * 100.0);
    }

    private static void warnScaled(Key block, int stateIndex, String percent) {
        TranslationManager.warn("resource_pack.bedrock.block.scaled", block.asString(), Integer.toString(stateIndex), percent);
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
                        BedrockTerrainAtlas.validateAnimation(data, face.texture().sprite().asString());
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

    public record Result(JsonArray customBlocks, JsonArray vanillaOverrides) {
    }

    private record GeneratedGeometry(JsonObject entry, float scale) {
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
        file.addProperty(FORMAT_VERSION, BedrockFormatVersions.GEOMETRY);
        file.add("minecraft:geometry", geometries);
        Files.createDirectories(path.getParent());
        GsonHelper.writeJsonFile(file, path);
    }
}
