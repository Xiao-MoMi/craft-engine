package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.BedrockPackMappings;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockModelConverter;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockTextureAtlas;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.HashUtils;
import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BedrockBowWriter {
    private final JavaModelResolver resolver;
    private final BedrockTextureAtlas.TextureSource textures;
    private final BedrockItemIcons icons;
    private final BedrockBlockItems blockItems;
    private final Path root;
    private final BedrockModelConverter converter = new BedrockModelConverter();

    public BedrockBowWriter(JavaModelResolver resolver, BedrockTextureAtlas.TextureSource textures, BedrockItemIcons icons,
                            BedrockBlockItems blockItems, Path root) {
        this.resolver = resolver;
        this.textures = textures;
        this.icons = icons;
        this.blockItems = blockItems;
        this.root = root;
    }

    public Result write(JavaBowModel bow, List<BedrockItemSource> items) throws IOException {
        Key owner = items.getFirst().id();
        String name = HashUtils.sha1(owner.asString().getBytes(StandardCharsets.UTF_8));
        Map<JavaItemModelLeaf, Integer> states = new LinkedHashMap<>();
        states.put(bow.idle(), 0);
        states.computeIfAbsent(bow.pulling(), ignored -> states.size());
        for (JavaBowModel.Stage stage : bow.stages()) {
            states.computeIfAbsent(stage.model(), ignored -> states.size());
        }
        JsonObject attachable = BedrockItemWriter.attachable(owner, "", "", "");
        JsonObject description = attachable.getAsJsonObject("minecraft:attachable").getAsJsonObject("description");
        description.add("geometry", new JsonObject());
        description.getAsJsonObject("textures").remove("default");
        description.add("animations", new JsonObject());
        description.add("render_controllers", new JsonArray());
        JsonObject scripts = new JsonObject();
        JsonArray initialize = new JsonArray();
        JsonArray update = new JsonArray();
        JsonArray animate = new JsonArray();
        scripts.add("initialize", initialize);
        scripts.add("pre_animation", update);
        scripts.add("animate", animate);
        description.add("scripts", scripts);
        // Match vanilla bow.json's tick clock, while retaining the Java pack's scale and thresholds.
        update.add("variable.ce_bow_using = context.item_slot == 'main_hand' && query.main_hand_item_use_duration > 0.0;");
        update.add("variable.ce_bow_ticks = math.max(0, query.main_hand_item_max_duration - (query.main_hand_item_use_duration - query.frame_alpha + 1.0));");
        String time = bow.remaining() ? "math.max(0, query.main_hand_item_use_duration - query.frame_alpha + 1.0)" : "variable.ce_bow_ticks";
        update.add("variable.ce_bow_duration = (" + time + ") * " + bow.scale() + ";");
        String selected = Integer.toString(states.get(bow.pulling()));
        for (JavaBowModel.Stage stage : bow.stages()) {
            selected = "(variable.ce_bow_duration >= " + stage.threshold() + " ? " + states.get(stage.model()) + " : " + selected + ')';
        }
        update.add("variable.ce_bow_state = variable.ce_bow_using ? " + selected + " : 0;");
        update.add("variable.ce_bow_late_stage = " + (bow.stages().isEmpty() ? "0" : "variable.ce_bow_duration >= " + bow.stages().getFirst().threshold()) + ';');
        JsonObject controllers = new JsonObject();
        String icon = null;
        BedrockPackMappings.DisplayBlock displayBlock = null;
        for (Map.Entry<JavaItemModelLeaf, Integer> state : states.entrySet()) {
            int index = state.getValue();
            String prefix = "ce_bow_" + index + '_';
            Key resource = Key.of("ce", "bow_" + name + '_' + index);
            JavaItemModelLeaf leaf = state.getKey();
            JavaModel model = this.resolver.resolve(leaf.model());
            BedrockModelConverter.Result converted = this.converter.convert(model, "geometry.craftengine.bow." + name + '.' + index, this.textures, leaf.tints());
            List<BedrockAnimatedTextures.Track> tracks = BedrockAnimatedTextures.write(converted, resource, this.root);
            BedrockItemWriter.writeResources(converted, resource, this.root, tracks, prefix, true);
            if (index == 0) {
                if (model.geometryKind() == JavaModel.GeometryKind.GENERATED && tracks.isEmpty()) {
                    icon = this.icons.write(converted, owner, leaf.tints(), this.root);
                } else {
                    displayBlock = this.blockItems.write(converted, resource, this.root, tracks, false);
                }
            }
            JsonObject stage = BedrockItemWriter.attachable(owner, converted.geometry().identifier(),
                    "textures/" + resource.namespace() + '/' + resource.value(), "animation." + resource.namespace() + '.' + resource.value() + '.');
            JsonObject stageDescription = stage.getAsJsonObject("minecraft:attachable").getAsJsonObject("description");
            renameDefault(stageDescription.getAsJsonObject("geometry"), prefix);
            renameDefault(stageDescription.getAsJsonObject("textures"), prefix);
            BedrockAnimatedItem.bind(stageDescription, converted.geometry().identifier(), tracks, prefix);
            merge(description.getAsJsonObject("geometry"), stageDescription.getAsJsonObject("geometry"));
            merge(description.getAsJsonObject("textures"), stageDescription.getAsJsonObject("textures"));
            for (Map.Entry<String, JsonElement> animation : stageDescription.getAsJsonObject("animations").entrySet()) {
                description.getAsJsonObject("animations").add(prefix + animation.getKey(), animation.getValue());
            }
            String condition = "variable.ce_bow_state == " + index;
            JsonObject stageScripts = stageDescription.getAsJsonObject("scripts");
            for (JsonElement animation : stageScripts.getAsJsonArray("animate")) {
                for (Map.Entry<String, JsonElement> entry : animation.getAsJsonObject().entrySet()) {
                    JsonObject gated = new JsonObject();
                    gated.addProperty(prefix + entry.getKey(), condition + " && (" + entry.getValue().getAsString() + ')');
                    animate.add(gated);
                }
            }
            if (!tracks.isEmpty()) {
                initialize.addAll(stageScripts.getAsJsonArray("initialize"));
                update.addAll(stageScripts.getAsJsonArray("pre_animation"));
            }
            String controller = "controller.render.ce_bow_" + name + '_' + index;
            controllers.add(controller, staticController(prefix));
            addController(description, controller, condition);
            for (BedrockAnimatedTextures.Track track : tracks) {
                addController(description, "controller.render.ce_anim_" + track.resource(), condition);
            }
        }
        if (initialize.isEmpty()) {
            scripts.remove("initialize");
        }
        writeFile("render_controllers/ce_bow_" + name + ".json", "render_controllers", controllers);
        for (BedrockItemSource item : items) {
            description.addProperty("identifier", item.id().asString());
            Path path = this.root.resolve("attachables/" + item.id().namespace() + '/' + item.id().value() + ".attachable.json");
            Files.createDirectories(path.getParent());
            GsonHelper.writeJsonFile(attachable, path);
        }
        return new Result(icon, displayBlock, states.size());
    }

    private static JsonObject staticController(String prefix) {
        JsonObject controller = new JsonObject();
        controller.addProperty("geometry", "Geometry." + prefix + "default");
        JsonObject material = new JsonObject();
        material.addProperty("*", "variable.is_enchanted ? Material.enchanted : Material.default");
        JsonArray materials = new JsonArray();
        materials.add(material);
        controller.add("materials", materials);
        JsonArray textures = new JsonArray();
        textures.add("Texture." + prefix + "default");
        textures.add("Texture.enchanted");
        controller.add("textures", textures);
        return controller;
    }

    private static void addController(JsonObject description, String controller, String condition) {
        JsonObject entry = new JsonObject();
        entry.addProperty(controller, condition);
        description.getAsJsonArray("render_controllers").add(entry);
    }

    private static void renameDefault(JsonObject object, String prefix) {
        object.add(prefix + "default", object.remove("default"));
    }

    private static void merge(JsonObject target, JsonObject source) {
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            target.add(entry.getKey(), entry.getValue());
        }
    }

    private void writeFile(String path, String field, JsonObject value) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("format_version", "1.10.0");
        json.add(field, value);
        Path file = this.root.resolve(path);
        Files.createDirectories(file.getParent());
        GsonHelper.writeJsonFile(json, file);
    }

    public record Result(String icon, BedrockPackMappings.DisplayBlock displayBlock, int models) {
    }
}
