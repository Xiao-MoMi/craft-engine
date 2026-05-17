package net.momirealms.craftengine.bukkit.item.factory;

import net.momirealms.craftengine.bukkit.item.ComponentItemWrapper;
import net.momirealms.craftengine.bukkit.item.DataComponentTypes;
import net.momirealms.craftengine.core.plugin.CraftEngine;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ComponentItemFactory1_21_4 extends ComponentItemFactory1_21_2 {

    public ComponentItemFactory1_21_4(CraftEngine plugin) {
        super(plugin);
    }

    @Override
    protected Optional<Integer> customModelData(ComponentItemWrapper item) {
        Optional<Object> optional = item.getComponentAsJava(DataComponentTypes.CUSTOM_MODEL_DATA);
        if (optional.isEmpty()) return Optional.empty();
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) optional.get();
        @SuppressWarnings("unchecked")
        List<Float> floats = (List<Float>) data.get("floats");
        if (floats == null || floats.isEmpty()) return Optional.empty();
        return Optional.of((int) Math.floor(floats.get(0)));
    }

    @Override
    protected void customModelData(ComponentItemWrapper item, Integer data) {
        if (data == null) {
            item.resetComponent(DataComponentTypes.CUSTOM_MODEL_DATA);
        } else {
            item.setJavaComponent(DataComponentTypes.CUSTOM_MODEL_DATA, Map.of("floats", List.of(data.floatValue())));
        }
    }
}
