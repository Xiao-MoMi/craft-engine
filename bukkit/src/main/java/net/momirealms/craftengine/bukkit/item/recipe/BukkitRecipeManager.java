package net.momirealms.craftengine.bukkit.item.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.item.CloneableConstantItem;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.reflection.bukkit.CraftBukkitReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistries;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistryOps;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.MaterialUtils;
import net.momirealms.craftengine.bukkit.util.RecipeUtils;
import net.momirealms.craftengine.core.item.*;
import net.momirealms.craftengine.core.item.recipe.*;
import net.momirealms.craftengine.core.item.recipe.Recipe;
import net.momirealms.craftengine.core.item.recipe.vanilla.*;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.*;
import org.bukkit.inventory.recipe.CookingBookCategory;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import java.io.Reader;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Consumer;

public class BukkitRecipeManager extends AbstractRecipeManager<ItemStack> {
    private static BukkitRecipeManager instance;

    // 将自定义配方转为“广义”配方，接受更加宽容的输入
    // 部分过程借助bukkit完成，部分直接通过nms方法注册
    private static final Map<Key, BukkitRecipeConvertor<? extends Recipe<ItemStack>>> MIXED_RECIPE_CONVERTORS = new HashMap<>();
    private static final List<Object> injectedIngredients = new ArrayList<>();
    private static final IdentityHashMap<Recipe<ItemStack>, Object> CE_RECIPE_2_NMS_HOLDER = new IdentityHashMap<>();
    private static Object nmsRecipeManager;

    private static void registerNMSSmithingRecipe(Object recipe) {
        try {
            CoreReflections.method$RecipeManager$addRecipe.invoke(nmsRecipeManager(), recipe);
        } catch (IllegalAccessException | InvocationTargetException e) {
            CraftEngine.instance().logger().warn("Failed to register smithing recipe", e);
        }
    }

    private static void registerBukkitShapedRecipe(Object recipe) {
        try {
            Object craftRecipe = CraftBukkitReflections.method$CraftShapedRecipe$fromBukkitRecipe.invoke(null, recipe);
            CraftBukkitReflections.method$CraftRecipe$addToCraftingManager.invoke(craftRecipe);
        } catch (IllegalAccessException | InvocationTargetException e) {
            CraftEngine.instance().logger().warn("Failed to register shaped recipe", e);
        }
    }

    private static void registerBukkitShapelessRecipe(Object recipe) {
        try {
            Object craftRecipe = CraftBukkitReflections.method$CraftShapelessRecipe$fromBukkitRecipe.invoke(null, recipe);
            CraftBukkitReflections.method$CraftRecipe$addToCraftingManager.invoke(craftRecipe);
        } catch (IllegalAccessException | InvocationTargetException e) {
            CraftEngine.instance().logger().warn("Failed to register shapeless recipe", e);
        }
    }

    private static void registerBukkitSmeltingRecipe(Object recipe) {
        try {
            Object craftRecipe = CraftBukkitReflections.method$CraftFurnaceRecipe$fromBukkitRecipe.invoke(null, recipe);
            CraftBukkitReflections.method$CraftRecipe$addToCraftingManager.invoke(craftRecipe);
        } catch (IllegalAccessException | InvocationTargetException e) {
            CraftEngine.instance().logger().warn("Failed to register smelting recipe", e);
        }
    }

    private static void registerBukkitSmokingRecipe(Object recipe) {
        try {
            Object craftRecipe = CraftBukkitReflections.method$CraftSmokingRecipe$fromBukkitRecipe.invoke(null, recipe);
            CraftBukkitReflections.method$CraftRecipe$addToCraftingManager.invoke(craftRecipe);
        } catch (IllegalAccessException | InvocationTargetException e) {
            CraftEngine.instance().logger().warn("Failed to register smoking recipe", e);
        }
    }

    private static void registerBukkitBlastingRecipe(Object recipe) {
        try {
            Object craftRecipe = CraftBukkitReflections.method$CraftBlastingRecipe$fromBukkitRecipe.invoke(null, recipe);
            CraftBukkitReflections.method$CraftRecipe$addToCraftingManager.invoke(craftRecipe);
        } catch (IllegalAccessException | InvocationTargetException e) {
            CraftEngine.instance().logger().warn("Failed to register blasting recipe", e);
        }
    }

    private static void registerBukkitCampfireRecipe(Object recipe) {
        try {
            Object craftRecipe = CraftBukkitReflections.method$CraftCampfireRecipe$fromBukkitRecipe.invoke(null, recipe);
            CraftBukkitReflections.method$CraftRecipe$addToCraftingManager.invoke(craftRecipe);
        } catch (IllegalAccessException | InvocationTargetException e) {
            CraftEngine.instance().logger().warn("Failed to register campfire recipe", e);
        }
    }

    private static void registerBukkitStoneCuttingRecipe(Object recipe) {
        try {
            Object craftRecipe = CraftBukkitReflections.method$CraftStonecuttingRecipe$fromBukkitRecipe.invoke(null, recipe);
            CraftBukkitReflections.method$CraftRecipe$addToCraftingManager.invoke(craftRecipe);
        } catch (IllegalAccessException | InvocationTargetException e) {
            CraftEngine.instance().logger().warn("Failed to register stonecutting recipe", e);
        }
    }

    static {
        MIXED_RECIPE_CONVERTORS.put(RecipeTypes.SMITHING_TRANSFORM, (BukkitRecipeConvertor<CustomSmithingTransformRecipe<ItemStack>>) (id, recipe) -> {
            try {
                Object nmsRecipe = createMinecraftSmithingTransformRecipe(recipe);
                if (VersionHelper.isOrAbove1_21_2()) {
                    nmsRecipe = CoreReflections.constructor$RecipeHolder.newInstance(
                            CraftBukkitReflections.method$CraftRecipe$toMinecraft.invoke(null, new NamespacedKey(id.namespace(), id.value())), nmsRecipe);
                } else if (VersionHelper.isOrAbove1_20_2()) {
                    nmsRecipe = CoreReflections.constructor$RecipeHolder.newInstance(KeyUtils.toResourceLocation(id), nmsRecipe);
                } else {
                    Object finalNmsRecipe0 = nmsRecipe;
                    return () -> registerNMSSmithingRecipe(finalNmsRecipe0);
                }
                Object finalNmsRecipe = nmsRecipe;
                return () -> registerNMSSmithingRecipe(finalNmsRecipe);
            } catch (InvalidRecipeIngredientException e) {
                throw e;
            } catch (Exception e) {
                CraftEngine.instance().logger().warn("Failed to convert smithing transform recipe", e);
                return null;
            }
        });
        // TODO DO NOT USE BUKKIT RECIPE AS BRIDGE IN FUTURE VERSIONS, WE SHOULD DIRECTLY CONSTRUCT THOSE NMS RECIPES
        MIXED_RECIPE_CONVERTORS.put(RecipeTypes.SHAPED, (BukkitRecipeConvertor<CustomShapedRecipe<ItemStack>>) (id, recipe) -> {
            ShapedRecipe shapedRecipe = new ShapedRecipe(new NamespacedKey(id.namespace(), id.value()), recipe.result(ItemBuildContext.EMPTY));
            if (recipe.group() != null) shapedRecipe.setGroup(Objects.requireNonNull(recipe.group()));
            if (recipe.category() != null) shapedRecipe.setCategory(CraftingBookCategory.valueOf(Objects.requireNonNull(recipe.category()).name()));
            shapedRecipe.shape(recipe.pattern().pattern());
            for (Map.Entry<Character, Ingredient<ItemStack>> entry : recipe.pattern().ingredients().entrySet()) {
                shapedRecipe.setIngredient(entry.getKey(), ingredientToBukkitRecipeChoice(entry.getValue()));
            }
            return () -> {
                registerBukkitShapedRecipe(shapedRecipe);
                injectShapedRecipe(id, recipe);
            };
        });
        MIXED_RECIPE_CONVERTORS.put(RecipeTypes.SHAPELESS, (BukkitRecipeConvertor<CustomShapelessRecipe<ItemStack>>) (id, recipe) -> {
            ShapelessRecipe shapelessRecipe = new ShapelessRecipe(new NamespacedKey(id.namespace(), id.value()), recipe.result(ItemBuildContext.EMPTY));
            if (recipe.group() != null) shapelessRecipe.setGroup(Objects.requireNonNull(recipe.group()));
            if (recipe.category() != null) shapelessRecipe.setCategory(CraftingBookCategory.valueOf(Objects.requireNonNull(recipe.category()).name()));
            for (Ingredient<ItemStack> ingredient : recipe.ingredientsInUse()) {
                shapelessRecipe.addIngredient(ingredientToBukkitRecipeChoice(ingredient));
            }
            return () -> {
                registerBukkitShapelessRecipe(shapelessRecipe);
                injectShapelessRecipe(id, recipe);
            };
        });
        MIXED_RECIPE_CONVERTORS.put(RecipeTypes.SMELTING, (BukkitRecipeConvertor<CustomSmeltingRecipe<ItemStack>>) (id, recipe) -> {
            FurnaceRecipe furnaceRecipe = new FurnaceRecipe(
                    new NamespacedKey(id.namespace(), id.value()), recipe.result(ItemBuildContext.EMPTY),
                    ingredientToBukkitRecipeChoice(recipe.ingredient()),
                    recipe.experience(), recipe.cookingTime()
            );
            if (recipe.group() != null) furnaceRecipe.setGroup(Objects.requireNonNull(recipe.group()));
            if (recipe.category() != null) furnaceRecipe.setCategory(CookingBookCategory.valueOf(Objects.requireNonNull(recipe.category()).name()));
            return () -> {
                registerBukkitSmeltingRecipe(furnaceRecipe);
                injectCookingRecipe(id, recipe);
            };
        });
        MIXED_RECIPE_CONVERTORS.put(RecipeTypes.SMOKING, (BukkitRecipeConvertor<CustomSmokingRecipe<ItemStack>>) (id, recipe) -> {
            SmokingRecipe smokingRecipe = new SmokingRecipe(
                    new NamespacedKey(id.namespace(), id.value()), recipe.result(ItemBuildContext.EMPTY),
                    ingredientToBukkitRecipeChoice(recipe.ingredient()),
                    recipe.experience(), recipe.cookingTime()
            );
            if (recipe.group() != null) smokingRecipe.setGroup(Objects.requireNonNull(recipe.group()));
            if (recipe.category() != null) smokingRecipe.setCategory(CookingBookCategory.valueOf(Objects.requireNonNull(recipe.category()).name()));
            return () -> {
                registerBukkitSmokingRecipe(smokingRecipe);
                injectCookingRecipe(id, recipe);
            };
        });
        MIXED_RECIPE_CONVERTORS.put(RecipeTypes.BLASTING, (BukkitRecipeConvertor<CustomBlastingRecipe<ItemStack>>) (id, recipe) -> {
            BlastingRecipe blastingRecipe = new BlastingRecipe(
                    new NamespacedKey(id.namespace(), id.value()), recipe.result(ItemBuildContext.EMPTY),
                    ingredientToBukkitRecipeChoice(recipe.ingredient()),
                    recipe.experience(), recipe.cookingTime()
            );
            if (recipe.group() != null) blastingRecipe.setGroup(Objects.requireNonNull(recipe.group()));
            if (recipe.category() != null) blastingRecipe.setCategory(CookingBookCategory.valueOf(Objects.requireNonNull(recipe.category()).name()));
            return () -> {
                registerBukkitBlastingRecipe(blastingRecipe);
                injectCookingRecipe(id, recipe);
            };
        });
        MIXED_RECIPE_CONVERTORS.put(RecipeTypes.CAMPFIRE_COOKING, (BukkitRecipeConvertor<CustomCampfireRecipe<ItemStack>>) (id, recipe) -> {
            CampfireRecipe campfireRecipe = new CampfireRecipe(
                    new NamespacedKey(id.namespace(), id.value()), recipe.result(ItemBuildContext.EMPTY),
                    ingredientToBukkitRecipeChoice(recipe.ingredient()),
                    recipe.experience(), recipe.cookingTime()
            );
            if (recipe.group() != null) campfireRecipe.setGroup(Objects.requireNonNull(recipe.group()));
            if (recipe.category() != null) campfireRecipe.setCategory(CookingBookCategory.valueOf(Objects.requireNonNull(recipe.category()).name()));
            return () -> {
                registerBukkitCampfireRecipe(campfireRecipe);
                injectCookingRecipe(id, recipe);
            };
        });
        MIXED_RECIPE_CONVERTORS.put(RecipeTypes.STONECUTTING, (BukkitRecipeConvertor<CustomStoneCuttingRecipe<ItemStack>>) (id, recipe) -> {
            List<ItemStack> itemStacks = new ArrayList<>();
            for (UniqueKey item : recipe.ingredient().items()) {
                itemStacks.add(BukkitItemManager.instance().buildItemStack(item.key(), null));
            }
            StonecuttingRecipe stonecuttingRecipe = new StonecuttingRecipe(
                    new NamespacedKey(id.namespace(), id.value()), recipe.result(ItemBuildContext.EMPTY),
                    new RecipeChoice.ExactChoice(itemStacks)
            );
            if (recipe.group() != null) stonecuttingRecipe.setGroup(Objects.requireNonNull(recipe.group()));
            return () -> {
                registerBukkitStoneCuttingRecipe(stonecuttingRecipe);
            };
        });
    }

    /*
     * 注册全过程：
     *
     * 0.准备阶段偷取flag以减少注册的性能开销
     * 1.先读取用户配置自定义配方
     * 2.延迟加载中为自定义配方生成转换为nms配方的任务
     * 3.读取全部的数据包配方并转换为自定义配方，对必要的含有tag配方添加先移除后注册nms配方的任务
     * 4.主线程完成剩余任务
     * 5.归还flag
     */

    private final BukkitCraftEngine plugin;
    private final RecipeEventListener recipeEventListener;
    private final CrafterEventListener crafterEventListener;
    // To optimize recipes loading, will return the flag later
    private Object stolenFeatureFlagSet;
    // Some delayed tasks on main thread
    private final List<Runnable> delayedTasksOnMainThread = new ArrayList<>();

    public BukkitRecipeManager(BukkitCraftEngine plugin) {
        instance = this;
        this.plugin = plugin;
        this.recipeEventListener = new RecipeEventListener(plugin, this, plugin.itemManager());
        this.crafterEventListener = VersionHelper.isOrAbove1_21() ? new CrafterEventListener(plugin, this, plugin.itemManager()) : null;
        try {
            nmsRecipeManager = CoreReflections.method$MinecraftServer$getRecipeManager.invoke(CoreReflections.method$MinecraftServer$getServer.invoke(null));
        } catch (ReflectiveOperationException e) {
            plugin.logger().warn("Failed to get minecraft recipe manager", e);
        }
    }

    public Object nmsRecipeHolderByRecipe(Recipe<ItemStack> recipe) {
        if (super.isReloading) return null;
        return CE_RECIPE_2_NMS_HOLDER.get(recipe);
    }

    public static Object nmsRecipeManager() {
        return nmsRecipeManager;
    }

    public static BukkitRecipeManager instance() {
        return instance;
    }

    @Override
    public void delayedInit() {
        Bukkit.getPluginManager().registerEvents(this.recipeEventListener, this.plugin.javaPlugin());
        if (this.crafterEventListener != null) {
            Bukkit.getPluginManager().registerEvents(this.crafterEventListener, this.plugin.javaPlugin());
        }
    }

    @Override
    public void load() {
        if (!Config.enableRecipeSystem()) return;
        super.isReloading = true;
        if (VersionHelper.isOrAbove1_21_2()) {
            try {
                this.stolenFeatureFlagSet = CoreReflections.field$RecipeManager$featureflagset.get(nmsRecipeManager);
                CoreReflections.field$RecipeManager$featureflagset.set(nmsRecipeManager, null);
            } catch (ReflectiveOperationException e) {
                this.plugin.logger().warn("Failed to steal featureflagset", e);
            }
        }
    }

    @Override
    public void unload() {
        if (!Config.enableRecipeSystem()) return;
        super.unload();
        if (VersionHelper.isOrAbove1_21_2()) {
            if (Bukkit.getServer().isStopping()) {
                try {
                    CoreReflections.method$RecipeManager$finalizeRecipeLoading.invoke(nmsRecipeManager);
                } catch (ReflectiveOperationException e) {
                    this.plugin.logger().warn("Failed to unregister recipes", e);
                }
            } else {
                this.plugin.scheduler().executeSync(() -> {
                    try {
                        CoreReflections.method$RecipeManager$finalizeRecipeLoading.invoke(nmsRecipeManager);
                    } catch (ReflectiveOperationException e) {
                        this.plugin.logger().warn("Failed to unregister recipes", e);
                    }
                });
            }
        }
    }

    @Override
    public void delayedLoad() {
        if (!Config.enableRecipeSystem()) return;
        this.injectDataPackRecipes();
    }

    @Override
    public void disable() {
        unload();
        CE_RECIPE_2_NMS_HOLDER.clear();
        HandlerList.unregisterAll(this.recipeEventListener);
        if (this.crafterEventListener != null) {
            HandlerList.unregisterAll(this.crafterEventListener);
        }
    }

    @Override
    protected void unregisterPlatformRecipe(Key key) {
        unregisterNMSRecipe(new NamespacedKey(key.namespace(), key.value()));
    }

    @Override
    protected void registerPlatformRecipe(Key id, Recipe<ItemStack> recipe) {
        try {
            Runnable converted = findNMSRecipeConvertor(recipe).convert(id, recipe);
            if (converted != null) {
                this.delayedTasksOnMainThread.add(converted);
            }
        } catch (InvalidRecipeIngredientException e) {
            throw new LocalizedResourceConfigException("warning.config.recipe.invalid_ingredient", e.ingredient());
        } catch (Exception e) {
            this.plugin.logger().warn("Failed to convert recipe " + id, e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Recipe<ItemStack>> BukkitRecipeConvertor<T> findNMSRecipeConvertor(T recipe) {
        return (BukkitRecipeConvertor<T>) MIXED_RECIPE_CONVERTORS.get(recipe.type());
    }

    private void unregisterNMSRecipe(NamespacedKey key) {
        try {
            if (VersionHelper.isOrAbove1_21_2()) {
                Object recipeMap = CoreReflections.field$RecipeManager$recipes.get(nmsRecipeManager);
                CoreReflections.method$RecipeMap$removeRecipe.invoke(recipeMap, CraftBukkitReflections.method$CraftRecipe$toMinecraft.invoke(null, key));
            } else {
                Bukkit.removeRecipe(key);
            }
        } catch (ReflectiveOperationException e) {
            CraftEngine.instance().logger().warn("Failed to unregister nms recipes", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void injectDataPackRecipes() {
        try {
            Object fileToIdConverter = CoreReflections.method$FileToIdConverter$json.invoke(null, VersionHelper.isOrAbove1_21() ? "recipe" : "recipes");
            Object minecraftServer = CoreReflections.method$MinecraftServer$getServer.invoke(null);
            Object packRepository = CoreReflections.method$MinecraftServer$getPackRepository.invoke(minecraftServer);
            List<Object> selected = (List<Object>) CoreReflections.field$PackRepository$selected.get(packRepository);
            List<Object> packResources = new ArrayList<>();
            for (Object pack : selected) {
                packResources.add(CoreReflections.method$Pack$open.invoke(pack));
            }

            boolean hasDisabledAny = !Config.disabledVanillaRecipes().isEmpty();
            try (AutoCloseable resourceManager = (AutoCloseable) CoreReflections.constructor$MultiPackResourceManager.newInstance(CoreReflections.instance$PackType$SERVER_DATA, packResources)) {
                Map<Object, Object> scannedResources = (Map<Object, Object>) CoreReflections.method$FileToIdConverter$listMatchingResources.invoke(fileToIdConverter, resourceManager);
                for (Map.Entry<Object, Object> entry : scannedResources.entrySet()) {
                    Key id = extractKeyFromResourceLocation(entry.getKey().toString());
                    // now CraftEngine takes over everything
//                    // Maybe it's unregistered by other plugins
//                    if (Bukkit.getRecipe(new NamespacedKey(id.namespace(), id.value())) == null) {
//                        continue;
//                    }
                    if (Config.disableAllVanillaRecipes()) {
                        this.delayedTasksOnMainThread.add(() -> unregisterPlatformRecipe(id));
                        continue;
                    }
                    if (hasDisabledAny && Config.disabledVanillaRecipes().contains(id)) {
                        this.delayedTasksOnMainThread.add(() -> unregisterPlatformRecipe(id));
                        continue;
                    }
                    markAsDataPackRecipe(id);
                    Reader reader = (Reader) CoreReflections.method$Resource$openAsReader.invoke(entry.getValue());
                    JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
                    String type = jsonObject.get("type").getAsString();
                    switch (type) {
                        case "minecraft:crafting_shaped" -> {
                            VanillaShapedRecipe recipe = this.recipeReader.readShaped(jsonObject);
                            handleDataPackShapedRecipe(id, recipe, (this.delayedTasksOnMainThread::add));
                        }
                        case "minecraft:crafting_shapeless" -> {
                            VanillaShapelessRecipe recipe = this.recipeReader.readShapeless(jsonObject);
                            handleDataPackShapelessRecipe(id, recipe, (this.delayedTasksOnMainThread::add));
                        }
                        case "minecraft:smelting" -> {
                            VanillaSmeltingRecipe recipe = this.recipeReader.readSmelting(jsonObject);
                            handleDataPackCookingRecipe(id, recipe, CustomSmeltingRecipe::new, (this.delayedTasksOnMainThread::add));
                        }
                        case "minecraft:blasting" -> {
                            VanillaBlastingRecipe recipe = this.recipeReader.readBlasting(jsonObject);
                            handleDataPackCookingRecipe(id, recipe, CustomBlastingRecipe::new, (this.delayedTasksOnMainThread::add));
                        }
                        case "minecraft:smoking" -> {
                            VanillaSmokingRecipe recipe = this.recipeReader.readSmoking(jsonObject);
                            handleDataPackCookingRecipe(id, recipe, CustomSmokingRecipe::new, (this.delayedTasksOnMainThread::add));
                        }
                        case "minecraft:campfire_cooking" -> {
                            VanillaCampfireRecipe recipe = this.recipeReader.readCampfire(jsonObject);
                            handleDataPackCookingRecipe(id, recipe, CustomCampfireRecipe::new, (this.delayedTasksOnMainThread::add));
                        }
                        case "minecraft:smithing_transform" -> {
                            VanillaSmithingTransformRecipe recipe = this.recipeReader.readSmithingTransform(jsonObject);
                            handleDataPackSmithingTransform(id, recipe, (this.delayedTasksOnMainThread::add));
                        }
                        case "minecraft:stonecutting" -> {
                            VanillaStoneCuttingRecipe recipe = this.recipeReader.readStoneCutting(jsonObject);
                            handleDataPackStoneCuttingRecipe(id, recipe);
                        }
                    }
                }
            }
        } catch (Exception e) {
            this.plugin.logger().warn("Failed to read data pack recipes", e);
        }
    }

    @Override
    public void runDelayedSyncTasks() {
        if (!Config.enableRecipeSystem()) return;
        try {
            // run delayed tasks
            for (Runnable r : this.delayedTasksOnMainThread) {
                r.run();
            }
            this.delayedTasksOnMainThread.clear();

            // give flags back on 1.21.2+
            if (VersionHelper.isOrAbove1_21_2() && this.stolenFeatureFlagSet != null) {
                CoreReflections.field$RecipeManager$featureflagset.set(nmsRecipeManager(), this.stolenFeatureFlagSet);
                this.stolenFeatureFlagSet = null;
            }

            // refresh recipes
            if (VersionHelper.isOrAbove1_21_2()) {
                CoreReflections.method$RecipeManager$finalizeRecipeLoading.invoke(nmsRecipeManager());
            }

            // send to players
            CoreReflections.method$DedicatedPlayerList$reloadRecipes.invoke(CraftBukkitReflections.field$CraftServer$playerList.get(Bukkit.getServer()));

            // now we need to remove the fake `exact`
            if (VersionHelper.isOrAbove1_21_4()) {
                for (Object ingredient : injectedIngredients) {
                    CoreReflections.field$Ingredient$itemStacks1_21_4.set(ingredient, null);
                }
            } else if (VersionHelper.isOrAbove1_21_2()) {
                for (Object ingredient : injectedIngredients) {
                    CoreReflections.field$Ingredient$itemStacks1_21_2.set(ingredient, null);
                }
            }

            // clear cache
            injectedIngredients.clear();

            CE_RECIPE_2_NMS_HOLDER.clear();
            // create mappings
            for (Map.Entry<Key, Recipe<ItemStack>> entry : this.byId.entrySet()) {
                Optional<Object> nmsRecipe = getOptionalNMSRecipe(entry.getKey());
                nmsRecipe.ifPresent(o -> CE_RECIPE_2_NMS_HOLDER.put(entry.getValue(), o));
            }

            super.isReloading = false;
        } catch (Exception e) {
            this.plugin.logger().warn("Failed to run delayed recipe tasks", e);
        }
    }

    private void handleDataPackStoneCuttingRecipe(Key id, VanillaStoneCuttingRecipe recipe) {
        ItemStack result = createDataPackResultStack(recipe.result());
        Set<UniqueKey> holders = new HashSet<>();
        for (String item : recipe.ingredient()) {
            if (item.charAt(0) == '#') {
                Key tag = Key.from(item.substring(1));
                holders.addAll(this.plugin.itemManager().tagToItems(tag));
            } else {
                holders.add(UniqueKey.create(Key.from(item)));
            }
        }
        CustomStoneCuttingRecipe<ItemStack> ceRecipe = new CustomStoneCuttingRecipe<>(
                id, recipe.group(), Ingredient.of(holders),
                new CustomRecipeResult<>(new CloneableConstantItem(recipe.result().isCustom() ? Key.of("!internal:custom") : Key.of(recipe.result().id()), result), recipe.result().count())
        );
        this.registerInternalRecipe(id, ceRecipe);
    }

    private void handleDataPackShapelessRecipe(Key id, VanillaShapelessRecipe recipe, Consumer<Runnable> callback) {
        NamespacedKey key = new NamespacedKey(id.namespace(), id.value());
        ItemStack result = createDataPackResultStack(recipe.result());
        boolean hasCustomItemInTag = false;
        List<Ingredient<ItemStack>> ingredientList = new ArrayList<>();
        for (List<String> list : recipe.ingredients()) {
            Set<UniqueKey> holders = new HashSet<>();
            for (String item : list) {
                if (item.charAt(0) == '#') {
                    Key tag = Key.of(item.substring(1));
                    if (!hasCustomItemInTag) {
                        if (!plugin.itemManager().tagToCustomItems(tag).isEmpty()) {
                            hasCustomItemInTag = true;
                        }
                    }
                    holders.addAll(plugin.itemManager().tagToItems(tag));
                } else {
                    holders.add(UniqueKey.create(Key.from(item)));
                }
            }
            ingredientList.add(Ingredient.of(holders));
        }
        CustomShapelessRecipe<ItemStack> ceRecipe = new CustomShapelessRecipe<>(
                id, recipe.category(), recipe.group(), ingredientList,
                new CustomRecipeResult<>(new CloneableConstantItem(recipe.result().isCustom() ? Key.of("!internal:custom") : Key.of(recipe.result().id()), result), recipe.result().count())
        );
        if (hasCustomItemInTag) {
            Runnable converted = findNMSRecipeConvertor(ceRecipe).convert(id, ceRecipe);
            callback.accept(() -> {
                unregisterNMSRecipe(key);
                converted.run();
            });
        }
        this.registerInternalRecipe(id, ceRecipe);
    }

    private void handleDataPackShapedRecipe(Key id, VanillaShapedRecipe recipe, Consumer<Runnable> callback) {
        NamespacedKey key = new NamespacedKey(id.namespace(), id.value());
        ItemStack result = createDataPackResultStack(recipe.result());
        boolean hasCustomItemInTag = false;
        Map<Character, Ingredient<ItemStack>> ingredients = new HashMap<>();
        for (Map.Entry<Character, List<String>> entry : recipe.ingredients().entrySet()) {
            Set<UniqueKey> holders = new HashSet<>();
            for (String item : entry.getValue()) {
                if (item.charAt(0) == '#') {
                    Key tag = Key.from(item.substring(1));
                    if (!hasCustomItemInTag) {
                        if (!plugin.itemManager().tagToCustomItems(tag).isEmpty()) {
                            hasCustomItemInTag = true;
                        }
                    }
                    holders.addAll(plugin.itemManager().tagToItems(tag));
                } else {
                    holders.add(UniqueKey.create(Key.from(item)));
                }
            }
            ingredients.put(entry.getKey(), Ingredient.of(holders));
        }
        CustomShapedRecipe<ItemStack> ceRecipe = new CustomShapedRecipe<>(
                id, recipe.category(), recipe.group(),
                new CustomShapedRecipe.Pattern<>(recipe.pattern(), ingredients),
                new CustomRecipeResult<>(new CloneableConstantItem(recipe.result().isCustom() ? Key.of("!internal:custom") : Key.of(recipe.result().id()), result), recipe.result().count())
        );
        if (hasCustomItemInTag) {
            Runnable converted = findNMSRecipeConvertor(ceRecipe).convert(id, ceRecipe);
            callback.accept(() -> {
                unregisterNMSRecipe(key);
                converted.run();
            });
        }
        this.registerInternalRecipe(id, ceRecipe);
    }

    private void handleDataPackCookingRecipe(Key id,
                                             VanillaCookingRecipe recipe,
                                             HeptaFunction<Key, CookingRecipeCategory, String, Ingredient<ItemStack>, Integer, Float, CustomRecipeResult<ItemStack>, CustomCookingRecipe<ItemStack>> constructor2,
                                             Consumer<Runnable> callback) {
        NamespacedKey key = new NamespacedKey(id.namespace(), id.value());
        ItemStack result = createDataPackResultStack(recipe.result());
        Set<UniqueKey> holders = new HashSet<>();
        boolean hasCustomItemInTag = readVanillaIngredients(false, recipe.ingredient(), holders::add);
        CustomCookingRecipe<ItemStack> ceRecipe = constructor2.apply(
                id, recipe.category(), recipe.group(),
                Ingredient.of(holders),
                recipe.cookingTime(), recipe.experience(),
                new CustomRecipeResult<>(new CloneableConstantItem(recipe.result().isCustom() ? Key.of("!internal:custom") : Key.of(recipe.result().id()), result), recipe.result().count())
        );
        if (hasCustomItemInTag) {
            Runnable converted = findNMSRecipeConvertor(ceRecipe).convert(id, ceRecipe);
            callback.accept(() -> {
                unregisterNMSRecipe(key);
                converted.run();
            });
        }
        this.registerInternalRecipe(id, ceRecipe);
    }

    private void handleDataPackSmithingTransform(Key id, VanillaSmithingTransformRecipe recipe, Consumer<Runnable> callback) {
        NamespacedKey key = new NamespacedKey(id.namespace(), id.value());
        ItemStack result = createDataPackResultStack(recipe.result());
        boolean hasCustomItemInTag;

        Set<UniqueKey> additionHolders = new HashSet<>();
        hasCustomItemInTag = readVanillaIngredients(false, recipe.addition(), additionHolders::add);
        Set<UniqueKey> templateHolders = new HashSet<>();
        hasCustomItemInTag = readVanillaIngredients(hasCustomItemInTag, recipe.template(), templateHolders::add);
        Set<UniqueKey> baseHolders = new HashSet<>();
        hasCustomItemInTag = readVanillaIngredients(hasCustomItemInTag, recipe.base(), baseHolders::add);

        CustomSmithingTransformRecipe<ItemStack> ceRecipe = new CustomSmithingTransformRecipe<>(
                id,
                baseHolders.isEmpty() ? null : Ingredient.of(baseHolders),
                templateHolders.isEmpty() ? null : Ingredient.of(templateHolders),
                additionHolders.isEmpty() ? null : Ingredient.of(additionHolders),
                new CustomRecipeResult<>(new CloneableConstantItem(recipe.result().isCustom() ? Key.of("!internal:custom") : Key.of(recipe.result().id()), result), recipe.result().count()),
                true,
                List.of()
        );

        if (hasCustomItemInTag) {
            Runnable converted = findNMSRecipeConvertor(ceRecipe).convert(id, ceRecipe);
            callback.accept(() -> {
                unregisterNMSRecipe(key);
                converted.run();
            });
        }
        this.registerInternalRecipe(id, ceRecipe);
    }

    private boolean readVanillaIngredients(boolean hasCustomItemInTag, List<String> ingredients, Consumer<UniqueKey> holderConsumer) {
        for (String item : ingredients) {
            if (item.charAt(0) == '#') {
                Key tag = Key.from(item.substring(1));
                if (!hasCustomItemInTag) {
                    if (!this.plugin.itemManager().tagToCustomItems(tag).isEmpty()) {
                        hasCustomItemInTag = true;
                    }
                }
                for (UniqueKey holder : this.plugin.itemManager().tagToItems(tag)) {
                    holderConsumer.accept(holder);
                }
            } else {
                holderConsumer.accept(UniqueKey.create(Key.from(item)));
            }
        }
        return hasCustomItemInTag;
    }

    private ItemStack createDataPackResultStack(RecipeResult result) {
        ItemStack itemStack;
        if (result.components() == null) {
            itemStack = new ItemStack(Objects.requireNonNull(MaterialUtils.getMaterial(result.id())));
            itemStack.setAmount(result.count());
        } else {
            // 低版本无法应用nbt或组件,所以这里是1.20.5+
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("id", result.id());
            jsonObject.addProperty("count", result.count());
            jsonObject.add("components", result.components());
            Object nmsStack = CoreReflections.instance$ItemStack$CODEC.parse(MRegistryOps.JSON, jsonObject)
                    .resultOrPartial((itemId) -> plugin.logger().severe("Tried to load invalid item: '" + itemId + "'")).orElse(null);
            if (nmsStack == null) {
                return new ItemStack(Material.STONE);
            }
            try {
                itemStack = FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(nmsStack);
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
        for (UniqueKey holder : ingredient.items()) {
            materials.add(getMaterialById(holder.key()));
        }
        return new RecipeChoice.MaterialChoice(new ArrayList<>(materials));
    }

    private static Material getMaterialById(Key key) {
        Material material = MaterialUtils.getMaterial(key);
        if (material != null) {
            return material;
        }
        Optional<CustomItem<ItemStack>> optionalItem = BukkitItemManager.instance().getCustomItem(key);
        return optionalItem.map(itemStackCustomItem -> MaterialUtils.getMaterial(itemStackCustomItem.material())).orElseThrow(() -> new InvalidRecipeIngredientException(key.asString()));
    }

    private static List<Object> getIngredientLooks(List<UniqueKey> holders) {
        List<Object> itemStacks = new ArrayList<>();
        for (UniqueKey holder : holders) {
            Optional<? extends BuildableItem<ItemStack>> buildableItem = BukkitItemManager.instance().getBuildableItem(holder.key());
            if (buildableItem.isPresent()) {
                ItemStack itemStack = buildableItem.get().buildItemStack(ItemBuildContext.EMPTY, 1);
                Object nmsStack = FastNMS.INSTANCE.method$CraftItemStack$asNMSCopy(itemStack);
                itemStacks.add(nmsStack);
            } else {
                Item<ItemStack> barrier = BukkitItemManager.instance().createWrappedItem(ItemKeys.BARRIER, null);
                barrier.customNameJson(AdventureHelper.componentToJson(Component.text(holder.key().asString()).color(NamedTextColor.RED)));
            }
        }
        return itemStacks;
    }

    // 无论是什么注入什么配方类型的方法，其本质都是注入ingredient
    private static void injectShapedRecipe(Key id, CustomShapedRecipe<ItemStack> recipe) {
        try {
            List<Ingredient<ItemStack>> actualIngredients = recipe.parsedPattern().ingredients()
                    .stream()
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();

            Object shapedRecipe = getOptionalNMSRecipe(id).get();
            if (VersionHelper.isOrAbove1_20_2()) {
                shapedRecipe = CoreReflections.field$RecipeHolder$recipe.get(shapedRecipe);
            }

            if (VersionHelper.isOrAbove1_21_2()) {
                CoreReflections.field$ShapedRecipe$placementInfo.set(shapedRecipe, null);
            }

            List<Object> ingredients = RecipeUtils.getIngredientsFromShapedRecipe(shapedRecipe);
            injectIngredients(ingredients, actualIngredients);
        } catch (ReflectiveOperationException e) {
            CraftEngine.instance().logger().warn("Failed to inject shaped recipe", e);
        }
    }

    private static void injectShapelessRecipe(Key id, CustomShapelessRecipe<ItemStack> recipe) {
        try {
            List<Ingredient<ItemStack>> actualIngredients = recipe.ingredientsInUse();

            Object shapelessRecipe = getOptionalNMSRecipe(id).get();
            if (VersionHelper.isOrAbove1_20_2()) {
                shapelessRecipe = CoreReflections.field$RecipeHolder$recipe.get(shapelessRecipe);
            }

            if (VersionHelper.isOrAbove1_21_2()) {
                CoreReflections.field$ShapelessRecipe$placementInfo.set(shapelessRecipe, null);
            }
            @SuppressWarnings("unchecked")
            List<Object> ingredients = (List<Object>) CoreReflections.field$ShapelessRecipe$ingredients.get(shapelessRecipe);
            injectIngredients(ingredients, actualIngredients);
        } catch (ReflectiveOperationException e) {
            CraftEngine.instance().logger().warn("Failed to inject shapeless recipe", e);
        }
    }

    private static void injectCookingRecipe(Key id, CustomCookingRecipe<ItemStack> recipe) {
        try {
            Ingredient<ItemStack> actualIngredient = recipe.ingredient();
            Object smeltingRecipe = getOptionalNMSRecipe(id).get();
            if (VersionHelper.isOrAbove1_20_2()) {
                smeltingRecipe = CoreReflections.field$RecipeHolder$recipe.get(smeltingRecipe);
            }

            Object ingredient;
            if (VersionHelper.isOrAbove1_21_2()) {
                ingredient = CoreReflections.field$SingleItemRecipe$input.get(smeltingRecipe);
            } else {
                ingredient = CoreReflections.field$AbstractCookingRecipe$input.get(smeltingRecipe);
            }
            injectIngredients(List.of(ingredient), List.of(actualIngredient));
        } catch (ReflectiveOperationException e) {
            CraftEngine.instance().logger().warn("Failed to inject cooking recipe", e);
        }
    }

    // 获取nms配方，请注意1.20.1获取配方本身，而1.20.2+获取的是配方的holder
    // recipe on 1.20.1 and holder on 1.20.2+
    private static Optional<Object> getOptionalNMSRecipe(Key id) throws ReflectiveOperationException {
        if (VersionHelper.isOrAbove1_21_2()) {
            Object resourceKey = FastNMS.INSTANCE.method$ResourceKey$create(MRegistries.RECIPE, KeyUtils.toResourceLocation(id));
            @SuppressWarnings("unchecked")
            Optional<Object> optional = (Optional<Object>) CoreReflections.method$RecipeManager$byKey.invoke(nmsRecipeManager, resourceKey);
            return optional;
        } else {
            Object resourceLocation = KeyUtils.toResourceLocation(id);
            @SuppressWarnings("unchecked")
            Optional<Object> optional = (Optional<Object>) CoreReflections.method$RecipeManager$byKey.invoke(nmsRecipeManager, resourceLocation);
            return optional;
        }
    }

    // 注入原料，这个方法受不同服务端fork和版本影响极大，需要每个版本测试
    // 此过程是为了避免自己处理“广义”配方与客户端的注册通讯
    private static void injectIngredients(List<Object> fakeIngredients, List<Ingredient<ItemStack>> actualIngredients) throws ReflectiveOperationException {
        if (fakeIngredients.size() != actualIngredients.size()) {
            throw new IllegalArgumentException("Ingredient count mismatch");
        }
        for (int i = 0; i < fakeIngredients.size(); i++) {
            Object ingredient = fakeIngredients.get(i);
            Ingredient<ItemStack> actualIngredient = actualIngredients.get(i);
            List<Object> items = getIngredientLooks(actualIngredient.items());
            if (VersionHelper.isOrAbove1_21_4()) {
                CoreReflections.field$Ingredient$itemStacks1_21_4.set(ingredient, new HashSet<>(items));
            } else if (VersionHelper.isOrAbove1_21_2()) {
                CoreReflections.field$Ingredient$itemStacks1_21_2.set(ingredient, items);
            } else {
                Object itemStackArray = Array.newInstance(CoreReflections.clazz$ItemStack, items.size());
                for (int j = 0; j < items.size(); j++) {
                    Array.set(itemStackArray, j, items.get(j));
                }
                CoreReflections.field$Ingredient$itemStacks1_20_1.set(ingredient, itemStackArray);
            }
            injectedIngredients.add(ingredient);
        }
    }

    // 1.20-1.21.2
    private static Object toMinecraftIngredient(Ingredient<ItemStack> ingredient) throws ReflectiveOperationException  {
        if (ingredient == null) {
            return CraftBukkitReflections.method$CraftRecipe$toIngredient.invoke(null, null, true);
        } else {
            RecipeChoice choice = ingredientToBukkitRecipeChoice(ingredient);
            return CraftBukkitReflections.method$CraftRecipe$toIngredient.invoke(null, choice, true);
        }
    }

    // 1.21.2+
    private static Optional<Object> toOptionalMinecraftIngredient(Ingredient<ItemStack> ingredient) throws ReflectiveOperationException {
        if (ingredient == null) {
            return Optional.empty();
        } else {
            RecipeChoice choice = ingredientToBukkitRecipeChoice(ingredient);
            Object mcIngredient = CraftBukkitReflections.method$CraftRecipe$toIngredient.invoke(null, choice, true);
            return Optional.of(mcIngredient);
        }
    }

    // 1.21.5+
    private static Object toTransmuteResult(ItemStack item) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        Object itemStack = FastNMS.INSTANCE.method$CraftItemStack$asNMSCopy(item);
        Object nmsItem = CoreReflections.method$ItemStack$getItem.invoke(itemStack);
        return CoreReflections.constructor$TransmuteResult.newInstance(nmsItem);
    }

    // create nms smithing recipe for different versions
    private static Object createMinecraftSmithingTransformRecipe(CustomSmithingTransformRecipe<ItemStack> recipe) throws ReflectiveOperationException {
        if (VersionHelper.isOrAbove1_21_5()) {
            return CoreReflections.constructor$SmithingTransformRecipe.newInstance(
                    toOptionalMinecraftIngredient(recipe.template()),
                    toMinecraftIngredient(recipe.base()),
                    toOptionalMinecraftIngredient(recipe.addition()),
                    toTransmuteResult(recipe.result(ItemBuildContext.EMPTY))
            );
        } else if (VersionHelper.isOrAbove1_21_2()) {
            return CoreReflections.constructor$SmithingTransformRecipe.newInstance(
                    toOptionalMinecraftIngredient(recipe.template()),
                    toOptionalMinecraftIngredient(recipe.base()),
                    toOptionalMinecraftIngredient(recipe.addition()),
                    FastNMS.INSTANCE.method$CraftItemStack$asNMSCopy(recipe.result(ItemBuildContext.EMPTY))
            );
        } else if (VersionHelper.isOrAbove1_20_2()) {
            return CoreReflections.constructor$SmithingTransformRecipe.newInstance(
                    toMinecraftIngredient(recipe.template()),
                    toMinecraftIngredient(recipe.base()),
                    toMinecraftIngredient(recipe.addition()),
                    FastNMS.INSTANCE.method$CraftItemStack$asNMSCopy(recipe.result(ItemBuildContext.EMPTY))
            );
        } else {
            return CoreReflections.constructor$SmithingTransformRecipe.newInstance(
                    KeyUtils.toResourceLocation(recipe.id()),
                    toMinecraftIngredient(recipe.template()),
                    toMinecraftIngredient(recipe.base()),
                    toMinecraftIngredient(recipe.addition()),
                    FastNMS.INSTANCE.method$CraftItemStack$asNMSCopy(recipe.result(ItemBuildContext.EMPTY))
            );
        }
    }
}
