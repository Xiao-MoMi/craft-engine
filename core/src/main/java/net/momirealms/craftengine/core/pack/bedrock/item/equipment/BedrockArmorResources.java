package net.momirealms.craftengine.core.pack.bedrock.item.equipment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRegistrationPlan;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureUsage;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.ImageUtils;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class BedrockArmorResources {
    private final JavaModelResources resources;
    private final Map<Key, JsonObject> definitions = new HashMap<>();
    private final Map<TextureKey, String> textures = new HashMap<>();

    BedrockArmorResources(JavaModelResources resources) {
        this.resources = resources;
    }

    static boolean hasAsset(ItemRegistrationPlan.Entry entry) {
        JsonElement value = entry.wornProperties().components().get(DataComponentKeys.EQUIPPABLE);
        return value != null && value.isJsonObject() && value.getAsJsonObject().has("asset_id");
    }

    @Nullable
    Appearance resolve(ItemRegistrationPlan.Entry entry, Path root) {
        if (!hasAsset(entry)) {
            return null;
        }
        JsonObject equipment = entry.wornProperties().components().get(DataComponentKeys.EQUIPPABLE).getAsJsonObject();
        try {
            Slot slot = Slot.from(equipment.get("slot").getAsString());
            Key asset = Key.of(equipment.get("asset_id").getAsString());
            JsonObject definition = this.definitions.get(asset);
            if (definition == null) {
                definition = this.resources.pack().readJson("assets/" + asset.namespace() + "/equipment/" + asset.value() + ".json");
                if (definition == null) {
                    throw new IOException("Missing equipment definition " + asset);
                }
                this.definitions.put(asset, definition);
            }
            JsonObject layers = definition.getAsJsonObject("layers");
            if (layers == null || !layers.has(slot.layer())) {
                return null;
            }
            Integer dye = dye(entry.wornProperties().components().get(DataComponentKeys.DYED_COLOR));
            TextureKey key = new TextureKey(asset, slot.layer(), dye);
            String texture = this.textures.get(key);
            if (texture == null) {
                BufferedImage image = this.compose(layers.getAsJsonArray(slot.layer()), slot.layer(), dye);
                texture = "textures/craftengine/equipment/" + asset.namespace() + '/' + asset.value() + '/' + slot.layer()
                        + (dye == null ? "_undyed" : "_" + Integer.toHexString(dye));
                ImageUtils.writePng(image, root.resolve(texture + ".png"));
                this.textures.put(key, texture);
            }
            return new Appearance(slot, texture);
        } catch (IOException | RuntimeException exception) {
            TranslationManager.warn("resource_pack.bedrock.item.armor_skipped", entry.bedrockId().asString(), equipment.toString(), exception.toString());
            return null;
        }
    }

    BufferedImage compose(JsonArray layers, String layerType, @Nullable Integer dye) throws IOException {
        List<BufferedImage> images = new ArrayList<>(layers.size());
        for (JsonElement element : layers) {
            JsonObject layer = element.getAsJsonObject();
            if (layer.has("use_player_texture") && layer.get("use_player_texture").getAsBoolean()) {
                throw new UnsupportedOperationException("Player textures are not supported for armor");
            }
            Integer color = layerColor(layer, dye);
            if (color == null) {
                continue;
            }
            Key texture = Key.of(layer.get("texture").getAsString());
            Key path = Key.of(texture.namespace(), "entity/equipment/" + layerType + '/' + texture.value());
            TextureData data = this.resources.textures().read(new JavaModel.Texture(path, false, false, true), TextureUsage.ITEM);
            if (data.animation() != null) {
                throw new UnsupportedOperationException("Animated armor texture " + path);
            }
            BufferedImage image = data.image();
            if (image.getWidth() != image.getHeight() * 2) {
                throw new UnsupportedOperationException("Expected a 2:1 humanoid armor texture: " + path);
            }
            images.add(color == 0xffffff ? image : ImageUtils.multiplyColor(image, color));
        }
        return images.isEmpty() ? new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB) : ImageUtils.composite(images);
    }

    @Nullable
    private static Integer layerColor(JsonObject layer, @Nullable Integer dye) {
        if (!layer.has("dyeable")) {
            return 0xffffff;
        }
        if (dye != null) {
            return dye;
        }
        JsonObject dyeable = layer.getAsJsonObject("dyeable");
        return dyeable.has("color_when_undyed") ? dyeable.get("color_when_undyed").getAsInt() & 0xffffff : null;
    }

    @Nullable
    private static Integer dye(@Nullable JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return null;
        }
        return (value.isJsonObject() ? value.getAsJsonObject().get("rgb").getAsInt() : value.getAsInt()) & 0xffffff;
    }

    private record TextureKey(Key asset, String layer, @Nullable Integer dye) {
    }

    record Slot(String part, String layer, String visibility) {

        static Slot from(String name) {
            return switch (name) {
                case "head" -> new Slot("helmet", "humanoid", "helmet");
                case "chest" -> new Slot("chestplate", "humanoid", "chest");
                case "legs" -> new Slot("leggings", "humanoid_leggings", "leg");
                case "feet" -> new Slot("boots", "humanoid", "boot");
                default -> throw new UnsupportedOperationException("Unsupported armor slot " + name);
            };
        }
    }

    record Appearance(Slot slot, String texture) {

        String condition() {
            return "!(context.item_slot == 'main_hand' || context.item_slot == 'off_hand') && !query.is_owner_identifier_any('geyser:item_display', 'geyser:block_display')";
        }

        void addResources(String prefix, JsonObject textures, JsonObject geometries, JsonObject controllers, JsonArray active) {
            textures.addProperty("ce_armor", this.texture);
            textures.addProperty("ce_armor_glint", "textures/misc/enchanted_actor_glint");
            geometries.addProperty("ce_armor", "geometry.humanoid.armor." + this.slot.part());
            JsonObject controller = new JsonObject();
            controller.addProperty("geometry", "Geometry.ce_armor");
            JsonArray materials = new JsonArray();
            JsonObject material = new JsonObject();
            material.addProperty("*", "variable.is_enchanted ? Material.ce_armor_enchanted : Material.ce_armor");
            materials.add(material);
            controller.add("materials", materials);
            JsonArray textureList = new JsonArray();
            textureList.add("Texture.ce_armor");
            textureList.add("Texture.ce_armor_glint");
            controller.add("textures", textureList);
            String name = "controller.render." + prefix + ".ce_armor";
            controllers.add(name, controller);
            JsonObject selected = new JsonObject();
            selected.addProperty(name, this.condition());
            active.add(selected);
        }

        void addMaterialsAndScripts(JsonObject materials, JsonObject scripts) {
            materials.addProperty("ce_armor", "armor");
            materials.addProperty("ce_armor_enchanted", "armor_enchanted");
            String visible = "variable." + this.slot.visibility() + "_layer_visible";
            scripts.addProperty("parent_setup", visible + " = (" + this.condition() + ") ? 0.0 : " + visible + ";");
        }
    }
}
