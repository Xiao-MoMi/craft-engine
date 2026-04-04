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
import net.momirealms.craftengine.proxy.minecraft.server.MinecraftServerProxy;
import net.momirealms.craftengine.proxy.minecraft.server.packs.PackTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.server.packs.repository.PackProxy;
import net.momirealms.craftengine.proxy.minecraft.server.packs.repository.PackRepositoryProxy;
import net.momirealms.craftengine.proxy.minecraft.server.packs.resources.MultiPackResourceManagerProxy;
import net.momirealms.craftengine.proxy.minecraft.server.packs.resources.ResourceProxy;
import net.momirealms.craftengine.proxy.minecraft.server.players.PlayerListProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.crafting.RecipeManagerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.crafting.RecipeTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.entity.AbstractFurnaceBlockEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.entity.BlastFurnaceBlockEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.entity.FurnaceBlockEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.entity.SmokerBlockEntityProxy;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.HandlerList;
import org.bukkit.potion.PotionBrewer;

import java.io.Reader;
import java.util.*;
import java.util.function.Function;

public final class BukkitRecipeManager extends AbstractRecipeManager {
    private static BukkitRecipeManager instance;
    public static final NamespacedKey FURNACE_LAST_USER = new NamespacedKey("craftengine", "furnace_last_user");

    public static final Map<Key, Function<Recipe, Object>> RECIPE_GENERATOR = Map.of(
            RecipeSerializers.SHAPED, recipe -> FastNMS.INSTANCE.createShapedRecipe((CustomShapedRecipe) recipe),
            RecipeSerializers.SHAPELESS, recipe -> FastNMS.INSTANCE.createShapelessRecipe((CustomShapelessRecipe) recipe),
            RecipeSerializers.SMELTING, recipe -> FastNMS.INSTANCE.createSmeltingRecipe((CustomSmeltingRecipe) recipe),
            RecipeSerializers.BLASTING, recipe -> FastNMS.INSTANCE.createBlastingRecipe((CustomBlastingRecipe) recipe),
            RecipeSerializers.SMOKING, recipe -> FastNMS.INSTANCE.createSmokingRecipe((CustomSmokingRecipe) recipe),
            RecipeSerializers.CAMPFIRE_COOKING, recipe -> FastNMS.INSTANCE.createCampfireRecipe((CustomCampfireRecipe) recipe),
            RecipeSerializers.STONECUTTING, recipe -> FastNMS.INSTANCE.createStonecuttingRecipe((CustomStoneCuttingRecipe) recipe),
            RecipeSerializers.SMITHING_TRIM, recipe -> FastNMS.INSTANCE.createSmithingTrimRecipe((CustomSmithingTrimRecipe) recipe),
            RecipeSerializers.SMITHING_TRANSFORM, recipe -> FastNMS.INSTANCE.createSmithingTransformRecipe((CustomSmithingTransformRecipe) recipe)
    );

    // nms 模块需要使用此方法
    public static List<Object> getIngredientLooks(Ingredient ingredient) {
        List<Object> itemStacks = new ArrayList<>();
        for (UniqueKey holder : ingredient.items()) {
            Optional<? extends BuildableItem> buildableItem = BukkitItemManager.instance().getBuildableItem(holder.key());
            if (buildableItem.isPresent()) {
                itemStacks.add(buildableItem.get().buildItem(ItemBuildContext.empty(), ingredient.count()).getMinecraftItem());
            } else {
                Item barrier = BukkitItemManager.instance().createWrappedItem(ItemKeys.BARRIER, null);
                assert barrier != null;
                barrier.customNameJson(AdventureHelper.componentToJson(Component.text(holder.key().asString()).color(NamedTextColor.RED)));
                itemStacks.add(barrier.getMinecraftItem());
            }
        }
        return itemStacks;
    }

    private final BukkitCraftEngine plugin;
    private final RecipeEventListener recipeEventListener;
    private final CrafterEventListener crafterEventListener;
    // 需要在主线程卸载的配方
    private final List<Key> nativeRecipesToUnregister = new ArrayList<>();
    private final List<Key> brewingRecipesToUnregister = new ArrayList<>();
    // 已经被替换过的数据包配方
    private final Set<Key> replacedDatapackRecipes = new HashSet<>();
    // 换成的数据包配方
    private Map<Key, JsonObject> lastDatapackRecipes = Map.of();
    private Object lastRecipeManager = null;

    public BukkitRecipeManager(BukkitCraftEngine plugin) {
        super(createRecipeRegistry());
        instance = this;
        this.plugin = plugin;
        this.recipeEventListener = new RecipeEventListener(plugin, this, plugin.itemManager());
        this.crafterEventListener = VersionHelper.isOrAbove1_21() ? new CrafterEventListener(plugin, this, plugin.itemManager()) : null;
    }

    public static RecipeRegistry createRecipeRegistry() {
        if (VersionHelper.isOrAbove1_21_2()) {
            return new RecipeRegistry1_21_2();
        } else if (VersionHelper.isOrAbove1_20_5()) {
            return new RecipeRegistry1_20_5();
        } else if (VersionHelper.isOrAbove1_20_2()) {
            return new RecipeRegistry1_20_2();
        } else {
            return new RecipeRegistry1_20();
        }
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
    }

    @Override
    public void unload() {
        if (!Config.enableRecipeSystem()) return;
        // 安排卸载任务，这些任务会在load后执行。如果没有load说明服务器已经关闭了，那就不需要管卸载了。
        if (!Bukkit.isStopping()) {
            for (Recipe recipe : this.nativeRecipes) {
                Key id = recipe.id();
                // 不要卸载数据包配方，只记录自定义的配方
                if (isDataPackRecipe(id)) continue;
                this.nativeRecipesToUnregister.add(id);
            }
            for (Recipe recipe : this.brewingRecipes) {
                this.brewingRecipesToUnregister.add(recipe.id());
            }
        }
        super.unload();
    }

    @Override
    public void delayedLoad() {
        if (!Config.enableRecipeSystem()) return;

        // 准备注册
        super.recipeRegistry.prepareRegistration();

        // 先注销之前注册的配方
        if (!this.nativeRecipesToUnregister.isEmpty()) {
            for (Key recipeId : this.nativeRecipesToUnregister) {
                super.recipeRegistry.unregister(recipeId);
            }
            this.nativeRecipesToUnregister.clear();
        }

        // 注册配方
        ExceptionCollector<Exception> collector = new ExceptionCollector<>(Exception.class);
        for (Recipe recipe : super.nativeRecipes) {
            Key id = recipe.id();
            if (isDataPackRecipe(id)) {
                // 如果这个数据包配方已经被换成了注入配方，那么是否需要重新注册取决于其是否含有tag，且tag里有自定义物品
                if (!this.replacedDatapackRecipes.add(id)) {
                    outer: {
                        for (Ingredient ingredient : recipe.ingredientsInUse()) {
                            if (ingredient.hasCustomItem()) {
                                break outer;
                            }
                        }
                        // 没有自定义物品，且被注入过了，那么就不需要移除后重新注册
                        continue;
                    }
                }
                super.recipeRegistry.unregister(id);
            }
            try {
                super.recipeRegistry.register(id, RECIPE_GENERATOR.get(recipe.serializerType()).apply(recipe));
            } catch (Exception e) {
                collector.add(e);
            }

        }

        // 重新注入特殊配方
        if (!VersionHelper.isOrAbove26_1()) { // fixme 26.1+
            super.recipeRegistry.unregister(RecipeInjector.ARMOR_DYE);
            super.recipeRegistry.unregister(RecipeInjector.REPAIR_ITEM);
            super.recipeRegistry.unregister(RecipeInjector.FIREWORK_STAR_FADE);
            super.recipeRegistry.register(RecipeInjector.ARMOR_DYE, RecipeInjector.ARMOR_DYE_RECIPE);
            super.recipeRegistry.register(RecipeInjector.REPAIR_ITEM, RecipeInjector.REPAIR_ITEM_RECIPE);
            super.recipeRegistry.register(RecipeInjector.FIREWORK_STAR_FADE, RecipeInjector.FIREWORK_STAR_FADE_RECIPE);
        }

        // 完成注册
        super.recipeRegistry.finalizeRegistration();

        // 刷新配方
        if (VersionHelper.isOrAbove1_21_2()) {
            Object manager = minecraftRecipeManager();
            RecipeManagerProxy.INSTANCE.finalizeRecipeLoading(manager, RecipeManagerProxy.INSTANCE.getEnabledFlags(manager));
        }
        // 1.21.6以下直接发包
        if (!VersionHelper.isOrAbove1_21_6() || VersionHelper.isFolia()) {
            PlayerListProxy.INSTANCE.reloadRecipeData(CraftServerProxy.INSTANCE.getPlayerList(Bukkit.getServer()));
        }

        Optional.ofNullable(collector.result()).ifPresent(t -> {
            this.plugin.logger().warn("Failed to load recipes", t);
        });
    }

    @Override
    public void runDelayedSyncTasks() {
        if (!Config.enableRecipeSystem()) return;

        // 处理酿造配方
        if (VersionHelper.isOrAbove1_20_2()) {
            PotionBrewer potionBrewer = Bukkit.getPotionBrewer();
            if (!this.brewingRecipesToUnregister.isEmpty()) {
                for (Key potion : this.brewingRecipesToUnregister) {
                    potionBrewer.removePotionMix(KeyUtils.toNamespacedKey(potion));
                }
                this.brewingRecipesToUnregister.clear();
            }
            if (!super.brewingRecipes.isEmpty()) {
                for (CustomBrewingRecipe recipe : super.brewingRecipes) {
                    PotionMix potionMix = new PotionMix(KeyUtils.toNamespacedKey(recipe.id()),
                            ItemStackUtils.getBukkitStack(recipe.result(ItemBuildContext.empty())),
                            PotionMix.createPredicateChoice(container -> {
                                Item wrapped = this.plugin.itemManager().wrap(container);
                                return recipe.container().test(UniqueIdItem.of(wrapped));
                            }),
                            PotionMix.createPredicateChoice(ingredient -> {
                                Item wrapped = this.plugin.itemManager().wrap(ingredient);
                                return recipe.ingredient().test(UniqueIdItem.of(wrapped));
                            })
                    );
                    potionBrewer.addPotionMix(potionMix);
                }
            }
        }

        // 重载资源
        if (VersionHelper.isOrAbove1_21_6() && !VersionHelper.isFolia()) {
            PlayerListProxy.INSTANCE.reloadResources(CraftServerProxy.INSTANCE.getPlayerList(Bukkit.getServer()));
        }
    }

    @Override
    public void disable() {
        unload();
        HandlerList.unregisterAll(this.recipeEventListener);
    }

    @Override
    public void loadDataPackRecipes() {
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
            this.nativeRecipesToUnregister.addAll(this.lastDatapackRecipes.keySet());
            return;
        }

        Set<Key> disabledRecipes = Config.disabledVanillaRecipes();
        boolean hasDisabledAny = !disabledRecipes.isEmpty();

        for (Map.Entry<Key, JsonObject> entry : this.lastDatapackRecipes.entrySet()) {
            Key id = entry.getKey();
            if (hasDisabledAny && disabledRecipes.contains(entry.getKey())) {
                this.nativeRecipesToUnregister.add(id);
                continue;
            }
            JsonObject jsonObject = entry.getValue();
            try {
                Key serializerType = Key.of(jsonObject.get("type").getAsString());
                RecipeSerializer<? extends Recipe> serializer = BuiltInRegistries.RECIPE_SERIALIZER.getValue(serializerType);
                if (serializer == null) {
                    continue;
                }
                Recipe recipe = serializer.readJson(id, jsonObject);
                markAsDataPackRecipe(id);
                registerRecipeInternal(recipe, false);
            } catch (Throwable e) {
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

    public static void injectFurnaceBlockEntity(Object blockEntity) {
        Object recipeType = null;
        if (SmokerBlockEntityProxy.CLASS.isInstance(blockEntity)) {
            recipeType = RecipeTypeProxy.SMOKING;
        } else if (BlastFurnaceBlockEntityProxy.CLASS.isInstance(blockEntity)) {
            recipeType = RecipeTypeProxy.BLASTING;
        } else if (FurnaceBlockEntityProxy.CLASS.isInstance(blockEntity)) {
            recipeType = RecipeTypeProxy.SMELTING;
        }
        if (recipeType != null) {
            AbstractFurnaceBlockEntityProxy.INSTANCE.setQuickCheck(blockEntity, FastNMS.INSTANCE.createInjectedFurnaceCachedCheck(recipeType, blockEntity));
        }
    }
}
