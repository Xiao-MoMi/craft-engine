package net.momirealms.craftengine.core.item.equipment;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.processor.ItemProcessor;
import net.momirealms.craftengine.core.item.processor.OverwritableEquippableAssetIdProcessor;
import net.momirealms.craftengine.core.pack.Identifier;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.util.CharacterUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class ComponentBasedEquipment extends AbstractEquipment implements Supplier<JsonObject> {
    public static final EquipmentFactory<ComponentBasedEquipment> FACTORY = new Factory();
    private final EnumMap<EquipmentLayerType, List<Layer>> layers;

    public ComponentBasedEquipment(Key assetId) {
        super(assetId);
        this.layers = new EnumMap<>(EquipmentLayerType.class);
    }

    @Override
    public List<ItemProcessor> modifiers() {
        return List.of(new OverwritableEquippableAssetIdProcessor(this.assetId));
    }

    public EnumMap<EquipmentLayerType, List<Layer>> layers() {
        return layers;
    }

    public void addLayer(EquipmentLayerType layerType, List<Layer> layer) {
        this.layers.put(layerType, layer);
    }

    @Override
    public JsonObject get() {
        JsonObject jsonObject = new JsonObject();
        JsonObject layersJson = new JsonObject();
        jsonObject.add("layers", layersJson);
        for (Map.Entry<EquipmentLayerType, List<ComponentBasedEquipment.Layer>> entry : layers.entrySet()) {
            EquipmentLayerType type = entry.getKey();
            List<ComponentBasedEquipment.Layer> layerList = entry.getValue();
            setLayers(layersJson, layerList, type.id());
        }
        return jsonObject;
    }

    private void setLayers(JsonObject layersJson, List<ComponentBasedEquipment.Layer> layers, String key) {
        if (layers == null || layers.isEmpty()) return;
        JsonArray layersArray = new JsonArray();
        for (ComponentBasedEquipment.Layer layer : layers) {
            layersArray.add(layer.get());
        }
        layersJson.add(key, layersArray);
    }

    private static class Factory implements EquipmentFactory<ComponentBasedEquipment> {

        @Override
        public ComponentBasedEquipment create(Key id, ConfigSection section) {
            ComponentBasedEquipment equipment = new ComponentBasedEquipment(id);
            for (Map.Entry<String, Object> entry : section.values().entrySet()) {
                String key = entry.getKey();
                EquipmentLayerType layerType = EquipmentLayerType.byId(key);
                if (layerType != null) {
                    equipment.addLayer(layerType, Layer.fromConfig(layerType, section.assemblePath(key), entry.getValue()));
                } else {
                    throw new KnownResourceException("resource.equipment.unknown_layer_type", section.path(), key);
                }
            }
            return equipment;
        }
    }

    public record Layer(Key texture, DyeableData data, boolean usePlayerTexture) implements Supplier<JsonObject> {

        @NotNull
        public static List<Layer> fromConfig(EquipmentLayerType layer, String path, Object obj) {
            switch (obj) {
                case String texture -> {
                    texture = CharacterUtils.replaceBackslashWithSlash(texture);
                    if (!Identifier.isValid(texture)) {
                        throw new KnownResourceException(ConfigConstants.PARSE_IDENTIFIER_FAILED, path, texture);
                    }
                    Key textureKey = Key.of(texture);
                    return List.of(new Layer(getCorrectTexturePath(textureKey, layer), null, false));
                }
                case Map<?, ?> map -> {
                    ConfigSection data = ConfigSection.of(path, MiscUtils.castToMap(map));
                    Key textureKey = data.getNonNullIdentifier("texture");
                    return List.of(new Layer(
                            getCorrectTexturePath(textureKey, layer),
                            DyeableData.fromObj(data.get("dyeable")),
                            data.getBoolean("use_player_texture", "use-player-texture")
                    ));
                }
                case List<?> list -> {
                    List<Layer> layers = new ArrayList<>();
                    for (Object inner : list) {
                        layers.addAll(fromConfig(layer, path, inner));
                    }
                    return layers;
                }
                case null, default -> {
                    return List.of();
                }
            }
        }

        @Override
        public JsonObject get() {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("texture", this.texture.asMinimalString());
            if (this.data != null) {
                jsonObject.add("dyeable", this.data.get());
            }
            if (this.usePlayerTexture) {
                jsonObject.addProperty("use_player_texture", true);
            }
            return jsonObject;
        }

        private static Key getCorrectTexturePath(Key path, EquipmentLayerType layerType) {
            String prefix = "entity/equipment/" + layerType.id() + "/";
            if (path.value().startsWith(prefix)) {
                return Key.of(path.namespace(), path.value().substring(prefix.length()));
            }
            return path;
        }

        public record DyeableData(@Nullable Integer colorWhenUndyed) implements Supplier<JsonObject> {

            public static DyeableData fromObj(Object obj) {
                if (obj instanceof Map<?,?> map) {
                    Map<String, Object> data = MiscUtils.castToMap(map, false);
                    if (data.containsKey("color-when-undyed")) {
                        return new DyeableData(ResourceConfigUtils.getAsInt(data.get("color-when-undyed"), "color-when-undyed"));
                    }
                }
                return null;
            }

            @Override
            public JsonObject get() {
                JsonObject dyeData = new JsonObject();
                if (this.colorWhenUndyed != null) {
                    dyeData.addProperty("color_when_undyed", this.colorWhenUndyed);
                }
                return dyeData;
            }
        }

        @Override
        public @NotNull String toString() {
            return "Layer{" +
                    "texture='" + texture + '\'' +
                    ", data=" + data +
                    ", usePlayerTexture=" + usePlayerTexture +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "ComponentBasedEquipment{" +
                "layers=" + this.layers +
                '}';
    }
}
