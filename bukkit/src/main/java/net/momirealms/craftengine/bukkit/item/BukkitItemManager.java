package net.momirealms.craftengine.bukkit.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.item.behavior.AxeItemBehavior;
import net.momirealms.craftengine.bukkit.item.behavior.FlintAndSteelItemBehavior;
import net.momirealms.craftengine.bukkit.item.factory.BukkitItemFactory;
import net.momirealms.craftengine.bukkit.item.listener.ArmorEventListener;
import net.momirealms.craftengine.bukkit.item.listener.DebugStickListener;
import net.momirealms.craftengine.bukkit.item.listener.ItemEventListener;
import net.momirealms.craftengine.bukkit.item.listener.SlotChangeListener;
import net.momirealms.craftengine.bukkit.item.recipe.BukkitRecipeManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBuiltInRegistries;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MItems;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistries;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistryOps;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.*;
import net.momirealms.craftengine.core.item.recipe.DatapackRecipeResult;
import net.momirealms.craftengine.core.item.recipe.IngredientUnlockable;
import net.momirealms.craftengine.core.item.recipe.UniqueIdItem;
import net.momirealms.craftengine.core.pack.AbstractPackManager;
import net.momirealms.craftengine.core.plugin.compatibility.ItemSource;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.UniqueKey;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.core.MappedRegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryAccessProxy;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.network.chat.ComponentProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.ResourceKeyProxy;
import net.momirealms.craftengine.proxy.minecraft.tags.TagKeyProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.equipment.trim.*;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.incendo.cloud.suggestion.Suggestion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@SuppressWarnings("unchecked")
public class BukkitItemManager extends AbstractItemManager<ItemStack> {
    static {
        registerVanillaItemExtraBehavior(FlintAndSteelItemBehavior.INSTANCE, ItemKeys.FLINT_AND_STEEL);
        registerVanillaItemExtraBehavior(AxeItemBehavior.INSTANCE, ItemKeys.AXES);
    }

    private static BukkitItemManager instance;
    private final BukkitItemFactory<? extends ItemWrapper<ItemStack>> factory;
    private final BukkitCraftEngine plugin;
    private final ItemEventListener itemEventListener;
    private final DebugStickListener debugStickListener;
    private final ArmorEventListener armorEventListener;
    private final SlotChangeListener slotChangeListener;
    private final NetworkItemHandler<ItemStack> networkItemHandler;
    private final Object bedrockItemHolder;
    private final Item<ItemStack> emptyItem;
    private final UniqueIdItem<ItemStack> emptyUniqueItem;
    private Set<Key> lastRegisteredPatterns = Set.of();

    public BukkitItemManager(BukkitCraftEngine plugin) {
        super(plugin);
        instance = this;
        this.plugin = plugin;
        this.factory = BukkitItemFactory.create(plugin);
        this.itemEventListener = new ItemEventListener(plugin, this);
        this.debugStickListener = new DebugStickListener(plugin);
        this.armorEventListener = new ArmorEventListener();
        this.slotChangeListener = VersionHelper.isOrAbove1_20_3() ? new SlotChangeListener(this) : null;
        this.networkItemHandler = VersionHelper.isOrAbove1_20_5() ? new ModernNetworkItemHandler() : new LegacyNetworkItemHandler();
        this.registerAllVanillaItems();
        this.bedrockItemHolder = RegistryProxy.INSTANCE.get$1(MBuiltInRegistries.ITEM, ResourceKeyProxy.INSTANCE.create(MRegistries.ITEM, KeyUtils.toIdentifier(Key.of("minecraft:bedrock")))).orElseThrow();
        this.registerCustomTrimMaterial();
        this.loadLastRegisteredPatterns();
        ItemStack emptyStack = FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(ItemStackProxy.EMPTY);
        this.emptyItem = this.factory.wrap(emptyStack);
        this.emptyUniqueItem = UniqueIdItem.of(this.emptyItem);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void delayedLoad() {
        super.delayedLoad();
        List<ItemSource<ItemStack>> sources = new ArrayList<>();
        for (String externalSource : Config.recipeIngredientSources()) {
            String sourceId = externalSource.toLowerCase(Locale.ENGLISH);
            ItemSource<?> itemSource = this.plugin.compatibilityManager().getItemSource(sourceId);
            if (itemSource != null) {
                sources.add((ItemSource<ItemStack>) itemSource);
            }
        }
        this.factory.resetRecipeIngredientSources(sources.isEmpty() ? null : sources.toArray(new ItemSource[0]));
    }

    @Override
    public UniqueIdItem<ItemStack> uniqueEmptyItem() {
        return this.emptyUniqueItem;
    }

    @Override
    public void delayedInit() {
        Bukkit.getPluginManager().registerEvents(this.itemEventListener, this.plugin.javaPlugin());
        Bukkit.getPluginManager().registerEvents(this.debugStickListener, this.plugin.javaPlugin());
        Bukkit.getPluginManager().registerEvents(this.armorEventListener, this.plugin.javaPlugin());
        if (this.slotChangeListener != null) Bukkit.getPluginManager().registerEvents(this.slotChangeListener, this.plugin.javaPlugin());
    }

    @Override
    public NetworkItemHandler<ItemStack> networkItemHandler() {
        return this.networkItemHandler;
    }

    public static BukkitItemManager instance() {
        return instance;
    }

    @Override
    public Optional<Item<ItemStack>> s2c(Item<ItemStack> item, @Nullable Player player) {
        if (item.isEmpty()) return Optional.empty();
        return this.networkItemHandler.s2c(item, player);
    }

    @Override
    public Optional<Item<ItemStack>> c2s(Item<ItemStack> item) {
        if (item.isEmpty()) return Optional.empty();
        return this.networkItemHandler.c2s(item);
    }

    public Optional<ItemStack> s2c(ItemStack item, Player player) {
        if (item.isEmpty()) return Optional.empty();
        return this.networkItemHandler.s2c(wrap(item), player).map(Item::getItem);
    }

    public Optional<ItemStack> c2s(ItemStack item) {
        if (item.isEmpty()) return Optional.empty();
        return this.networkItemHandler.c2s(wrap(item)).map(Item::getItem);
    }

    @Override
    public Item<ItemStack> build(DatapackRecipeResult result) {
        if (result.components() == null) {
            ItemStack itemStack = createVanillaItemStack(Key.of(result.id()));
            return wrap(itemStack).count(result.count());
        } else {
            // 低版本无法应用nbt或组件,所以这里是1.20.5+
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("id", result.id());
            jsonObject.addProperty("count", result.count());
            jsonObject.add("components", result.components());
            Object nmsStack = ItemStackProxy.INSTANCE.getCodec().parse(MRegistryOps.JSON, jsonObject)
                    .resultOrPartial((error) -> plugin.logger().severe("Tried to load invalid item: '" + error + "'")).orElse(null);
            if (nmsStack == null) {
                return this.emptyItem;
            }
            return wrap(FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(nmsStack));
        }
    }

    @Override
    public Optional<BuildableItem<ItemStack>> getVanillaItem(Key key) {
        ItemStack vanilla = createVanillaItemStack(key);
        if (vanilla == null) {
            return Optional.empty();
        }
        return Optional.of(CloneableConstantItem.of(this.wrap(vanilla)));
    }

    @Override
    public int fuelTime(ItemStack itemStack) {
        if (ItemStackUtils.isEmpty(itemStack)) return 0;
        Optional<CustomItem<ItemStack>> customItem = wrap(itemStack).getCustomItem();
        return customItem.map(it -> it.settings().fuelTime()).orElse(0);
    }

    @Override
    public int fuelTime(Key id) {
        return getCustomItem(id).map(it -> it.settings().fuelTime()).orElse(0);
    }

    @Override
    public void disable() {
        this.unload();
        HandlerList.unregisterAll(this.itemEventListener);
        HandlerList.unregisterAll(this.debugStickListener);
        HandlerList.unregisterAll(this.armorEventListener);
        if (this.slotChangeListener != null) HandlerList.unregisterAll(this.slotChangeListener);
        this.persistLastRegisteredPatterns();
    }

    @Override
    protected void registerArmorTrimPattern(Collection<Key> equipments) {
        if (equipments.isEmpty()) return;
        this.lastRegisteredPatterns = new HashSet<>(equipments);
        // 可能还没加载
        if (Config.sacrificedAssetId() != null) {
            this.lastRegisteredPatterns.add(Config.sacrificedAssetId());
        }
        Object registryAccess = RegistryUtils.getRegistryAccess();
        Object registry = RegistryAccessProxy.INSTANCE.lookupOrThrow(registryAccess, MRegistries.TRIM_PATTERN);
        MappedRegistryProxy.INSTANCE.setFrozen(registry, false);
        for (Key assetId : this.lastRegisteredPatterns) {
            Object identifier = KeyUtils.toIdentifier(assetId);
            Object previous = RegistryUtils.getRegistryValue(registry, identifier);
            if (previous == null) {
                Object trimPattern = createTrimPattern(assetId);
                Object holder = RegistryProxy.INSTANCE.registerForHolder$1(registry, identifier, trimPattern);
                HolderProxy.ReferenceProxy.INSTANCE.bindValue(holder, trimPattern);
                HolderProxy.ReferenceProxy.INSTANCE.setTags(holder, Set.of());
            }
        }
        MappedRegistryProxy.INSTANCE.setFrozen(registry, true);
    }

    private void persistLastRegisteredPatterns() {
        Path persistTrimPatternPath = this.plugin.dataFolderPath()
                .resolve("cache")
                .resolve("trim_patterns.json");
        try {
            Files.createDirectories(persistTrimPatternPath.getParent());
            JsonObject json = new JsonObject();
            JsonArray jsonElements = new JsonArray();
            for (Key key : this.lastRegisteredPatterns) {
                jsonElements.add(new JsonPrimitive(key.toString()));
            }
            json.add("patterns", jsonElements);
            if (jsonElements.isEmpty()) {
                if (Files.exists(persistTrimPatternPath)) {
                    Files.delete(persistTrimPatternPath);
                }
            } else {
                GsonHelper.writeJsonFile(json, persistTrimPatternPath);
            }
        } catch (IOException e) {
            this.plugin.logger().warn("Failed to persist registered trim patterns.", e);
        }
    }

    // 需要持久化存储上一次注册的新trim类型，如果注册晚了，加载世界可能导致一些物品损坏
    private void loadLastRegisteredPatterns() {
        Path persistTrimPatternPath = this.plugin.dataFolderPath()
                .resolve("cache")
                .resolve("trim_patterns.json");
        if (Files.exists(persistTrimPatternPath) && Files.isRegularFile(persistTrimPatternPath)) {
            try {
                JsonObject cache = GsonHelper.readJsonFile(persistTrimPatternPath).getAsJsonObject();
                JsonArray patterns = cache.getAsJsonArray("patterns");
                Set<Key> trims = new HashSet<>();
                for (JsonElement element : patterns) {
                    if (element instanceof JsonPrimitive primitive) {
                        trims.add(Key.of(primitive.getAsString()));
                    }
                }
                this.registerArmorTrimPattern(trims);
                this.lastRegisteredPatterns = trims;
            } catch (IOException e) {
                this.plugin.logger().warn("Failed to load registered trim patterns.", e);
            }
        }
    }

    private void registerCustomTrimMaterial() {
        Object registryAccess = RegistryUtils.getRegistryAccess();
        Object registry = RegistryAccessProxy.INSTANCE.lookupOrThrow(registryAccess, MRegistries.TRIM_MATERIAL);
        Object identifier = KeyUtils.toIdentifier(Key.of("minecraft", AbstractPackManager.NEW_TRIM_MATERIAL));
        Object previous = RegistryUtils.getRegistryValue(registry, identifier);
        if (previous == null) {
            MappedRegistryProxy.INSTANCE.setFrozen(registry, false);
            Object trimMaterial = createTrimMaterial();
            Object holder = RegistryProxy.INSTANCE.registerForHolder$1(registry, identifier, trimMaterial);
            HolderProxy.ReferenceProxy.INSTANCE.bindValue(holder, trimMaterial);
            HolderProxy.ReferenceProxy.INSTANCE.setTags(holder, Set.of());
            MappedRegistryProxy.INSTANCE.setFrozen(registry, true);
        }
    }

    private Object createTrimPattern(Key key) {
        if (VersionHelper.isOrAbove1_21_5()) {
            return TrimPatternProxy.INSTANCE.newInstance(KeyUtils.toIdentifier(key), ComponentProxy.INSTANCE.empty(), false);
        } else if (VersionHelper.isOrAbove1_20_2()) {
            return TrimPatternProxy.INSTANCE.newInstance(KeyUtils.toIdentifier(key), this.bedrockItemHolder, ComponentProxy.INSTANCE.empty(), false);
        } else {
            return TrimPatternProxy.INSTANCE.newInstance(KeyUtils.toIdentifier(key), this.bedrockItemHolder, ComponentProxy.INSTANCE.empty());
        }
    }

    private Object createTrimMaterial() {
        if (VersionHelper.isOrAbove1_21_5()) {
            Object assetGroup = MaterialAssetGroupProxy.INSTANCE.create("custom");
            return TrimMaterialProxy.INSTANCE.newInstance(assetGroup, ComponentProxy.INSTANCE.empty());
        } else if (VersionHelper.isOrAbove1_21_4()) {
            return TrimMaterialProxy.INSTANCE.newInstance("custom", this.bedrockItemHolder, Map.of(), ComponentProxy.INSTANCE.empty());
        } else {
            return TrimMaterialProxy.INSTANCE.newInstance("custom", this.bedrockItemHolder, 0f, Map.of(), ComponentProxy.INSTANCE.empty());
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public Item<ItemStack> fromByteArray(byte[] bytes) {
        return this.factory.wrap(Bukkit.getUnsafe().deserializeItem(bytes));
    }

    @Override
    public ItemStack buildCustomItemStack(Key id, Player player) {
        return Optional.ofNullable(this.customItemsById.get(id)).map(it -> it.buildItemStack(ItemBuildContext.of(player), 1)).orElse(null);
    }

    @Override
    public ItemStack buildItemStack(Key id, @Nullable Player player) {
        ItemStack customItem = buildCustomItemStack(id, player);
        if (customItem != null) {
            return customItem;
        }
        return createVanillaItemStack(id);
    }

    @Override
    public Item<ItemStack> createCustomWrappedItem(Key id, Player player) {
        return Optional.ofNullable(customItemsById.get(id)).map(it -> it.buildItem(player)).orElse(null);
    }

    @Override
    public Item<ItemStack> createWrappedItem(Key id, @Nullable Player player) {
        CustomItem<ItemStack> customItem = this.customItemsById.get(id);
        if (customItem != null) {
            return customItem.buildItem(player);
        }
        ItemStack itemStack = this.createVanillaItemStack(id);
        if (itemStack != null) {
            return wrap(itemStack);
        }
        return null;
    }

    @Nullable
    private ItemStack createVanillaItemStack(Key id) {
        Object item = RegistryUtils.getRegistryValue(MBuiltInRegistries.ITEM, KeyUtils.toIdentifier(id));
        if (item == MItems.AIR && !id.equals(ItemKeys.AIR)) {
            return null;
        }
        return FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(ItemStackProxy.INSTANCE.newInstance(item, 1));
    }

    @Override
    public @NotNull Item<ItemStack> wrap(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return this.emptyItem;
        return this.factory.wrap(itemStack);
    }

    @Override
    protected CustomItem.Builder<ItemStack> createPlatformItemBuilder(UniqueKey id, Key materialId, Key clientBoundMaterialId) {
        Object item = RegistryUtils.getRegistryValue(MBuiltInRegistries.ITEM, KeyUtils.toIdentifier(materialId));
        Object clientBoundItem = materialId == clientBoundMaterialId ? item : RegistryUtils.getRegistryValue(MBuiltInRegistries.ITEM, KeyUtils.toIdentifier(clientBoundMaterialId));
        if (item == MItems.AIR) {
            throw new LocalizedResourceConfigException("warning.config.item.invalid_material", materialId.toString());
        }
        if (clientBoundItem == MItems.AIR) {
            throw new LocalizedResourceConfigException("warning.config.item.invalid_material", clientBoundMaterialId.toString());
        }
        return BukkitCustomItem.builder(item, clientBoundItem)
                .id(id)
                .material(materialId)
                .clientBoundMaterial(clientBoundMaterialId);
    }

    private void registerAllVanillaItems() {
        for (Object item : (Iterable<?>) MBuiltInRegistries.ITEM) {
            Object identifier = RegistryProxy.INSTANCE.getKey(MBuiltInRegistries.ITEM, item);
            Key itemKey = KeyUtils.identifierToKey(identifier);
            VANILLA_ITEMS.add(itemKey);
            super.cachedVanillaItemSuggestions.add(Suggestion.suggestion(itemKey.asString()));
            UniqueKey uniqueKey = UniqueKey.create(itemKey);
            Object mcHolder = RegistryProxy.INSTANCE.get$1(MBuiltInRegistries.ITEM, ResourceKeyProxy.INSTANCE.create(MRegistries.ITEM, identifier)).orElseThrow();
            Set<Object> tags = HolderProxy.ReferenceProxy.INSTANCE.getTags(mcHolder);
            for (Object tag : tags) {
                Key tagId = KeyUtils.identifierToKey(TagKeyProxy.INSTANCE.getLocation(tag));
                VANILLA_ITEM_TAGS.computeIfAbsent(tagId, (key) -> new ArrayList<>()).add(uniqueKey);
            }
        }
    }

    // 1.20-1.21.4 template 不为空
    // 1.21.5+ pattern 不为空
    @Override
    public Item<ItemStack> applyTrim(Item<ItemStack> base, Item<ItemStack> addition, Item<ItemStack> template, Key pattern) {
        Object registryAccess = RegistryUtils.getRegistryAccess();
        Optional<?> optionalMaterial;
        if (VersionHelper.isOrAbove1_20_5()) {
            optionalMaterial = TrimMaterialsProxy.INSTANCE.getFromIngredient$0(registryAccess, addition.getLiteralObject());
        } else {
            optionalMaterial = TrimMaterialsProxy.INSTANCE.getFromIngredient$1(registryAccess, addition.getLiteralObject());
        }
        Optional<?> optionalPattern;
        if (VersionHelper.isOrAbove1_21_5()) {
            optionalPattern = RegistryProxy.INSTANCE.get$0(RegistryAccessProxy.INSTANCE.lookupOrThrow(registryAccess, MRegistries.TRIM_PATTERN), KeyUtils.toIdentifier(pattern));
        } else if (VersionHelper.isOrAbove1_20_5()) {
            optionalPattern = TrimPatternsProxy.INSTANCE.getFromTemplate$1(registryAccess, template.getLiteralObject());
        } else {
            optionalPattern = TrimPatternsProxy.INSTANCE.getFromTemplate$0(registryAccess, template.getLiteralObject());
        }
        if (optionalMaterial.isPresent() && optionalPattern.isPresent()) {
            Object armorTrim = ArmorTrimProxy.INSTANCE.newInstance(optionalMaterial.get(), optionalPattern.get());
            Object previousTrim;
            if (VersionHelper.isOrAbove1_20_5()) {
                previousTrim = base.getExactComponent(DataComponentKeys.TRIM);
            } else {
                if (VersionHelper.isOrAbove1_20_2()) {
                    previousTrim = ArmorTrimProxy.INSTANCE.getTrim(registryAccess, base.getLiteralObject(), true);
                } else {
                    previousTrim = ArmorTrimProxy.INSTANCE.getTrim(registryAccess, base.getLiteralObject());
                }
            }
            if (armorTrim.equals(previousTrim)) {
                return this.emptyItem;
            }
            Item<ItemStack> newItem = base.copyWithCount(1);
            if (VersionHelper.isOrAbove1_20_5()) {
                newItem.setExactComponent(DataComponentKeys.TRIM, armorTrim);
            } else {
                ArmorTrimProxy.INSTANCE.setTrim(registryAccess, newItem.getLiteralObject(), armorTrim);
            }
            return newItem;
        }
        return this.emptyItem;
    }

    public void unlockRecipeOnInventoryChanged(org.bukkit.entity.Player player, Item<ItemStack> item) {
        Key itemId = item.id();
        BukkitServerPlayer serverPlayer = BukkitAdaptors.adapt(player);
        if (serverPlayer == null) return;
        serverPlayer.addObtainedItem(itemId);
        List<IngredientUnlockable> recipes = BukkitRecipeManager.instance().ingredientUnlockablesByChangedItem(itemId);
        if (recipes.isEmpty()) return;
        List<NamespacedKey> recipesToUnlock = new ArrayList<>(4);
        for (IngredientUnlockable recipe : recipes) {
            NamespacedKey recipeBukkitId = KeyUtils.toNamespacedKey(recipe.id());
            if (!player.hasDiscoveredRecipe(recipeBukkitId)) {
                if (recipe.canUnlock(serverPlayer, serverPlayer.obtainedItems())) {
                    recipesToUnlock.add(recipeBukkitId);
                }
            }
        }
        if (!recipesToUnlock.isEmpty()) {
            player.discoverRecipes(recipesToUnlock);
        }
    }
}
