package net.momirealms.craftengine.bukkit.item.recipe;

import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.item.recipe.Ingredient;
import net.momirealms.craftengine.core.item.recipe.UniqueIdItem;
import net.momirealms.craftengine.core.item.recipe.input.BrewingInput;
import net.momirealms.craftengine.core.item.recipe.input.CraftingInput;
import net.momirealms.craftengine.core.item.recipe.input.SingleItemInput;
import net.momirealms.craftengine.core.item.recipe.input.SmithingInput;
import net.momirealms.craftengine.core.item.recipe.remainder.CraftRemainder;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.NonNullListProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.ContainerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.inventory.CraftingContainerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackTemplateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.crafting.CraftingInputProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.crafting.IngredientProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.crafting.RecipeInputProxy;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

final class RecipeConversions {
    private RecipeConversions() {
    }

    static Object ingredient(Ingredient ingredient) {
        if (ingredient == null) {
            return emptyIngredient();
        }
        Stream<Object> items = ingredient.minecraftItems().stream()
                .map(key -> RegistryUtils.getRegistryValue(BuiltInRegistriesProxy.ITEM, KeyUtils.toIdentifier(key.key())));
        return IngredientProxy.INSTANCE.of(VersionHelper.isOrAbove1_21_2 ? items : items.map(item -> ItemStackProxy.INSTANCE.newInstance(item, 1)));
    }

    static Object visualIngredient(Ingredient ingredient) {
        if (ingredient == null) {
            return emptyIngredient();
        }
        List<Object> stacks = BukkitRecipeManager.getIngredientLooks(ingredient);
        if (VersionHelper.isOrAbove1_21_2) {
            Object visual = IngredientProxy.INSTANCE.ofStacks(stacks);
            return visual != null ? visual : ingredient(ingredient);
        }

        Object visual = IngredientProxy.INSTANCE.of(stacks.stream());
        Object[] exact = stacks.toArray(size -> (Object[]) Array.newInstance(ItemStackProxy.CLASS, size));
        IngredientProxy.INSTANCE.setExact(visual, true);
        IngredientProxy.INSTANCE.setItemStacks(visual, exact);
        return visual;
    }

    static Object emptyIngredient() {
        return VersionHelper.isOrAbove1_21_2 ? IngredientProxy.INSTANCE.of(Stream.empty()) : IngredientProxy.INSTANCE.empty();
    }

    static CraftingInput craftingInput(Object input) {
        int width;
        int height;
        List<Object> stacks;
        if (VersionHelper.isOrAbove1_21) {
            width = CraftingInputProxy.INSTANCE.width(input);
            height = CraftingInputProxy.INSTANCE.height(input);
            stacks = CraftingInputProxy.INSTANCE.items(input);
        } else {
            width = CraftingContainerProxy.INSTANCE.getWidth(input);
            height = CraftingContainerProxy.INSTANCE.getHeight(input);
            stacks = CraftingContainerProxy.INSTANCE.getItems(input);
        }
        List<UniqueIdItem> items = new ArrayList<>(stacks.size());
        for (Object stack : stacks) {
            items.add(wrap(stack));
        }
        return CraftingInput.of(width, height, items);
    }

    static SingleItemInput singleInput(Object input) {
        return new SingleItemInput(wrap(item(input, 0)));
    }

    static SmithingInput smithingInput(Object input) {
        return new SmithingInput(wrap(item(input, 1)), wrap(item(input, 0)), wrap(item(input, 2)));
    }

    static BrewingInput brewingInput(Object input) {
        return new BrewingInput(wrap(item(input, 0)), wrap(item(input, 1)));
    }

    static Object item(Object input, int slot) {
        return VersionHelper.isOrAbove1_21 ? RecipeInputProxy.INSTANCE.getItem(input, slot) : ContainerProxy.INSTANCE.getItem(input, slot);
    }

    static UniqueIdItem wrap(Object stack) {
        return UniqueIdItem.of(BukkitItemManager.instance().wrap(stack));
    }

    @SuppressWarnings("unchecked")
    static List<Object> nonNullList(int size, Object value) {
        return (List<Object>) NonNullListProxy.INSTANCE.withSize(size, value);
    }

    static Object remainingItems(Key recipeId, Object input) {
        int size = VersionHelper.isOrAbove1_21 ? CraftingInputProxy.INSTANCE.size(input) : ContainerProxy.INSTANCE.getContainerSize(input);
        List<Object> items = nonNullList(size, ItemStackProxy.EMPTY);
        for (int slot = 0; slot < size; slot++) {
            items.set(slot, remainder(recipeId, item(input, slot)));
        }
        return items;
    }

    private static Object remainder(Key recipeId, Object stack) {
        if (ItemStackProxy.INSTANCE.isEmpty(stack)) {
            return ItemStackProxy.EMPTY;
        }
        Item item = BukkitItemManager.instance().wrap(stack);
        ItemDefinition definition = item.getDefinition().orElse(null);
        if (definition != null) {
            CraftRemainder remainder = definition.settings().craftRemainder();
            if (remainder != null) {
                Item remaining = remainder.remainder(recipeId, item);
                return remaining == null ? ItemStackProxy.EMPTY : remaining.minecraftItem();
            }
        }
        Object nativeItem = ItemStackProxy.INSTANCE.getItem(stack);
        if (VersionHelper.isOrAbove26_1) {
            Object template = ItemProxy.INSTANCE.getCraftingRemainderTemplate(nativeItem);
            return template == null ? ItemStackProxy.EMPTY : ItemStackTemplateProxy.INSTANCE.create(template);
        }
        if (VersionHelper.isOrAbove1_21_2) {
            return ItemProxy.INSTANCE.getCraftingRemainder(nativeItem);
        }
        Object remaining = ItemProxy.INSTANCE.getCraftingRemainingItem(nativeItem);
        return remaining == null ? ItemStackProxy.EMPTY : ItemStackProxy.INSTANCE.newInstance(remaining, 1);
    }
}
