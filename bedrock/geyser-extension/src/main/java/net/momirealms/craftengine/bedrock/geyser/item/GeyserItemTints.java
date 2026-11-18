package net.momirealms.craftengine.bedrock.geyser.item;

import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.CustomModelData;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;

import java.util.Map;

public final class GeyserItemTints {
    private final Map<String, Map<String, Integer>> bindings;

    public GeyserItemTints(Map<String, Map<String, Integer>> bindings) {
        this.bindings = Map.copyOf(bindings);
    }

    public boolean contains(ItemData item) {
        return item != null && this.bindings.containsKey(item.getDefinition().getIdentifier());
    }

    public boolean isEmpty() {
        return this.bindings.isEmpty();
    }

    public int color(GeyserItemStack stack, ItemData item, String view) {
        return this.color(stack == null || stack.isEmpty() ? null : stack.getAllComponents(), item, view);
    }

    int color(DataComponents components, ItemData item, String view) {
        Map<String, Integer> sources = this.bindings.get(item.getDefinition().getIdentifier());
        int source = sources == null ? -1 : sources.getOrDefault(view, -2);
        if (components == null || source < -1) {
            return -1;
        }
        if (source == -1) {
            Integer color = components.get(DataComponentTypes.DYED_COLOR);
            return color == null ? -1 : color & 0xffffff;
        }
        CustomModelData data = components.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        return data == null || source >= data.colors().size() ? -1 : data.colors().get(source) & 0xffffff;
    }

    public ItemData render(GeyserItemStack stack, ItemData item) {
        return this.contains(item) ? this.render(stack.getAllComponents(), item) : item;
    }

    ItemData render(DataComponents components, ItemData item) {
        if (!this.contains(item)) {
            return item;
        }
        int color = this.color(components, item, "gui");
        NbtMap tag = item.getTag();
        int previous = tag != null && tag.containsKey("customColor") ? tag.getInt("customColor") : -1;
        if (previous == color) {
            return item;
        }
        NbtMapBuilder builder = tag == null ? NbtMap.builder() : tag.toBuilder();
        if (color < 0) {
            builder.remove("customColor");
        } else {
            builder.putInt("customColor", color);
        }
        return item.toBuilder().tag(builder.build()).build();
    }
}
