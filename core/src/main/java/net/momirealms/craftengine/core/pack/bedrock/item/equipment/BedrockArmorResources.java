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
            String equipmentSlot = equipment.get("slot").getAsString();
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
            if (layers == null) {
                return null;
            }
            Integer dye = dye(entry.wornProperties().components().get(DataComponentKeys.DYED_COLOR));
            List<Layer> appearances = new ArrayList<>();
            for (Slot slot : Slot.values()) {
                if (!slot.equipmentSlot.equals(equipmentSlot) || !layers.has(slot.layer)) {
                    continue;
                }
                TextureKey key = new TextureKey(asset, slot.layer, dye);
                String texture = this.textures.get(key);
                if (texture == null) {
                    BufferedImage image = this.compose(layers.getAsJsonArray(slot.layer), slot.layer, dye);
                    texture = "textures/craftengine/equipment/" + asset.namespace() + '/' + asset.value() + '/' + slot.layer
                            + (dye == null ? "_undyed" : "_" + Integer.toHexString(dye));
                    ImageUtils.writePng(image, root.resolve(texture + ".png"));
                    this.textures.put(key, texture);
                }
                appearances.add(new Layer(slot, texture));
            }
            return appearances.isEmpty() ? null : new Appearance(List.copyOf(appearances));
        } catch (IOException | RuntimeException exception) {
            TranslationManager.warn("resource_pack.bedrock.item.armor_skipped", entry.bedrockId().asString(), equipment.toString(), exception.toString());
            return null;
        }
    }

    BufferedImage compose(JsonArray layers, String layerType, @Nullable Integer dye) throws IOException {
        List<BufferedImage> images = new ArrayList<>(layers.size());
        for (JsonElement element : layers) {
            JsonObject layer = element.getAsJsonObject();
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
            boolean rectangular = !layerType.equals("horse_body");
            if (image.getWidth() != image.getHeight() * (rectangular ? 2 : 1)) {
                throw new UnsupportedOperationException("Unexpected equipment texture aspect ratio for " + path);
            }
            images.add(color == 0xffffff ? image : ImageUtils.multiplyColor(image, color));
        }
        return images.isEmpty() ? new BufferedImage(64, layerType.equals("horse_body") ? 64 : 32, BufferedImage.TYPE_INT_ARGB) : ImageUtils.composite(images);
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

    enum Slot {
        HELMET("head", "humanoid", "geometry.humanoid.armor.helmet", "helmet"),
        CHESTPLATE("chest", "humanoid", "geometry.humanoid.armor.chestplate", "chest"),
        LEGGINGS("legs", "humanoid_leggings", "geometry.humanoid.armor.leggings", "leg"),
        BOOTS("feet", "humanoid", "geometry.humanoid.armor.boots", "boot"),
        WINGS("chest", "wings", "geometry.elytra", "chest"),
        WOLF("body", "wolf_body", "geometry.craftengine.wolf.armor", null),
        HORSE("body", "horse_body", null, null);

        private final String equipmentSlot;
        private final String layer;
        private final String geometry;
        private final String visibility;

        Slot(String equipmentSlot, String layer, @Nullable String geometry, @Nullable String visibility) {
            this.equipmentSlot = equipmentSlot;
            this.layer = layer;
            this.geometry = geometry;
            this.visibility = visibility;
        }
    }

    record Layer(Slot slot, String texture) {

        String alias() {
            return "ce_equipment_" + this.slot.layer;
        }

        String condition() {
            String condition = "!(context.item_slot == 'main_hand' || context.item_slot == 'off_hand') && !query.is_owner_identifier_any('geyser:item_display', 'geyser:block_display')";
            return this.slot == Slot.WOLF ? condition + " && query.is_owner_identifier_any('minecraft:wolf') && !query.is_baby" : condition;
        }
    }

    record Appearance(List<Layer> layers) {

        void registerAnimals(Key id, BedrockAnimalEquipment animals) {
            for (Layer layer : this.layers) {
                if (layer.slot == Slot.HORSE) {
                    animals.registerHorse(id, layer.texture);
                } else if (layer.slot == Slot.WOLF) {
                    animals.enableWolf();
                }
            }
        }

        void addResources(String prefix, JsonObject textures, JsonObject geometries, JsonObject controllers, JsonArray active) {
            for (Layer layer : this.layers) {
                if (layer.slot.geometry == null) {
                    continue;
                }
                String alias = layer.alias();
                textures.addProperty(alias, layer.texture);
                textures.addProperty("ce_equipment_glint", "textures/misc/enchanted_actor_glint");
                geometries.addProperty(alias, layer.slot.geometry);
                JsonObject controller = new JsonObject();
                controller.addProperty("geometry", "Geometry." + alias);
                JsonArray materials = new JsonArray();
                JsonObject material = new JsonObject();
                material.addProperty("*", "variable.is_enchanted ? Material." + alias + "_enchanted : Material." + alias);
                materials.add(material);
                controller.add("materials", materials);
                JsonArray textureList = new JsonArray();
                textureList.add("Texture." + alias);
                textureList.add("Texture.ce_equipment_glint");
                controller.add("textures", textureList);
                String name = "controller.render." + prefix + '.' + alias;
                controllers.add(name, controller);
                JsonObject selected = new JsonObject();
                selected.addProperty(name, layer.condition());
                active.add(selected);
            }
        }

        void addMaterialsAndScripts(JsonObject materials, JsonObject scripts, JsonObject animations) {
            StringBuilder parentSetup = new StringBuilder();
            for (Layer layer : this.layers) {
                if (layer.slot.geometry == null) {
                    continue;
                }
                String alias = layer.alias();
                boolean wings = layer.slot == Slot.WINGS;
                materials.addProperty(alias, wings ? "elytra" : "armor");
                materials.addProperty(alias + "_enchanted", wings ? "elytra_glint" : "armor_enchanted");
                if (layer.slot.visibility != null) {
                    String visible = "variable." + layer.slot.visibility + "_layer_visible";
                    parentSetup.append(visible).append(" = (").append(layer.condition()).append(") ? 0.0 : ").append(visible).append(';');
                }
                if (wings) {
                    animations.addProperty("ce_elytra_controller", "controller.animation.elytra.default");
                    for (String state : List.of("default", "gliding", "sneaking", "sleeping", "swimming")) {
                        animations.addProperty(state, "animation.elytra." + state);
                    }
                    JsonObject selected = new JsonObject();
                    selected.addProperty("ce_elytra_controller", layer.condition());
                    scripts.getAsJsonArray("animate").add(selected);
                }
            }
            if (!parentSetup.isEmpty()) {
                scripts.addProperty("parent_setup", parentSetup.toString());
            }
        }
    }
}
