package net.momirealms.craftengine.core.item.equipment;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.modifier.EquippableAssetIdModifier;
import net.momirealms.craftengine.core.item.modifier.ItemDataModifier;
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

public class ComponentBasedEquipment extends AbstractEquipment implements Supplier<JsonObject> {
    public static final Factory FACTORY = new Factory();
    private final EnumMap<EquipmentLayerType, List<Layer>> layers;

    public ComponentBasedEquipment(Key assetId) {
        super(assetId);
        this.layers = new EnumMap<>(EquipmentLayerType.class);
    }

    @Override
    public Key type() {
        return Equipments.COMPONENT;
    }

    @Override
    public <I> List<ItemDataModifier<I>> modifiers() {
        return List.of(new EquippableAssetIdModifier<>(this.assetId));
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

    public static class Factory implements EquipmentFactory {

        @Override
        public ComponentBasedEquipment create(Key id, Map<String, Object> args) {
            ComponentBasedEquipment equipment = new ComponentBasedEquipment(id);
            for (Map.Entry<String, Object> entry : args.entrySet()) {
                EquipmentLayerType layerType = EquipmentLayerType.byId(entry.getKey());
                if (layerType != null) {
                    equipment.addLayer(layerType, Layer.fromConfig(entry.getValue()));
                }
            }
            return equipment;
        }
    }

    public record Layer(String texture, DyeableData data, boolean usePlayerTexture) implements Supplier<JsonObject> {

        @NotNull
        public static List<Layer> fromConfig(Object obj) {
            switch (obj) {
                case String texture -> {
                    return List.of(new Layer(texture, null, false));
                }
                case Map<?, ?> map -> {
                    Map<String, Object> data = MiscUtils.castToMap(map, false);
                    String texture = data.get("texture").toString();
                    return List.of(new Layer(texture,
                            DyeableData.fromObj(data.get("dyeable")),
                            ResourceConfigUtils.getAsBoolean(data.getOrDefault("use-player-texture", false), "use-player-texture")
                    ));
                }
                case List<?> list -> {
                    List<Layer> layers = new ArrayList<>();
                    for (Object inner : list) {
                        layers.addAll(fromConfig(inner));
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
            jsonObject.addProperty("texture", texture);
            if (this.data != null) {
                jsonObject.add("dyeable", this.data.get());
            }
            if (this.usePlayerTexture) {
                jsonObject.addProperty("use_player_texture", true);
            }
            return jsonObject;
        }

        public record DyeableData(@Nullable Integer colorWhenUndyed) implements Supplier<JsonObject> {

            public static DyeableData fromObj(Object obj) {
                if (obj instanceof Map<?,?> map) {
                    Map<String, Object> data = MiscUtils.castToMap(map, false);
                    if (data.containsKey("color-when-undyed")) {
                        return new DyeableData(ResourceConfigUtils.getAsInt(data.get("color-when-undyed"), "color-when-undyed"));
                    }
                }
                return new DyeableData(null);
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
    }
}
