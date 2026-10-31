package net.momirealms.craftengine.core.pack.bedrock.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockElementGeometry;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockModelTransform;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTerrainAtlas;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureUsage;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourcePackUtils;
import net.momirealms.craftengine.core.util.VectorUtils;
import net.momirealms.craftengine.core.world.collision.AABB;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.*;

public final class BedrockWorldBlocks {
    private static final Logger LOGGER = Logger.getLogger("CraftEngine");
    private final JavaPackTextureSource textures;
    private final JavaBlockModelResolver models;
    private final BedrockTerrainAtlas atlas = new BedrockTerrainAtlas();

    public BedrockWorldBlocks(JavaResourcePack resources) {
        this.textures = new JavaPackTextureSource(resources);
        this.models = new JavaBlockModelResolver(resources, new JavaModelResolver(resources, this.textures));
    }

    public Result write(List<WorldBlockSource> sources, Path root) throws IOException {
        JsonArray entries = new JsonArray();
        JsonArray diagnostics = new JsonArray();
        Map<String, List<WorldBlockSource>> groups = new LinkedHashMap<>();
        for (WorldBlockSource source : sources) {
            if (source.visual() == null) {
                diagnose(source, "No visual block state", diagnostics);
            } else {
                groups.computeIfAbsent(source.visual().getAsString(), ignored -> new ArrayList<>()).add(source);
            }
        }
        this.atlas.read(root);
        Path blockFile = root.resolve("blocks.json");
        JsonObject blocks = Files.isRegularFile(blockFile) ? GsonHelper.readJsonObjectFromFile(blockFile) : new JsonObject();
        for (Map.Entry<String, List<WorldBlockSource>> group : groups.entrySet()) {
            try {
                WorldBlockSource first = group.getValue().getFirst();
                JsonObject properties = components(first);
                for (WorldBlockSource source : group.getValue()) {
                    if (source.entityRenderer()) {
                        throw new UnsupportedOperationException("Block requires a specialized entity renderer");
                    }
                    if (source != first && !properties.equals(components(source))) {
                        throw new UnsupportedOperationException("Shared visual state has conflicting collision, selection, light or friction: " + group.getKey());
                    }
                }
                List<JavaBlockModelResolver.Part> parts = this.models.resolve(first.visual());
                String name = "craftengine/world/" + first.id().namespace() + '/' + first.id().value() + "/state_" + first.stateIndex();
                JsonObject entry = this.geometry(name, parts, group.getValue(), root);
                entry.addProperty("java_state", group.getKey());
                for (Map.Entry<String, JsonElement> property : properties.entrySet()) {
                    entry.add(property.getKey(), property.getValue());
                }
                JsonArray states = new JsonArray();
                for (WorldBlockSource source : group.getValue()) {
                    JsonObject server = new JsonObject();
                    server.addProperty("id", source.serverStateId());
                    server.addProperty("state", source.serverState());
                    server.addProperty("block_id", source.id().asString());
                    server.addProperty("hardness", source.hardness());
                    states.add(server);
                }
                entry.add("server_states", states);
                entries.add(entry);
                JsonObject block = new JsonObject();
                block.addProperty("sound", "stone");
                blocks.add("geyser_custom:" + name, block);
            } catch (UnsupportedOperationException exception) {
                // A visual state has one client definition; rejecting the entire group avoids applying the first state's shape to the others.
                for (WorldBlockSource source : group.getValue()) {
                    diagnose(source, exception.getMessage(), diagnostics);
                }
            }
        }
        if (!entries.isEmpty()) {
            blocks.add(FORMAT_VERSION, VectorUtils.toJson(1, 1, 0));
            GsonHelper.writeJsonFile(blocks, blockFile);
            this.atlas.finish(root);
        }
        return new Result(entries, diagnostics);
    }

    private static void diagnose(WorldBlockSource source, String reason, JsonArray diagnostics) {
        JsonObject diagnostic = new JsonObject();
        diagnostic.addProperty("block_id", source.id().asString());
        diagnostic.addProperty("state_index", source.stateIndex());
        diagnostic.addProperty("reason", reason);
        diagnostics.add(diagnostic);
    }

    private static JsonObject components(WorldBlockSource source) {
        if (source.collisionBoxes().size() > 16) {
            throw new UnsupportedOperationException("Bedrock supports at most 16 collision boxes per state");
        }
        JsonObject properties = new JsonObject();
        properties.addProperty("light_emission", source.lightEmission());
        properties.addProperty("light_dampening", source.lightDampening());
        float friction = 1 - source.friction();
        if (!Float.isFinite(friction) || friction < 0 || friction > 0.9f) {
            throw new UnsupportedOperationException("Block friction is outside Bedrock's supported range");
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
            throw new UnsupportedOperationException("Block shape exceeds Bedrock's bounds (height limit " + maxHeight + "): " + box);
        }
        JsonObject result = new JsonObject();
        result.add("origin", VectorUtils.toJson((float) (8 - box.maxX * 16), (float) (box.minY * 16), (float) (box.minZ * 16 - 8)));
        result.add("size", VectorUtils.toJson((float) ((box.maxX - box.minX) * 16), (float) ((box.maxY - box.minY) * 16), (float) ((box.maxZ - box.minZ) * 16)));
        return result;
    }

    private JsonObject geometry(String name, List<JavaBlockModelResolver.Part> parts, List<WorldBlockSource> sources, Path root) throws IOException {
        Map<JavaModel.Texture, BedrockElementGeometry.Material> bindings = new LinkedHashMap<>();
        Map<String, TextureData> textures = new LinkedHashMap<>();
        Map<JavaModel.Face, BedrockElementGeometry.Material> faces = new LinkedHashMap<>();
        JsonObject materials = new JsonObject();
        JsonArray cubes = new JsonArray();
        for (JavaBlockModelResolver.Part part : parts) {
            for (JavaModel.Element element : part.model().elements()) {
                for (JavaModel.Face face : element.faces().values()) {
                    BedrockElementGeometry.Material material = bindings.get(face.texture());
                    if (material == null) {
                        TextureData data = this.textures.read(face.texture(), TextureUsage.BLOCK);
                        float size = Math.max(data.image().getWidth(), data.image().getHeight());
                        String slot = "layer" + bindings.size();
                        material = new BedrockElementGeometry.Material(slot, data.image().getWidth() / size, data.image().getHeight() / size);
                        bindings.put(face.texture(), material);
                        textures.put(slot, data);
                    }
                    faces.put(face, material);
                }
            }
            JsonArray converted = BedrockElementGeometry.convert(part.model(), faces);
            BedrockModelTransform.apply(converted, part.transform(), 1);
            cubes.addAll(converted);
        }
        float scale = BedrockElementGeometry.fitWorldBlock(cubes);
        for (Map.Entry<String, TextureData> texture : textures.entrySet()) {
            String tile = name + '/' + texture.getKey();
            this.atlas.write(texture.getValue(), 0xffffff, tile, root);
            materials.addProperty(texture.getKey(), tile);
        }
        String id = "geometry." + ResourcePackUtils.bedrockResourceIdentifier(Key.ce(name.substring("craftengine/".length()))) + ".world";
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
        Path path = root.resolve("models/blocks/" + name + ".geo.json");
        Files.createDirectories(path.getParent());
        GsonHelper.writeJsonFile(file, path);
        if (scale < 1) {
            for (WorldBlockSource source : sources) {
                LOGGER.warning(String.format(Locale.ROOT, "Scaled Bedrock world model %s (state %d) to %.4f%% to fit block geometry bounds.",
                        source.id().asString(), source.stateIndex(), scale * 100.0));
            }
        }
        JsonObject entry = new JsonObject();
        entry.addProperty(NAME, name);
        entry.addProperty(GEOMETRY, id);
        entry.add(MATERIALS, materials);
        return entry;
    }

    public record Result(JsonArray entries, JsonArray diagnostics) {
    }
}
