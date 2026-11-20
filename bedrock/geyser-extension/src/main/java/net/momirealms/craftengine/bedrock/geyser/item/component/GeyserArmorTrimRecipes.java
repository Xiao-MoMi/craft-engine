package net.momirealms.craftengine.bedrock.geyser.item.component;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.SmithingTrimRecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.DefaultDescriptor;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount;
import org.cloudburstmc.protocol.bedrock.packet.CraftingDataPacket;

import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;

final class GeyserArmorTrimRecipes {
    static final String BASE_TAG_PREFIX = "ce:trim_base/";

    private GeyserArmorTrimRecipes() {
    }

    static String baseTag(String identifier) {
        return BASE_TAG_PREFIX + identifier.replace(':', '/');
    }

    static boolean supports(SmithingTrimRecipeData recipe, Map<String, Int2ObjectMap<ItemDefinition>> items) {
        return recipe.getBase().getDescriptor() instanceof DefaultDescriptor base && items.containsKey(baseTag(base.getItemId().getIdentifier()));
    }

    static CraftingDataPacket translate(List<SmithingTrimRecipeData> recipes, Map<String, Int2ObjectMap<ItemDefinition>> items, IntSupplier nextId) {
        CraftingDataPacket packet = new CraftingDataPacket();
        for (SmithingTrimRecipeData recipe : recipes) {
            if (!(recipe.getBase().getDescriptor() instanceof DefaultDescriptor base)) {
                continue;
            }
            Int2ObjectMap<ItemDefinition> variants = items.get(baseTag(base.getItemId().getIdentifier()));
            if (variants == null) {
                continue;
            }
            for (ItemDefinition variant : variants.values()) {
                ItemDescriptorWithCount ingredient = new ItemDescriptorWithCount(new DefaultDescriptor(variant, 0), recipe.getBase().getCount());
                int netId = nextId.getAsInt();
                packet.getSmithingTrimData().add(SmithingTrimRecipeData.of("ce:trim/" + netId, ingredient, recipe.getAddition(), recipe.getTemplate(), recipe.getTag(), netId));
            }
        }
        return packet;
    }
}
