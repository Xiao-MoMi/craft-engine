package net.momirealms.craftengine.bukkit.item.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.papermc.paper.potion.PotionMix;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.injector.RecipeInjector;
import net.momirealms.craftengine.bukkit.plugin.reflection.ReflectionInitException;
import net.momirealms.craftengine.bukkit.plugin.reflection.bukkit.CraftBukkitReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistries;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.item.BuildableItem;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.item.recipe.*;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.*;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import java.io.Reader;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

// todo 在folia上替换recipe map使其线程安全
public class BukkitRecipeManager extends AbstractRecipeManager<ItemStack> {
    private static BukkitRecipeManager instance;

    private static final Consumer<Key> MINECRAFT_RECIPE_REMOVER = VersionHelper.isOrAbove1_21_2() ?
            (id -> {
                Object resourceKey = toRecipeResourceKey(id);
                FastNMS.INSTANCE.method$RecipeMap$removeRecipe(FastNMS.INSTANCE.field$RecipeManager$recipes(minecraftRecipeManager()), resourceKey);
            }) :
            (id -> {
                Object resourceLocation = KeyUtils.toResourceLocation(id);
                FastNMS.INSTANCE.method$RecipeManager$removeRecipe(minecraftRecipeManager(), resourceLocation);
            });
    private static final BiFunction<Key, Object, Object> MINECRAFT_RECIPE_ADDER =
            VersionHelper.isOrAbove1_21_2() ?
            (id, recipe) -> {
                Object resourceKey = toRecipeResourceKey(id);
                Object recipeHolder = FastNMS.INSTANCE.constructor$RecipeHolder(resourceKey, recipe);
                FastNMS.INSTANCE.method$RecipeManager$addRecipe(minecraftRecipeManager(), recipeHolder);
                return recipeHolder;
            } :
            VersionHelper.isOrAbove1_20_2() ?
            (id, recipe) -> {
                Object resourceLocation = KeyUtils.toResourceLocation(id);
                Object recipeHolder = FastNMS.INSTANCE.constructor$RecipeHolder(resourceLocation, recipe);
                FastNMS.INSTANCE.method$RecipeManager$addRecipe(minecraftRecipeManager(), recipeHolder);
                return recipeHolder;
            } :
            (id, recipe) -> {
                FastNMS.INSTANCE.method$RecipeManager$addRecipe(minecraftRecipeManager(), recipe);
                return recipe;
            };

    private static final Map<Key, Function<Recipe<ItemStack>, Object>> ADD_RECIPE_FOR_MINECRAFT_RECIPE_HOLDER = Map.of(
            RecipeSerializers.SHAPED, recipe -> {
                CustomShapedRecipe<ItemStack> shapedRecipe = (CustomShapedRecipe<ItemStack>) recipe;
                Object mcRecipe = FastNMS.INSTANCE.createShapedRecipe(shapedRecipe);
                return MINECRAFT_RECIPE_ADDER.apply(recipe.id(), mcRecipe);
            },
            RecipeSerializers.SHAPELESS, recipe -> {
                CustomShapelessRecipe<ItemStack> shapelessRecipe = (CustomShapelessRecipe<ItemStack>) recipe;
                Object mcRecipe = FastNMS.INSTANCE.createShapelessRecipe(shapelessRecipe);
                return MINECRAFT_RECIPE_ADDER.apply(recipe.id(), mcRecipe);
            },
            RecipeSerializers.SMELTING, recipe -> {
                CustomSmeltingRecipe<ItemStack> smeltingRecipe = (CustomSmeltingRecipe<ItemStack>) recipe;
                Object mcRecipe = FastNMS.INSTANCE.createSmeltingRecipe(smeltingRecipe);
                return MINECRAFT_RECIPE_ADDER.apply(recipe.id(), mcRecipe);
            },
            RecipeSerializers.BLASTING, recipe -> {
                CustomBlastingRecipe<ItemStack> blastingRecipe = (CustomBlastingRecipe<ItemStack>) recipe;
                Object mcRecipe = FastNMS.INSTANCE.createBlastingRecipe(blastingRecipe);
                return MINECRAFT_RECIPE_ADDER.apply(recipe.id(), mcRecipe);
            },
            RecipeSerializers.SMOKING, recipe -> {
                CustomSmokingRecipe<ItemStack> smokingRecipe = (CustomSmokingRecipe<ItemStack>) recipe;
                Object mcRecipe = FastNMS.INSTANCE.createSmokingRecipe(smokingRecipe);
                return MINECRAFT_RECIPE_ADDER.apply(recipe.id(), mcRecipe);
            },
            RecipeSerializers.CAMPFIRE_COOKING, recipe -> {
                CustomCampfireRecipe<ItemStack> campfireRecipe = (CustomCampfireRecipe<ItemStack>) recipe;
                Object mcRecipe = FastNMS.INSTANCE.createCampfireRecipe(campfireRecipe);
                return MINECRAFT_RECIPE_ADDER.apply(recipe.id(), mcRecipe);
            },
            RecipeSerializers.STONECUTTING, recipe -> {
                Object mcRecipe = FastNMS.INSTANCE.createStonecuttingRecipe((CustomStoneCuttingRecipe<ItemStack>) recipe);
                return MINECRAFT_RECIPE_ADDER.apply(recipe.id(), mcRecipe);
            },
            RecipeSerializers.SMITHING_TRIM, recipe -> {
                Object mcRecipe = FastNMS.INSTANCE.createSmithingTrimRecipe((CustomSmithingTrimRecipe<ItemStack>) recipe);
                return MINECRAFT_RECIPE_ADDER.apply(recipe.id(), mcRecipe);
            },
            RecipeSerializers.SMITHING_TRANSFORM, recipe -> {
                Object mcRecipe = FastNMS.INSTANCE.createSmithingTransformRecipe((CustomSmithingTransformRecipe<ItemStack>) recipe);
                return MINECRAFT_RECIPE_ADDER.apply(recipe.id(), mcRecipe);
            }
    );

    private static void modifyShapedRecipeIngredients(CustomShapedRecipe<ItemStack> recipe, Object shapedRecipe) {
        try {
            List<Ingredient<ItemStack>> actualIngredients = recipe.parsedPattern().ingredients()
                    .stream()
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();
            if (VersionHelper.isOrAbove1_21_2()) {
                CoreReflections.methodHandle$ShapedRecipe$placementInfoSetter.invokeExact(shapedRecipe, (Object) null);
            }
            List<Object> ingredients = getIngredientsFromShapedRecipe(shapedRecipe);
            modifyIngredients(ingredients, actualIngredients);
        } catch (Throwable e) {
            CraftEngine.instance().logger().warn("Failed to inject shaped recipe", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Object> getIngredientsFromShapedRecipe(Object recipe) {
        List<Object> ingredients = new ArrayList<>();
        try {
            if (VersionHelper.isOrAbove1_20_3()) {
                Object pattern = CoreReflections.methodHandle$1_20_3$ShapedRecipe$patternGetter.invokeExact(recipe);
                if (VersionHelper.isOrAbove1_21_2()) {
                    List<Optional<Object>> optionals = (List<Optional<Object>>) CoreReflections.methodHandle$ShapedRecipePattern$ingredients1_21_2Getter.invokeExact(pattern);
                    for (Optional<Object> optional : optionals) {
                        optional.ifPresent(ingredients::add);
                    }
                } else {
                    List<Object> objectList = (List<Object>) CoreReflections.methodHandle$ShapedRecipePattern$ingredients1_20_3Getter.invokeExact(pattern);
                    for (Object object : objectList) {
                        Object[] values = (Object[]) CoreReflections.methodHandle$Ingredient$valuesGetter.invokeExact(object);
                        // is empty or not
                        if (values.length != 0) {
                            ingredients.add(object);
                        }
                    }
                }
            } else {
                List<Object> objectList = (List<Object>) CoreReflections.methodHandle$1_20_1$ShapedRecipe$recipeItemsGetter.invokeExact(recipe);
                for (Object object : objectList) {
                    Object[] values = (Object[]) CoreReflections.methodHandle$Ingredient$valuesGetter.invokeExact(object);
                    if (values.length != 0) {
                        ingredients.add(object);
                    }
                }
            }
        } catch (Throwable e) {
            CraftEngine.instance().logger().warn("Failed to get ingredients from shaped recipe", e);
        }
        return ingredients;
    }

    private static void modifyShapelessRecipeIngredients(CustomShapelessRecipe<ItemStack> recipe, Object shapelessRecipe) {
        try {
            List<Ingredient<ItemStack>> actualIngredients = recipe.ingredientsInUse();
            if (VersionHelper.isOrAbove1_21_2()) {
                CoreReflections.methodHandle$ShapelessRecipe$placementInfoSetter.invokeExact(shapelessRecipe, (Object) null);
            }
            @SuppressWarnings("unchecked")
            List<Object> ingredients = (List<Object>) CoreReflections.methodHandle$ShapelessRecipe$ingredientsGetter.invokeExact(shapelessRecipe);
            modifyIngredients(ingredients, actualIngredients);
        } catch (Throwable e) {
            CraftEngine.instance().logger().warn("Failed to inject shapeless recipe", e);
        }
    }

    private static void modifyCookingRecipeIngredient(CustomCookingRecipe<ItemStack> recipe, Object cookingRecipe) {
        try {
            Ingredient<ItemStack> actualIngredient = recipe.ingredient();
            Object ingredient;
            if (VersionHelper.isOrAbove1_21_2()) {
                ingredient = CoreReflections.methodHandle$SingleItemRecipe$inputGetter.invokeExact(cookingRecipe);
            } else {
                ingredient = CoreReflections.methodHandle$AbstractCookingRecipe$inputGetter.invokeExact(cookingRecipe);
            }
            modifyIngredients(List.of(ingredient), List.of(actualIngredient));
        } catch (Throwable e) {
            CraftEngine.instance().logger().warn("Failed to inject cooking recipe", e);
        }
    }

    public static List<Object> getIngredientLooks(List<UniqueKey> holders) {
        List<Object> itemStacks = new ArrayList<>();
        for (UniqueKey holder : holders) {
            Optional<? extends BuildableItem<ItemStack>> buildableItem = BukkitItemManager.instance().getBuildableItem(holder.key());
            if (buildableItem.isPresent()) {
                ItemStack itemStack = buildableItem.get().buildItemStack(ItemBuildContext.EMPTY, 1);
                Object nmsStack = FastNMS.INSTANCE.method$CraftItemStack$asNMSCopy(itemStack);
                itemStacks.add(nmsStack);
            } else {
                Item<ItemStack> barrier = BukkitItemManager.instance().createWrappedItem(ItemKeys.BARRIER, null);
                assert barrier != null;
                barrier.customNameJson(AdventureHelper.componentToJson(Component.text(holder.key().asString()).color(NamedTextColor.RED)));
                itemStacks.add(barrier.getLiteralObject());
            }
        }
        return itemStacks;
    }

    private static void modifyIngredients(List<Object> fakeIngredients, List<Ingredient<ItemStack>> actualIngredients) throws Throwable {
        if (fakeIngredients.size() != actualIngredients.size()) {
            throw new IllegalArgumentException("Ingredient count mismatch");
        }
        for (int i = 0; i < fakeIngredients.size(); i++) {
            Object ingredient = fakeIngredients.get(i);
            Ingredient<ItemStack> actualIngredient = actualIngredients.get(i);
            List<Object> items = getIngredientLooks(actualIngredient.items());
            if (VersionHelper.isOrAbove1_21_4()) {
                CoreReflections.methodHandle$Ingredient$itemStacksSetter.invokeExact(ingredient, (Set<Object>) new CustomIngredientSet(items, actualIngredient));
            } else if (VersionHelper.isOrAbove1_21_2()) {
                CoreReflections.methodHandle$Ingredient$itemStacksSetter.invokeExact(ingredient, (List<Object>) new CustomIngredientList(items, actualIngredient));
            } else {
                Object itemStackArray = Array.newInstance(CoreReflections.clazz$ItemStack, items.size());
                for (int j = 0; j < items.size(); j++) {
                    Array.set(itemStackArray, j, items.get(j));
                }
                CoreReflections.methodHandle$Ingredient$itemStacksSetter.invokeExact(ingredient, (Object) itemStackArray);
            }
        }
    }

    public static Object toRecipeResourceKey(Key id) {
        return FastNMS.INSTANCE.method$ResourceKey$create(MRegistries.RECIPE, KeyUtils.toResourceLocation(id));
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
    // 欺骗服务端使其以为自己处于启动阶段
    private Object stolenFeatureFlagSet;
    // 需要在主线程卸载的配方
    private final List<Pair<Key, Boolean>> recipesToUnregister = new ArrayList<>();
    // 已经被替换过的数据包配方
    private final Set<Key> replacedDatapackRecipes = new HashSet<>();
    // 换成的数据包配方
    private Map<Key, Recipe<ItemStack>> lastDatapackRecipes = Map.of();
    private Object lastRecipeManager = null;

    public BukkitRecipeManager(BukkitCraftEngine plugin) {
        instance = this;
        this.plugin = plugin;
        this.recipeEventListener = new RecipeEventListener(plugin, this, plugin.itemManager());
    }

    public static Object minecraftRecipeManager() {
        return FastNMS.INSTANCE.method$MinecraftServer$getRecipeManager(FastNMS.INSTANCE.method$MinecraftServer$getServer());
    }

    public static BukkitRecipeManager instance() {
        return instance;
    }

    @Override
    public void delayedInit() {
        Bukkit.getPluginManager().registerEvents(this.recipeEventListener, this.plugin.javaPlugin());
    }

    @Override
    public void load() {
        if (!Config.enableRecipeSystem()) return;
        if (VersionHelper.isOrAbove1_21_2()) {
            try {
                this.stolenFeatureFlagSet = CoreReflections.methodHandle$RecipeManager$featureflagsetGetter.invokeExact(minecraftRecipeManager());
                CoreReflections.methodHandle$RecipeManager$featureflagsetSetter.invokeExact(minecraftRecipeManager(), (Object) null);
            } catch (Throwable e) {
                this.plugin.logger().warn("Failed to steal feature flag set", e);
            }
        }
    }

    @Override
    public void unload() {
        if (!Config.enableRecipeSystem()) return;
        // 安排卸载任务，这些任务会在load后执行。如果没有load说明服务器已经关闭了，那就不需要管卸载了。
        if (!Bukkit.isStopping()) {
            for (Map.Entry<Key, Recipe<ItemStack>> entry : this.byId.entrySet()) {
                Key id = entry.getKey();
                // 不要卸载数据包配方，只记录自定义的配方
                if (isDataPackRecipe(id)) continue;
                boolean isBrewingRecipe = entry.getValue() instanceof CustomBrewingRecipe<ItemStack>;
                this.recipesToUnregister.add(Pair.of(id, isBrewingRecipe));
            }
        }
        super.unload();
    }

    @Override
    public void delayedLoad() {
        if (!Config.enableRecipeSystem()) return;
        this.loadDataPackRecipes();
    }

    @Override
    public void disable() {
        unload();
        HandlerList.unregisterAll(this.recipeEventListener);
    }

    @Override
    protected void unregisterPlatformRecipeMainThread(Key key, boolean isBrewingRecipe) {
        if (isBrewingRecipe) {
            Bukkit.getPotionBrewer().removePotionMix(new NamespacedKey(key.namespace(), key.value()));
        } else {
            MINECRAFT_RECIPE_REMOVER.accept(key);
        }
    }

    @Override
    protected void registerPlatformRecipeMainThread(Recipe<ItemStack> recipe) {
        Key id = recipe.id();
        if (recipe instanceof CustomBrewingRecipe<ItemStack> brewingRecipe) {
            if (!VersionHelper.isOrAbove1_20_2()) return;
            PotionMix potionMix = new PotionMix(new NamespacedKey(id.namespace(), id.value()),
                    brewingRecipe.result(ItemBuildContext.EMPTY),
                    PotionMix.createPredicateChoice(container -> {
                        Item<ItemStack> wrapped = this.plugin.itemManager().wrap(container);
                        return brewingRecipe.container().test(UniqueIdItem.of(wrapped));
                    }),
                    PotionMix.createPredicateChoice(ingredient -> {
                        Item<ItemStack> wrapped = this.plugin.itemManager().wrap(ingredient);
                        return brewingRecipe.ingredient().test(UniqueIdItem.of(wrapped));
                    })
            );
            Bukkit.getPotionBrewer().addPotionMix(potionMix);
        } else {
            // 如果是数据包配方
            if (isDataPackRecipe(id)) {
                // 如果这个数据包配方已经被换成了注入配方，那么是否需要重新注册取决于其是否含有tag，且tag里有自定义物品
                if (!this.replacedDatapackRecipes.add(id)) {
                    outer: {
                        for (Ingredient<ItemStack> ingredient : recipe.ingredientsInUse()) {
                            if (ingredient.hasCustomItem()) {
                                break outer;
                            }
                        }
                        // 没有自定义物品，且被注入过了，那么就不需要移除后重新注册
                        return;
                    }
                }
                MINECRAFT_RECIPE_REMOVER.accept(id);
            }
            ADD_RECIPE_FOR_MINECRAFT_RECIPE_HOLDER.get(recipe.serializerType()).apply(recipe);
        }
    }

    private void loadDataPackRecipes() {
        Object currentRecipeManager = minecraftRecipeManager();
        if (currentRecipeManager != this.lastRecipeManager) {
            this.lastRecipeManager = currentRecipeManager;
            this.replacedDatapackRecipes.clear();
            try {
                this.lastDatapackRecipes = scanResources();
            } catch (Throwable e) {
                this.plugin.logger().warn("Failed to load datapack recipes", e);
            }
        }

        if (Config.disableAllVanillaRecipes()) {
            this.recipesToUnregister.addAll(this.lastDatapackRecipes.keySet().stream().map(it -> Pair.of(it, false)).toList());
            return;
        }

        boolean hasDisabledAny = !Config.disabledVanillaRecipes().isEmpty();
        for (Map.Entry<Key, Recipe<ItemStack>> entry : this.lastDatapackRecipes.entrySet()) {
            if (hasDisabledAny && Config.disabledVanillaRecipes().contains(entry.getKey())) {
                this.recipesToUnregister.add(Pair.of(entry.getKey(), false));
                continue;
            }
            markAsDataPackRecipe(entry.getKey());
            registerInternalRecipe(entry.getKey(), entry.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Key, Recipe<ItemStack>> scanResources() throws Throwable {
        Object fileToIdConverter = CoreReflections.methodHandle$FileToIdConverter$json.invokeExact((String) (VersionHelper.isOrAbove1_21() ? "recipe" : "recipes"));
        Object minecraftServer = FastNMS.INSTANCE.method$MinecraftServer$getServer();
        Object packRepository = CoreReflections.methodHandle$MinecraftServer$getPackRepository.invokeExact(minecraftServer);
        List<Object> selected = (List<Object>) CoreReflections.methodHandle$PackRepository$selectedGetter.invokeExact(packRepository);
        List<Object> packResources = new ArrayList<>();
        for (Object pack : selected) {
            packResources.add(CoreReflections.methodHandle$Pack$open.invokeExact(pack));
        }
        Map<Key, Recipe<ItemStack>> recipes = new HashMap<>();

        try (AutoCloseable resourceManager = (AutoCloseable) CoreReflections.methodHandle$MultiPackResourceManagerConstructor.invokeExact(CoreReflections.instance$PackType$SERVER_DATA, packResources)) {
            Map<Object, Object> scannedResources = (Map<Object, Object>) CoreReflections.methodHandle$FileToIdConverter$listMatchingResources.invokeExact(fileToIdConverter, resourceManager);
            for (Map.Entry<Object, Object> entry : scannedResources.entrySet()) {
                Key id = extractKeyFromResourceLocation(entry.getKey().toString());
                Reader reader = (Reader) CoreReflections.methodHandle$Resource$openAsReader.invokeExact(entry.getValue());
                JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
                Key serializerType = Key.of(jsonObject.get("type").getAsString());
                RecipeSerializer<ItemStack, ? extends Recipe<ItemStack>> serializer = (RecipeSerializer<ItemStack, ? extends Recipe<ItemStack>>) BuiltInRegistries.RECIPE_SERIALIZER.getValue(serializerType);
                if (serializer == null) {
                    continue;
                }
                try {
                    Recipe<ItemStack> recipe = serializer.readJson(id, jsonObject);
                    recipes.put(id, recipe);
                } catch (Exception e) {
                    this.plugin.logger().warn("Failed to load data pack recipe " + id + ". Json: " + jsonObject, e);
                }
            }
        } catch (Throwable e) {
            this.plugin.logger().warn("Unknown error occurred when loading data pack recipes", e);
        }
        return recipes;
    }

    private Key extractKeyFromResourceLocation(String input) {
        int prefixEndIndex = input.indexOf(':');
        String prefix = input.substring(0, prefixEndIndex);
        int lastSlashIndex = input.lastIndexOf('/');
        int lastDotIndex = input.lastIndexOf('.');
        String fileName = input.substring(lastSlashIndex + 1, lastDotIndex);
        return Key.of(prefix, fileName);
    }

    @Override
    public void runDelayedSyncTasks() {
        if (!Config.enableRecipeSystem()) return;

        // 卸载掉需要卸载的配方（禁用的原版配方+注册的自定义配方）
        for (Pair<Key, Boolean> pair : this.recipesToUnregister) {
            unregisterPlatformRecipeMainThread(pair.left(), pair.right());
        }

        this.recipesToUnregister.clear();

        // 注册新的配方
        for (Recipe<ItemStack> recipe : this.byId.values()) {
            try {
                registerPlatformRecipeMainThread(recipe);
            } catch (Exception e) {
                this.plugin.logger().warn("Failed to register recipe " + recipe.id().toString(), e);
            }
        }

        // 重新注入特殊配方
        try {
            Key dyeRecipeId = Key.from("armor_dye");
            MINECRAFT_RECIPE_REMOVER.accept(dyeRecipeId);
            MINECRAFT_RECIPE_ADDER.apply(dyeRecipeId, RecipeInjector.createCustomDyeRecipe(dyeRecipeId));
            Key repairRecipeId = Key.from("repair_item");
            MINECRAFT_RECIPE_REMOVER.accept(repairRecipeId);
            MINECRAFT_RECIPE_ADDER.apply(repairRecipeId, RecipeInjector.createRepairItemRecipe(repairRecipeId));
            Key fireworkStarFadeRecipeId = Key.from("firework_star_fade");
            MINECRAFT_RECIPE_REMOVER.accept(fireworkStarFadeRecipeId);
            MINECRAFT_RECIPE_ADDER.apply(fireworkStarFadeRecipeId, RecipeInjector.createFireworkStarFadeRecipe(fireworkStarFadeRecipeId));
        } catch (ReflectiveOperationException e) {
            throw new ReflectionInitException("Failed to inject special recipes", e);
        }

        try {
            // give flags back on 1.21.2+
            if (VersionHelper.isOrAbove1_21_2() && this.stolenFeatureFlagSet != null) {
                CoreReflections.methodHandle$RecipeManager$featureflagsetSetter.invokeExact(minecraftRecipeManager(), (Object) this.stolenFeatureFlagSet);
                this.stolenFeatureFlagSet = null;
            }

            // refresh recipes
            if (VersionHelper.isOrAbove1_21_2()) {
                CoreReflections.methodHandle$RecipeManager$finalizeRecipeLoading.invokeExact(minecraftRecipeManager());
            }

            // send to players
            CoreReflections.methodHandle$DedicatedPlayerList$reloadRecipes.invokeExact(CraftBukkitReflections.methodHandle$CraftServer$playerListGetter.invokeExact(Bukkit.getServer()));
        } catch (Throwable e) {
            this.plugin.logger().warn("Failed to run delayed recipe tasks", e);
        }
    }
}
