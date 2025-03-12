package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;

import java.util.Map;

public class ComponentModifier<I> implements ItemModifier<I> {
    private final Map<String, Object> arguments;

    public ComponentModifier(Map<String, Object> arguments) {
        this.arguments = arguments;
    }

    @Override
    public String name() {
        return "components";
    }

    @Override
    public void apply(Item<I> item, ItemBuildContext context) {
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            item.setComponent(key, value);
        }
    }
}
