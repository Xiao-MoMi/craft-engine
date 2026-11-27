package net.momirealms.craftengine.bedrock.geyser.item;

import net.momirealms.craftengine.bedrock.BedrockTintSource;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.CustomModelData;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;

import java.util.Map;

public record GeyserItemTints(int gui, int head, int firstRight, int thirdRight, int firstLeft, int thirdLeft) {
    static final GeyserItemTints DEFAULT = new GeyserItemTints(BedrockTintSource.NONE, BedrockTintSource.DYED_COLOR,
            BedrockTintSource.DYED_COLOR, BedrockTintSource.DYED_COLOR, BedrockTintSource.DYED_COLOR, BedrockTintSource.DYED_COLOR);

    static GeyserItemTints from(Map<String, Integer> sources) {
        return new GeyserItemTints(
                sources.getOrDefault("gui", BedrockTintSource.NONE),
                sources.getOrDefault("head", BedrockTintSource.NONE),
                sources.getOrDefault("firstperson_righthand", BedrockTintSource.NONE),
                sources.getOrDefault("thirdperson_righthand", BedrockTintSource.NONE),
                sources.getOrDefault("firstperson_lefthand", BedrockTintSource.NONE),
                sources.getOrDefault("thirdperson_lefthand", BedrockTintSource.NONE)
        );
    }

    public static int color(GeyserItemStack stack, int source) {
        if (source == BedrockTintSource.NONE || stack == null || stack.isEmpty()) {
            return -1;
        }
        return color(stack::getComponent, source);
    }

    static int color(ComponentGetter components, int source) {
        if (source == BedrockTintSource.FIREWORK) {
            return GeyserComponentColors.firework(components.get(DataComponentTypes.FIREWORK_EXPLOSION));
        }
        if (source == BedrockTintSource.POTION) {
            return GeyserComponentColors.potion(components.get(DataComponentTypes.POTION_CONTENTS));
        }
        if (source == BedrockTintSource.DYED_COLOR) {
            Integer color = components.get(DataComponentTypes.DYED_COLOR);
            return color == null ? -1 : color & 0xffffff;
        }
        if (source < 0) {
            return -1;
        }
        CustomModelData data = components.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        return data == null || source >= data.colors().size() ? -1 : data.colors().get(source) & 0xffffff;
    }

    ItemData render(ComponentGetter components, ItemData item) {
        if (this == DEFAULT) {
            return item;
        }
        int color = color(components, this.gui);
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

    @FunctionalInterface
    interface ComponentGetter {
        <T> T get(DataComponentType<T> type);
    }
}
