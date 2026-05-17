package net.momirealms.craftengine.core.item.equipment;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.processor.ItemProcessor;
import net.momirealms.craftengine.core.item.processor.OverwritableEquippableAssetIdProcessor;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        return this.layers;
    }

    public void addLayer(EquipmentLayerType layerType, List<Layer> layer) {
        this.layers.put(layerType, layer);
    }

    @Override
    public JsonObject get() {
        JsonObject jsonObject = new JsonObject();
        JsonObject layersJson = new JsonObject();
        jsonObject.add("layers", layersJson);
        for (Map.Entry<EquipmentLayerType, List<ComponentBasedEquipment.Layer>> entry : this.layers.entrySet()) {
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
            for (String layerTypeName : section.keySet()) {
                EquipmentLayerType layerType = EquipmentLayerType.byId(layerTypeName);
                if (layerType != null) {
                    equipment.addLayer(layerType, Layer.fromConfig(layerType, section.getNonNullValue(layerTypeName, ConfigConstants.ARGUMENT_IDENTIFIER)));
                }
            }
            return equipment;
        }
    }

    public record Layer(Key texture, DyeableData data, boolean usePlayerTexture) implements Supplier<JsonObject> {
        private static final String[] USE_PLAYER_TEXTURE = new String[] {"use_player_texture", "use-player-texture"};

        @NotNull
        public static Layer fromConfig(EquipmentLayerType layer, ConfigSection section) {
            Key textureKey = section.getNonNullIdentifier("texture");
            return new Layer(
                    getCorrectTexturePath(textureKey, layer),
                    section.getValue("dyeable", v -> DyeableData.fromConfig(v.getAsSection())),
                    section.getBoolean(USE_PLAYER_TEXTURE)
            );
        }

        @NotNull
        public static List<Layer> fromConfig(EquipmentLayerType layer, ConfigValue value) {
            if (value.is(Map.class)) {
                return List.of(fromConfig(layer, value.getAsSection()));
            } else if (value.is(List.class)) {
                return value.getAsList(v -> fromConfig(layer, v.getAsSection()));
            } else {
                Key texture = value.getAsIdentifier();
                return List.of(new Layer(getCorrectTexturePath(texture, layer), null, false));
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
            private static final String[] COLOR_WHEN_UNDYED = new String[] {"color_when_undyed", "color-when-undyed"};

            public static DyeableData fromConfig(ConfigSection section) {
                ConfigValue colorWhenUndyed = section.getValue(COLOR_WHEN_UNDYED);
                if (colorWhenUndyed != null) {
                    return new DyeableData(colorWhenUndyed.getAsInt());
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
