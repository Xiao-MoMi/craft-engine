package net.momirealms.craftengine.core.item.recipe;

import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.recipe.input.CraftingInput;
import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.item.recipe.result.CustomRecipeResult;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.CommonFunctions;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Predicate;

public final class CustomShapedRecipe extends CustomCraftingTableRecipe {
    public static final Serializer SERIALIZER = new Serializer();
    private final ParsedPattern parsedPattern;
    private final Pattern pattern;
    private final boolean ingredientCountSupport;

    public CustomShapedRecipe(Key id,
                              boolean showNotification,
                              CustomRecipeResult result,
                              CustomRecipeResult visualResult,
                              String group,
                              CraftingRecipeCategory category,
                              Pattern pattern,
                              Function<Context>[] craftingFunctions,
                              Predicate<Context> craftingCondition,
                              boolean alwaysRebuildOutput,
                              boolean ingredientCountSupport) {
        super(id, showNotification, result, visualResult, group, category, craftingFunctions, craftingCondition, alwaysRebuildOutput);
        this.pattern = pattern;
        this.parsedPattern = pattern.parse();
        this.ingredientCountSupport = ingredientCountSupport;
    }

    public ParsedPattern parsedPattern() {
        return this.parsedPattern;
    }

    @Override
    public boolean matches(RecipeInput input) {
        return this.parsedPattern.matches((CraftingInput) input);
    }

    @Override
    public void takeInput(@NotNull RecipeInput input, int ignore) {
        this.parsedPattern.matchesAndTake((CraftingInput) input, ignore);
    }

    public boolean ingredientCountSupport() {
        return this.ingredientCountSupport;
    }

    @Override
    public List<Ingredient> ingredientsInUse() {
        return new ArrayList<>(this.pattern.ingredients().values());
    }

    @Override
    public @NotNull Key serializerType() {
        return RecipeSerializers.SHAPED;
    }

    public Pattern pattern() {
        return this.pattern;
    }

    public record Pattern(String[] pattern, Map<Character, Ingredient> ingredients) {

        public ParsedPattern parse() {
                String[] shrunk = shrink(this.pattern);
                return new ParsedPattern(shrunk[0].length(), shrunk.length,
                        toIngredientArray(shrunk, this.ingredients));
        }
    }

    public static class ParsedPattern {
        private final int width;
        private final int height;
        private final Optional<Ingredient>[] ingredients;
        private final Optional<Ingredient>[] mirroredIngredients;
        private final int ingredientCount;
        private final boolean symmetrical;

        @SuppressWarnings("unchecked")
        public ParsedPattern(int width, int height, Optional<Ingredient>[] ingredients) {
            this.height = height;
            this.width = width;
            this.symmetrical = isSymmetrical(width, height, ingredients);
            this.ingredients = ingredients;
            if (this.symmetrical) {
                this.mirroredIngredients = ingredients;
            } else {
                this.mirroredIngredients = new Optional[ingredients.length];
                for (int i = 0; i < this.height; i++) {
                    for (int j = 0; j < this.width; j++) {
                        Optional<Ingredient> ingredient = this.ingredients[j + i * this.width];
                        this.mirroredIngredients[this.width - j - 1 + i * this.width] = ingredient;
                    }
                }
            }
            int count = 0;
            for (Optional<Ingredient> ingredient : this.ingredients) {
                if (ingredient.isPresent()) {
                    count++;
                }
            }
            this.ingredientCount = count;
        }

        public Optional<Ingredient>[] ingredients() {
            return ingredients;
        }

        public int width() {
            return width;
        }

        public int height() {
            return height;
        }

        public boolean matches(CraftingInput input) {
            if (input.ingredientCount == this.ingredientCount) {
                if (input.width == this.width && input.height == this.height) {
                    if (!this.symmetrical && this.matches(input, true)) {
                        return true;
                    }
                    return this.matches(input, false);
                }
            }
            return false;
        }

        private boolean matches(CraftingInput input, boolean mirrored) {
            Optional<Ingredient>[] ingredients = mirrored ? this.mirroredIngredients : this.ingredients;
            for (int i = 0, size = input.size(); i < size; i++) {
                Optional<Ingredient> optional = ingredients[i];
                UniqueIdItem itemStack = input.getItem(i);
                if (!Ingredient.isInstance(optional, itemStack)) {
                    return false;
                }
            }
            return true;
        }

        private void takeIngredients(CraftingInput input, boolean mirrored, int left) {
            Optional<Ingredient>[] ingredients = mirrored ? this.mirroredIngredients : this.ingredients;
            for (int i = 0, size = input.size(); i < size; i++) {
                Optional<Ingredient> optional = ingredients[i];
                UniqueIdItem itemStack = input.getItem(i);
                if (optional.isPresent() && Ingredient.isInstance(optional, itemStack)) {
                    int toTake = optional.get().count() - left;
                    if (toTake > 0) {
                        itemStack.item().shrink(toTake);
                    }
                }
            }
        }

        public boolean matchesAndTake(CraftingInput input, int left) {
            if (input.ingredientCount == this.ingredientCount) {
                if (input.width == this.width && input.height == this.height) {
                    if (!this.symmetrical && this.matches(input, true)) {
                        takeIngredients(input, true, left);
                        return true;
                    }
                    if (this.matches(input, false)) {
                        takeIngredients(input, false, left);
                        return true;
                    }
                }
            }
            return false;
        }

        private static <T> boolean isSymmetrical(int width, int height, T[] list) {
            if (width != 1) {
                int i = width / 2;
                for (int j = 0; j < height; j++) {
                    for (int k = 0; k < i; k++) {
                        int l = width - 1 - k;
                        T o1 = list[k + j * width];
                        T o2 = list[l + j * width];
                        if (!o1.equals(o2)) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
    }

    public static class Serializer extends AbstractRecipeSerializer<CustomShapedRecipe> {

        @SuppressWarnings({"unchecked", "DuplicatedCode"})
        @Override
        public CustomShapedRecipe readConfig(Key id, ConfigSection section) {
            List<String> pattern = section.getNonEmptyList("pattern", ConfigValue::getAsString);
            if (!validatePattern(pattern)) {
                throw new KnownResourceException("resource.recipe.shaped.invalid_pattern", section.assemblePath("pattern"), pattern.toString());
            }
            ConfigSection ingredientSection = section.getNonNullSection(INGREDIENTS);
            Map<Character, Ingredient> ingredients = new HashMap<>();
            boolean hasAdditionalIngredients = false;
            for (String ingredientChar : ingredientSection.keySet()) {
                if (ingredientChar.length() != 1 || ingredientChar.equals(" ")) {
                    throw new KnownResourceException("resource.recipe.shaped.invalid_symbol", ingredientSection.path(), ingredientChar);
                }
                char ch = ingredientChar.charAt(0);
                Ingredient ingredient = ingredientSection.getNonNullValue(ingredientChar, ConfigConstants.ARGUMENT_LIST, super::parseIngredient);
                ingredients.put(ch, ingredient);
                if (ingredient.count() > 1) {
                    hasAdditionalIngredients = true;
                }
            }
            return new CustomShapedRecipe(
                    id,
                    section.getBoolean(SHOW_NOTIFICATIONS, true),
                    super.parseResult(section.getNonNullValue("result", ConfigConstants.ARGUMENT_SECTION)),
                    section.getValue(VISUAL_RESULT, super::parseResult),
                    section.getString("group"),
                    section.getEnum("category", CraftingRecipeCategory.class),
                    new Pattern(pattern.toArray(new String[0]), ingredients),
                    section.getList(FUNCTIONS, CommonFunctions::fromConfig).toArray(new Function[0]),
                    MiscUtils.allOf(section.getList(CONDITIONS, CommonConditions::fromConfig)),
                    section.getBoolean(ALWAYS_REBUILD_RESULT, true),
                    hasAdditionalIngredients
            );
        }

        @Override
        public CustomShapedRecipe readJson(Key id, JsonObject json) {
            Map<Character, Ingredient> ingredients = Maps.transformValues(VANILLA_RECIPE_HELPER.shapedIngredientMap(json.getAsJsonObject("key")), this::parseVanillaIngredient);
            return new CustomShapedRecipe(id,
                    true,
                    parseResult(VANILLA_RECIPE_HELPER.craftingResult(json.get("result"))),
                    null,
                    VANILLA_RECIPE_HELPER.readGroup(json),
                    VANILLA_RECIPE_HELPER.craftingCategory(json),
                    new Pattern(VANILLA_RECIPE_HELPER.craftingShapedPattern(json), ingredients),
                    null,
                    null,
                    false,
                    false
            );
        }

        private boolean validatePattern(List<String> pattern) {
            String first = pattern.getFirst();
            int length = first.length();
            for (String s : pattern) {
                if (s.length() != length) {
                    return false;
                }
                if (s.length() > 3) {
                    return false;
                }
            }
            return pattern.size() <= 3;
        }
    }

    @SuppressWarnings("unchecked")
    public static Optional<Ingredient>[] toIngredientArray(String[] pattern, Map<Character, Ingredient> ingredients) {
        List<Optional<Ingredient>> result = new ArrayList<>();
        String[] shrunkPattern = shrink(pattern);
        for (String pa : shrunkPattern) {
            for (int j = 0; j < pa.length(); j++) {
                char ch = pa.charAt(j);
                if (ch == ' ') {
                    result.add(Optional.empty());
                } else {
                    Optional<Ingredient> ingredient = Optional.ofNullable(ingredients.get(ch));
                    if (ingredient.isEmpty()) {
                        throw new IllegalArgumentException("Invalid ingredient: " + ch);
                    }
                    result.add(ingredient);
                }
            }
        }
        return result.toArray(new Optional[0]);
    }

    public static String[] shrink(String[] patterns) {
        int minStart = Integer.MAX_VALUE;
        int maxEnd = 0;
        int leadingEmptyPatterns = 0;
        int consecutiveEmptyPatterns = 0;
        for (int i = 0; i < patterns.length; i++) {
            String pattern = patterns[i];
            minStart = Math.min(minStart, firstNonSpace(pattern));
            int patternEnd = lastNonSpace(pattern);
            maxEnd = Math.max(maxEnd, patternEnd);
            if (patternEnd < 0) {
                if (leadingEmptyPatterns == i) {
                    leadingEmptyPatterns++;
                }
                consecutiveEmptyPatterns++;
            } else {
                consecutiveEmptyPatterns = 0;
            }
        }
        if (patterns.length == consecutiveEmptyPatterns) {
            return new String[0];
        } else {
            String[] result = new String[patterns.length - consecutiveEmptyPatterns - leadingEmptyPatterns];
            for (int j = 0; j < result.length; j++) {
                result[j] = patterns[j + leadingEmptyPatterns].substring(minStart, maxEnd + 1);
            }
            return result;
        }
    }

    private static int firstNonSpace(String line) {
        int index = 0;
        while (index < line.length() && line.charAt(index) == ' ') {
            index++;
        }
        return index;
    }

    private static int lastNonSpace(String line) {
        int index = line.length() - 1;
        while (index >= 0 && line.charAt(index) == ' ') {
            index--;
        }
        return index;
    }
}
