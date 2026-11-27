package net.momirealms.craftengine.core.pack.bedrock.item.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.FORMAT_VERSION;
import static net.momirealms.craftengine.core.pack.bedrock.BedrockFormatVersions.*;

public final class BedrockAnimalEquipment {
    private static final int HORSE_ITEMS_PER_QUERY = 16;
    private boolean wolf;
    private final Map<Key, String> horseTextures = new LinkedHashMap<>();

    void enableWolf() {
        this.wolf = true;
    }

    void registerHorse(Key item, String texture) {
        this.horseTextures.put(item, texture);
    }

    public void write(Path root) throws IOException {
        if (this.wolf) {
            Path geometry = root.resolve("models/entity/ce_wolf_armor.geo.json");
            Files.createDirectories(geometry.getParent());
            JsonObject model = GsonHelper.readJsonFromResource("/bedrock/equipment/wolf.geo.json").getAsJsonObject();
            model.addProperty(FORMAT_VERSION, GEOMETRY);
            GsonHelper.writeJsonFile(model, geometry);
        }
        if (this.horseTextures.isEmpty()) {
            return;
        }
        JsonObject entity = GsonHelper.readJsonFromResource("/bedrock/equipment/horse.entity.json").getAsJsonObject();
        entity.addProperty(FORMAT_VERSION, CLIENT_ENTITY);
        JsonObject description = entity.getAsJsonObject("minecraft:client_entity").getAsJsonObject("description");
        JsonObject controllerFile = GsonHelper.readJsonFromResource("/bedrock/equipment/horse.render_controllers.json").getAsJsonObject();
        controllerFile.addProperty(FORMAT_VERSION, RENDER_CONTROLLER);
        JsonObject controllers = controllerFile.getAsJsonObject("render_controllers");
        JsonObject controller = controllers.remove("controller.render.horse.v4").getAsJsonObject();
        String controllerName = "controller.render.ce.horse";
        controllers.add(controllerName, controller);
        JsonArray active = new JsonArray();
        active.add(controllerName);
        description.add("render_controllers", active);

        JsonArray textures = new JsonArray();
        textures.add("Texture.armor_none");
        Map<String, List<Key>> itemsByTexture = new LinkedHashMap<>();
        for (Map.Entry<Key, String> entry : this.horseTextures.entrySet()) {
            itemsByTexture.computeIfAbsent(entry.getValue(), ignored -> new ArrayList<>()).add(entry.getKey());
        }
        JsonArray preAnimation = description.getAsJsonObject("scripts").getAsJsonArray("pre_animation");
        preAnimation.add("variable.ce_horse_equipment = 0;");
        for (Map.Entry<String, List<Key>> entry : itemsByTexture.entrySet()) {
            int index = textures.size();
            String alias = "ce_horse_" + index;
            description.getAsJsonObject("textures").addProperty(alias, entry.getKey());
            textures.add("Texture." + alias);
            // 分批写入独立表达式，避免大量马铠变体耗尽客户端的 Molang 解析栈。
            List<Key> items = entry.getValue();
            for (int start = 0; start < items.size(); start += HORSE_ITEMS_PER_QUERY) {
                StringBuilder query = new StringBuilder("!variable.ce_horse_equipment && query.is_item_name_any('slot.armor.body', 0");
                int end = Math.min(start + HORSE_ITEMS_PER_QUERY, items.size());
                for (int i = start; i < end; i++) {
                    query.append(", '").append(items.get(i).asString()).append("'");
                }
                preAnimation.add(query.append(") ? { variable.ce_horse_equipment = ").append(index).append("; };").toString());
            }
        }
        controller.getAsJsonObject("arrays").getAsJsonObject("textures").add("Array.ce_horse_equipment", textures);
        JsonArray renderedTextures = controller.getAsJsonArray("textures");
        String vanillaArmor = renderedTextures.get(2).getAsString();
        renderedTextures.set(2, new JsonPrimitive("!query.is_baby && variable.ce_horse_equipment ? Array.ce_horse_equipment[variable.ce_horse_equipment] : (" + vanillaArmor + ")"));
        // 自定义颜色已烘焙到装备贴图，保留原版马铠的染色分支，避免重复着色。
        JsonObject color = controller.getAsJsonObject("color");
        for (Map.Entry<String, JsonElement> entry : color.entrySet()) {
            entry.setValue(new JsonPrimitive("variable.ce_horse_equipment ? this : (" + entry.getValue().getAsString() + ")"));
        }
        Files.createDirectories(root.resolve("entity"));
        Files.createDirectories(root.resolve("render_controllers"));
        GsonHelper.writeJsonFile(entity, root.resolve("entity/horse_v3.entity.json"));
        GsonHelper.writeJsonFile(controllerFile, root.resolve("render_controllers/ce_horse.render_controllers.json"));
    }
}
