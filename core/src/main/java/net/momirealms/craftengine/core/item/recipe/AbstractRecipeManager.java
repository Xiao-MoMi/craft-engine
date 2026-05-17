package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.AbstractItemManager;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.item.recipe.result.CustomRecipeResult;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.IdSectionConfigParser;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStage;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStages;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.UniqueKey;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractRecipeManager implements RecipeManager {
    private final CraftEngine plugin;
    protected final Map<RecipeType, List<Recipe>> byType = new EnumMap<>(RecipeType.class);
    protected final Map<Key, Recipe> byId = new LinkedHashMap<>();
    protected final Map<Key, List<Recipe>> byResult = new HashMap<>();
    protected final Map<Key, List<Recipe>> byIngredient = new HashMap<>();
    protected final Map<Key, List<IngredientUnlockable>> ingredientUnlockable = new HashMap<>();
    protected final List<Recipe> nativeRecipes = new ArrayList<>();
    protected final List<CustomBrewingRecipe> brewingRecipes = new ArrayList<>();
    protected final Set<Key> dataPackRecipes = new HashSet<>();
    protected final ConfigParser recipeParser;
    protected final RecipeRegistry recipeRegistry;

    public AbstractRecipeManager(RecipeRegistry recipeRegistry, CraftEngine plugin) {
        this.recipeParser = new RecipeParser();
        this.recipeRegistry = recipeRegistry;
        this.plugin = plugin;
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
        this.nativeRecipes.clear();
        this.brewingRecipes.clear();
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
    public Optional<Recipe> recipeById(Key key) {
        return Optional.ofNullable(this.byId.get(key));
    }

    @Override
    public List<Recipe> recipesByType(RecipeType type) {
        return this.byType.getOrDefault(type, List.of());
    }

    @Override
    public List<Recipe> recipeByResult(Key result) {
        return this.byResult.getOrDefault(result, List.of());
    }

    @Override
    public List<Recipe> recipeByIngredient(Key ingredient) {
        return this.byIngredient.getOrDefault(ingredient, List.of());
    }

    @Nullable
    @Override
    public Recipe recipeByInput(RecipeType type, RecipeInput input) {
        List<Recipe> recipes = this.byType.get(type);
        if (recipes == null) return null;
        for (Recipe recipe : recipes) {
            if (recipe.matches(input)) {
                return recipe;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public Recipe recipeByInput(RecipeType type, RecipeInput input, Key lastRecipe) {
        if (lastRecipe != null) {
            Recipe last = this.byId.get(lastRecipe);
            if (last != null && last.matches(input)) {
                return last;
            }
        }
        return recipeByInput(type, input);
    }

    public List<IngredientUnlockable> ingredientUnlockablesByChangedItem(Key item) {
        return this.ingredientUnlockable.getOrDefault(item, List.of());
    }

    protected abstract void loadDataPackRecipes();

    protected synchronized void registerRecipeInternal(Recipe recipe, boolean unlockOnIngredientObtained) {
        // 原版配方被覆写了
        if (this.byId.containsKey(recipe.id())) return;
        this.byType.computeIfAbsent(recipe.type(), k -> new ArrayList<>()).add(recipe);
        this.byId.put(recipe.id(), recipe);
        if (recipe instanceof CustomBrewingRecipe brewingRecipe) {
            this.brewingRecipes.add(brewingRecipe);
        } else {
            this.nativeRecipes.add(recipe);
        }
        List<Ingredient> ingredients = recipe.ingredientsInUse();
        if (recipe.canBeSearched()) {
            if (recipe instanceof AbstractFixedResultRecipe fixedResult) {
                this.byResult.computeIfAbsent(fixedResult.result().item().id(), k -> new ArrayList<>()).add(recipe);
            }
            HashSet<Key> usedKeys = new HashSet<>();
            for (Ingredient ingredient : ingredients) {
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
            Set<UniqueKey> usedKeys = new HashSet<>();
            for (Ingredient ingredient : ingredients) {
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
    }

    private final class RecipeParser extends IdSectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] {"recipes", "recipe"};
        private final AtomicInteger count = new AtomicInteger(0);
        private static final Key DYES = Key.of("dyes");

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public int count() {
            return this.count.get();
        }

        @Override
        public boolean async() {
            return Config.multiThreadedConfigLoad();
        }

        @Override
        public LoadingStage loadingStage() {
            return LoadingStages.RECIPE;
        }

        @Override
        public void preProcess() {
            this.count.set(0);
        }

        @Override
        public void postProcess() {
            registerDyeRecipes();
            loadDataPackRecipes();
        }

        private void registerDyeRecipes() {
            if (VersionHelper.isOrAbove26_1) {
                boolean hasCustomItem = false;
                AbstractItemManager itemManager = (AbstractItemManager) AbstractRecipeManager.this.plugin.itemManager();
                Map<Key, ItemDefinition> dyeableItems = itemManager.dyeableItems();
                if (!dyeableItems.isEmpty()) {
                    Set<UniqueKey> itemIds = new HashSet<>(itemManager.itemIdsByTag(DYES));
                    Set<UniqueKey> minecraftItemIds = new HashSet<>();
                    for (UniqueKey uniqueKey : itemIds) {
                        List<UniqueKey> ingredientSubstitutes = itemManager.getIngredientSubstitutes(uniqueKey.key());
                        if (!ingredientSubstitutes.isEmpty()) {
                            itemIds.addAll(ingredientSubstitutes);
                        }
                    }
                    for (UniqueKey holder : itemIds) {
                        Optional<ItemDefinition> optionalCustomItem = itemManager.getItemDefinition(holder.key());
                        UniqueKey vanillaItem;
                        if (optionalCustomItem.isPresent()) {
                            ItemDefinition itemDefinition = optionalCustomItem.get();
                            if (itemDefinition.isVanillaItem()) {
                                vanillaItem = holder;
                            } else {
                                vanillaItem = UniqueKey.create(itemDefinition.material());
                                hasCustomItem = true;
                            }
                        } else {
                            if (itemManager.isVanillaItem(holder.key())) {
                                vanillaItem = holder;
                            } else {
                                return;
                            }
                        }
                        if (vanillaItem == UniqueKey.AIR) {
                            return;
                        }
                        minecraftItemIds.add(vanillaItem);
                    }

                    for (Map.Entry<Key, ItemDefinition> itemEntry : dyeableItems.entrySet()) {
                        Key itemId = itemEntry.getKey();
                        ItemDefinition itemDefinition = itemEntry.getValue();
                        Key recipeId = Key.of(itemId.namespace, "generated_dye_" + itemId.value);
                        registerRecipeInternal(new CustomDyeRecipe(
                                recipeId,
                                true,
                                new CustomRecipeResult(itemDefinition, 1, null),
                                null, null,
                                CraftingRecipeCategory.MISC,
                                Ingredient.of(
                                        List.of(IngredientElement.item(itemId)),
                                        Set.of(UniqueKey.create(itemId)),
                                        Set.of(UniqueKey.create(itemDefinition.material())),
                                        !itemDefinition.isVanillaItem(),
                                        1
                                ),
                                Ingredient.of(
                                        List.of(IngredientElement.tag(DYES)),
                                        itemIds,
                                        minecraftItemIds,
                                        hasCustomItem,
                                        1
                                ),
                                null, null, false, false
                        ), Config.unlockOnIngredientObtained());
                    }
                }
            }
        }

        @Override
        public List<LoadingStage> dependencies() {
            return List.of(LoadingStages.TEMPLATE, LoadingStages.ITEM);
        }

        private static final String[] UNLOCK_ON_INGREDIENT_OBTAINED = new String[] {"unlock_on_ingredient_obtained", "unlock-on-ingredient-obtained"};

        @Override
        public void parseSection(@NotNull Pack pack, @NotNull Path path, @NotNull Key id, @NotNull ConfigSection section) {
            if (!Config.enableRecipeSystem()) return;
            boolean unlockOnIngredientObtained = section.getBoolean(UNLOCK_ON_INGREDIENT_OBTAINED, Config.unlockOnIngredientObtained());
            Recipe recipe = RecipeSerializers.fromConfig(id, section);
            registerRecipeInternal(recipe, unlockOnIngredientObtained);
            this.count.incrementAndGet();
        }
    }
}
