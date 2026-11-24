package net.momirealms.craftengine.bedrock.geyser.item.component;

import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.ShapedRecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.ShapelessRecipeData;
import org.cloudburstmc.protocol.bedrock.packet.CraftingDataPacket;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

final class GeyserRecipeLore {
    private final Map<String, GeyserItemText> itemText;
    private final String locale;
    private final Map<GeyserItemText, List<String>> localized = new IdentityHashMap<>();

    private GeyserRecipeLore(Map<String, GeyserItemText> itemText, String locale) {
        this.itemText = itemText;
        this.locale = locale;
    }

    static void patch(CraftingDataPacket packet, Map<String, GeyserItemText> itemText, String locale) {
        if (packet.getShapedData().isEmpty() && packet.getShapelessData().isEmpty()) {
            return;
        }
        GeyserRecipeLore patcher = new GeyserRecipeLore(itemText, locale);
        packet.getShapedData().replaceAll(patcher::patchShaped);
        packet.getShapelessData().replaceAll(patcher::patchShapeless);
    }

    private ShapedRecipeData patchShaped(ShapedRecipeData recipe) {
        List<ItemData> results = this.patchResults(recipe.getResults());
        if (results == recipe.getResults()) {
            return recipe;
        }
        return ShapedRecipeData.of(recipe.getType(), recipe.getId(), recipe.getWidth(), recipe.getHeight(), recipe.getIngredients(), results,
                recipe.getUuid(), recipe.getTag(), recipe.getPriority(), recipe.getNetId(), recipe.isAssumeSymetry(), recipe.getRequirement());
    }

    private ShapelessRecipeData patchShapeless(ShapelessRecipeData recipe) {
        if (!"crafting_table".equals(recipe.getTag())) {
            return recipe;
        }
        List<ItemData> results = this.patchResults(recipe.getResults());
        if (results == recipe.getResults()) {
            return recipe;
        }
        return ShapelessRecipeData.of(recipe.getType(), recipe.getId(), recipe.getIngredients(), results,
                recipe.getUuid(), recipe.getTag(), recipe.getPriority(), recipe.getNetId(), recipe.getRequirement());
    }

    private List<ItemData> patchResults(List<ItemData> results) {
        List<ItemData> updated = results;
        for (int i = 0; i < results.size(); i++) {
            ItemData item = results.get(i);
            GeyserItemText text = this.itemText.get(item.getDefinition().getIdentifier());
            if (text == null || !text.hasLore()) {
                continue;
            }
            NbtMap tag = item.getTag() == null ? NbtMap.EMPTY : item.getTag();
            NbtMap display = tag.getCompound("display");
            if (display.containsKey("Lore")) {
                continue;
            }
            List<String> lore = this.localized.computeIfAbsent(text, value -> value.lore(this.locale));
            NbtMap displayWithLore = display.toBuilder().putList("Lore", NbtType.STRING, lore).build();
            if (updated == results) {
                updated = new ArrayList<>(results);
            }
            NbtMap updatedTag = tag.toBuilder().putCompound("display", displayWithLore).build();
            updated.set(i, item.toBuilder().tag(updatedTag).build());
        }
        return updated;
    }
}
