package net.momirealms.craftengine.core.pack.bedrock.item.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.pack.bedrock.BedrockResourceException;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ResolvedItemBinding;
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

final class BedrockEquipmentResources {
    private static final Slot[] SLOTS = Slot.values();
    private static final List<String> ELYTRA_STATES = List.of("default", "gliding", "sneaking", "sleeping", "swimming");
    private static final List<String> TRIM_PATTERNS = List.of(
            "bolt",
            "coast",
            "dune",
            "eye",
            "flow",
            "host",
            "raiser",
            "rib",
            "sentry",
            "shaper",
            "silence",
            "snout",
            "spire",
            "tide",
            "vex",
            "ward",
            "wayfinder",
            "wild"
    );
    private final JavaModelResources resources;
    private final Map<Key, JsonObject> definitions = new HashMap<>();
    private final Map<TextureKey, String> textures = new HashMap<>();

    BedrockEquipmentResources(JavaModelResources resources) {
        this.resources = resources;
    }

    Appearance resolve(ResolvedItemBinding entry, @Nullable String equipmentSlot, @Nullable Key asset, Path root) throws IOException {
        if (asset == null) {
            return Appearance.EMPTY;
        }
        try {
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
                return Appearance.EMPTY;
            }
            Integer dye = dye(entry.clientProperties().components().get(DataComponentKeys.DYED_COLOR));
            List<Layer> appearances = new ArrayList<>();
            for (Slot slot : SLOTS) {
                if (!slot.equipmentSlot.equals(equipmentSlot) || !layers.has(slot.layer)) {
                    continue;
                }
                TextureKey key = new TextureKey(asset, slot.layer, dye);
                String texture = this.textures.get(key);
                if (texture == null) {
                    BufferedImage image = this.compose(layers.getAsJsonArray(slot.layer), slot.layer, dye);
                    texture = "textures/ce/equipment/" + asset.namespace() + '/' + asset.value() + '/' + slot.layer
                            + (dye == null ? "_undyed" : "_" + Integer.toHexString(dye));
                    ImageUtils.writePng(image, root.resolve(texture + ".png"));
                    this.textures.put(key, texture);
                }
                appearances.add(new Layer(slot, texture));
            }
            return appearances.isEmpty() ? Appearance.EMPTY : new Appearance(List.copyOf(appearances));
        } catch (BedrockResourceException exception) {
            JsonElement equipment = entry.clientProperties().components().get(DataComponentKeys.EQUIPPABLE);
            TranslationManager.warn("resource_pack.bedrock.item.armor_skipped", entry.bedrockId().asString(), equipment.toString(), exception.getLocalizedMessage());
            return Appearance.EMPTY;
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
                throw new BedrockResourceException("resource_pack.bedrock.reason.armor_animation", path.asString());
            }
            BufferedImage image = data.image();
            boolean rectangular = !layerType.equals("horse_body");
            if (image.getWidth() != image.getHeight() * (rectangular ? 2 : 1)) {
                throw new BedrockResourceException("resource_pack.bedrock.reason.armor_aspect_ratio", path.asString());
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
        WOLF("body", "wolf_body", "geometry.ce.wolf.armor", null),
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

        boolean supportsTrim() {
            return this == HELMET || this == CHESTPLATE || this == LEGGINGS || this == BOOTS;
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
        static final Appearance EMPTY = new Appearance(List.of());

        void registerVariant(Key id, boolean generated, BedrockAnimalEquipment animals, BedrockElytraEquipment elytra) {
            for (Layer layer : this.layers) {
                switch (layer.slot) {
                    case HORSE -> animals.registerHorse(id, layer.texture);
                    case WOLF -> animals.enableWolf();
                    case WINGS -> {
                        // 鞘翅贴图绑定到已生成的物品 ID，供客户端切换穿戴外观
                        if (generated) {
                            elytra.register(id, layer.texture);
                        }
                    }
                    default -> {
                    }
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
                if (layer.slot.supportsTrim()) {
                    // 客户端以 default 贴图合成 trim_path；护腿必须使用内层纹饰 UV，不能从自定义物品 ID 推断。
                    textures.addProperty("default", layer.texture);
                    geometries.addProperty("default", layer.slot.geometry);
                    for (String pattern : TRIM_PATTERNS) {
                        textures.addProperty(pattern + "_trim", "textures/trims/" + pattern + (layer.slot == Slot.LEGGINGS ? "_leggings" : ""));
                    }
                }
                JsonObject controller = new JsonObject();
                controller.addProperty("geometry", "Geometry." + alias);
                JsonArray materials = new JsonArray();
                JsonObject material = new JsonObject();
                material.addProperty("*", "variable.is_enchanted ? Material." + alias + "_enchanted : Material." + alias);
                materials.add(material);
                controller.add("materials", materials);
                JsonArray textureList = new JsonArray();
                textureList.add(layer.slot.supportsTrim() ? "variable.has_trim ? variable.trim_path : Texture." + alias : "Texture." + alias);
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
            if (this.layers.isEmpty()) {
                return;
            }
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
                    for (String state : ELYTRA_STATES) {
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
