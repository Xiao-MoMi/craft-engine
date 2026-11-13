package net.momirealms.craftengine.bedrock.geyser.entity.display;

import net.momirealms.craftengine.bedrock.BedrockHeldItemType;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;

final class GeyserHeldItemTypes {
    private GeyserHeldItemTypes() {
    }

    static BedrockHeldItemType resolve(ItemData item, boolean block) {
        String id = item.getDefinition().getIdentifier();
        if (block) {
            return BedrockHeldItemType.BLOCK;
        }
        switch (id) {
            case "minecraft:bow" -> {
                return BedrockHeldItemType.BOW;
            }
            case "minecraft:shield" -> {
                return BedrockHeldItemType.SHIELD;
            }
            case "minecraft:trident" -> {
                return BedrockHeldItemType.TRIDENT;
            }
            case "minecraft:mace" -> {
                return BedrockHeldItemType.HANDHELD;
            }
            case "minecraft:crossbow", "minecraft:spyglass" -> {
                return BedrockHeldItemType.NATIVE;
            }
        }
        if (id.startsWith("minecraft:") && id.endsWith("_spear")) {
            return BedrockHeldItemType.SPEAR;
        }
        if (id.equals("minecraft:fishing_rod")) {
            return BedrockHeldItemType.FISHING_ROD;
        }
        NbtMap data = item.getDefinition().getComponentData();
        if (data != null) {
            NbtMap components = data.getCompound("components");
            NbtMap properties = components.getCompound("item_properties");
            if (properties.containsKey("hand_equipped")) {
                return properties.getBoolean("hand_equipped") ? BedrockHeldItemType.HANDHELD : BedrockHeldItemType.SPRITE;
            }
            if (components.containsKey("minecraft:hand_equipped")) {
                return components.getCompound("minecraft:hand_equipped").getBoolean("value") ? BedrockHeldItemType.HANDHELD : BedrockHeldItemType.SPRITE;
            }
        }
        // Geyser 的原版组件表不包含所有旧物品；这些原版工具由客户端内置的 hand_equipped 分支渲染。
        return id.endsWith("_sword") || id.endsWith("_axe") || id.endsWith("_pickaxe")
                || id.endsWith("_shovel") || id.endsWith("_hoe")
                ? BedrockHeldItemType.HANDHELD : BedrockHeldItemType.SPRITE;
    }
}
