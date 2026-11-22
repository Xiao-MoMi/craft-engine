package net.momirealms.craftengine.bukkit.item.recipe;

import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.recipe.*;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.TypedInstanceProxy;
import net.momirealms.craftengine.proxy.minecraft.core.component.PotionsPredicateProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.RegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackTemplateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.crafting.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;

import static java.util.Objects.requireNonNull;
import static net.momirealms.craftengine.bukkit.item.recipe.RecipeGenerator.Kind.*;

final class NativeRecipeFactory {
    private static final Object[] COOKING_CATEGORIES = CookingBookCategoryProxy.CLASS.getEnumConstants();

    private NativeRecipeFactory() {
    }

    static Object shaped(CustomShapedRecipe recipe) {
        Class<?> nativeType = ShapedRecipeProxy.CLASS;
        Object result = resultStack(recipe);
        String[] rows = recipe.pattern().pattern();
        Map<Character, Object> visual = new HashMap<>();
        Map<Character, Object> rough = new HashMap<>();
        recipe.pattern().ingredients().forEach((symbol, ingredient) -> {
            visual.put(symbol, RecipeConversions.visualIngredient(ingredient));
            rough.put(symbol, RecipeConversions.ingredient(ingredient));
        });

        if (!VersionHelper.isOrAbove1_20_3) {
            int width = rows[0].length();
            List<Object> visualGrid = ingredientGrid(rows, width, visual);
            List<Object> roughGrid = ingredientGrid(rows, width, rough);
            Object category = category(recipe.category());
            Object companion = RecipeGenerator.vanilla(nativeType,
                    legacyId(recipe.id(), recipe.group(), category, width, rows.length, roughGrid, result, recipe.showNotification()));
            return RecipeGenerator.create(nativeType, SHAPED, recipe, (input, level) -> vanillaMatches(companion, input, level),
                    legacyId(recipe.id(), recipe.group(), category, width, rows.length, visualGrid, result, recipe.showNotification()));
        }

        Object visualPattern = ShapedRecipePatternProxy.INSTANCE.of(visual, rows);
        Object roughPattern = ShapedRecipePatternProxy.INSTANCE.of(rough, rows);
        BiPredicate<Object, Object> roughMatch = VersionHelper.isOrAbove1_21
                ? (input, level) -> ShapedRecipePatternProxy.INSTANCE.matches(roughPattern, input)
                : (input, level) -> ShapedRecipePatternProxy.INSTANCE.matches$legacy(roughPattern, input);
        if (VersionHelper.isOrAbove26_1) {
            return RecipeGenerator.create(nativeType, SHAPED, recipe, roughMatch,
                    commonInfo(recipe), craftingBookInfo(recipe), visualPattern, stackTemplate(result));
        }
        return RecipeGenerator.create(nativeType, SHAPED, recipe, roughMatch,
                recipe.group(), category(recipe.category()), visualPattern, result, recipe.showNotification());
    }

    static Object shapeless(CustomShapelessRecipe recipe) {
        Class<?> nativeType = ShapelessRecipeProxy.CLASS;
        List<Ingredient> ingredients = recipe.ingredientsInUse();
        List<Object> visual = ingredients.stream().map(RecipeConversions::visualIngredient).toList();
        List<Object> rough = ingredients.stream().map(RecipeConversions::ingredient).toList();
        Object result = resultStack(recipe);
        Object[] visualArguments;
        Object[] roughArguments;
        if (VersionHelper.isOrAbove26_1) {
            Object common = commonInfo(recipe);
            Object book = craftingBookInfo(recipe);
            Object template = stackTemplate(result);
            visualArguments = new Object[] {common, book, template, visual};
            roughArguments = new Object[] {common, book, template, rough};
        } else {
            Object category = category(recipe.category());
            visualArguments = legacyId(recipe.id(), recipe.group(), category, result, shapelessIngredients(visual));
            roughArguments = legacyId(recipe.id(), recipe.group(), category, result, shapelessIngredients(rough));
        }
        Object companion = RecipeGenerator.vanilla(nativeType, roughArguments);
        return RecipeGenerator.create(nativeType, SHAPELESS, recipe, (input, level) -> vanillaMatches(companion, input, level), visualArguments);
    }

    static Object cooking(Class<?> nativeType, CustomCookingRecipe recipe) {
        Object visual = RecipeConversions.visualIngredient(recipe.ingredient());
        Object rough = RecipeConversions.ingredient(recipe.ingredient());
        Object result = recipe.result().buildItem(ItemBuildContext.empty()).minecraftItem();
        Object category = COOKING_CATEGORIES[recipe.category().ordinal()];
        BiPredicate<Object, Object> match = (input, level) -> IngredientProxy.INSTANCE.test(rough, RecipeConversions.item(input, 0));
        if (VersionHelper.isOrAbove26_1) {
            Object book = RecipeGenerator.vanilla(CookingBookInfoProxy.CLASS, category, recipe.group());
            return RecipeGenerator.create(nativeType, COOKING, recipe, match,
                    commonInfo(recipe), book, visual, stackTemplate(result), recipe.experience(), recipe.cookingTime());
        }
        return RecipeGenerator.create(nativeType, COOKING, recipe, match,
                legacyId(recipe.id(), recipe.group(), category, visual, result, recipe.experience(), recipe.cookingTime()));
    }

    static Object stonecutting(CustomStoneCuttingRecipe recipe) {
        Object ingredient = RecipeConversions.ingredient(recipe.ingredient());
        Object result = recipe.result().buildItem(ItemBuildContext.empty()).minecraftItem();
        Object[] arguments = VersionHelper.isOrAbove26_1
                ? new Object[] {commonInfo(recipe), ingredient, stackTemplate(result)}
                : legacyId(recipe.id(), recipe.group(), ingredient, result);
        return RecipeGenerator.create(StonecutterRecipeProxy.CLASS, STONECUTTING, recipe, null, arguments);
    }

    static Object smithingTransform(CustomSmithingTransformRecipe recipe) {
        Object base = RecipeConversions.ingredient(recipe.base());
        Object result = recipe.buildVisualOrActualResult(ItemBuildContext.empty()).minecraftItem();
        Object[] arguments;
        if (VersionHelper.isOrAbove26_1) {
            arguments = new Object[] {commonInfo(recipe), optionalIngredient(recipe.template()), base, optionalIngredient(recipe.addition()), smithingResult(result)};
        } else if (VersionHelper.isOrAbove1_21_5) {
            arguments = new Object[] {optionalIngredient(recipe.template()), base, optionalIngredient(recipe.addition()), smithingResult(result)};
        } else if (VersionHelper.isOrAbove1_21_2) {
            Optional<Object> optionalBase = VersionHelper.isOrAbove1_21_4 && recipe.base() == null ? Optional.empty() : Optional.of(base);
            arguments = new Object[] {optionalIngredient(recipe.template()), optionalBase, optionalIngredient(recipe.addition()), result};
        } else {
            arguments = legacyId(recipe.id(), RecipeConversions.ingredient(recipe.template()), base, RecipeConversions.ingredient(recipe.addition()), result);
        }
        return RecipeGenerator.create(SmithingTransformRecipeProxy.CLASS, SMITHING, recipe, null, arguments);
    }

    static Object smithingTrim(CustomSmithingTrimRecipe recipe) {
        Object template = RecipeConversions.ingredient(recipe.template());
        Object base = RecipeConversions.ingredient(recipe.base());
        Object addition = RecipeConversions.ingredient(recipe.addition());
        Object[] arguments;
        if (VersionHelper.isOrAbove1_21_5) {
            Key key = requireNonNull(recipe.pattern(), "pattern should not be null");
            Object pattern = requireNonNull(
                    RegistryUtils.getHolderById(RegistryUtils.lookupOrThrow(RegistriesProxy.TRIM_PATTERN), KeyUtils.toIdentifier(key)),
                    "Pattern " + key + " doesn't exist.");
            arguments = VersionHelper.isOrAbove26_1
                    ? new Object[] {commonInfo(recipe), template, base, addition, pattern}
                    : new Object[] {template, base, addition, pattern};
        } else if (VersionHelper.isOrAbove1_21_2) {
            arguments = new Object[] {optionalIngredient(recipe.template()), optionalIngredient(recipe.base()), optionalIngredient(recipe.addition())};
        } else {
            arguments = legacyId(recipe.id(), template, base, addition);
        }
        return RecipeGenerator.create(SmithingTrimRecipeProxy.CLASS, SMITHING, recipe, null, arguments);
    }

    static Object dye(CustomDyeRecipe recipe) {
        if (!VersionHelper.isOrAbove26_1) {
            throw new UnsupportedOperationException("Native dye recipes require Minecraft 26.1 or newer");
        }
        Object target = RecipeConversions.ingredient(recipe.target());
        Object dye = RecipeConversions.ingredient(recipe.dye());
        return RecipeGenerator.create(DyeRecipeProxy.CLASS, DYE, recipe, (input, level) -> matchesDye(input, target, dye),
                commonInfo(recipe), craftingBookInfo(recipe),
                RecipeConversions.visualIngredient(recipe.target()), RecipeConversions.visualIngredient(recipe.dye()), stackTemplate(resultStack(recipe)));
    }

    static Object brewing(CustomBrewingRecipe recipe) {
        return RecipeGenerator.create(BrewingRecipeProxy.CLASS, BREWING, recipe, null,
                potionIngredient(recipe.container()), potionIngredient(recipe.ingredient()),
                stackTemplate(recipe.result(ItemBuildContext.empty()).minecraftItem()));
    }

    private static Object potionIngredient(Ingredient ingredient) {
        Optional<Object> predicate = ingredient.predicate() instanceof BrewingPotionPredicate potion
                ? Optional.of(potion.predicate()) : Optional.empty();
        return PotionsPredicateProxy.PotionIngredient.INSTANCE.newInstance(RecipeConversions.ingredient(ingredient), predicate);
    }

    private static boolean matchesDye(Object input, Object target, Object dye) {
        CraftingInputProxy access = CraftingInputProxy.INSTANCE;
        if (access.ingredientCount(input) < 2) {
            return false;
        }
        boolean hasTarget = false;
        boolean hasDye = false;
        int size = access.size(input);
        for (int slot = 0; slot < size; slot++) {
            Object stack = access.getItem(input, slot);
            if (ItemStackProxy.INSTANCE.isEmpty(stack)) {
                continue;
            }
            if (IngredientProxy.INSTANCE.test(target, stack)) {
                if (hasTarget) {
                    return false;
                }
                hasTarget = true;
            } else if (IngredientProxy.INSTANCE.test(dye, stack)) {
                hasDye = true;
            } else {
                return false;
            }
        }
        return hasTarget && hasDye;
    }

    private static List<Object> ingredientGrid(String[] rows, int width, Map<Character, Object> entries) {
        Object empty = RecipeConversions.emptyIngredient();
        List<Object> grid = RecipeConversions.nonNullList(rows.length * width, empty);
        for (int y = 0; y < rows.length; y++) {
            for (int x = 0; x < rows[y].length(); x++) {
                grid.set(y * width + x, entries.getOrDefault(rows[y].charAt(x), empty));
            }
        }
        return grid;
    }

    private static List<Object> shapelessIngredients(List<Object> ingredients) {
        if (VersionHelper.isOrAbove1_21_2) {
            return ingredients;
        }
        List<Object> list = RecipeConversions.nonNullList(ingredients.size(), RecipeConversions.emptyIngredient());
        for (int slot = 0; slot < list.size(); slot++) {
            list.set(slot, ingredients.get(slot));
        }
        return list;
    }

    private static boolean vanillaMatches(Object recipe, Object input, Object level) {
        return VersionHelper.isOrAbove1_21
                ? RecipeProxy.INSTANCE.matches(recipe, input, level)
                : RecipeProxy.INSTANCE.matches$legacy(recipe, input, level);
    }

    private static Object category(CraftingRecipeCategory category) {
        return CraftingBookCategoryProxy.VALUES[category.ordinal()];
    }

    private static Object commonInfo(Recipe recipe) {
        return RecipeGenerator.vanilla(RecipeCommonInfoProxy.CLASS, recipe.showNotification());
    }

    private static Object craftingBookInfo(CustomCraftingTableRecipe recipe) {
        return RecipeGenerator.vanilla(CraftingBookInfoProxy.CLASS, category(recipe.category()), recipe.group());
    }

    private static Object resultStack(CustomCraftingTableRecipe recipe) {
        return recipe.buildVisualOrActualResult(ItemBuildContext.empty()).minecraftItem();
    }

    private static Object stackTemplate(Object result) {
        return ItemStackTemplateProxy.INSTANCE.fromNonEmptyStack(result);
    }

    private static Object smithingResult(Object result) {
        ItemStackProxy stacks = ItemStackProxy.INSTANCE;
        int count = stacks.getCount(result);
        Object components = stacks.getComponentsPatch(result);
        if (VersionHelper.isOrAbove26_1) {
            return ItemStackTemplateProxy.INSTANCE.newInstance(TypedInstanceProxy.INSTANCE.typeHolder(result), count, components);
        }
        Object itemHolder = ItemProxy.INSTANCE.getBuiltInRegistryHolder(stacks.getItem(result));
        return RecipeGenerator.vanilla(TransmuteResultProxy.CLASS, itemHolder, count, components);
    }

    private static Optional<Object> optionalIngredient(Ingredient ingredient) {
        return Optional.ofNullable(ingredient).map(RecipeConversions::ingredient);
    }

    private static Object[] legacyId(Key id, Object... arguments) {
        if (VersionHelper.isOrAbove1_20_2) {
            return arguments;
        }
        Object[] result = new Object[arguments.length + 1];
        result[0] = KeyUtils.toIdentifier(id);
        System.arraycopy(arguments, 0, result, 1, arguments.length);
        return result;
    }
}
