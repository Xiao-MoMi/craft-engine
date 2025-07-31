package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.recipe.input.CraftingInput;
import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.util.Color;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.Tristate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomDyeRecipe<T> implements Recipe<T> {
    public static final Key ID = Key.of("armor_dye");
    private static final Key DYEABLE = Key.of("dyeable");

    public CustomDyeRecipe() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public T assemble(RecipeInput input, ItemBuildContext context) {
        List<Color> colors = new ArrayList<>();
        CraftingInput<T> craftingInput = (CraftingInput<T>) input;
        Item<T> itemToDye = null;
        for (UniqueIdItem<T> uniqueIdItem : craftingInput) {
            if (uniqueIdItem.isEmpty()) {
                continue;
            }
            if (isDyeable(uniqueIdItem)) {
                itemToDye = uniqueIdItem.item().copyWithCount(1);
            } else {
                Color dyeColor = getDyeColor(uniqueIdItem);
                if (dyeColor != null) {
                    colors.add(dyeColor);
                } else {
                    return null;
                }
            }
        }
        if (itemToDye == null || itemToDye.isEmpty() || colors.isEmpty()) {
            return null;
        }
        return applyDyes(itemToDye, colors).getItem();
    }

    private Item<T> applyDyes(Item<T> item, List<Color> colors) {
        int totalRed = 0;
        int totalGreen = 0;
        int totalBlue = 0;
        int totalMaxComponent = 0;
        int colorCount = 0;
        Optional<Color> existingColor = item.dyedColor();
        existingColor.ifPresent(colors::add);
        for (Color color : colors) {
            int dyeRed = color.r();
            int dyeGreen = color.g();
            int dyeBlue = color.b();
            totalMaxComponent += Math.max(dyeRed, Math.max(dyeGreen, dyeBlue));
            totalRed += dyeRed;
            totalGreen += dyeGreen;
            totalBlue += dyeBlue;
            ++colorCount;
        }
        int avgRed = totalRed / colorCount;
        int avgGreen = totalGreen / colorCount;
        int avgBlue = totalBlue / colorCount;
        float avgMaxComponent = (float) totalMaxComponent / (float)colorCount;
        float currentMaxComponent = (float) Math.max(avgRed, Math.max(avgGreen, avgBlue));
        avgRed = (int) ((float) avgRed * avgMaxComponent / currentMaxComponent);
        avgGreen = (int) ((float) avgGreen * avgMaxComponent / currentMaxComponent);
        avgBlue = (int) ((float) avgBlue * avgMaxComponent / currentMaxComponent);
        Color finalColor = new Color(0, avgRed, avgGreen, avgBlue);
        return item.dyedColor(finalColor);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean matches(RecipeInput input) {
        CraftingInput<T> craftingInput = (CraftingInput<T>) input;
        if (craftingInput.ingredientCount() < 2) {
            return false;
        }
        boolean hasItemToDye = false;
        boolean hasDye = false;
        for(int i = 0; i < craftingInput.size(); ++i) {
            UniqueIdItem<T> item = craftingInput.getItem(i);
            if (!item.isEmpty()) {
                if (isDyeable(item)) {
                    if (hasItemToDye) {
                        return false;
                    }
                    hasItemToDye = true;
                } else {
                    if (!isDye(item)) {
                        return false;
                    }
                    hasDye = true;
                }
            }
        }
        return hasDye && hasItemToDye;
    }

    private boolean isDyeable(final UniqueIdItem<T> item) {
        Optional<CustomItem<T>> optionalCustomItem = item.item().getCustomItem();
        if (optionalCustomItem.isPresent()) {
            CustomItem<T> customItem = optionalCustomItem.get();
            if (customItem.settings().dyeable() == Tristate.FALSE) {
                return false;
            }
            if (customItem.settings().dyeable() == Tristate.TRUE) {
                return true;
            }
        }
        return item.item().is(DYEABLE);
    }

    private boolean isDye(final UniqueIdItem<T> item) {
        Item<T> dyeItem = item.item();
        Optional<CustomItem<T>> optionalCustomItem = item.item().getCustomItem();
        if (optionalCustomItem.isPresent()) {
            CustomItem<T> customItem = optionalCustomItem.get();
            return customItem.settings().dyeColor() != null || dyeItem.isDyeItem();
        }
        return dyeItem.isDyeItem();
    }

    @Nullable
    private Color getDyeColor(final UniqueIdItem<T> item) {
        Item<T> dyeItem = item.item();
        Optional<CustomItem<T>> optionalCustomItem = item.item().getCustomItem();
        if (optionalCustomItem.isPresent()) {
            CustomItem<T> customItem = optionalCustomItem.get();
            return Optional.ofNullable(customItem.settings().dyeColor()).orElseGet(() -> dyeItem.dyeColor().orElse(null));
        }
        return dyeItem.dyeColor().orElse(null);
    }

    @Override
    public List<Ingredient<T>> ingredientsInUse() {
        return List.of();
    }

    @Override
    public @NotNull Key type() {
        return RecipeTypes.SPECIAL;
    }

    @Override
    public Key id() {
        return ID;
    }
}
