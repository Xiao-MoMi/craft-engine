package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockGeometry;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockGeometryWriter;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.GsonHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class BedrockAnimatedItem {

    private BedrockAnimatedItem() {
    }

    static BedrockGeometry select(BedrockGeometry source, String material, String identifier, int width, int height) {
        List<BedrockGeometry.Cube> cubes = new ArrayList<>();
        for (BedrockGeometry.Cube cube : source.cubes()) {
            Map<Direction, BedrockGeometry.Face> faces = new EnumMap<>(Direction.class);
            for (Map.Entry<Direction, BedrockGeometry.Face> entry : cube.faces().entrySet()) {
                BedrockGeometry.Face face = entry.getValue();
                if (Objects.equals(face.material(), material)) {
                    faces.put(entry.getKey(), face.material() == null ? face
                            : new BedrockGeometry.Face(face.u(), face.v(), face.width(), face.height(), face.rotation()));
                }
            }
            if (!faces.isEmpty()) {
                cubes.add(new BedrockGeometry.Cube(cube.origin(), cube.size(), cube.rotation(), faces));
            }
        }
        return new BedrockGeometry(identifier, width, height, cubes, source.bounds());
    }

    static void write(BedrockGeometry geometry, BedrockGeometry.Bounds bounds, List<BedrockAnimatedTextures.Track> tracks,
                      Path root, String prefix, boolean bow) throws IOException {
        for (BedrockAnimatedTextures.Track track : tracks) {
            BedrockGeometry part = select(geometry, track.material(), geometry.identifier() + '.' + track.material(), track.size(), track.size());
            Path path = root.resolve("models/entity/ce_anim/" + track.resource() + ".geo.json");
            Files.createDirectories(path.getParent());
            GsonHelper.writeJsonFile(BedrockGeometryWriter.toJson(part, BedrockItemDisplay.rig(part, prefix + track.material() + '_', bow), bounds), path);
            JsonArray frames = new JsonArray();
            for (int index = 0; index < track.frames().size(); index++) {
                frames.add("Texture." + prefix + track.material() + '_' + index);
            }
            JsonObject textures = new JsonObject();
            textures.add("Array.frames", frames);
            JsonObject arrays = new JsonObject();
            arrays.add("textures", textures);
            JsonObject controller = new JsonObject();
            controller.add("arrays", arrays);
            controller.addProperty("geometry", "Geometry." + prefix + track.material());
            JsonObject material = new JsonObject();
            material.addProperty("*", "variable.is_enchanted ? Material.enchanted : Material.default");
            JsonArray materials = new JsonArray();
            materials.add(material);
            controller.add("materials", materials);
            JsonArray selected = new JsonArray();
            selected.add("Array.frames[math.floor(variable.ce_anim_" + track.resource() + " / " + track.interval() + ")]");
            selected.add("Texture.enchanted");
            controller.add("textures", selected);
            JsonObject controllers = new JsonObject();
            controllers.add("controller.render.ce_anim_" + track.resource(), controller);
            JsonObject file = new JsonObject();
            file.addProperty("format_version", "1.8.0");
            file.add("render_controllers", controllers);
            Path controllerPath = root.resolve("render_controllers/" + track.resource() + ".json");
            Files.createDirectories(controllerPath.getParent());
            GsonHelper.writeJsonFile(file, controllerPath);
        }
    }

    static void bind(JsonObject description, String geometry, List<BedrockAnimatedTextures.Track> tracks, String prefix) {
        if (tracks.isEmpty()) {
            return;
        }
        JsonObject textures = description.getAsJsonObject("textures");
        JsonObject geometries = description.getAsJsonObject("geometry");
        JsonArray controllers = description.getAsJsonArray("render_controllers");
        JsonArray initialize = new JsonArray();
        JsonArray update = new JsonArray();
        for (BedrockAnimatedTextures.Track track : tracks) {
            for (int index = 0; index < track.frames().size(); index++) {
                textures.addProperty(prefix + track.material() + '_' + index, track.frames().get(index));
            }
            geometries.addProperty(prefix + track.material(), geometry + '.' + track.material());
            controllers.add("controller.render.ce_anim_" + track.resource());
            String clock = "variable.ce_anim_" + track.resource();
            initialize.add(clock + " = 0;");
            update.add(clock + " = math.mod(" + clock + " + query.delta_time * 20, " + track.frames().size() * track.interval() + ");");
        }
        JsonObject scripts = description.getAsJsonObject("scripts");
        scripts.add("initialize", initialize);
        scripts.add("pre_animation", update);
    }
}
