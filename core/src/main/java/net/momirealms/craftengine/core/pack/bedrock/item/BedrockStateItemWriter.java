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
import net.momirealms.craftengine.core.pack.model.definition.BaseItemModel;
import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

final class BedrockStateItemWriter {
    private final JavaModelResolver resolver;
    private final BedrockTextureAtlas.TextureSource textures;
    private final BedrockItemIcons icons;
    private final BedrockBlockItems blockItems;
    private final Path root;
    private final BedrockModelConverter converter = new BedrockModelConverter();

    public BedrockStateItemWriter(JavaModelResolver resolver, BedrockTextureAtlas.TextureSource textures, BedrockItemIcons icons,
                            BedrockBlockItems blockItems, Path root) {
        this.resolver = resolver;
        this.textures = textures;
        this.icons = icons;
        this.blockItems = blockItems;
        this.root = root;
    }

    Result write(Key owner, String kind, Map<BaseItemModel, String> states, BaseItemModel inventory,
                 JsonArray update, boolean bow, BedrockWeaponDisplay weapon) throws IOException {
        String name = HashUtils.sha1(owner.asString().getBytes(StandardCharsets.UTF_8));
        JsonObject attachable = BedrockItemWriter.attachable(owner, "", "", "");
        JsonObject description = attachable.getAsJsonObject("minecraft:attachable").getAsJsonObject("description");
        description.add("geometry", new JsonObject());
        description.getAsJsonObject("textures").remove("default");
        description.add("animations", new JsonObject());
        description.add("render_controllers", new JsonArray());
        JsonObject scripts = new JsonObject();
        JsonArray initialize = new JsonArray();
        JsonArray animate = new JsonArray();
        scripts.add("initialize", initialize);
        scripts.add("pre_animation", update);
        scripts.add("animate", animate);
        description.add("scripts", scripts);
        JsonObject controllers = new JsonObject();
        String icon = null;
        BedrockPackMappings.DisplayBlock displayBlock = null;
        int index = 0;
        for (Map.Entry<BaseItemModel, String> state : states.entrySet()) {
            String prefix = "ce_" + kind + '_' + index + '_';
            Key resource = Key.of("ce", kind + '_' + name + '_' + index);
            BaseItemModel leaf = state.getKey();
            JavaModel model = this.resolver.resolve(leaf.path());
            List<Integer> tints = BedrockItemRendering.tints(leaf);
            BedrockModelConverter.Result converted = this.converter.convert(model, "geometry.craftengine." + kind + '.' + name + '.' + index, this.textures, tints);
            List<BedrockAnimatedTextures.Track> tracks = BedrockAnimatedTextures.write(converted, resource, this.root);
            BedrockItemWriter.writeResources(converted, resource, this.root, tracks, prefix, bow, weapon);
            if (leaf.equals(inventory)) {
                if (model.geometryKind() == JavaModel.GeometryKind.GENERATED && tracks.isEmpty()) {
                    icon = this.icons.write(converted, owner, tints, this.root);
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
            String condition = state.getValue();
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
            String controller = "controller.render.ce_" + kind + '_' + name + '_' + index;
            controllers.add(controller, staticController(prefix));
            addController(description, controller, condition);
            for (BedrockAnimatedTextures.Track track : tracks) {
                addController(description, "controller.render.ce_anim_" + track.resource(), condition);
            }
            index++;
        }
        if (initialize.isEmpty()) {
            scripts.remove("initialize");
        }
        if (update.isEmpty()) {
            scripts.remove("pre_animation");
        }
        if (icon == null && displayBlock == null) {
            BedrockNativeItemWriter.Appearance appearance = new BedrockNativeItemWriter(this.resolver, this.textures, this.icons, this.blockItems, this.root).write(inventory, owner);
            icon = appearance.icon();
            displayBlock = appearance.displayBlock();
        }
        this.writeFile("render_controllers/ce_" + kind + '_' + name + ".json", "render_controllers", controllers);
        return new Result(icon, displayBlock, states.size() + (states.containsKey(inventory) ? 0 : 1), attachable);
    }

    void writeAttachable(JsonObject attachable, Key identifier) throws IOException {
        attachable.getAsJsonObject("minecraft:attachable").getAsJsonObject("description").addProperty("identifier", BedrockItemSource.bedrockIdentifier(identifier));
        Path path = this.root.resolve("attachables/" + identifier.namespace() + '/' + identifier.value() + ".attachable.json");
        Files.createDirectories(path.getParent());
        GsonHelper.writeJsonFile(attachable, path);
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

    record Result(String icon, BedrockPackMappings.DisplayBlock displayBlock, int models, JsonObject attachable) {
    }
}
