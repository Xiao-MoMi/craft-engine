package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.recipe.input.CraftingInput;
import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CustomShapedRecipe<T> extends CraftingTableRecipe<T> {
    public static final Factory<?> FACTORY = new Factory<CustomShapedRecipe<?>>();
    private final ParsedPattern<T> parsedPattern;
    private final Pattern<T> pattern;

    public CustomShapedRecipe(Key id, CraftingRecipeCategory category, String group, Pattern<T> pattern, CustomRecipeResult<T> result) {
        super(id, category, group, result);
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
    public @NotNull Key type() {
        return RecipeTypes.SHAPED;
    }

    public Pattern<T> pattern() {
        return pattern;
    }

    public record Pattern<T>(String[] pattern, Map<Character, Ingredient<T>> ingredients) {

        public ParsedPattern<T> parse() {
                String[] shrunk = shrink(pattern);
                return new ParsedPattern<>(shrunk[0].length(), shrunk.length,
                        toIngredientList(
                                shrunk,
                                ingredients
                        ));
        }
    }

    public static class ParsedPattern<T> {
        private final int width;
        private final int height;
        private final List<Optional<Ingredient<T>>> ingredients;
        private final int ingredientCount;
        private final boolean symmetrical;

        public ParsedPattern(int width, int height, List<Optional<Ingredient<T>>> ingredients) {
            this.height = height;
            this.width = width;
            this.ingredientCount = (int) ingredients.stream().flatMap(Optional::stream).count();
            this.symmetrical = isSymmetrical(width, height, ingredients);
            this.ingredients = ingredients;
        }

        public List<Optional<Ingredient<T>>> ingredients() {
            return ingredients;
        }

        public int width() {
            return width;
        }

        public int height() {
            return height;
        }

        public boolean matches(CraftingInput<T> input) {
            if (input.ingredientCount() == this.ingredientCount) {
                if (input.width() == this.width && input.height() == this.height) {
                    if (!this.symmetrical && this.matches(input, true)) {
                        return true;
                    }
                    return this.matches(input, false);
                }
            }
            return false;
        }

        private boolean matches(CraftingInput<T> input, boolean mirrored) {
            for (int i = 0; i < this.height; i++) {
                for (int j = 0; j < this.width; j++) {
                    Optional<Ingredient<T>> optional;
                    if (mirrored) {
                        optional = this.ingredients.get(this.width - j - 1 + i * this.width);
                    } else {
                        optional = this.ingredients.get(j + i * this.width);
                    }
                    OptimizedIDItem<T> itemStack = input.getItem(j, i);
                    if (!Ingredient.isInstance(optional, itemStack)) {
                        return false;
                    }
                }
            }
            return true;
        }

        private static <T> boolean isSymmetrical(int width, int height, List<T> list) {
            if (width != 1) {
                int i = width / 2;
                for (int j = 0; j < height; j++) {
                    for (int k = 0; k < i; k++) {
                        int l = width - 1 - k;
                        T o1 = list.get(k + j * width);
                        T o2 = list.get(l + j * width);
                        if (!o1.equals(o2)) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
    }

    public static class Factory<A> implements RecipeFactory<A> {

        @SuppressWarnings({"unchecked", "rawtypes", "DuplicatedCode"})
        @Override
        public Recipe<A> create(Key id, Map<String, Object> arguments) {
            List<String> pattern = MiscUtils.getAsStringList(arguments.get("pattern"));
            if (pattern.isEmpty()) {
                throw new IllegalArgumentException("pattern cannot be empty");
            }
            if (!validatePattern(pattern)) {
                throw new IllegalArgumentException("Invalid pattern: " + pattern);
            }
            Map<String, Object> ingredientMap = MiscUtils.castToMap(arguments.get("ingredients"), true);
            if (ingredientMap == null) {
                throw new IllegalArgumentException("ingredients cannot be empty");
            }
            CraftingRecipeCategory recipeCategory = arguments.containsKey("category") ? CraftingRecipeCategory.valueOf(arguments.get("category").toString().toUpperCase(Locale.ENGLISH)) : null;
            String group = arguments.containsKey("group") ? arguments.get("group").toString() : null;
            Map<Character, Ingredient<A>> ingredients = new HashMap<>();
            for (Map.Entry<String, Object> entry : ingredientMap.entrySet()) {
                String key = entry.getKey();
                if (key.length() != 1) {
                    throw new IllegalArgumentException("Invalid key: " + key);
                }
                char ch = key.charAt(0);
                List<String> items = MiscUtils.getAsStringList(entry.getValue());
                Set<Holder<Key>> holders = new HashSet<>();
                for (String item : items) {
                    if (item.charAt(0) == '#') {
                        holders.addAll(CraftEngine.instance().itemManager().tagToItems(Key.of(item.substring(1))));
                    } else {
                        holders.add(BuiltInRegistries.OPTIMIZED_ITEM_ID.get(Key.of(item)).orElseThrow(() -> new IllegalArgumentException("Invalid vanilla/custom item: " + item)));
                    }
                }
                ingredients.put(ch, Ingredient.of(holders));
            }
            return new CustomShapedRecipe(
                    id,
                    recipeCategory,
                    group,
                    new Pattern<>(pattern.toArray(new String[0]), ingredients),
                    parseResult(arguments)
            );
        }

        private boolean validatePattern(List<String> pattern) {
            String first = pattern.get(0);
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

    public static <T> List<Optional<Ingredient<T>>> toIngredientList(String[] pattern, Map<Character, Ingredient<T>> ingredients) {
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
        return result;
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
