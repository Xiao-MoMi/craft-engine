package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.plugin.config.IdSectionConfigParser;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStage;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStages;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.UniqueKey;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;

public abstract class AbstractRecipeManager<T> implements RecipeManager<T> {
    protected final Map<RecipeType, List<Recipe<T>>> byType = new EnumMap<>(RecipeType.class);
    protected final Map<Key, Recipe<T>> byId = new LinkedHashMap<>();
    protected final Map<Key, List<Recipe<T>>> byResult = new HashMap<>();
    protected final Map<Key, List<Recipe<T>>> byIngredient = new HashMap<>();
    protected final Map<Key, List<IngredientUnlockable>> ingredientUnlockable = new HashMap<>();
    protected final Set<Key> dataPackRecipes = new HashSet<>();
    protected final RecipeParser recipeParser;

    public AbstractRecipeManager() {
        this.recipeParser = new RecipeParser();
    }

    @Override
    public ConfigParser parser() {
        return this.recipeParser;
    }

    @Override
    public void unload() {
        this.dataPackRecipes.clear();
        this.byType.clear();
        this.byId.clear();
        this.byResult.clear();
        this.byIngredient.clear();
        this.ingredientUnlockable.clear();
    }

    protected void markAsDataPackRecipe(Key key) {
        this.dataPackRecipes.add(key);
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
    public List<Recipe<T>> recipesByType(RecipeType type) {
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
    public Recipe<T> recipeByInput(RecipeType type, RecipeInput input) {
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
    public Recipe<T> recipeByInput(RecipeType type, RecipeInput input, Key lastRecipe) {
        if (lastRecipe != null) {
            Recipe<T> last = this.byId.get(lastRecipe);
            if (last != null && last.matches(input)) {
                return last;
            }
        }
        return recipeByInput(type, input);
    }

    public List<IngredientUnlockable> ingredientUnlockablesByChangedItem(Key item) {
        return this.ingredientUnlockable.getOrDefault(item, List.of());
    }

    protected boolean registerInternalRecipe(Key id, Recipe<T> recipe, boolean unlockOnIngredientObtained) {
        if (this.byId.containsKey(id)) return false;
        this.byType.computeIfAbsent(recipe.type(), k -> new ArrayList<>()).add(recipe);
        this.byId.put(id, recipe);
        if (recipe instanceof AbstractFixedResultRecipe<?> fixedResult) {
            this.byResult.computeIfAbsent(fixedResult.result().item().id(), k -> new ArrayList<>()).add(recipe);
        }
        List<Ingredient<T>> ingredients = recipe.ingredientsInUse();
        if (recipe.canBeSearchedByIngredients()) {
            HashSet<Key> usedKeys = new HashSet<>();
            for (Ingredient<T> ingredient : ingredients) {
                for (UniqueKey holder : ingredient.items()) {
                    Key key = holder.key();
                    if (usedKeys.add(key)) {
                        this.byIngredient.computeIfAbsent(key, k -> new ArrayList<>()).add(recipe);
                    }
                }
            }
        }
        if (unlockOnIngredientObtained) {
            List<IngredientUnlockable.Requirement> requirements =  new ArrayList<>(4);
            HashSet<UniqueKey> usedKeys = new HashSet<>();
            for (Ingredient<T> ingredient : ingredients) {
                List<UniqueKey> items = ingredient.items();
                if (items.size() > 1) {
                    requirements.add(new IngredientUnlockable.Multiple(items.toArray(new UniqueKey[0])));
                } else if (!items.isEmpty()) {
                    requirements.add(new IngredientUnlockable.Single(items.getFirst()));
                }
                usedKeys.addAll(items);
            }
            IngredientUnlockable unlockable = new IngredientUnlockable(recipe, requirements.toArray(new IngredientUnlockable.Requirement[0]));
            for (UniqueKey usedKey : usedKeys) {
                this.ingredientUnlockable.computeIfAbsent(usedKey.key(), l -> new ArrayList<>()).add(unlockable);
            }
        }
        return true;
    }

    public final class RecipeParser extends IdSectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] {"recipes", "recipe"};

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public int count() {
            return Math.max(0, AbstractRecipeManager.this.byId.size() - AbstractRecipeManager.this.dataPackRecipes.size());
        }

        @Override
        public LoadingStage loadingStage() {
            return LoadingStages.RECIPE;
        }

        @Override
        public List<LoadingStage> dependencies() {
            return List.of(LoadingStages.TEMPLATE, LoadingStages.ITEM);
        }

        @Override
        public void parseSection(Pack pack, Path path, String node, Key id, Map<String, Object> section) {
            if (!Config.enableRecipeSystem()) return;
            if (AbstractRecipeManager.this.byId.containsKey(id)) {
                throw new LocalizedResourceConfigException("warning.config.recipe.duplicate");
            }
            boolean unlockOnIngredientObtained = (boolean) section.getOrDefault("unlock-on-ingredient-obtained", Config.unlockOnIngredientObtained());
            Recipe<T> recipe = RecipeSerializers.fromMap(id, section);
            try {
                registerInternalRecipe(id, recipe, unlockOnIngredientObtained);
            } catch (LocalizedResourceConfigException e) {
                throw e;
            } catch (Exception e) {
                CraftEngine.instance().logger().warn("Failed to register custom recipe " + id, e);
            }
        }
    }

    protected abstract void unregisterPlatformRecipeMainThread(Key key, boolean isBrewingRecipe);

    protected abstract void registerPlatformRecipeMainThread(Recipe<T> recipe);
}
