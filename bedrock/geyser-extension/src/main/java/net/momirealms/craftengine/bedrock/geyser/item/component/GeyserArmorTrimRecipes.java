package net.momirealms.craftengine.bedrock.geyser.item.component;

import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.SmithingTrimRecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.DefaultDescriptor;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemTagDescriptor;
import org.cloudburstmc.protocol.bedrock.packet.CraftingDataPacket;

import java.util.List;
import java.util.Set;
import java.util.function.IntSupplier;

final class GeyserArmorTrimRecipes {
    static final String BASE_TAG_PREFIX = "ce:trim_base/";

    private GeyserArmorTrimRecipes() {
    }

    static String baseTag(String identifier) {
        return BASE_TAG_PREFIX + identifier.replace(':', '/');
    }

    static boolean supports(SmithingTrimRecipeData recipe, Set<String> bases) {
        return recipe.getBase().getDescriptor() instanceof DefaultDescriptor base
                && bases.contains(baseTag(base.getItemId().getIdentifier()));
    }

    static CraftingDataPacket translate(List<SmithingTrimRecipeData> recipes, Set<String> bases, IntSupplier nextId) {
        CraftingDataPacket packet = new CraftingDataPacket();
        for (SmithingTrimRecipeData recipe : recipes) {
            if (!supports(recipe, bases)) {
                continue;
            }
            DefaultDescriptor base = (DefaultDescriptor) recipe.getBase().getDescriptor();
            ItemDescriptorWithCount ingredient = new ItemDescriptorWithCount(
                    new ItemTagDescriptor(baseTag(base.getItemId().getIdentifier())), recipe.getBase().getCount());
            int netId = nextId.getAsInt();
            packet.getSmithingTrimData().add(SmithingTrimRecipeData.of("ce:trim/" + netId,
                    ingredient, recipe.getAddition(), recipe.getTemplate(), recipe.getTag(), netId));
        }
        return packet;
    }
}
