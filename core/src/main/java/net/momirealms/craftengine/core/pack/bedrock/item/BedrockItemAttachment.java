package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourcePackUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.FORMAT_VERSION;

final class BedrockItemAttachment {
    static final String IN_HAND = "context.item_slot == 'main_hand' || context.item_slot == 'off_hand'";

    private final Key id;
    private final String resource;
    final String prefix;
    private final JsonObject textures = new JsonObject();
    private final JsonObject geometryNames = new JsonObject();
    private final JsonArray geometries = new JsonArray();
    final JsonObject animations = new JsonObject();
    final JsonObject animationNames = new JsonObject();
    final JsonArray animate = new JsonArray();
    final JsonArray controllers = new JsonArray();

    BedrockItemAttachment(Key id) {
        this.id = id;
        this.resource = id.namespace() + '/' + id.value();
        this.prefix = ResourcePackUtils.bedrockResourceIdentifier(id);
        this.textures.addProperty("enchanted", "textures/misc/enchanted_item_glint");
    }

    void addGeometry(String alias, String geometryId, String texture, JsonObject geometry) {
        this.geometryNames.addProperty(alias, geometryId);
        this.textures.addProperty(alias, texture);
        this.geometries.add(geometry);
    }

    void showInHand(String controller) {
        this.controllers.add(GsonHelper.createObject(controller, new JsonPrimitive(IN_HAND)));
    }

    Path controllerPath(Path root) {
        return root.resolve("render_controllers/" + this.resource + ".render_controllers.json");
    }

    void write(Path root, @Nullable JsonArray preAnimation) throws IOException {
        JsonObject geometryFile = new JsonObject();
        geometryFile.addProperty(FORMAT_VERSION, "1.16.0");
        geometryFile.add("minecraft:geometry", this.geometries);
        writeJson(geometryFile, root.resolve("models/entity/" + this.resource + ".geo.json"));

        JsonObject animationFile = new JsonObject();
        animationFile.addProperty(FORMAT_VERSION, preAnimation == null ? "1.8.0" : "1.10.0");
        animationFile.add("animations", this.animations);
        writeJson(animationFile, root.resolve("animations/" + this.resource + ".animation.json"));

        JsonObject description = new JsonObject();
        description.addProperty("identifier", this.id.asString());
        JsonObject materials = new JsonObject();
        materials.addProperty("default", preAnimation == null ? "entity_alphatest_one_sided" : "entity_alphatest");
        materials.addProperty("enchanted", "entity_alphatest_glint");
        description.add("materials", materials);
        description.add("textures", this.textures);
        description.add("geometry", this.geometryNames);
        description.add("animations", this.animationNames);
        JsonObject scripts = new JsonObject();
        scripts.add("animate", this.animate);
        if (preAnimation != null) {
            scripts.add("pre_animation", preAnimation);
        }
        description.add("scripts", scripts);
        description.add("render_controllers", this.controllers);
        JsonObject file = new JsonObject();
        file.addProperty(FORMAT_VERSION, "1.10.0");
        file.add("minecraft:attachable", GsonHelper.createObject("description", description));
        writeJson(file, root.resolve("attachables/" + this.resource + ".attachable.json"));
    }

    static void writeJson(JsonObject json, Path path) throws IOException {
        Files.createDirectories(path.getParent());
        GsonHelper.writeJsonFile(json, path);
    }
}
