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
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistries;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.item.BuildableItem;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.item.recipe.*;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.CraftServerProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.FileToIdConverterProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.ResourceKeyProxy;
import net.momirealms.craftengine.proxy.minecraft.server.MinecraftServerProxy;
import net.momirealms.craftengine.proxy.minecraft.server.packs.PackTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.server.packs.repository.PackProxy;
import net.momirealms.craftengine.proxy.minecraft.server.packs.repository.PackRepositoryProxy;
import net.momirealms.craftengine.proxy.minecraft.server.packs.resources.MultiPackResourceManagerProxy;
import net.momirealms.craftengine.proxy.minecraft.server.packs.resources.ResourceProxy;
import net.momirealms.craftengine.proxy.minecraft.server.players.PlayerListProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.crafting.RecipeHolderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.crafting.RecipeManagerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.crafting.RecipeMapProxy;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import java.io.Reader;
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
                RecipeMapProxy.INSTANCE.removeRecipe(RecipeManagerProxy.INSTANCE.getRecipes(minecraftRecipeManager()), resourceKey);
            }) :
            (id -> {
                Object identifier = KeyUtils.toIdentifier(id);
                RecipeManagerProxy.INSTANCE.removeRecipe$1(minecraftRecipeManager(), identifier);
            });
    private static final BiFunction<Key, Object, Object> MINECRAFT_RECIPE_ADDER =
            VersionHelper.isOrAbove1_21_2() ?
            (id, recipe) -> {
                Object resourceKey = toRecipeResourceKey(id);
                Object recipeHolder = RecipeHolderProxy.INSTANCE.newInstance$0(resourceKey, recipe);
                RecipeManagerProxy.INSTANCE.addRecipe$0(minecraftRecipeManager(), recipeHolder);
                return recipeHolder;
            } :
            VersionHelper.isOrAbove1_20_2() ?
            (id, recipe) -> {
                Object identifier = KeyUtils.toIdentifier(id);
                Object recipeHolder = RecipeHolderProxy.INSTANCE.newInstance$1(identifier, recipe);
                RecipeManagerProxy.INSTANCE.addRecipe$0(minecraftRecipeManager(), recipeHolder);
                return recipeHolder;
            } :
            (id, recipe) -> {
                RecipeManagerProxy.INSTANCE.addRecipe$1(minecraftRecipeManager(), recipe);
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

    // nms 模块需要使用此方法
    public static List<Object> getIngredientLooks(Ingredient<ItemStack> ingredient) {
        List<Object> itemStacks = new ArrayList<>();
        for (UniqueKey holder : ingredient.items()) {
            Optional<? extends BuildableItem<ItemStack>> buildableItem = BukkitItemManager.instance().getBuildableItem(holder.key());
            if (buildableItem.isPresent()) {
                ItemStack itemStack = buildableItem.get().buildItemStack(ItemBuildContext.empty(), ingredient.count());
                Object nmsStack = FastNMS.INSTANCE.field$CraftItemStack$handle(ItemStackUtils.ensureCraftItemStack(itemStack));
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

    public static Object toRecipeResourceKey(Key id) {
        return ResourceKeyProxy.INSTANCE.create(MRegistries.RECIPE, KeyUtils.toIdentifier(id));
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
    // 欺骗服务端使其以为自己处于启动阶段
    private Object stolenFeatureFlagSet;
    // 需要在主线程卸载的配方
    private final List<Pair<Key, Boolean>> recipesToUnregister = new ArrayList<>();
    // 已经被替换过的数据包配方
    private final Set<Key> replacedDatapackRecipes = new HashSet<>();
    // 换成的数据包配方
    private Map<Key, JsonObject> lastDatapackRecipes = Map.of();
    private Object lastRecipeManager = null;

    public BukkitRecipeManager(BukkitCraftEngine plugin) {
        instance = this;
        this.plugin = plugin;
        this.recipeEventListener = new RecipeEventListener(plugin, this, plugin.itemManager());
        this.crafterEventListener = VersionHelper.isOrAbove1_21() ? new CrafterEventListener(plugin, this, plugin.itemManager()) : null;
    }

    public static Object minecraftRecipeManager() {
        return MinecraftServerProxy.INSTANCE.getRecipeManager(MinecraftServerProxy.INSTANCE.getServer());
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
        if (VersionHelper.isOrAbove1_21_2()) {
            this.stolenFeatureFlagSet = RecipeManagerProxy.INSTANCE.getFeatureFlagSet(minecraftRecipeManager());
            RecipeManagerProxy.INSTANCE.setFeatureFlagSet(minecraftRecipeManager(), null);
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
                    brewingRecipe.result(ItemBuildContext.empty()),
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
        for (Map.Entry<Key, JsonObject> entry : this.lastDatapackRecipes.entrySet()) {
            Key id = entry.getKey();
            if (hasDisabledAny && Config.disabledVanillaRecipes().contains(entry.getKey())) {
                this.recipesToUnregister.add(Pair.of(id, false));
                continue;
            }

            JsonObject jsonObject = entry.getValue();
            Key serializerType = Key.of(jsonObject.get("type").getAsString());
            @SuppressWarnings("unchecked")
            RecipeSerializer<ItemStack, ? extends Recipe<ItemStack>> serializer = (RecipeSerializer<ItemStack, ? extends Recipe<ItemStack>>) BuiltInRegistries.RECIPE_SERIALIZER.getValue(serializerType);
            if (serializer == null) {
                continue;
            }

            try {
                Recipe<ItemStack> recipe = serializer.readJson(id, jsonObject);
                markAsDataPackRecipe(id);
                registerInternalRecipe(id, recipe, false);
            } catch (Exception e) {
                this.plugin.logger().warn("Failed to load data pack recipe " + id + ". Json: " + jsonObject, e);
            }
        }
    }

    private Map<Key, JsonObject> scanResources() {
        Object fileToIdConverter = FileToIdConverterProxy.INSTANCE.json(VersionHelper.isOrAbove1_21() ? "recipe" : "recipes");
        Object minecraftServer = MinecraftServerProxy.INSTANCE.getServer();
        Object packRepository = MinecraftServerProxy.INSTANCE.getPackRepository(minecraftServer);
        List<Object> selected = PackRepositoryProxy.INSTANCE.getSelected(packRepository);
        List<Object> packResources = new ArrayList<>();
        for (Object pack : selected) {
            packResources.add(PackProxy.INSTANCE.open(pack));
        }
        Map<Key, JsonObject> recipes = new HashMap<>();
        try (AutoCloseable resourceManager = (AutoCloseable) MultiPackResourceManagerProxy.INSTANCE.newInstance(PackTypeProxy.SERVER_DATA, packResources)) {
            Map<Object, Object> scannedResources = FileToIdConverterProxy.INSTANCE.listMatchingResources(fileToIdConverter, resourceManager);
            for (Map.Entry<Object, Object> entry : scannedResources.entrySet()) {
                Key id = extractKeyFromIdentifier(entry.getKey().toString());
                try (Reader reader = ResourceProxy.INSTANCE.openAsReader(entry.getValue())) {
                    JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
                    recipes.put(id, jsonObject);
                }
            }
        } catch (Throwable e) {
            this.plugin.logger().warn("Unknown error occurred when loading data pack recipes", e);
        }
        return recipes;
    }

    private Key extractKeyFromIdentifier(String input) {
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
                RecipeManagerProxy.INSTANCE.setFeatureFlagSet(minecraftRecipeManager(), this.stolenFeatureFlagSet);
                this.stolenFeatureFlagSet = null;
            }

            // refresh recipes
            if (VersionHelper.isOrAbove1_21_2()) {
                RecipeManagerProxy.INSTANCE.finalizeRecipeLoading(minecraftRecipeManager());
            }

            // send to players
            PlayerListProxy.INSTANCE.reloadRecipeData(CraftServerProxy.INSTANCE.getPlayerList(Bukkit.getServer()));
        } catch (Throwable e) {
            this.plugin.logger().warn("Failed to run delayed recipe tasks", e);
        }
    }
}
