package net.momirealms.craftengine.bukkit.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.item.behavior.AxeItemBehavior;
import net.momirealms.craftengine.bukkit.item.behavior.FlintAndSteelItemBehavior;
import net.momirealms.craftengine.bukkit.item.factory.BukkitItemFactory;
import net.momirealms.craftengine.bukkit.item.listener.ArmorEventListener;
import net.momirealms.craftengine.bukkit.item.listener.ItemEventListener;
import net.momirealms.craftengine.bukkit.item.listener.SlotChangeListener;
import net.momirealms.craftengine.bukkit.item.recipe.BukkitRecipeManager;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.RegistryOps;
import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.*;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.network.NetworkItemHandler;
import net.momirealms.craftengine.core.item.processor.ObfuscatedItemModelProcessor;
import net.momirealms.craftengine.core.item.recipe.DatapackRecipeResult;
import net.momirealms.craftengine.core.item.recipe.IngredientUnlockable;
import net.momirealms.craftengine.core.pack.AbstractPackManager;
import net.momirealms.craftengine.core.plugin.compatibility.ItemSource;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.core.MappedRegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.RegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.network.chat.ComponentProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.ResourceKeyProxy;
import net.momirealms.craftengine.proxy.minecraft.tags.TagKeyProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.equipment.trim.*;
import net.momirealms.sparrow.nbt.CompoundTag;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@SuppressWarnings("unchecked")
public final class BukkitItemManager extends AbstractItemManager {
    static {
        registerVanillaItemExtraBehavior(FlintAndSteelItemBehavior.INSTANCE, ItemKeys.FLINT_AND_STEEL);
        registerVanillaItemExtraBehavior(AxeItemBehavior.INSTANCE, ItemKeys.AXES);
    }

    private static BukkitItemManager instance;
    private final BukkitItemFactory<? extends BukkitItemWrapper> factory;
    private final BukkitCraftEngine plugin;
    private final ItemEventListener itemEventListener;
    private final ArmorEventListener armorEventListener;
    private final SlotChangeListener slotChangeListener;
    private final NetworkItemHandler networkItemHandler;
    private final Object bedrockItemHolder;
    private final BukkitItem emptyItem;
    private Set<Key> lastRegisteredPatterns = Set.of();
    private boolean hasExternalRecipeSource = false;
    private ItemSource[] recipeIngredientSources = null;

    public BukkitItemManager(BukkitCraftEngine plugin) {
        super(plugin);
        instance = this;
        this.plugin = plugin;
        this.factory = BukkitItemFactory.create(plugin);
        this.itemEventListener = new ItemEventListener(plugin, this);
        this.armorEventListener = new ArmorEventListener();
        this.slotChangeListener = VersionHelper.isOrAbove1_20_3 ? new SlotChangeListener(this) : null;
        this.networkItemHandler = VersionHelper.isOrAbove1_20_5 ? new ModernNetworkItemHandler(this) : new LegacyNetworkItemHandler();
        this.registerAllVanillaItems();
        this.bedrockItemHolder = Objects.requireNonNull(RegistryUtils.getHolder(BuiltInRegistriesProxy.ITEM, ResourceKeyProxy.INSTANCE.create(RegistriesProxy.ITEM, KeyUtils.toIdentifier(Key.of("minecraft:bedrock")))));
        this.registerCustomTrimMaterial();
        this.loadLastRegisteredPatterns();
        this.loadItemModelMappings();
        this.emptyItem = wrap(ItemStackProxy.EMPTY);
    }

    @Override
    public void delayedLoad() {
        super.delayedLoad();
        List<ItemSource> sources = new ArrayList<>();
        for (String externalSource : Config.recipeIngredientSources()) {
            String sourceId = externalSource.toLowerCase(Locale.ENGLISH);
            ItemSource itemSource = this.plugin.compatibilityManager().getItemSource(sourceId);
            if (itemSource != null) {
                sources.add(itemSource);
            }
        }
        if (sources.isEmpty()) {
            this.recipeIngredientSources = null;
            this.hasExternalRecipeSource = false;
        } else {
            this.recipeIngredientSources = sources.toArray(new ItemSource[0]);
            this.hasExternalRecipeSource = true;
        }
    }

    @Override
    public UniqueKey getIngredientKey(Item item) {
        if (item.isEmpty()) {
            return null;
        }
        if (this.hasExternalRecipeSource) {
            for (ItemSource source : this.recipeIngredientSources) {
                String id = source.id(item);
                if (id != null) {
                    return UniqueKey.create(Key.of(source.plugin(), StringUtils.normalizeString(id)));
                }
            }
        }
        return UniqueKey.create(item.id());
    }

    @Override
    public void delayedInit() {
        Bukkit.getPluginManager().registerEvents(this.itemEventListener, this.plugin.javaPlugin());
        Bukkit.getPluginManager().registerEvents(this.armorEventListener, this.plugin.javaPlugin());
        if (this.slotChangeListener != null) Bukkit.getPluginManager().registerEvents(this.slotChangeListener, this.plugin.javaPlugin());
    }

    public NetworkItemHandler networkItemHandler() {
        return this.networkItemHandler;
    }

    public static BukkitItemManager instance() {
        return instance;
    }

    @Override
    public Optional<Item> s2c(Item item, @Nullable Player player) {
        if (item.isEmpty()) return Optional.empty();
        return this.networkItemHandler.s2c(item, player);
    }

    @Override
    public Optional<Item> c2s(Item item) {
        if (item.isEmpty()) return Optional.empty();
        return this.networkItemHandler.c2s(item);
    }

    public Optional<ItemStack> s2c(ItemStack item, Player player) {
        if (item.isEmpty()) return Optional.empty();
        return this.networkItemHandler.s2c(wrap(item), player).map(ItemStackUtils::getBukkitStack);
    }

    public Optional<ItemStack> c2s(ItemStack item) {
        if (item.isEmpty()) return Optional.empty();
        return this.networkItemHandler.c2s(wrap(item)).map(ItemStackUtils::getBukkitStack);
    }

    @Override
    public Item build(DatapackRecipeResult result) {
        if (result.components() == null) {
            ItemStack itemStack = createVanillaItemStack(Key.of(result.id()));
            return wrap(itemStack).count(result.count());
        } else {
            // 低版本无法应用nbt或组件,所以这里是1.20.5+
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("id", result.id());
            jsonObject.addProperty("count", result.count());
            jsonObject.add("components", result.components());
            Object nmsStack = ItemStackProxy.INSTANCE.getCodec().parse(RegistryOps.JSON, jsonObject)
                    .resultOrPartial((error) -> plugin.logger().error("Tried to load invalid item: '" + error + "'")).orElse(null);
            if (nmsStack == null) {
                return this.emptyItem;
            }
            return wrap(ItemStackUtils.getBukkitStack(nmsStack));
        }
    }

    @Override
    public Optional<BuildableItem> getVanillaItem(Key key) {
        ItemStack vanilla = createVanillaItemStack(key);
        if (vanilla == null) {
            return Optional.empty();
        }
        return Optional.of(CloneableItem.of(this.wrap(vanilla)));
    }

    @Override
    public void disable() {
        this.unload();
        HandlerList.unregisterAll(this.itemEventListener);
        HandlerList.unregisterAll(this.armorEventListener);
        if (this.slotChangeListener != null) HandlerList.unregisterAll(this.slotChangeListener);
    }

    @Override
    protected void registerArmorTrimPattern(Collection<Key> equipments) {
        if (equipments.isEmpty()) return;
        this.lastRegisteredPatterns = new HashSet<>(equipments);
        // 可能还没加载
        if (Config.sacrificedAssetId() != null) {
            this.lastRegisteredPatterns.add(Config.sacrificedAssetId());
        }
        Object registry = RegistryUtils.lookupOrThrow(RegistriesProxy.TRIM_PATTERN);
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
        this.persistLastRegisteredPatterns();
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

    public void persistItemModelMappings() {
        Path itemModelObfPath = this.plugin.dataFolderPath()
                .resolve("cache")
                .resolve("item_model_obfuscation.json");
        try {
            Files.createDirectories(itemModelObfPath.getParent());
            JsonObject json = new JsonObject();
            for (Map.Entry<Key, Key> entry : ObfuscatedItemModelProcessor.getMappings().entrySet()) {
                json.addProperty(entry.getKey().toString(), entry.getValue().toString());
            }
            GsonHelper.writeJsonFile(json, itemModelObfPath);
        } catch (IOException e) {
            this.plugin.logger().warn("Failed to persist item model obfuscation mappings.", e);
        }
    }

    private void loadItemModelMappings() {
        Path itemModelObfPath = this.plugin.dataFolderPath()
                .resolve("cache")
                .resolve("item_model_obfuscation.json");
        if (Files.exists(itemModelObfPath) && Files.isRegularFile(itemModelObfPath)) {
            try {
                JsonObject cache = GsonHelper.readJsonObjectFromFile(itemModelObfPath);
                if (cache == null) return;
                Map<Key, Key> mappings = new HashMap<>();
                for (Map.Entry<String, JsonElement> entry : cache.entrySet()) {
                    if (entry.getValue() instanceof JsonPrimitive primitive) {
                        mappings.put(Key.of(entry.getKey()), Key.of(primitive.getAsString()));
                    }
                }
                ObfuscatedItemModelProcessor.setMappings(mappings);
            } catch (IOException e) {
                this.plugin.logger().warn("Failed to load item model obfuscation mappings.", e);
            }
        }
    }

    // 需要持久化存储上一次注册的新trim类型，如果注册晚了，加载世界可能导致一些物品损坏
    private void loadLastRegisteredPatterns() {
        Path persistTrimPatternPath = this.plugin.dataFolderPath()
                .resolve("cache")
                .resolve("trim_patterns.json");
        if (Files.exists(persistTrimPatternPath) && Files.isRegularFile(persistTrimPatternPath)) {
            try {
                JsonObject cache = GsonHelper.readJsonObjectFromFile(persistTrimPatternPath);
                if (cache == null) return;
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
        Object registry = RegistryUtils.lookupOrThrow(RegistriesProxy.TRIM_MATERIAL);
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
        if (VersionHelper.isOrAbove1_21_5) {
            return TrimPatternProxy.INSTANCE.newInstance(KeyUtils.toIdentifier(key), ComponentProxy.INSTANCE.empty(), false);
        } else if (VersionHelper.isOrAbove1_20_2) {
            return TrimPatternProxy.INSTANCE.newInstance(KeyUtils.toIdentifier(key), this.bedrockItemHolder, ComponentProxy.INSTANCE.empty(), false);
        } else {
            return TrimPatternProxy.INSTANCE.newInstance(KeyUtils.toIdentifier(key), this.bedrockItemHolder, ComponentProxy.INSTANCE.empty());
        }
    }

    private Object createTrimMaterial() {
        if (VersionHelper.isOrAbove1_21_5) {
            Object assetGroup = MaterialAssetGroupProxy.INSTANCE.create("custom");
            return TrimMaterialProxy.INSTANCE.newInstance(assetGroup, ComponentProxy.INSTANCE.empty());
        } else if (VersionHelper.isOrAbove1_21_4) {
            return TrimMaterialProxy.INSTANCE.newInstance("custom", this.bedrockItemHolder, Map.of(), ComponentProxy.INSTANCE.empty());
        } else {
            return TrimMaterialProxy.INSTANCE.newInstance("custom", this.bedrockItemHolder, 0f, Map.of(), ComponentProxy.INSTANCE.empty());
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public BukkitItem fromBytes(byte[] bytes) {
        return wrap(Bukkit.getUnsafe().deserializeItem(bytes));
    }

    @Override
    public Item fromNBT(CompoundTag tag) {
        return wrap(ItemStackUtils.parseMinecraftItem(tag, VersionHelper.WORLD_VERSION));
    }

    @Override
    public BukkitItem createCustomWrappedItem(Key id, Player player) {
        return Optional.ofNullable(itemDefinitionById.get(id)).map(it -> (BukkitItem) it.buildItem(player)).orElse(null);
    }

    @Override
    public BukkitItem createWrappedItem(Key id, @Nullable Player player) {
        ItemDefinition itemDefinition = this.itemDefinitionById.get(id);
        if (itemDefinition != null) {
            return (BukkitItem) itemDefinition.buildItem(player);
        }
        ItemStack itemStack = this.createVanillaItemStack(id);
        if (itemStack != null) {
            return wrap(itemStack);
        }
        return null;
    }

    @Nullable
    private ItemStack createVanillaItemStack(Key id) {
        Object item = RegistryUtils.getRegistryValue(BuiltInRegistriesProxy.ITEM, KeyUtils.toIdentifier(id));
        if (item == ItemsProxy.AIR && !id.equals(ItemKeys.AIR)) {
            return null;
        }
        return ItemStackUtils.getBukkitStack(ItemStackProxy.INSTANCE.newInstance(item, 1));
    }

    @Override
    public @NotNull BukkitItem wrap(Object itemStack) {
        if (itemStack == null) return this.emptyItem;
        return new BukkitItem((ItemFactory<BukkitItemWrapper>) this.factory, this.factory.wrap(itemStack));
    }

    @Override
    protected ItemDefinition.Builder createPlatformItemBuilder(UniqueKey id, Key materialId, Key clientBoundMaterialId) {
        Object item = RegistryUtils.getRegistryValue(BuiltInRegistriesProxy.ITEM, KeyUtils.toIdentifier(materialId));
        Object clientBoundItem = materialId == clientBoundMaterialId ? item : RegistryUtils.getRegistryValue(BuiltInRegistriesProxy.ITEM, KeyUtils.toIdentifier(clientBoundMaterialId));
        if (item == ItemsProxy.AIR) {
            throw new KnownResourceException("resource.item.invalid_material", materialId.toString());
        }
        if (clientBoundItem == ItemsProxy.AIR) {
            throw new KnownResourceException("resource.item.invalid_material", clientBoundMaterialId.toString());
        }
        return BukkitItemDefinition.builder(item, clientBoundItem)
                .id(id)
                .material(materialId)
                .clientBoundMaterial(clientBoundMaterialId);
    }

    private void registerAllVanillaItems() {
        for (Object item : (Iterable<?>) BuiltInRegistriesProxy.ITEM) {
            Object identifier = RegistryProxy.INSTANCE.getKey(BuiltInRegistriesProxy.ITEM, item);
            Key itemKey = KeyUtils.identifierToKey(identifier);
            VANILLA_ITEMS.add(itemKey);
            UniqueKey uniqueKey = UniqueKey.create(itemKey);
            Object mcHolder = Objects.requireNonNull(RegistryUtils.getHolder(BuiltInRegistriesProxy.ITEM, ResourceKeyProxy.INSTANCE.create(RegistriesProxy.ITEM, identifier)));
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
    public Item applyTrim(Item base, Item addition, Item template, Key pattern) {
        Object registryAccess = RegistryUtils.getRegistryAccess();
        Optional<?> optionalMaterial;
        if (VersionHelper.isOrAbove26_1) {
            optionalMaterial = (Optional<?>) addition.getExactComponent(DataComponentKeys.PROVIDES_TRIM_MATERIAL);
        } else if (VersionHelper.isOrAbove1_20_5) {
            optionalMaterial = TrimMaterialsProxy.INSTANCE.getFromIngredient$0(registryAccess, addition.minecraftItem());
        } else {
            optionalMaterial = TrimMaterialsProxy.INSTANCE.getFromIngredient$1(registryAccess, addition.minecraftItem());
        }
        Optional<?> optionalPattern;
        if (VersionHelper.isOrAbove1_21_5) {
            optionalPattern = RegistryProxy.INSTANCE.get$0(RegistryUtils.lookupOrThrow(RegistriesProxy.TRIM_PATTERN), KeyUtils.toIdentifier(pattern));
        } else if (VersionHelper.isOrAbove1_20_5) {
            optionalPattern = TrimPatternsProxy.INSTANCE.getFromTemplate$1(registryAccess, template.minecraftItem());
        } else {
            optionalPattern = TrimPatternsProxy.INSTANCE.getFromTemplate$0(registryAccess, template.minecraftItem());
        }
        if (optionalMaterial.isPresent() && optionalPattern.isPresent()) {
            Object armorTrim = ArmorTrimProxy.INSTANCE.newInstance(optionalMaterial.get(), optionalPattern.get());
            Object previousTrim;
            if (VersionHelper.isOrAbove1_20_5) {
                previousTrim = base.getExactComponent(DataComponentKeys.TRIM);
            } else {
                if (VersionHelper.isOrAbove1_20_2) {
                    previousTrim = ArmorTrimProxy.INSTANCE.getTrim(registryAccess, base.minecraftItem(), true);
                } else {
                    previousTrim = ArmorTrimProxy.INSTANCE.getTrim(registryAccess, base.minecraftItem());
                }
            }
            if (armorTrim.equals(previousTrim)) {
                return this.emptyItem;
            }
            Item newItem = base.copyWithCount(1);
            if (VersionHelper.isOrAbove1_20_5) {
                newItem.setExactComponent(DataComponentKeys.TRIM, armorTrim);
            } else {
                ArmorTrimProxy.INSTANCE.setTrim(registryAccess, newItem.minecraftItem(), armorTrim);
            }
            return newItem;
        }
        return this.emptyItem;
    }

    public void unlockRecipeOnInventoryChanged(org.bukkit.entity.Player player, Item item) {
        Key itemId = item.id();
        BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
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

    @Override
    public Item emptyItem() {
        return this.emptyItem;
    }
}
