package net.momirealms.craftengine.core.item.recipe;

import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.recipe.input.CraftingInput;
import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.item.recipe.result.CustomRecipeResult;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CustomShapedRecipe<T> extends CustomCraftingTableRecipe<T> {
    public static final Serializer<?> SERIALIZER = new Serializer<CustomShapedRecipe<?>>();
    private final ParsedPattern<T> parsedPattern;
    private final Pattern<T> pattern;

    public CustomShapedRecipe(Key id,
                              boolean showNotification,
                              CustomRecipeResult<T> result,
                              CustomRecipeResult<T> visualResult,
                              String group,
                              CraftingRecipeCategory category,
                              Pattern<T> pattern,
                              Function<Context>[] craftingFunctions,
                              Condition<Context> craftingCondition,
                              boolean alwaysRebuildOutput) {
        super(id, showNotification, result, visualResult, group, category, craftingFunctions, craftingCondition, alwaysRebuildOutput);
        this.pattern = pattern;
        this.parsedPattern = pattern.parse();
    }

    public ParsedPattern<T> parsedPattern() {
        return parsedPattern;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean matches(RecipeInput input) {
        return this.parsedPattern.matches((CraftingInput<T>) input);
    }

    @Override
    public List<Ingredient<T>> ingredientsInUse() {
        return new ArrayList<>(this.pattern.ingredients().values());
    }

    @Override
    public @NotNull Key serializerType() {
        return RecipeSerializers.SHAPED;
    }

    public Pattern<T> pattern() {
        return pattern;
    }

    public record Pattern<T>(String[] pattern, Map<Character, Ingredient<T>> ingredients) {

        public ParsedPattern<T> parse() {
                String[] shrunk = shrink(pattern);
                return new ParsedPattern<>(shrunk[0].length(), shrunk.length,
                        toIngredientArray(shrunk, ingredients));
        }
    }

    public static class ParsedPattern<T> {
        private final int width;
        private final int height;
        private final Optional<Ingredient<T>>[] ingredients;
        private final Optional<Ingredient<T>>[] mirroredIngredients;
        private final int ingredientCount;
        private final boolean symmetrical;

        @SuppressWarnings("unchecked")
        public ParsedPattern(int width, int height, Optional<Ingredient<T>>[] ingredients) {
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
                        Optional<Ingredient<T>> ingredient = this.ingredients[j + i * this.width];
                        this.mirroredIngredients[this.width - j - 1 + i * this.width] = ingredient;
                    }
                }
            }
            int count = 0;
            for (Optional<Ingredient<T>> ingredient : this.ingredients) {
                if (ingredient.isPresent()) {
                    count++;
                }
            }
            this.ingredientCount = count;
        }

        public Optional<Ingredient<T>>[] ingredients() {
            return ingredients;
        }

        public int width() {
            return width;
        }

        public int height() {
            return height;
        }

        public boolean matches(CraftingInput<T> input) {
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

        private boolean matches(CraftingInput<T> input, boolean mirrored) {
            if (mirrored) {
                for (int i = 0, size = input.size(); i < size; i++) {
                    Optional<Ingredient<T>> optional = this.mirroredIngredients[i];
                    UniqueIdItem<T> itemStack = input.getItem(i);
                    if (!Ingredient.isInstance(optional, itemStack)) {
                        return false;
                    }
                }
            } else {
                for (int i = 0, size = input.size(); i < size; i++) {
                    Optional<Ingredient<T>> optional = this.ingredients[i];
                    UniqueIdItem<T> itemStack = input.getItem(i);
                    if (!Ingredient.isInstance(optional, itemStack)) {
                        return false;
                    }
                }
            }
            return true;
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

    public static class Serializer<A> extends AbstractRecipeSerializer<A, CustomShapedRecipe<A>> {

        @SuppressWarnings({"unchecked", "rawtypes", "DuplicatedCode"})
        @Override
        public CustomShapedRecipe<A> readMap(Key id, Map<String, Object> arguments) {
            List<String> pattern = MiscUtils.getAsStringList(arguments.get("pattern"));
            if (pattern.isEmpty()) {
                throw new LocalizedResourceConfigException("warning.config.recipe.shaped.missing_pattern");
            }
            if (!validatePattern(pattern)) {
                throw new LocalizedResourceConfigException("warning.config.recipe.shaped.invalid_pattern", pattern.toString());
            }
            Object ingredientObj = getIngredientOrThrow(arguments);
            Map<Character, Ingredient<A>> ingredients = new HashMap<>();
            for (Map.Entry<String, Object> entry : ResourceConfigUtils.getAsMap(ingredientObj, "ingredient").entrySet()) {
                String key = entry.getKey();
                if (key.length() != 1) {
                    throw new LocalizedResourceConfigException("warning.config.recipe.shaped.invalid_symbol", key);
                }
                char ch = key.charAt(0);
                ingredients.put(ch, parseIngredient(entry.getValue()));
            }
            return new CustomShapedRecipe(id,
                    showNotification(arguments),
                    parseResult(arguments),
                    parseVisualResult(arguments),
                    arguments.containsKey("group") ? arguments.get("group").toString() : null, craftingRecipeCategory(arguments),
                    new Pattern<>(pattern.toArray(new String[0]), ingredients),
                    functions(arguments),
                    conditions(arguments),
                    ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("always-rebuild-result", true), "always-rebuild-result")
            );
        }

        @Override
        public CustomShapedRecipe<A> readJson(Key id, JsonObject json) {
            Map<Character, Ingredient<A>> ingredients = Maps.transformValues(VANILLA_RECIPE_HELPER.shapedIngredientMap(json.getAsJsonObject("key")), this::toIngredient);
            return new CustomShapedRecipe<>(id,
                    true,
                    parseResult(VANILLA_RECIPE_HELPER.craftingResult(json.get("result"))),
                    null,
                    VANILLA_RECIPE_HELPER.readGroup(json),
                    VANILLA_RECIPE_HELPER.craftingCategory(json),
                    new Pattern<>(VANILLA_RECIPE_HELPER.craftingShapedPattern(json), ingredients),
                    null,
                    null,
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
    public static <T> Optional<Ingredient<T>>[] toIngredientArray(String[] pattern, Map<Character, Ingredient<T>> ingredients) {
        List<Optional<Ingredient<T>>> result = new ArrayList<>();
        String[] shrunkPattern = shrink(pattern);
        for (String pa : shrunkPattern) {
            for (int j = 0; j < pa.length(); j++) {
                char ch = pa.charAt(j);
                if (ch == ' ') {
                    result.add(Optional.empty());
                } else {
                    Optional<Ingredient<T>> ingredient = Optional.ofNullable(ingredients.get(ch));
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
