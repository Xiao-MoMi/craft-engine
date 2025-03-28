package net.momirealms.craftengine.bukkit.item.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.saicone.rtag.item.ItemObject;
import com.saicone.rtag.tag.TagCompound;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.item.CloneableConstantItem;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.MaterialUtils;
import net.momirealms.craftengine.bukkit.util.RecipeUtils;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.recipe.Recipe;
import net.momirealms.craftengine.core.item.recipe.*;
import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.item.recipe.vanilla.*;
import net.momirealms.craftengine.core.item.recipe.vanilla.reader.VanillaRecipeReader1_20;
import net.momirealms.craftengine.core.item.recipe.vanilla.reader.VanillaRecipeReader1_20_5;
import net.momirealms.craftengine.core.item.recipe.vanilla.reader.VanillaRecipeReader1_21_2;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigManager;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.util.HeptaFunction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.PentaFunction;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.*;
import org.bukkit.inventory.recipe.CookingBookCategory;
import org.bukkit.inventory.recipe.CraftingBookCategory;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class BukkitRecipeManager implements RecipeManager<ItemStack> {
    private static final Map<Key, BiConsumer<NamespacedKey, Recipe<ItemStack>>> BUKKIT_RECIPE_FACTORIES = new HashMap<>();
    private static Object minecraftRecipeManager;
    private static final List<Object> injectedIngredients = new ArrayList<>();
    private static final IdentityHashMap<Recipe<ItemStack>, Object> recipeToMcRecipeHolder = new IdentityHashMap<>();
    private static BukkitRecipeManager instance;

    static {
        BUKKIT_RECIPE_FACTORIES.put(RecipeTypes.SMITHING_TRANSFORM, (key, recipe) -> {
            CustomSmithingTransformRecipe<ItemStack> ceRecipe = (CustomSmithingTransformRecipe<ItemStack>) recipe;
            ceRecipe.addition();
            // bukkit api doesn't allow empty material choices, that's why we do this
            try {
                Object smithingRecipe = createMinecraftSmithingTransformRecipe(ceRecipe);
                if (VersionHelper.isVersionNewerThan1_21_2()) {
                    smithingRecipe = Reflections.constructor$RecipeHolder.newInstance(Reflections.method$CraftRecipe$toMinecraft.invoke(null, key), smithingRecipe);
                } else if (VersionHelper.isVersionNewerThan1_20_2()) {
                    smithingRecipe = Reflections.constructor$RecipeHolder.newInstance(Reflections.method$ResourceLocation$fromNamespaceAndPath.invoke(null, key.namespace(), key.value()), smithingRecipe);
                }
                Reflections.method$RecipeManager$addRecipe.invoke(minecraftRecipeManager(), smithingRecipe);
            } catch (Exception e) {
                CraftEngine.instance().logger().warn("Failed to convert transform recipe", e);
            }
        });
        BUKKIT_RECIPE_FACTORIES.put(RecipeTypes.SHAPED, (key, recipe) -> {
            CustomShapedRecipe<ItemStack> ceRecipe = (CustomShapedRecipe<ItemStack>) recipe;
            ShapedRecipe shapedRecipe = new ShapedRecipe(key, ceRecipe.result(ItemBuildContext.EMPTY));
            if (ceRecipe.group() != null) {
                shapedRecipe.setGroup(Objects.requireNonNull(ceRecipe.group()));
            }
            if (ceRecipe.category() != null) {
                shapedRecipe.setCategory(CraftingBookCategory.valueOf(Objects.requireNonNull(ceRecipe.category()).name()));
            }
            shapedRecipe.shape(ceRecipe.pattern().pattern());
            for (Map.Entry<Character, Ingredient<ItemStack>> entry : ceRecipe.pattern().ingredients().entrySet()) {
                shapedRecipe.setIngredient(entry.getKey(), ingredientToBukkitRecipeChoice(entry.getValue()));
            }
            try {
                Object craftRecipe = Reflections.method$CraftShapedRecipe$fromBukkitRecipe.invoke(null, shapedRecipe);
                Reflections.method$CraftRecipe$addToCraftingManager.invoke(craftRecipe);
                injectShapedRecipe(new Key(key.namespace(), key.value()), ceRecipe);
            } catch (Exception e) {
                CraftEngine.instance().logger().warn("Failed to convert shaped recipe", e);
            }
        });
        BUKKIT_RECIPE_FACTORIES.put(RecipeTypes.SHAPELESS, (key, recipe) -> {
            CustomShapelessRecipe<ItemStack> ceRecipe = (CustomShapelessRecipe<ItemStack>) recipe;
            ShapelessRecipe shapelessRecipe = new ShapelessRecipe(key, ceRecipe.result(ItemBuildContext.EMPTY));
            if (ceRecipe.group() != null) {
                shapelessRecipe.setGroup(Objects.requireNonNull(ceRecipe.group()));
            }
            if (ceRecipe.category() != null) {
                shapelessRecipe.setCategory(CraftingBookCategory.valueOf(Objects.requireNonNull(ceRecipe.category()).name()));
            }
            for (Ingredient<ItemStack> ingredient : ceRecipe.ingredientsInUse()) {
                shapelessRecipe.addIngredient(ingredientToBukkitRecipeChoice(ingredient));
            }
            try {
                Object craftRecipe = Reflections.method$CraftShapelessRecipe$fromBukkitRecipe.invoke(null, shapelessRecipe);
                Reflections.method$CraftRecipe$addToCraftingManager.invoke(craftRecipe);
                injectShapelessRecipe(new Key(key.namespace(), key.value()), ceRecipe);
            } catch (Exception e) {
                CraftEngine.instance().logger().warn("Failed to convert shapeless recipe", e);
            }
        });
        BUKKIT_RECIPE_FACTORIES.put(RecipeTypes.SMELTING, (key, recipe) -> {
            CustomSmeltingRecipe<ItemStack> ceRecipe = (CustomSmeltingRecipe<ItemStack>) recipe;
            FurnaceRecipe furnaceRecipe = new FurnaceRecipe(
                    key, ceRecipe.result(ItemBuildContext.EMPTY),
                    ingredientToBukkitRecipeChoice(ceRecipe.ingredient()),
                    ceRecipe.experience(), ceRecipe.cookingTime()
            );
            if (ceRecipe.group() != null) {
                furnaceRecipe.setGroup(Objects.requireNonNull(ceRecipe.group()));
            }
            if (ceRecipe.category() != null) {
                furnaceRecipe.setCategory(CookingBookCategory.valueOf(Objects.requireNonNull(ceRecipe.category()).name()));
            }
            try {
                Object craftRecipe = Reflections.method$CraftFurnaceRecipe$fromBukkitRecipe.invoke(null, furnaceRecipe);
                Reflections.method$CraftRecipe$addToCraftingManager.invoke(craftRecipe);
                injectCookingRecipe(new Key(key.namespace(), key.value()), ceRecipe);
            } catch (Exception e) {
                CraftEngine.instance().logger().warn("Failed to convert smelting recipe", e);
            }
        });
        BUKKIT_RECIPE_FACTORIES.put(RecipeTypes.SMOKING, (key, recipe) -> {
            CustomSmokingRecipe<ItemStack> ceRecipe = (CustomSmokingRecipe<ItemStack>) recipe;
            SmokingRecipe smokingRecipe = new SmokingRecipe(
                    key, ceRecipe.result(ItemBuildContext.EMPTY),
                    ingredientToBukkitRecipeChoice(ceRecipe.ingredient()),
                    ceRecipe.experience(), ceRecipe.cookingTime()
            );
            if (ceRecipe.group() != null) {
                smokingRecipe.setGroup(Objects.requireNonNull(ceRecipe.group()));
            }
            if (ceRecipe.category() != null) {
                smokingRecipe.setCategory(CookingBookCategory.valueOf(Objects.requireNonNull(ceRecipe.category()).name()));
            }
            try {
                Object craftRecipe = Reflections.method$CraftSmokingRecipe$fromBukkitRecipe.invoke(null, smokingRecipe);
                Reflections.method$CraftRecipe$addToCraftingManager.invoke(craftRecipe);
                injectCookingRecipe(new Key(key.namespace(), key.value()), ceRecipe);
            } catch (Exception e) {
                CraftEngine.instance().logger().warn("Failed to convert smoking recipe", e);
            }
        });
        BUKKIT_RECIPE_FACTORIES.put(RecipeTypes.BLASTING, (key, recipe) -> {
            CustomBlastingRecipe<ItemStack> ceRecipe = (CustomBlastingRecipe<ItemStack>) recipe;
            BlastingRecipe blastingRecipe = new BlastingRecipe(
                    key, ceRecipe.result(ItemBuildContext.EMPTY),
                    ingredientToBukkitRecipeChoice(ceRecipe.ingredient()),
                    ceRecipe.experience(), ceRecipe.cookingTime()
            );
            if (ceRecipe.group() != null) {
                blastingRecipe.setGroup(Objects.requireNonNull(ceRecipe.group()));
            }
            if (ceRecipe.category() != null) {
                blastingRecipe.setCategory(CookingBookCategory.valueOf(Objects.requireNonNull(ceRecipe.category()).name()));
            }
            try {
                Object craftRecipe = Reflections.method$CraftBlastingRecipe$fromBukkitRecipe.invoke(null, blastingRecipe);
                Reflections.method$CraftRecipe$addToCraftingManager.invoke(craftRecipe);
                injectCookingRecipe(new Key(key.namespace(), key.value()), ceRecipe);
            } catch (Exception e) {
                CraftEngine.instance().logger().warn("Failed to convert blasting recipe", e);
            }
        });
        BUKKIT_RECIPE_FACTORIES.put(RecipeTypes.CAMPFIRE_COOKING, (key, recipe) -> {
            CustomCampfireRecipe<ItemStack> ceRecipe = (CustomCampfireRecipe<ItemStack>) recipe;
            CampfireRecipe campfireRecipe = new CampfireRecipe(
                    key, ceRecipe.result(ItemBuildContext.EMPTY),
                    ingredientToBukkitRecipeChoice(ceRecipe.ingredient()),
                    ceRecipe.experience(), ceRecipe.cookingTime()
            );
            if (ceRecipe.group() != null) {
                campfireRecipe.setGroup(Objects.requireNonNull(ceRecipe.group()));
            }
            if (ceRecipe.category() != null) {
                campfireRecipe.setCategory(CookingBookCategory.valueOf(Objects.requireNonNull(ceRecipe.category()).name()));
            }
            try {
                Object craftRecipe = Reflections.method$CraftCampfireRecipe$fromBukkitRecipe.invoke(null, campfireRecipe);
                Reflections.method$CraftRecipe$addToCraftingManager.invoke(craftRecipe);
                injectCookingRecipe(new Key(key.namespace(), key.value()), ceRecipe);
            } catch (Exception e) {
                CraftEngine.instance().logger().warn("Failed to convert campfire recipe", e);
            }
        });
        BUKKIT_RECIPE_FACTORIES.put(RecipeTypes.STONECUTTING, (key, recipe) -> {
            CustomStoneCuttingRecipe<ItemStack> ceRecipe = (CustomStoneCuttingRecipe<ItemStack>) recipe;
            List<ItemStack> itemStacks = new ArrayList<>();
            for (Holder<Key> item : ceRecipe.ingredient().items()) {
                itemStacks.add(BukkitItemManager.instance().buildItemStack(item.value(), null));
            }
            StonecuttingRecipe stonecuttingRecipe = new StonecuttingRecipe(
                    key, ceRecipe.result(ItemBuildContext.EMPTY),
                    new RecipeChoice.ExactChoice(itemStacks)
            );
            if (ceRecipe.group() != null) {
                stonecuttingRecipe.setGroup(Objects.requireNonNull(ceRecipe.group()));
            }
            try {
                Object craftRecipe = Reflections.method$CraftStonecuttingRecipe$fromBukkitRecipe.invoke(null, stonecuttingRecipe);
                Reflections.method$CraftRecipe$addToCraftingManager.invoke(craftRecipe);
            } catch (Exception e) {
                CraftEngine.instance().logger().warn("Failed to convert stone cutting recipe", e);
            }
        });
    }

    private final BukkitCraftEngine plugin;
    private final RecipeEventListener recipeEventListener;
    private final CrafterEventListener crafterEventListener;
    private final Map<Key, List<Recipe<ItemStack>>> byType;
    private final Map<Key, Recipe<ItemStack>> byId;
    private final Map<Key, List<Recipe<ItemStack>>> byResult;
    private final Map<Key, List<Recipe<ItemStack>>> byIngredient;
    private final VanillaRecipeReader recipeReader;
    private final List<NamespacedKey> injectedDataPackRecipes;
    private final List<NamespacedKey> registeredCustomRecipes;
    // data pack recipe resource locations [minecraft:xxx]
    private final Set<Key> dataPackRecipes;

    private Object stolenFeatureFlagSet;

    public BukkitRecipeManager(BukkitCraftEngine plugin) {
        instance = this;
        this.plugin = plugin;
        this.byType = new HashMap<>();
        this.byId = new HashMap<>();
        this.byIngredient = new HashMap<>();
        this.byResult = new HashMap<>();
        this.injectedDataPackRecipes = new ArrayList<>();
        this.registeredCustomRecipes = new ArrayList<>();
        this.dataPackRecipes = new HashSet<>();
        this.recipeEventListener = new RecipeEventListener(plugin, this, plugin.itemManager());
        if (VersionHelper.isVersionNewerThan1_21()) {
            this.crafterEventListener = new CrafterEventListener(plugin, this, plugin.itemManager());
        } else {
            this.crafterEventListener = null;
        }
        if (VersionHelper.isVersionNewerThan1_21_2()) {
            this.recipeReader = new VanillaRecipeReader1_21_2();
        } else if (VersionHelper.isVersionNewerThan1_20_5()) {
            this.recipeReader = new VanillaRecipeReader1_20_5();
        } else {
            this.recipeReader = new VanillaRecipeReader1_20();
        }
        try {
            minecraftRecipeManager = Reflections.method$MinecraftServer$getRecipeManager.invoke(Reflections.method$MinecraftServer$getServer.invoke(null));
        } catch (ReflectiveOperationException e) {
            plugin.logger().warn("Failed to get minecraft recipe manager", e);
        }
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
    public Optional<Recipe<ItemStack>> getRecipeById(Key key) {
        return Optional.ofNullable(this.byId.get(key));
    }

    @Override
    public void delayedInit() {
        Bukkit.getPluginManager().registerEvents(this.recipeEventListener, this.plugin.bootstrap());
        if (this.crafterEventListener != null) {
            Bukkit.getPluginManager().registerEvents(this.crafterEventListener, this.plugin.bootstrap());
        }
    }

    @Override
    public void load() {
        if (!ConfigManager.enableRecipeSystem()) return;
        if (VersionHelper.isVersionNewerThan1_21_2()) {
            try {
                this.stolenFeatureFlagSet = Reflections.field$RecipeManager$featureflagset.get(minecraftRecipeManager);
                Reflections.field$RecipeManager$featureflagset.set(minecraftRecipeManager, null);
            } catch (ReflectiveOperationException e) {
                this.plugin.logger().warn("Failed to steal featureflagset", e);
            }
        }
    }

    @Override
    public void unload() {
        this.byType.clear();
        this.byId.clear();
        this.byResult.clear();
        this.byIngredient.clear();
        this.dataPackRecipes.clear();

        try {
            // do not unregister them
//            for (NamespacedKey key : this.injectedDataPackRecipes) {
//                unregisterRecipe(key);
//            }
            for (NamespacedKey key : this.registeredCustomRecipes) {
                unregisterRecipe(key);
            }
            if (VersionHelper.isVersionNewerThan1_21_2()) {
                Reflections.method$RecipeManager$finalizeRecipeLoading.invoke(minecraftRecipeManager);
            }
        } catch (ReflectiveOperationException e) {
            plugin.logger().warn("Failed to unregister recipes", e);
        }

        this.registeredCustomRecipes.clear();
        this.injectedDataPackRecipes.clear();

        recipeToMcRecipeHolder.clear();
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this.recipeEventListener);
        if (this.crafterEventListener != null) {
            HandlerList.unregisterAll(this.crafterEventListener);
        }
        unload();
    }

    private void unregisterRecipe(NamespacedKey key) throws ReflectiveOperationException {
        if (VersionHelper.isVersionNewerThan1_21_2()) {
            Object recipeMap = Reflections.field$RecipeManager$recipes.get(minecraftRecipeManager);
            Reflections.method$RecipeMap$removeRecipe.invoke(recipeMap, Reflections.method$CraftRecipe$toMinecraft.invoke(null, key));
        } else {
            Bukkit.removeRecipe(key);
        }
    }

    @Override
    public void parseSection(Pack pack, Path path, Key id, Map<String, Object> section) {
        if (!ConfigManager.enableRecipeSystem()) return;
        if (this.byId.containsKey(id)) {
            this.plugin.logger().warn(path, "Duplicated recipe " + id);
            return;
        }
        Recipe<ItemStack> recipe = RecipeTypes.fromMap(id, section);
        NamespacedKey key = NamespacedKey.fromString(id.toString());
        BUKKIT_RECIPE_FACTORIES.get(recipe.type()).accept(key, recipe);
        try {
            this.registeredCustomRecipes.add(key);
            addInternalRecipe(id, recipe);
        } catch (Exception e) {
            plugin.logger().warn("Failed to add custom recipe " + id, e);
        }
    }

    @Override
    public List<Recipe<ItemStack>> getRecipes(Key type) {
        return this.byType.getOrDefault(type, List.of());
    }

    @Override
    public List<Recipe<ItemStack>> getRecipeByResult(Key result) {
        return this.byResult.getOrDefault(result, List.of());
    }

    @Override
    public List<Recipe<ItemStack>> getRecipeByIngredient(Key ingredient) {
        return this.byIngredient.getOrDefault(ingredient, List.of());
    }

    private void addInternalRecipe(Key id, Recipe<ItemStack> recipe) {
        this.byType.computeIfAbsent(recipe.type(), k -> new ArrayList<>()).add(recipe);
        this.byId.put(id, recipe);
        this.byResult.computeIfAbsent(recipe.result().item().id(), k -> new ArrayList<>()).add(recipe);
        HashSet<Key> usedKeys = new HashSet<>();
        for (Ingredient<ItemStack> ingredient : recipe.ingredientsInUse()) {
            for (Holder<Key> holder : ingredient.items()) {
                Key key = holder.value();
                if (usedKeys.add(key)) {
                    this.byIngredient.computeIfAbsent(key, k -> new ArrayList<>()).add(recipe);
                }
            }
        }
    }

    @Nullable
    @Override
    public Recipe<ItemStack> getRecipe(Key type, RecipeInput input) {
        List<Recipe<ItemStack>> recipes = this.byType.get(type);
        if (recipes == null) return null;
        for (Recipe<ItemStack> recipe : recipes) {
            if (recipe.matches(input)) {
                return recipe;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public Recipe<ItemStack> getRecipe(Key type, RecipeInput input, Key lastRecipe) {
        if (lastRecipe != null) {
            Recipe<ItemStack> last = byId.get(lastRecipe);
            if (last != null && last.matches(input)) {
                return last;
            }
        }
        return getRecipe(type, input);
    }

    @Override
    public CompletableFuture<Void> delayedLoad() {
        if (!ConfigManager.enableRecipeSystem()) return CompletableFuture.completedFuture(null);
        return this.processVanillaRecipes().thenRun(() -> {
            try {
                // give flags back on 1.21.2+
                if (VersionHelper.isVersionNewerThan1_21_2() && this.stolenFeatureFlagSet != null) {
                    Reflections.field$RecipeManager$featureflagset.set(minecraftRecipeManager, this.stolenFeatureFlagSet);
                    this.stolenFeatureFlagSet = false;
                }

                // refresh recipes
                if (VersionHelper.isVersionNewerThan1_21_2()) {
                    Reflections.method$RecipeManager$finalizeRecipeLoading.invoke(minecraftRecipeManager);
                }

                // send to players
                Reflections.method$DedicatedPlayerList$reloadRecipes.invoke(Reflections.field$CraftServer$playerList.get(Bukkit.getServer()));

                // now we need to remove the fake `exact`
                if (VersionHelper.isVersionNewerThan1_21_4()) {
                    for (Object ingredient : injectedIngredients) {
                        Reflections.field$Ingredient$itemStacks1_21_4.set(ingredient, null);
                    }
                } else if (VersionHelper.isVersionNewerThan1_21_2()) {
                    for (Object ingredient : injectedIngredients) {
                        Reflections.field$Ingredient$itemStacks1_21_2.set(ingredient, null);
                    }
                }

                // clear cache
                injectedIngredients.clear();
            } catch (Exception e) {
                this.plugin.logger().warn("Failed to run delayed recipe tasks", e);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<Void> processVanillaRecipes() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            List<Runnable> injectLogics = new ArrayList<>();
            plugin.scheduler().async().execute(() -> {
                try {
                    Object fileToIdConverter = Reflections.method$FileToIdConverter$json.invoke(null, VersionHelper.isVersionNewerThan1_21() ? "recipe" : "recipes");
                    Object minecraftServer = Reflections.method$MinecraftServer$getServer.invoke(null);
                    Object packRepository = Reflections.method$MinecraftServer$getPackRepository.invoke(minecraftServer);
                    List<Object> selected = (List<Object>) Reflections.field$PackRepository$selected.get(packRepository);
                    List<Object> packResources = new ArrayList<>();
                    for (Object pack : selected) {
                        packResources.add(Reflections.method$Pack$open.invoke(pack));
                    }
                    try (AutoCloseable resourceManager = (AutoCloseable) Reflections.constructor$MultiPackResourceManager.newInstance(Reflections.instance$PackType$SERVER_DATA, packResources)) {
                        Map<Object, Object> scannedResources = (Map<Object, Object>) Reflections.method$FileToIdConverter$listMatchingResources.invoke(fileToIdConverter, resourceManager);
                        for (Map.Entry<Object, Object> entry : scannedResources.entrySet()) {
                            Key id = extractKeyFromResourceLocation(entry.getKey().toString());
                            this.dataPackRecipes.add(id);
                            // Maybe it's unregistered by other plugins
                            if (Bukkit.getRecipe(new NamespacedKey(id.namespace(), id.value())) == null) {
                                continue;
                            }
                            Reader reader = (Reader) Reflections.method$Resource$openAsReader.invoke(entry.getValue());
                            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
                            String type = jsonObject.get("type").getAsString();
                            switch (type) {
                                case "minecraft:crafting_shaped" -> {
                                    VanillaShapedRecipe recipe = this.recipeReader.readShaped(jsonObject);
                                    handleDataPackShapedRecipe(id, recipe, (injectLogics::add));
                                }
                                case "minecraft:crafting_shapeless" -> {
                                    VanillaShapelessRecipe recipe = this.recipeReader.readShapeless(jsonObject);
                                    handleDataPackShapelessRecipe(id, recipe, (injectLogics::add));
                                }
                                case "minecraft:smelting" -> {
                                    VanillaSmeltingRecipe recipe = this.recipeReader.readSmelting(jsonObject);
                                    handleDataPackCookingRecipe(id, recipe, FurnaceRecipe::new, CustomSmeltingRecipe::new, Reflections.method$CraftFurnaceRecipe$fromBukkitRecipe, (injectLogics::add));
                                }
                                case "minecraft:blasting" -> {
                                    VanillaBlastingRecipe recipe = this.recipeReader.readBlasting(jsonObject);
                                    handleDataPackCookingRecipe(id, recipe, BlastingRecipe::new, CustomBlastingRecipe::new, Reflections.method$CraftBlastingRecipe$fromBukkitRecipe, (injectLogics::add));
                                }
                                case "minecraft:smoking" -> {
                                    VanillaSmokingRecipe recipe = this.recipeReader.readSmoking(jsonObject);
                                    handleDataPackCookingRecipe(id, recipe, SmokingRecipe::new, CustomSmokingRecipe::new, Reflections.method$CraftSmokingRecipe$fromBukkitRecipe, (injectLogics::add));
                                }
                                case "minecraft:campfire_cooking" -> {
                                    VanillaCampfireRecipe recipe = this.recipeReader.readCampfire(jsonObject);
                                    handleDataPackCookingRecipe(id, recipe, CampfireRecipe::new, CustomCampfireRecipe::new, Reflections.method$CraftCampfireRecipe$fromBukkitRecipe, (injectLogics::add));
                                }
                                case "minecraft:stonecutting" -> {
                                    VanillaStoneCuttingRecipe recipe = this.recipeReader.readStoneCutting(jsonObject);
                                    handleDataPackStoneCuttingRecipe(id, recipe);
                                }
                                case "minecraft:smithing_transform" -> {
                                    VanillaSmithingTransformRecipe recipe = this.recipeReader.readSmithingTransform(jsonObject);
                                    handleDataPackSmithingTransform(id, recipe, (injectLogics::add));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.logger().warn("Failed to read data pack recipes", e);
                } finally {
                    plugin.scheduler().sync().run(() -> {
                        try {
                            for (Runnable runnable : injectLogics) {
                                runnable.run();
                            }
                        } catch (Exception e) {
                            CraftEngine.instance().logger().warn("Failed to register recipes", e);
                        } finally {
                            future.complete(null);
                        }
                    });
                }
            });
        } catch (Exception e) {
            this.plugin.logger().warn("Failed to inject vanilla recipes", e);
        }
        return future;
    }

    private boolean readVanillaIngredients(boolean hasCustomItemInTag, List<String> ingredients, Consumer<Material> materialConsumer, Consumer<Holder<Key>> holderConsumer) {
        for (String item : ingredients) {
            if (item.charAt(0) == '#') {
                Key tag = Key.from(item.substring(1));
                for (Material material : tagToMaterials(tag)) {
                    materialConsumer.accept(material);
                }
                if (!hasCustomItemInTag) {
                    if (!plugin.itemManager().tagToCustomItems(tag).isEmpty()) {
                        hasCustomItemInTag = true;
                    }
                }
                for (Holder<Key> holder : plugin.itemManager().tagToItems(tag)) {
                    holderConsumer.accept(holder);
                }
            } else {
                materialConsumer.accept(MaterialUtils.getMaterial(item));
                holderConsumer.accept(BuiltInRegistries.OPTIMIZED_ITEM_ID.get(Key.from(item)).orElseThrow());
            }
        }
        return hasCustomItemInTag;
    }

    private void handleDataPackShapelessRecipe(Key id, VanillaShapelessRecipe recipe, Consumer<Runnable> callback) {
        NamespacedKey key = new NamespacedKey(id.namespace(), id.value());
        ItemStack result = createResultStack(recipe.result());
        ShapelessRecipe shapelessRecipe = new ShapelessRecipe(key, result);
        if (recipe.group() != null) {
            shapelessRecipe.setGroup(recipe.group());
        }
        if (recipe.category() != null) {
            shapelessRecipe.setCategory(CraftingBookCategory.valueOf(recipe.category().name()));
        }

        boolean hasCustomItemInTag = false;
        List<Ingredient<ItemStack>> ingredientList = new ArrayList<>();
        for (List<String> list : recipe.ingredients()) {
            Set<Material> materials = new HashSet<>();
            Set<Holder<Key>> holders = new HashSet<>();
            for (String item : list) {
                if (item.charAt(0) == '#') {
                    Key tag = Key.of(item.substring(1));
                    materials.addAll(tagToMaterials(tag));
                    if (!hasCustomItemInTag) {
                        if (!plugin.itemManager().tagToCustomItems(tag).isEmpty()) {
                            hasCustomItemInTag = true;
                        }
                    }
                    holders.addAll(plugin.itemManager().tagToItems(tag));
                } else {
                    materials.add(MaterialUtils.getMaterial(item));
                    holders.add(BuiltInRegistries.OPTIMIZED_ITEM_ID.get(Key.from(item)).orElseThrow());
                }
            }
            shapelessRecipe.addIngredient(new RecipeChoice.MaterialChoice(new ArrayList<>(materials)));
            ingredientList.add(Ingredient.of(holders));
        }

        CustomShapelessRecipe<ItemStack> ceRecipe = new CustomShapelessRecipe<>(
                id,
                recipe.category(),
                recipe.group(),
                ingredientList,
                new CustomRecipeResult<>(new CloneableConstantItem(recipe.result().isCustom() ? Key.of("!internal:custom") : Key.of(recipe.result().id()), result), recipe.result().count())
        );
        if (hasCustomItemInTag) {
            callback.accept(() -> {
                try {
                    unregisterRecipe(key);
                    Reflections.method$CraftRecipe$addToCraftingManager.invoke(Reflections.method$CraftShapelessRecipe$fromBukkitRecipe.invoke(null, shapelessRecipe));
                    injectShapelessRecipe(id, ceRecipe);
                } catch (Exception e) {
                    CraftEngine.instance().logger().warn("Failed to convert shapeless recipe", e);
                }
            });
            this.injectedDataPackRecipes.add(key);
        }
        this.addInternalRecipe(id, ceRecipe);
    }

    private void handleDataPackStoneCuttingRecipe(Key id, VanillaStoneCuttingRecipe recipe) {
        ItemStack result = createResultStack(recipe.result());
        Set<Holder<Key>> holders = new HashSet<>();

        for (String item : recipe.ingredient()) {
            if (item.charAt(0) == '#') {
                Key tag = Key.from(item.substring(1));
                holders.addAll(plugin.itemManager().tagToItems(tag));
            } else {
                holders.add(BuiltInRegistries.OPTIMIZED_ITEM_ID.get(Key.from(item)).orElseThrow());
            }
        }

        CustomStoneCuttingRecipe<ItemStack> ceRecipe = new CustomStoneCuttingRecipe<>(
                id,
                recipe.group(),
                Ingredient.of(holders),
                new CustomRecipeResult<>(new CloneableConstantItem(recipe.result().isCustom() ? Key.of("!internal:custom") : Key.of(recipe.result().id()), result), recipe.result().count())
        );
        this.addInternalRecipe(id, ceRecipe);
    }

    private void handleDataPackShapedRecipe(Key id, VanillaShapedRecipe recipe, Consumer<Runnable> callback) {
        NamespacedKey key = new NamespacedKey(id.namespace(), id.value());
        ItemStack result = createResultStack(recipe.result());
        ShapedRecipe shapedRecipe = new ShapedRecipe(key, result);
        if (recipe.group() != null) {
            shapedRecipe.setGroup(recipe.group());
        }
        if (recipe.category() != null) {
            shapedRecipe.setCategory(CraftingBookCategory.valueOf(recipe.category().name()));
        }
        shapedRecipe.shape(recipe.pattern());

        boolean hasCustomItemInTag = false;
        Map<Character, Ingredient<ItemStack>> ingredients = new HashMap<>();
        for (Map.Entry<Character, List<String>> entry : recipe.ingredients().entrySet()) {
            Set<Material> materials = new HashSet<>();
            Set<Holder<Key>> holders = new HashSet<>();
            for (String item : entry.getValue()) {
                if (item.charAt(0) == '#') {
                    Key tag = Key.from(item.substring(1));
                    materials.addAll(tagToMaterials(tag));
                    if (!hasCustomItemInTag) {
                        if (!plugin.itemManager().tagToCustomItems(tag).isEmpty()) {
                            hasCustomItemInTag = true;
                        }
                    }
                    holders.addAll(plugin.itemManager().tagToItems(tag));
                } else {
                    materials.add(MaterialUtils.getMaterial(item));
                    holders.add(BuiltInRegistries.OPTIMIZED_ITEM_ID.get(Key.from(item)).orElseThrow());
                }
            }
            shapedRecipe.setIngredient(entry.getKey(), new RecipeChoice.MaterialChoice(new ArrayList<>(materials)));
            ingredients.put(entry.getKey(), Ingredient.of(holders));
        }

        CustomShapedRecipe<ItemStack> ceRecipe = new CustomShapedRecipe<>(
                id,
                recipe.category(),
                recipe.group(),
                new CustomShapedRecipe.Pattern<>(recipe.pattern(), ingredients),
                new CustomRecipeResult<>(new CloneableConstantItem(recipe.result().isCustom() ? Key.of("!internal:custom") : Key.of(recipe.result().id()), result), recipe.result().count())
        );
        if (hasCustomItemInTag) {
            callback.accept(() -> {
                try {
                    unregisterRecipe(key);
                    Reflections.method$CraftRecipe$addToCraftingManager.invoke(Reflections.method$CraftShapedRecipe$fromBukkitRecipe.invoke(null, shapedRecipe));
                    injectShapedRecipe(id, ceRecipe);
                } catch (Exception e) {
                    CraftEngine.instance().logger().warn("Failed to convert shaped recipe", e);
                }
            });
            this.injectedDataPackRecipes.add(key);
        }
        this.addInternalRecipe(id, ceRecipe);
    }

    private void handleDataPackCookingRecipe(Key id,
                                             VanillaCookingRecipe recipe,
                                             PentaFunction<NamespacedKey, ItemStack, RecipeChoice, Float, Integer, org.bukkit.inventory.CookingRecipe<?>> constructor1,
                                             HeptaFunction<Key, CookingRecipeCategory, String, Ingredient<ItemStack>, Integer, Float, CustomRecipeResult<ItemStack>, CustomCookingRecipe<ItemStack>> constructor2,
                                             Method fromBukkitRecipeMethod,
                                             Consumer<Runnable> callback) {
        NamespacedKey key = new NamespacedKey(id.namespace(), id.value());
        ItemStack result = createResultStack(recipe.result());

        Set<Material> materials = new HashSet<>();
        Set<Holder<Key>> holders = new HashSet<>();

        boolean hasCustomItemInTag = readVanillaIngredients(false, recipe.ingredient(), materials::add, holders::add);
        org.bukkit.inventory.CookingRecipe<?> cookingRecipe = constructor1.apply(key, result, new RecipeChoice.MaterialChoice(new ArrayList<>(materials)), recipe.experience(), recipe.cookingTime());
        if (recipe.group() != null) {
            cookingRecipe.setGroup(recipe.group());
        }
        if (recipe.category() != null) {
            cookingRecipe.setCategory(CookingBookCategory.valueOf(recipe.category().name()));
        }

        CustomCookingRecipe<ItemStack> ceRecipe = constructor2.apply(
                id,
                recipe.category(),
                recipe.group(),
                Ingredient.of(holders),
                recipe.cookingTime(),
                recipe.experience(),
                new CustomRecipeResult<>(new CloneableConstantItem(recipe.result().isCustom() ? Key.of("!internal:custom") : Key.of(recipe.result().id()), result), recipe.result().count())
        );
        if (hasCustomItemInTag) {
            callback.accept(() -> {
                try {
                    unregisterRecipe(key);
                    Reflections.method$CraftRecipe$addToCraftingManager.invoke(fromBukkitRecipeMethod.invoke(null, cookingRecipe));
                    injectCookingRecipe(id, ceRecipe);
                } catch (Exception e) {
                    CraftEngine.instance().logger().warn("Failed to convert smelting recipe", e);
                }
            });
            this.injectedDataPackRecipes.add(key);
        }
        this.addInternalRecipe(id, ceRecipe);
    }

    private void handleDataPackSmithingTransform(Key id, VanillaSmithingTransformRecipe recipe, Consumer<Runnable> callback) {
        NamespacedKey key = new NamespacedKey(id.namespace(), id.value());
        ItemStack result = createResultStack(recipe.result());
        boolean hasCustomItemInTag;

        Set<Material> additionMaterials = new HashSet<>();
        Set<Holder<Key>> additionHolders = new HashSet<>();
        hasCustomItemInTag = readVanillaIngredients(false, recipe.addition(), additionMaterials::add, additionHolders::add);

        Set<Material> templateMaterials = new HashSet<>();
        Set<Holder<Key>> templateHolders = new HashSet<>();
        hasCustomItemInTag = readVanillaIngredients(hasCustomItemInTag, recipe.template(), templateMaterials::add, templateHolders::add);

        Set<Material> baseMaterials = new HashSet<>();
        Set<Holder<Key>> baseHolders = new HashSet<>();
        hasCustomItemInTag = readVanillaIngredients(hasCustomItemInTag, recipe.base(), baseMaterials::add, baseHolders::add);

        CustomSmithingTransformRecipe<ItemStack> ceRecipe = new CustomSmithingTransformRecipe<>(
                id,
                baseHolders.isEmpty() ? null : Ingredient.of(baseHolders),
                templateHolders.isEmpty() ? null : Ingredient.of(templateHolders),
                additionHolders.isEmpty() ? null : Ingredient.of(additionHolders),
                new CustomRecipeResult<>(new CloneableConstantItem(recipe.result().isCustom() ? Key.of("!internal:custom") : Key.of(recipe.result().id()), result), recipe.result().count()),
                true,
                List.of()
        );

        SmithingTransformRecipe transformRecipe = new SmithingTransformRecipe(key, result,
                new RecipeChoice.MaterialChoice(new ArrayList<>(templateMaterials)),
                new RecipeChoice.MaterialChoice(new ArrayList<>(baseMaterials)),
                new RecipeChoice.MaterialChoice(new ArrayList<>(additionMaterials))
        );

        if (hasCustomItemInTag) {
            callback.accept(() -> {
                try {
                    unregisterRecipe(key);
                    Reflections.method$CraftRecipe$addToCraftingManager.invoke(Reflections.method$CraftSmithingTransformRecipe$fromBukkitRecipe.invoke(null, transformRecipe));
                } catch (Exception e) {
                    CraftEngine.instance().logger().warn("Failed to convert transform recipe", e);
                }
            });
            this.injectedDataPackRecipes.add(key);
        }
        this.addInternalRecipe(id, ceRecipe);
    }

    private List<Material> tagToMaterials(Key tag) {
        Set<Material> materials = new HashSet<>();
        List<Holder<Key>> holders = this.plugin.itemManager().tagToVanillaItems(tag);
        if (holders != null) {
            for (Holder<Key> holder : holders) {
                materials.add(MaterialUtils.getMaterial(holder.value()));
            }
        }
        List<Holder<Key>> customItems = this.plugin.itemManager().tagToCustomItems(tag);
        if (customItems != null) {
            for (Holder<Key> holder : customItems) {
                this.plugin.itemManager().getCustomItem(holder.value()).ifPresent(it -> {
                    materials.add(MaterialUtils.getMaterial(it.material()));
                });
            }
        }
        return new ArrayList<>(materials);
    }

    private ItemStack createResultStack(RecipeResult result) {
        ItemStack itemStack;
        if (result.components() == null) {
            itemStack = new ItemStack(Objects.requireNonNull(MaterialUtils.getMaterial(result.id())));
            itemStack.setAmount(result.count());
        } else {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("id", result.id());
            jsonObject.addProperty("count", result.count());
            jsonObject.add("components", result.components());
            Object nmsStack = ItemObject.newItem(TagCompound.newTag(jsonObject.toString()));
            try {
                itemStack = (ItemStack) Reflections.method$CraftItemStack$asCraftMirror.invoke(null, nmsStack);
            } catch (Exception e) {
                this.plugin.logger().warn("Failed to create ItemStack mirror", e);
                return new ItemStack(Material.STICK);
            }
        }
        return itemStack;
    }

    private Key extractKeyFromResourceLocation(String input) {
        int prefixEndIndex = input.indexOf(':');
        String prefix = input.substring(0, prefixEndIndex);
        int lastSlashIndex = input.lastIndexOf('/');
        int lastDotIndex = input.lastIndexOf('.');
        String fileName = input.substring(lastSlashIndex + 1, lastDotIndex);
        return Key.of(prefix, fileName);
    }

    private static RecipeChoice ingredientToBukkitRecipeChoice(Ingredient<ItemStack> ingredient) {
        Set<Material> materials = new HashSet<>();
        for (Holder<Key> holder : ingredient.items()) {
            materials.add(getMaterialById(holder.value()));
        }
        return new RecipeChoice.MaterialChoice(new ArrayList<>(materials));
    }

    private static Material getMaterialById(Key key) {
        Material material = MaterialUtils.getMaterial(key);
        if (material != null) {
            return material;
        }
        Optional<CustomItem<ItemStack>> optionalItem = BukkitItemManager.instance().getCustomItem(key);
        return optionalItem.map(itemStackCustomItem -> MaterialUtils.getMaterial(itemStackCustomItem.material())).orElse(null);
    }

    private static List<Object> getIngredientLooks(List<Holder<Key>> holders) throws ReflectiveOperationException {
        List<Object> itemStacks = new ArrayList<>();
        for (Holder<Key> holder : holders) {
            ItemStack itemStack = BukkitItemManager.instance().getBuildableItem(holder.value()).get().buildItemStack(ItemBuildContext.EMPTY, 1);
            Object nmsStack = Reflections.method$CraftItemStack$asNMSCopy.invoke(null, itemStack);
            itemStacks.add(nmsStack);
        }
        return itemStacks;
    }

    private static void injectShapedRecipe(Key id, CustomShapedRecipe<ItemStack> recipe) throws ReflectiveOperationException {
        List<Ingredient<ItemStack>> actualIngredients = recipe.parsedPattern().ingredients()
                .stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        Object shapedRecipe = getNMSRecipe(id);
        recipeToMcRecipeHolder.put(recipe, shapedRecipe);
        if (VersionHelper.isVersionNewerThan1_20_2()) {
            shapedRecipe = Reflections.field$RecipeHolder$recipe.get(shapedRecipe);
        }

        if (VersionHelper.isVersionNewerThan1_21_2()) {
            Reflections.field$ShapedRecipe$placementInfo.set(shapedRecipe, null);
        }
        List<Object> ingredients = RecipeUtils.getIngredientsFromShapedRecipe(shapedRecipe);
        injectIngredients(ingredients, actualIngredients);
    }

    @SuppressWarnings("unchecked")
    private static void injectShapelessRecipe(Key id, CustomShapelessRecipe<ItemStack> recipe) throws ReflectiveOperationException {
        List<Ingredient<ItemStack>> actualIngredients = recipe.ingredientsInUse();

        Object shapelessRecipe = getNMSRecipe(id);
        recipeToMcRecipeHolder.put(recipe, shapelessRecipe);
        if (VersionHelper.isVersionNewerThan1_20_2()) {
            shapelessRecipe = Reflections.field$RecipeHolder$recipe.get(shapelessRecipe);
        }

        if (VersionHelper.isVersionNewerThan1_21_2()) {
            Reflections.field$ShapelessRecipe$placementInfo.set(shapelessRecipe, null);
        }
        List<Object> ingredients = (List<Object>) Reflections.field$ShapelessRecipe$ingredients.get(shapelessRecipe);
        injectIngredients(ingredients, actualIngredients);
    }

    private static void injectCookingRecipe(Key id, CustomCookingRecipe<ItemStack> recipe) throws ReflectiveOperationException {
        Ingredient<ItemStack> actualIngredient = recipe.ingredient();
        Object smeltingRecipe = getNMSRecipe(id);
        recipeToMcRecipeHolder.put(recipe, smeltingRecipe);
        if (VersionHelper.isVersionNewerThan1_20_2()) {
            smeltingRecipe = Reflections.field$RecipeHolder$recipe.get(smeltingRecipe);
        }

        Object ingredient;
        if (VersionHelper.isVersionNewerThan1_21_2()) {
            ingredient = Reflections.field$SingleItemRecipe$input.get(smeltingRecipe);
        } else {
            ingredient = Reflections.field$AbstractCookingRecipe$input.get(smeltingRecipe);
        }
        injectIngredients(List.of(ingredient), List.of(actualIngredient));
    }

    // recipe on 1.20.1 and holder on 1.20.2+
    private static Object getNMSRecipe(Key id) throws ReflectiveOperationException {
        if (VersionHelper.isVersionNewerThan1_21_2()) {
            Object resourceKey = Reflections.method$CraftRecipe$toMinecraft.invoke(null, new NamespacedKey(id.namespace(), id.value()));
            @SuppressWarnings("unchecked")
            Optional<Object> optional = (Optional<Object>) Reflections.method$RecipeManager$byKey.invoke(minecraftRecipeManager, resourceKey);
            if (optional.isEmpty()) {
                throw new IllegalArgumentException("Recipe " + id + " not found");
            }
            return optional.get();
        } else {
            Object resourceLocation = Reflections.method$ResourceLocation$fromNamespaceAndPath.invoke(null, id.namespace(), id.value());
            @SuppressWarnings("unchecked")
            Optional<Object> optional = (Optional<Object>) Reflections.method$RecipeManager$byKey.invoke(minecraftRecipeManager, resourceLocation);
            if (optional.isEmpty()) {
                throw new IllegalArgumentException("Recipe " + id + " not found");
            }
            return optional.get();
        }
    }

    private static void injectIngredients(List<Object> ingredients, List<Ingredient<ItemStack>> actualIngredients) throws ReflectiveOperationException {
        if (ingredients.size() != actualIngredients.size()) {
            throw new IllegalArgumentException("Ingredient count mismatch");
        }
        for (int i = 0; i < ingredients.size(); i++) {
            Object ingredient = ingredients.get(i);
            Ingredient<ItemStack> actualIngredient = actualIngredients.get(i);
            List<Object> items = getIngredientLooks(actualIngredient.items());
            if (VersionHelper.isVersionNewerThan1_21_4()) {
                Reflections.field$Ingredient$itemStacks1_21_4.set(ingredient, new HashSet<>(items));
            } else if (VersionHelper.isVersionNewerThan1_21_2()) {
                Reflections.field$Ingredient$itemStacks1_21_2.set(ingredient, items);
            } else {
                Object itemStackArray = Array.newInstance(Reflections.clazz$ItemStack, items.size());
                for (int j = 0; j < items.size(); j++) {
                    Array.set(itemStackArray, j, items.get(j));
                }
                Reflections.field$Ingredient$itemStacks1_20_1.set(ingredient, itemStackArray);
            }
            injectedIngredients.add(ingredient);
        }
    }

    // 1.20-1.21.2
    private static Object toMinecraftIngredient(Ingredient<ItemStack> ingredient) throws ReflectiveOperationException  {
        if (ingredient == null) {
            return Reflections.method$CraftRecipe$toIngredient.invoke(null, null, true);
        } else {
            RecipeChoice choice = ingredientToBukkitRecipeChoice(ingredient);
            return Reflections.method$CraftRecipe$toIngredient.invoke(null, choice, true);
        }
    }

    // 1.21.2+
    private static Optional<Object> toOptionalMinecraftIngredient(Ingredient<ItemStack> ingredient) throws ReflectiveOperationException {
        if (ingredient == null) {
            return Optional.empty();
        } else {
            RecipeChoice choice = ingredientToBukkitRecipeChoice(ingredient);
            Object mcIngredient = Reflections.method$CraftRecipe$toIngredient.invoke(null, choice, true);
            return Optional.of(mcIngredient);
        }
    }

    // 1.21.5+
    private static Object toTransmuteResult(ItemStack item) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        Object itemStack = Reflections.method$CraftItemStack$asNMSCopy.invoke(null, item);
        Object nmsItem = Reflections.method$ItemStack$getItem.invoke(itemStack);
        return Reflections.constructor$TransmuteResult.newInstance(nmsItem);
    }

    private static Object createMinecraftSmithingTransformRecipe(CustomSmithingTransformRecipe<ItemStack> ceRecipe) throws ReflectiveOperationException {
        if (VersionHelper.isVersionNewerThan1_21_5()) {
            return Reflections.constructor$SmithingTransformRecipe.newInstance(
                    toOptionalMinecraftIngredient(ceRecipe.template()),
                    toMinecraftIngredient(ceRecipe.base()),
                    toOptionalMinecraftIngredient(ceRecipe.addition()),
                    toTransmuteResult(ceRecipe.result(ItemBuildContext.EMPTY))
            );
        } else if (VersionHelper.isVersionNewerThan1_21_2()) {
            return Reflections.constructor$SmithingTransformRecipe.newInstance(
                    toOptionalMinecraftIngredient(ceRecipe.template()),
                    toOptionalMinecraftIngredient(ceRecipe.base()),
                    toOptionalMinecraftIngredient(ceRecipe.addition()),
                    Reflections.method$CraftItemStack$asNMSCopy.invoke(null, ceRecipe.result(ItemBuildContext.EMPTY))
            );
        } else if (VersionHelper.isVersionNewerThan1_20_2()) {
            return Reflections.constructor$SmithingTransformRecipe.newInstance(
                    toMinecraftIngredient(ceRecipe.template()),
                    toMinecraftIngredient(ceRecipe.base()),
                    toMinecraftIngredient(ceRecipe.addition()),
                    Reflections.method$CraftItemStack$asNMSCopy.invoke(null, ceRecipe.result(ItemBuildContext.EMPTY))
            );
        } else {
            return Reflections.constructor$SmithingTransformRecipe.newInstance(
                    Reflections.method$ResourceLocation$fromNamespaceAndPath.invoke(null, ceRecipe.id().namespace(), ceRecipe.id().value()),
                    toMinecraftIngredient(ceRecipe.template()),
                    toMinecraftIngredient(ceRecipe.base()),
                    toMinecraftIngredient(ceRecipe.addition()),
                    Reflections.method$CraftItemStack$asNMSCopy.invoke(null, ceRecipe.result(ItemBuildContext.EMPTY))
            );
        }
    }

    public Object getRecipeHolderByRecipe(Recipe<ItemStack> recipe) {
        return recipeToMcRecipeHolder.get(recipe);
    }

    public static Object minecraftRecipeManager() {
        return minecraftRecipeManager;
    }

    public static BukkitRecipeManager instance() {
        return instance;
    }
}
