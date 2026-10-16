package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.util.Key;

public final class BedrockItemRendering {

    private BedrockItemRendering() {
    }

    public static boolean displayHandheld(Key itemModel, BedrockItemSources sources) {
        BedrockItemSource item = sources.findItemByModel(itemModel);
        if (item == null || !item.javaItem().namespace().equals("minecraft")) {
            return false;
        }
        return switch (item.javaItem().value()) {
            case "wooden_axe", "stone_axe", "copper_axe", "iron_axe", "golden_axe", "diamond_axe", "netherite_axe",
                 "wooden_pickaxe", "stone_pickaxe", "copper_pickaxe", "iron_pickaxe", "golden_pickaxe", "diamond_pickaxe", "netherite_pickaxe",
                 "wooden_shovel", "stone_shovel", "copper_shovel", "iron_shovel", "golden_shovel", "diamond_shovel", "netherite_shovel",
                 "wooden_hoe", "stone_hoe", "copper_hoe", "iron_hoe", "golden_hoe", "diamond_hoe", "netherite_hoe",
                 "wooden_sword", "stone_sword", "copper_sword", "iron_sword", "golden_sword", "diamond_sword", "netherite_sword",
                 "mace" -> true;
            default -> false;
        };
    }
}
