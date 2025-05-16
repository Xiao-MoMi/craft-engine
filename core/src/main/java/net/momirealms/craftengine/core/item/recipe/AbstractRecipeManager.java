package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.item.recipe.vanilla.VanillaRecipeReader;
import net.momirealms.craftengine.core.item.recipe.vanilla.reader.VanillaRecipeReader1_20;
import net.momirealms.craftengine.core.item.recipe.vanilla.reader.VanillaRecipeReader1_20_5;
import net.momirealms.craftengine.core.item.recipe.vanilla.reader.VanillaRecipeReader1_21_2;
import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;

public abstract class AbstractRecipeManager<T> implements RecipeManager<T> {
    protected final VanillaRecipeReader recipeReader;
    protected final Map<Key, List<Recipe<T>>> byType = new HashMap<>();
    protected final Map<Key, Recipe<T>> byId = new HashMap<>();
    protected final Map<Key, List<Recipe<T>>> byResult = new HashMap<>();
    protected final Map<Key, List<Recipe<T>>> byIngredient = new HashMap<>();
    protected final Set<Key> dataPackRecipes = new HashSet<>();
    protected final Set<Key> customRecipes = new HashSet<>();
    private final RecipeParser recipeParser;

    public AbstractRecipeManager() {
        this.recipeReader = initVanillaRecipeReader();
        this.recipeParser = new RecipeParser();
    }

    @Override
    public ConfigParser parser() {
        return this.recipeParser;
    }

    private VanillaRecipeReader initVanillaRecipeReader() {
        if (VersionHelper.isOrAbove1_21_2()) {
            return new VanillaRecipeReader1_21_2();
        } else if (VersionHelper.isOrAbove1_20_5()) {
            return new VanillaRecipeReader1_20_5();
        } else {
            return new VanillaRecipeReader1_20();
        }
    }

    @Override
    public void unload() {
        this.dataPackRecipes.clear();
        this.byType.clear();
        this.byId.clear();
        this.byResult.clear();
        this.byIngredient.clear();
        for (Key key : this.customRecipes) {
            unregisterPlatformRecipe(key);
        }
        this.customRecipes.clear();
    }

    protected void markAsDataPackRecipe(Key key) {
        this.dataPackRecipes.add(key);
    }

    protected void markAsCustomRecipe(Key key) {
        this.customRecipes.add(key);
    }

    @Override
    public boolean isDataPackRecipe(Key key) {
        return this.dataPackRecipes.contains(key);
    }

    @Override
    public boolean isCustomRecipe(Key key) {
        return this.byId.containsKey(key);
    }

    @Override
    public Optional<Recipe<T>> recipeById(Key key) {
        return Optional.ofNullable(this.byId.get(key));
    }

    @Override
    public List<Recipe<T>> recipesByType(Key type) {
        return this.byType.getOrDefault(type, List.of());
    }

    @Override
    public List<Recipe<T>> recipeByResult(Key result) {
        return this.byResult.getOrDefault(result, List.of());
    }

    @Override
    public List<Recipe<T>> recipeByIngredient(Key ingredient) {
        return this.byIngredient.getOrDefault(ingredient, List.of());
    }

    @Nullable
    @Override
    public Recipe<T> recipeByInput(Key type, RecipeInput input) {
        List<Recipe<T>> recipes = this.byType.get(type);
        if (recipes == null) return null;
        for (Recipe<T> recipe : recipes) {
            if (recipe.matches(input)) {
                return recipe;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public Recipe<T> recipeByInput(Key type, RecipeInput input, Key lastRecipe) {
        if (lastRecipe != null) {
            Recipe<T> last = byId.get(lastRecipe);
            if (last != null && last.matches(input)) {
                return last;
            }
        }
        return recipeByInput(type, input);
    }

    protected void registerInternalRecipe(Key id, Recipe<T> recipe) {
        this.byType.computeIfAbsent(recipe.type(), k -> new ArrayList<>()).add(recipe);
        this.byId.put(id, recipe);
        this.byResult.computeIfAbsent(recipe.result().item().id(), k -> new ArrayList<>()).add(recipe);
        HashSet<Key> usedKeys = new HashSet<>();
        for (Ingredient<T> ingredient : recipe.ingredientsInUse()) {
            for (Holder<Key> holder : ingredient.items()) {
                Key key = holder.value();
                if (usedKeys.add(key)) {
                    this.byIngredient.computeIfAbsent(key, k -> new ArrayList<>()).add(recipe);
                }
            }
        }
    }

    public class RecipeParser implements ConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] {"recipes", "recipe"};

        @Override
        public int loadingSequence() {
            return LoadingSequence.RECIPE;
        }

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public void parseSection(Pack pack, Path path, Key id, Map<String, Object> section) {
            if (!Config.enableRecipeSystem()) return;
            if (AbstractRecipeManager.this.byId.containsKey(id)) {
                throw new LocalizedResourceConfigException("warning.config.recipe.duplicate", path, id);
            }
            Recipe<T> recipe = RecipeTypes.fromMap(id, section);
            try {
                markAsCustomRecipe(id);
                registerInternalRecipe(id, recipe);
                registerPlatformRecipe(id, recipe);
            } catch (Exception e) {
                CraftEngine.instance().logger().warn("Failed to register custom recipe " + id, e);
            }
        }
    }

    protected abstract void unregisterPlatformRecipe(Key key);

    protected abstract void registerPlatformRecipe(Key key, Recipe<T> recipe);
}
