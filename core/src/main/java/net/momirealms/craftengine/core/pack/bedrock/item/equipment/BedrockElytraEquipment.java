package net.momirealms.craftengine.core.pack.bedrock.item.equipment;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.ELYTRA_VARIANT_PROPERTY;

public final class BedrockElytraEquipment {
    private final Map<String, Integer> textures = new LinkedHashMap<>();
    private final Map<Key, Integer> variants = new LinkedHashMap<>();

    void register(Key item, String texture) {
        int variant = this.textures.computeIfAbsent(texture, ignored -> this.textures.size() + 1);
        this.variants.put(item, variant);
    }

    public Map<Key, Integer> variants() {
        return Map.copyOf(this.variants);
    }

    public void write(Path root) throws IOException {
        if (this.variants.isEmpty()) {
            return;
        }
        JsonObject attachable = GsonHelper.readJsonFromResource("/bedrock/equipment/elytra.attachable.json").getAsJsonObject();
        JsonObject description = attachable.getAsJsonObject("minecraft:attachable").getAsJsonObject("description");
        JsonObject textureDefinitions = description.getAsJsonObject("textures");
        JsonArray textures = new JsonArray();
        textures.add("Texture.default");
        for (Map.Entry<String, Integer> entry : this.textures.entrySet()) {
            String alias = "wing_" + entry.getValue();
            textureDefinitions.addProperty(alias, entry.getKey());
            textures.add("Texture." + alias);
        }

        JsonObject controller = new JsonObject();
        controller.addProperty("geometry", "Geometry.default");
        JsonArray materials = new JsonArray();
        JsonObject material = new JsonObject();
        material.addProperty("*", "variable.is_enchanted ? Material.enchanted : Material.default");
        materials.add(material);
        controller.add("materials", materials);
        JsonObject textureArrays = new JsonObject();
        textureArrays.add("Array.ce_wings", textures);
        JsonObject arrays = new JsonObject();
        arrays.add("textures", textureArrays);
        controller.add("arrays", arrays);
        JsonArray renderedTextures = new JsonArray();
        renderedTextures.add("Array.ce_wings[query.has_property('" + ELYTRA_VARIANT_PROPERTY + "') ? query.property('" + ELYTRA_VARIANT_PROPERTY + "') : 0]");
        renderedTextures.add("Texture.enchanted");
        controller.add("textures", renderedTextures);
        JsonObject controllers = new JsonObject();
        controllers.add("controller.render.craftengine.elytra", controller);
        JsonObject controllerFile = new JsonObject();
        controllerFile.addProperty("format_version", "1.10.0");
        controllerFile.add("render_controllers", controllers);

        Files.createDirectories(root.resolve("attachables"));
        Files.createDirectories(root.resolve("render_controllers"));
        GsonHelper.writeJsonFile(attachable, root.resolve("attachables/craftengine_elytra.json"));
        GsonHelper.writeJsonFile(controllerFile, root.resolve("render_controllers/craftengine_elytra.json"));
    }
}
