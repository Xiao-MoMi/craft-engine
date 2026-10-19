package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.pack.model.definition.BaseItemModel;
import net.momirealms.craftengine.core.pack.model.definition.ItemModel;
import net.momirealms.craftengine.core.pack.model.definition.tint.ConstantTint;
import net.momirealms.craftengine.core.pack.model.definition.tint.Tint;
import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class BedrockItemRendering {

    private BedrockItemRendering() {
    }

    public static BaseItemModel baseModel(ItemModel model) throws IOException {
        if (!(model instanceof BaseItemModel base)) {
            throw new IOException("Expected a static item model");
        }
        if (base.tints() != null) {
            for (Tint tint : base.tints()) {
                if (!(tint instanceof ConstantTint)) {
                    throw new IOException("Unsupported dynamic item tint: " + tint.get().get("type").getAsString());
                }
            }
        }
        return base;
    }

    public static List<Integer> tints(BaseItemModel model) throws IOException {
        baseModel(model);
        if (model.tints() == null || model.tints().isEmpty()) {
            return List.of();
        }
        List<Integer> colors = new ArrayList<>(model.tints().size());
        for (Tint tint : model.tints()) {
            ConstantTint constant = (ConstantTint) tint;
            colors.add(constant.value() == null ? 0xFFFFFF : constant.value().map(
                    value -> value & 0xFFFFFF,
                    value -> ((int) (value.get(0) * 255) << 16) | ((int) (value.get(1) * 255) << 8) | (int) (value.get(2) * 255)));
        }
        return colors;
    }

    public static boolean displayHandheld(BedrockItemSource item) {
        if (!item.javaItem().namespace().equals("minecraft")) {
            return false;
        }
        return switch (item.javaItem().value()) {
            case "wooden_axe", "stone_axe", "copper_axe", "iron_axe", "golden_axe", "diamond_axe", "netherite_axe",
                 "wooden_pickaxe", "stone_pickaxe", "copper_pickaxe", "iron_pickaxe", "golden_pickaxe", "diamond_pickaxe", "netherite_pickaxe",
                 "wooden_shovel", "stone_shovel", "copper_shovel", "iron_shovel", "golden_shovel", "diamond_shovel", "netherite_shovel",
                 "wooden_hoe", "stone_hoe", "copper_hoe", "iron_hoe", "golden_hoe", "diamond_hoe", "netherite_hoe",
                 "wooden_sword", "stone_sword", "copper_sword", "iron_sword", "golden_sword", "diamond_sword", "netherite_sword",
                 "mace", "fishing_rod" -> true;
            default -> false;
        };
    }

    public static boolean isSpear(Key material) {
        if (!material.namespace().equals("minecraft")) {
            return false;
        }
        return switch (material.value()) {
            case "wooden_spear", "stone_spear", "copper_spear", "iron_spear", "golden_spear", "diamond_spear", "netherite_spear" -> true;
            default -> false;
        };
    }
}
