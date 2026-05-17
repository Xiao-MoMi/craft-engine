package net.momirealms.craftengine.core.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.item.behavior.*;
import net.momirealms.craftengine.core.item.customdata.*;
import net.momirealms.craftengine.core.item.equipment.*;
import net.momirealms.craftengine.core.item.processor.*;
import net.momirealms.craftengine.core.item.setting.ItemSettings;
import net.momirealms.craftengine.core.item.updater.ItemUpdateConfig;
import net.momirealms.craftengine.core.item.updater.ItemUpdateResult;
import net.momirealms.craftengine.core.item.updater.ItemUpdater;
import net.momirealms.craftengine.core.item.updater.ItemUpdaters;
import net.momirealms.craftengine.core.pack.AbstractPackManager;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.PendingConfigSection;
import net.momirealms.craftengine.core.pack.allocator.IdAllocator;
import net.momirealms.craftengine.core.pack.model.definition.*;
import net.momirealms.craftengine.core.pack.model.definition.select.ChargeTypeSelectProperty;
import net.momirealms.craftengine.core.pack.model.definition.select.TrimMaterialSelectProperty;
import net.momirealms.craftengine.core.pack.model.generation.AbstractModelGenerator;
import net.momirealms.craftengine.core.pack.model.legacy.LegacyItemModel;
import net.momirealms.craftengine.core.pack.model.legacy.LegacyModelPredicate;
import net.momirealms.craftengine.core.pack.model.legacy.LegacyOverridesModel;
import net.momirealms.craftengine.core.pack.model.simplified.SimplifiedModelReader;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.*;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStage;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStages;
import net.momirealms.craftengine.core.plugin.context.CommonFunctions;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.EventTrigger;
import net.momirealms.craftengine.core.plugin.context.number.ConstantNumberProvider;
import net.momirealms.craftengine.core.util.*;
import org.incendo.cloud.suggestion.Suggestion;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class AbstractItemManager extends AbstractModelGenerator implements ItemManager {
    protected static final Map<Key, ItemBehavior> VANILLA_ITEM_EXTRA_BEHAVIORS = new HashMap<>();
    protected static final Set<Key> VANILLA_ITEMS = new HashSet<>(1024);
    protected static final Map<Key, List<UniqueKey>> VANILLA_ITEM_TAGS = new HashMap<>();
    // 解析器
    private final ItemParser itemParser;
    private final EquipmentParser equipmentParser;
    // 缓存
    protected final Map<Key, ItemDefinition> itemDefinitionById = new ConcurrentHashMap<>();
    protected final Map<String, ItemDefinition> itemDefinitionByPath = new ConcurrentHashMap<>();
    protected final Map<Key, List<UniqueKey>> customItemTags = new HashMap<>();
    protected final Map<Key, ModernItemModel> modernItemModels1_21_4 = new ConcurrentHashMap<>();
    protected final Map<Key, TreeSet<LegacyOverridesModel>> modernItemModels1_21_2 = new ConcurrentHashMap<>();
    protected final Map<Key, TreeSet<LegacyOverridesModel>> legacyOverrides = new ConcurrentHashMap<>();
    protected final Map<Key, TreeMap<Integer, ModernItemModel>> modernOverrides = new ConcurrentHashMap<>();
    protected final Map<Key, Equipment> equipments = new ConcurrentHashMap<>();
    protected final Map<Key, ItemDefinition> dyeableItems = new ConcurrentHashMap<>();
    // 指令补全
    protected final List<Suggestion> cachedCustomItemSuggestions = new ObjectArrayList<>();
    protected final List<Suggestion> cachedTotemSuggestions = new ObjectArrayList<>();
    // 替代配方材料
    protected final Map<Key, List<UniqueKey>> ingredientSubstitutes = new HashMap<>();
    // 有序物品id
    protected final List<Key> orderedItemIds = new ObjectArrayList<>();
    // 其他设置
    protected boolean featureFlag$keepOnDeathChance = false;
    protected boolean featureFlag$destroyOnDeathChance = false;

    protected AbstractItemManager(CraftEngine plugin) {
        super(plugin);
        this.itemParser = new ItemParser();
        this.equipmentParser = new EquipmentParser();
        CustomDataSerializers.registerSerializer(FurnitureDebugStickData.class, FurnitureDebugStickDataSerializer.INSTANCE);
        CustomDataSerializers.registerSerializer(BlockDebugStickData.class, BlockDebugStickDataSerializer.INSTANCE);
    }

    protected static void registerVanillaItemExtraBehavior(ItemBehavior behavior, Key... items) {
        for (Key key : items) {
            VANILLA_ITEM_EXTRA_BEHAVIORS.put(key, behavior);
        }
    }

    @Override
    public ConfigParser[] parsers() {
        return new ConfigParser[]{this.itemParser, this.equipmentParser};
    }

    @Override
    public void unload() {
        super.clearModelsToGenerate();
        this.clearFeatureFlags();
        this.itemDefinitionById.clear();
        this.itemDefinitionByPath.clear();
        this.cachedCustomItemSuggestions.clear();
        this.cachedTotemSuggestions.clear();
        this.legacyOverrides.clear();
        this.modernOverrides.clear();
        this.customItemTags.clear();
        this.equipments.clear();
        this.modernItemModels1_21_4.clear();
        this.modernItemModels1_21_2.clear();
        this.ingredientSubstitutes.clear();
        this.orderedItemIds.clear();
        this.dyeableItems.clear();
    }

    private void clearFeatureFlags() {
        this.featureFlag$keepOnDeathChance = false;
        this.featureFlag$destroyOnDeathChance = false;
    }

    @Override
    public Map<Key, Equipment> equipments() {
        return Collections.unmodifiableMap(this.equipments);
    }

    @Override
    public Optional<Equipment> getEquipment(Key key) {
        return Optional.ofNullable(this.equipments.get(key));
    }

    @Override
    public Optional<ItemDefinition> getItemDefinition(Key key) {
        return Optional.ofNullable(this.itemDefinitionById.get(key));
    }

    @Override
    public Optional<ItemDefinition> getItemDefinitionByPath(String path) {
        return Optional.ofNullable(this.itemDefinitionByPath.get(path));
    }

    @Override
    public List<UniqueKey> getIngredientSubstitutes(Key item) {
        if (VANILLA_ITEMS.contains(item)) {
            return Optional.ofNullable(this.ingredientSubstitutes.get(item)).orElse(Collections.emptyList());
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public ItemUpdateResult updateItem(Item item, Supplier<ItemBuildContext> contextSupplier) {
        Optional<ItemDefinition> optionalCustomItem = item.getDefinition();
        if (optionalCustomItem.isPresent()) {
            ItemDefinition itemDefinition = optionalCustomItem.get();
            Optional<ItemUpdateConfig> updater = itemDefinition.updater();
            if (updater.isPresent()) {
                return updater.get().update(item, contextSupplier);
            }
        }
        return new ItemUpdateResult(item, false, false);
    }

    @Override
    public List<UniqueKey> vanillaItemIdsByTag(Key tag) {
        return Collections.unmodifiableList(VANILLA_ITEM_TAGS.getOrDefault(tag, List.of()));
    }

    @Override
    public List<UniqueKey> customItemIdsByTag(Key tag) {
        return Collections.unmodifiableList(this.customItemTags.getOrDefault(tag, List.of()));
    }

    @Override
    public Collection<Suggestion> cachedCustomItemSuggestions() {
        return Collections.unmodifiableCollection(this.cachedCustomItemSuggestions);
    }

    @Override
    public Collection<Suggestion> cachedTotemSuggestions() {
        return Collections.unmodifiableCollection(this.cachedTotemSuggestions);
    }

    @Override
    public Optional<ItemBehavior> getItemBehavior(Key key) {
        Optional<ItemDefinition> definitionOptional = getItemDefinition(key);
        return definitionOptional.map(ItemDefinition::behavior).or(() -> Optional.ofNullable(VANILLA_ITEM_EXTRA_BEHAVIORS.get(key)));
    }

    @Override
    public Map<Key, ItemDefinition> loadedItems() {
        return Collections.unmodifiableMap(this.itemDefinitionById);
    }

    public List<Key> orderedItemIds() {
        return this.orderedItemIds;
    }

    @Override
    public Map<Key, ModernItemModel> modernItemModels1_21_4() {
        return Collections.unmodifiableMap(this.modernItemModels1_21_4);
    }

    @Override
    public Map<Key, TreeSet<LegacyOverridesModel>> modernItemModels1_21_2() {
        return Collections.unmodifiableMap(this.modernItemModels1_21_2);
    }

    @Override
    public Collection<Key> vanillaItems() {
        return Collections.unmodifiableCollection(VANILLA_ITEMS);
    }

    @Override
    public Map<Key, TreeSet<LegacyOverridesModel>> legacyItemOverrides() {
        return Collections.unmodifiableMap(this.legacyOverrides);
    }

    @Override
    public Map<Key, TreeMap<Integer, ModernItemModel>> modernItemOverrides() {
        return Collections.unmodifiableMap(this.modernOverrides);
    }

    @Override
    public boolean isVanillaItem(Key item) {
        return VANILLA_ITEMS.contains(item);
    }

    public boolean featureFlag$keepOnDeathChance() {
        return featureFlag$keepOnDeathChance;
    }

    public boolean featureFlag$destroyOnDeathChance() {
        return featureFlag$destroyOnDeathChance;
    }

    protected abstract ItemDefinition.Builder createPlatformItemBuilder(UniqueKey id, Key material, Key clientBoundMaterial);

    protected abstract void registerArmorTrimPattern(Collection<Key> equipments);

    // 26.1 +
    public Map<Key, ItemDefinition> dyeableItems() {
        return this.dyeableItems;
    }

    private final class EquipmentParser extends IdSectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] {"equipments", "equipment"};

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public void parseSection(@NotNull Pack pack, @NotNull Path path, @NotNull Key id, @NotNull ConfigSection section) {
            Equipment equipment = Equipments.fromConfig(id, section);
            AbstractItemManager.this.equipments.put(id, equipment);
        }

        @Override
        public void postProcess() {
            List<Key> trims = AbstractItemManager.this.equipments.values().stream()
                    .filter(TrimBasedEquipment.class::isInstance)
                    .map(Equipment::assetId)
                    .toList();
            registerArmorTrimPattern(trims);
        }

        @Override
        public boolean async() {
            return Config.multiThreadedConfigLoad();
        }

        @Override
        public LoadingStage loadingStage() {
            return LoadingStages.EQUIPMENT;
        }

        @Override
        public List<LoadingStage> dependencies() {
            return List.of(LoadingStages.TEMPLATE);
        }

        @Override
        public int count() {
            return AbstractItemManager.this.equipments.size();
        }
    }

    public void addOrMergeEquipment(ComponentBasedEquipment equipment) {
        Equipment previous = this.equipments.get(equipment.assetId());
        if (previous instanceof ComponentBasedEquipment another) {
            for (Map.Entry<EquipmentLayerType, List<ComponentBasedEquipment.Layer>> entry : equipment.layers().entrySet()) {
                another.addLayer(entry.getKey(), entry.getValue());
            }
        } else {
            this.equipments.put(equipment.assetId(), equipment);
        }
    }

    private final class ItemParser extends IdSectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] {"items", "item"};
        private final Map<Key, IdAllocator> idAllocators = new HashMap<>();
        private final List<CompletableFuture<?>> futures = Collections.synchronizedList(new ArrayList<>());

        @Override
        public int count() {
            return AbstractItemManager.this.itemDefinitionById.size();
        }

        private boolean isModernFormatRequired() {
            return Config.packMaxVersion().isAtOrAbove(MinecraftVersion.V1_21_4);
        }

        private boolean needsLegacyCompatibility() {
            return Config.packMinVersion().isBelow(MinecraftVersion.V1_21_4) || Config.alwaysGenerateModelOverrides();
        }

        private boolean needsCustomModelDataCompatibility() {
            return Config.packMinVersion().isBelow(MinecraftVersion.V1_21_2) || Config.alwaysUseCustomModelData();
        }

        private boolean needsItemModelCompatibility() {
            return Config.packMaxVersion().isAtOrAbove(MinecraftVersion.V1_21_2) && VersionHelper.isOrAbove1_21_2; //todo 能否通过客户端包解决问题
        }

        @Override
        public boolean async() {
            return Config.multiThreadedConfigLoad();
        }

        @Override
        public LoadingStage loadingStage() {
            return LoadingStages.ITEM;
        }

        @Override
        public List<LoadingStage> dependencies() {
            return List.of(LoadingStages.EQUIPMENT);
        }

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public void preProcess() {
            if (!this.idAllocators.isEmpty()) {
                this.idAllocators.clear();
            }
            if (!this.futures.isEmpty()) {
                this.futures.clear();
            }
            ObfuscatedItemModelProcessor.CAN_OBF.clear();
        }

        @Override
        public void postProcess() {
            for (Map.Entry<Key, IdAllocator> entry : this.idAllocators.entrySet()) {
                IdAllocator idAllocator = entry.getValue();
                idAllocator.processPendingAllocations();
                try {
                    idAllocator.saveToCache();
                } catch (IOException e) {
                    AbstractItemManager.this.plugin.logger().warn("Error while saving custom model data allocation for material " + entry.getKey().asString(), e);
                }
            }
            for (CompletableFuture<?> future : this.futures) {
                try {
                    future.join();
                } catch (CompletionException e) {
                    if (e.getCause() instanceof IdAllocator.IdExhaustedException || e.getCause() instanceof IdAllocator.IdConflictException) {
                        continue;
                    }
                    AbstractItemManager.this.plugin.logger().warn("Error while assigning custom model data", e);
                }
            }
            this.futures.clear();

            // 获取有序的物品id
            int size = this.pendingConfigSections.size();
            Object[] pendingElements = this.pendingConfigSections.elements();
            for (int i = 0; i < size; i++) {
                PendingConfigSection pending = (PendingConfigSection) pendingElements[i];
                ItemDefinition itemDefinition = AbstractItemManager.this.itemDefinitionById.get(pending.id);
                if (itemDefinition != null) {
                    Key id = itemDefinition.id();
                    AbstractItemManager.this.orderedItemIds.add(id);
                    if (itemDefinition.isVanillaItem()) continue;
                    // cache command suggestions
                    AbstractItemManager.this.cachedCustomItemSuggestions.add(Suggestion.suggestion(id.asString()));
                    // totem animations
                    if (VersionHelper.isOrAbove1_21_2) {
                        AbstractItemManager.this.cachedTotemSuggestions.add(Suggestion.suggestion(id.asString()));
                    } else if (itemDefinition.material().equals(ItemKeys.TOTEM_OF_UNDYING)) {
                        AbstractItemManager.this.cachedTotemSuggestions.add(Suggestion.suggestion(id.asString()));
                    }
                    // tags
                    ItemSettings settings = itemDefinition.settings();
                    Set<Key> tags = settings.tags();
                    for (Key tag : tags) {
                        AbstractItemManager.this.customItemTags.computeIfAbsent(tag, k -> new ArrayList<>()).add(itemDefinition.uniqueId());
                    }
                    // ingredient substitutes
                    List<Key> substitutes = settings.ingredientSubstitutes();
                    if (!substitutes.isEmpty()) {
                        for (Key key : substitutes) {
                            if (VANILLA_ITEMS.contains(key)) {
                                AbstractItemManager.this.ingredientSubstitutes.computeIfAbsent(key, k -> new ArrayList<>()).add(itemDefinition.uniqueId());
                            }
                        }
                    }
                    if (settings.keepOnDeathChance() != 0) {
                        AbstractItemManager.this.featureFlag$keepOnDeathChance = true;
                    }
                    if (settings.destroyOnDeathChance() != 0) {
                        AbstractItemManager.this.featureFlag$destroyOnDeathChance = true;
                    }
                }
            }
        }

        // 创建或获取已有的自动分配器
        private synchronized IdAllocator getOrCreateIdAllocator(Key key) {
            return this.idAllocators.computeIfAbsent(key, k -> {
                IdAllocator newAllocator = new IdAllocator(AbstractItemManager.this.plugin.dataFolderPath().resolve("cache").resolve("custom_model_data").resolve(k.value() + ".json"));
                newAllocator.reset(Config.customModelDataStartingValue(k), 16_777_216);
                try {
                    newAllocator.loadFromCache();
                } catch (IOException e) {
                    AbstractItemManager.this.plugin.logger().warn("Error while loading custom model data from cache for material " + k.asString(), e);
                }
                return newAllocator;
            });
        }

        private static final String[] MODEL_KEYS = new String[] {"model", "models", "texture", "textures", "legacy-model", "legacy_model"};
        private static final String[] CLIENT_BOUND_MATERIAL = new String[] {"client_bound_material", "client-bound-material"};
        private static final String[] CUSTOM_MODEL_DATA = new String[] {"custom_model_data", "custom-model-data"};
        private static final String[] ITEM_MODEL = new String[] {"item_model", "item-model"};
        private static final String[] CLIENT_BOUND_MODEL = new String[] {"client_bound_model", "client-bound-model"};
        private static final String[] CLIENT_BOUND_DATA = new String[] {"client_bound_data", "client-bound-data"};
        private static final String[] MODEL = new String[] {"model", "models"};
        private static final String[] TEXTURES = new String[] {"texture", "textures"};
        private static final String[] EVENTS = new String[] {"events", "event"};
        private static final String[] BEHAVIORS = new String[] {"behaviors", "behavior"};
        private static final String[] LEGACY_MODEL = new String[] {"legacy_model", "legacy-model"};
        private static final String[] OVERSIZED_IN_GUI = new String[] {"oversized_in_gui", "oversized-in-gui"};
        private static final String[] HAND_ANIMATION_ON_SWAP = new String[] {"hand_animation_on_swap", "hand-animation-on-swap"};
        private static final String[] SWAP_ANIMATION_SCALE = new String[] {"swap_animation_scale", "swap-animation-scale"};
        private static final String[] CATEGORIES = new String[] {"category", "categories"};
        private static final String[] SKIP_OBFUSCATION = new String[] {"skip_obfuscation", "skip-obfuscation"};

        @Override
        public void parseSection(@NotNull Pack pack, @NotNull Path path, @NotNull Key id, @NotNull ConfigSection section) {
            // 创建UniqueKey，仅缓存用
            UniqueKey uniqueId = UniqueKey.create(id);
            // 判断是不是原版物品
            boolean isVanillaItem = isVanillaItem(id);
            // 读取服务端侧材质
            Key material = isVanillaItem ? id : section.getValue("material", ConfigValue::getAsIdentifier, Config.defaultMaterial());
            // 读取客户端侧材质
            ConfigValue clientBoundMaterialValue = section.getValue(CLIENT_BOUND_MATERIAL);
            Key clientBoundMaterial = VersionHelper.PREMIUM && clientBoundMaterialValue != null ? clientBoundMaterialValue.getAsIdentifier() : material;

            // custom model data
            CompletableFuture<Integer> customModelDataFuture;
            boolean forceCustomModelData;

            if (!isVanillaItem) {
                // 如果用户指定了，说明要手动分配，不管他是什么版本，都强制设置模型值
                ConfigValue customModelDataValue = section.getValue(CUSTOM_MODEL_DATA);
                if (customModelDataValue != null) {
                    int customModelData = customModelDataValue.getAsInt();
                    if (customModelData > 0) {
                        if (customModelData > 16_777_216) {
                            throw new KnownResourceException("number.no_greater_than", customModelDataValue.path(), "custom_model_data", "16777216");
                        }
                        forceCustomModelData = true;
                        if (section.containsKey(MODEL_KEYS)) {
                            customModelDataFuture = getOrCreateIdAllocator(clientBoundMaterial).assignFixedId(id.asString(), customModelData);
                        } else {
                            customModelDataFuture = CompletableFuture.completedFuture(customModelData);
                        }
                    } else {
                        // 视为未分配
                        forceCustomModelData = false;
                        customModelDataFuture = CompletableFuture.completedFuture(0);
                    }
                }
                // 用户没指定custom-model-data，则看当前资源包版本兼容需求
                else {
                    forceCustomModelData = false;
                    // 如果最低版本要1.21.1以下支持
                    if (needsCustomModelDataCompatibility()) {
                        customModelDataFuture = getOrCreateIdAllocator(clientBoundMaterial).requestAutoId(id.asString());
                    }
                    // 否则不主动分配模型值
                    else {
                        customModelDataFuture = CompletableFuture.completedFuture(0);
                    }
                }
            } else {
                forceCustomModelData = false;
                // 原版物品不应该有这个
                customModelDataFuture = CompletableFuture.completedFuture(0);
            }

            // 当模型值完成分配的时候
            this.futures.add(customModelDataFuture.whenCompleteAsync((cmd, throwable) -> ResourceConfigUtils.runCatching(path, section.path(), () -> {
                int customModelData;
                if (throwable != null) {
                    if (throwable instanceof CompletionException e) {
                        // 检测custom model data 冲突
                        if (e.getCause() instanceof IdAllocator.IdConflictException exception) {
                            if (section.containsKey(MODEL_KEYS)) {
                                error(new KnownResourceException(path, "resource.item.custom_model_data_conflict", section.path(), String.valueOf(exception.id()), exception.previousOwner()));
                                return;
                            }
                            customModelData = exception.id();
                        }
                        // custom model data 已被用尽，不太可能
                        else if (e.getCause() instanceof IdAllocator.IdExhaustedException) {
                            error(new KnownResourceException(path, "resource.item.custom_model_data_exhausted", section.path(), clientBoundMaterial.asString()));
                            return;
                        }
                        // 未知错误
                        else {
                            return;
                        }
                    } else {
                        return;
                    }
                } else {
                    customModelData = cmd;
                }

                // item model
                Key itemModel = null;
                boolean forceItemModel = false;

                // 如果这个版本可以使用 item model
                if (!isVanillaItem && needsItemModelCompatibility()) {
                    // 如果用户主动设定了item model，那么肯定要设置
                    ConfigValue itemModelValue = section.getValue(ITEM_MODEL);
                    if (itemModelValue != null) {
                        itemModel = itemModelValue.getAsIdentifier();
                        forceItemModel = true;
                    }
                    // 用户没设置item model也没设置custom model data，那么为他生成一个基于物品id的item model
                    else if (customModelData == 0 || Config.alwaysUseItemModel()) {
                        itemModel = id;
                    }
                    // 用户没设置item model但是有custom model data，那么就使用custom model data
                }

                // 是否使用客户端侧模型
                boolean clientBoundModel = VersionHelper.PREMIUM && section.getBoolean(CLIENT_BOUND_MODEL, Config.globalClientboundModel());

                ItemDefinition.Builder itemBuilder = createPlatformItemBuilder(uniqueId, material, clientBoundMaterial);

                // 模型配置区域，如果这里被配置了，那么用户可以配置custom-model-data或item-model
                ConfigValue modelValue = section.getValue(MODEL);
                ConfigValue textureValue = section.getValue(TEXTURES);
                ConfigSection legacyModelSection = section.getSection(LEGACY_MODEL);
                boolean hasModelSection = modelValue != null || textureValue != null || legacyModelSection != null;

                if (customModelData > 0 && (hasModelSection || forceCustomModelData)) {
                    if (clientBoundModel) itemBuilder.clientBoundProcessor(new OverwritableCustomModelDataProcessor(ConstantNumberProvider.constant(customModelData)));
                    else itemBuilder.dataProcessor(new CustomModelDataProcessor(ConstantNumberProvider.constant(customModelData)));
                }
                if (itemModel != null && (hasModelSection || forceItemModel)) {
                    if (clientBoundModel) {
                        if (Config.obfuscateItemModel() && !section.getBoolean(SKIP_OBFUSCATION, false)) {
                            itemBuilder.clientBoundProcessor(new ObfuscatedItemModelProcessor(itemModel));
                            ObfuscatedItemModelProcessor.CAN_OBF.add(itemModel);
                        } else {
                            itemBuilder.clientBoundProcessor(new OverwritableItemModelProcessor(itemModel));
                        }
                    }
                    else itemBuilder.dataProcessor(new ItemModelProcessor(itemModel));
                }

                // 应用物品数据
                try {
                    ItemProcessors.collectProcessors(section.getSection("data"), itemBuilder::dataProcessor);
                } catch (KnownResourceException e) {
                    error(e, path);
                }

                // 应用客户端侧数据
                try {
                    if (VersionHelper.PREMIUM) {
                        ItemProcessors.collectProcessors(section.getSection(CLIENT_BOUND_DATA), itemBuilder::clientBoundProcessor);
                    }
                } catch (KnownResourceException e) {
                    error(e, path);
                }

                // 如果不是原版物品，那么加入ce的标识符
                if (!isVanillaItem)
                    itemBuilder.dataProcessor(new IdProcessor(id));

                // 事件
                Map<EventTrigger, List<net.momirealms.craftengine.core.plugin.context.function.Function<Context>>> events = new EnumMap<>(EventTrigger.class);
                try {
                    CommonFunctions.parseEvents(section.getValue(EVENTS), (t, f) -> events.computeIfAbsent(t, k -> new ArrayList<>(4)).add(f));
                } catch (KnownResourceException e) {
                    error(e, path);
                }

                // 设置
                ItemSettings settings = ItemSettings.of().disableVanillaBehavior(!isVanillaItem).triggerAdvancement(isVanillaItem);
                try {
                    ItemSettings.applyModifiers(settings, section.getSection("settings"));
                } catch (KnownResourceException e) {
                    error(e, path);
                }
                settings.lateInit();

                // 行为
                ItemBehavior behavior;
                try {
                    List<ItemBehavior> behaviors = new ArrayList<>(section.getList(BEHAVIORS, v -> ItemBehaviors.fromConfig(pack, path, id, v.getAsSection())));
                    Optional.ofNullable(VANILLA_ITEM_EXTRA_BEHAVIORS.get(material)).ifPresent(behaviors::add);
                    switch (behaviors.size()) {
                        case 0 -> behavior = EmptyItemBehavior.INSTANCE;
                        case 1 -> behavior = behaviors.getFirst();
                        case 2 -> behavior = new DualItemBehavior(behaviors.get(0), behaviors.get(1));
                        default -> behavior = new CompositeItemBehavior(behaviors);
                    }
                } catch (KnownResourceException e) {
                    error(e, path);
                    behavior = VANILLA_ITEM_EXTRA_BEHAVIORS.getOrDefault(material, EmptyItemBehavior.INSTANCE);
                }

                // 如果有物品更新器
                ConfigValue updaterValue = section.getValue("updater");
                if (updaterValue != null) {
                    ConfigSection updaterSection = updaterValue.getAsSection();
                    List<ItemUpdateConfig.Version> versions = new ArrayList<>(2);
                    for (String version : updaterSection.keySet()) {
                        try {
                            int versionInt = Integer.parseInt(version);
                            versions.add(new ItemUpdateConfig.Version(
                                    versionInt,
                                    updaterSection.getList(version, v -> ItemUpdaters.fromConfig(id, v.getAsSection())).toArray(new ItemUpdater[0])
                            ));
                        } catch (NumberFormatException ignored) {
                            error(new KnownResourceException(path, ConfigConstants.PARSE_INT_FAILED, updaterValue.path(), version));
                        }
                    }
                    ItemUpdateConfig config = new ItemUpdateConfig(versions);
                    itemBuilder.updater(config);
                    itemBuilder.dataProcessor(new ItemVersionProcessor(config.maxVersion()));
                }

                // 构建自定义物品
                ItemDefinition itemDefinition = itemBuilder
                        .isVanillaItem(isVanillaItem)
                        .behavior(behavior)
                        .settings(settings)
                        .events(events)
                        .build();

                AbstractItemManager.this.itemDefinitionById.put(id, itemDefinition);
                AbstractItemManager.this.itemDefinitionByPath.put(id.value(), itemDefinition);
                if (VersionHelper.isOrAbove26_1 && settings.dyeable() == Tristate.TRUE) {
                    AbstractItemManager.this.dyeableItems.put(id, itemDefinition);
                }

                // 如果有类别，则添加
                if (section.containsKey("category")) {
                    AbstractItemManager.this.plugin.itemBrowserManager().addExternalCategoryMember(id, section.getList(CATEGORIES, ConfigValue::getAsIdentifier));
                }

                if (!hasModelSection) {
                    return;
                }

                /*
                 * ========================
                 *
                 *       模型配置分界线
                 *
                 * ========================
                 */

                // 只对自定义物品有这个限制，既没有模型值也没有item-model
                if (!isVanillaItem && customModelData == 0 && itemModel == null) {
                    throw new KnownResourceException("resource.item.missing_model_id", section.path());
                }

                // 新版格式
                ItemModel modernModel;
                // 旧版格式
                TreeSet<LegacyOverridesModel> legacyOverridesModels;
                // 如果需要支持新版item model 或者用户需要旧版本兼容，但是没配置legacy-model
                if (isModernFormatRequired() || (needsLegacyCompatibility() && legacyModelSection == null)) {
                    if (textureValue != null) {
                        Key templateModel = itemModel != null && AbstractPackManager.PRESET_MODERN_MODELS_ITEM.containsKey(itemModel) ? itemModel : clientBoundMaterial;
                        SimplifiedModelReader simplifiedModelReader = AbstractPackManager.SIMPLIFIED_MODEL_READERS.get(templateModel);
                        modernModel = simplifiedModelReader.read(textureValue, Optional.ofNullable(modelValue).map(it -> {
                            if (it.is(Map.class)) {
                                ConfigSection modelSection = it.getAsSection();
                                return modelSection.getValue(new String[] {"path", "model"});
                            }
                            return it;
                        }), id);
                    } else if (modelValue != null) {
                        if (modelValue.is(List.class)) {
                            Key templateModel = itemModel != null && AbstractPackManager.PRESET_MODERN_MODELS_ITEM.containsKey(itemModel) ? itemModel : clientBoundMaterial;
                            SimplifiedModelReader simplifiedModelReader = AbstractPackManager.SIMPLIFIED_MODEL_READERS.get(templateModel);
                            modernModel = simplifiedModelReader.read(modelValue);
                        } else {
                            modernModel = ItemModels.fromConfig(modelValue);
                        }
                    } else {
                        throw KnownResourceException.missingArgument("model", ConfigConstants.ARGUMENT_ITEM_MODEL_DEFINITION);
                    }
                    modernModel.prepareModelGeneration(AbstractItemManager.this::prepareModelGeneration);
                } else {
                    modernModel = null;
                }
                // 如果需要旧版本兼容
                if (needsLegacyCompatibility()) {
                    if (legacyModelSection != null) {
                        LegacyItemModel legacyItemModel = LegacyItemModel.fromConfig(legacyModelSection, customModelData);
                        legacyItemModel.prepareModelGeneration(AbstractItemManager.this::prepareModelGeneration);
                        legacyOverridesModels = new TreeSet<>(legacyItemModel.overrides());
                    } else {
                        legacyOverridesModels = new TreeSet<>();
                        processModelRecursively(modernModel, new LinkedHashMap<>(), legacyOverridesModels, clientBoundMaterial, customModelData);
                        if (legacyOverridesModels.isEmpty()) {
                            throw new KnownResourceException("resource.item.model_definition.downgrade_failure", section.path());
                        }
                    }
                } else {
                    legacyOverridesModels = null;
                }

                boolean hasLegacyModel = legacyOverridesModels != null && !legacyOverridesModels.isEmpty();
                boolean hasModernModel = modernModel != null;

                // 自定义物品的model处理
                // 这个item-model是否存在，且是原版item-model
                boolean isVanillaItemModel = itemModel != null && AbstractPackManager.PRESET_ITEMS.containsKey(itemModel);
                if (!isVanillaItem) {
                    // 使用了自定义模型值
                    if (customModelData != 0) {
                        // 如果用户主动设置了item-model且为原版物品，则使用item-model为基础模型，否则使用其视觉材质对应的item-model
                        Key finalBaseModel = isVanillaItemModel ? itemModel : clientBoundMaterial;
                        // 添加新版item model
                        if (isModernFormatRequired() && hasModernModel) {
                            AbstractItemManager.this.modernOverrides.compute(finalBaseModel, (k, v) -> {
                                TreeMap<Integer, ModernItemModel> map = v;
                                if (map == null) {
                                    map = new TreeMap<>();
                                }
                                map.put(customModelData, new ModernItemModel(
                                        modernModel,
                                        section.getBoolean(HAND_ANIMATION_ON_SWAP, true),
                                        section.getBoolean(OVERSIZED_IN_GUI, true),
                                        section.getFloat(SWAP_ANIMATION_SCALE, 1f)
                                ));
                                return map;
                            });
                        }
                        // 添加旧版 overrides
                        if (needsLegacyCompatibility() && hasLegacyModel) {
                            AbstractItemManager.this.legacyOverrides.compute(finalBaseModel, (k, v) -> {
                                TreeSet<LegacyOverridesModel> set = v;
                                if (set == null) {
                                    set = new TreeSet<>();
                                }
                                set.addAll(legacyOverridesModels);
                                return set;
                            });
                        }
                    } else if (isVanillaItemModel) {
                        throw new KnownResourceException("resource.item.occupied_vanilla_item_model", section.assemblePath("item_model"), itemModel.asString());
                    }

                    // 使用了item-model组件，且不是原版物品的
                    if (itemModel != null && !isVanillaItemModel) {
                        if (isModernFormatRequired() && hasModernModel) {
                            AbstractItemManager.this.modernItemModels1_21_4.put(itemModel, new ModernItemModel(
                                    modernModel,
                                    section.getBoolean(HAND_ANIMATION_ON_SWAP, true),
                                    section.getBoolean(OVERSIZED_IN_GUI, true),
                                    section.getFloat(SWAP_ANIMATION_SCALE, 1f)
                            ));
                        }
                        if (needsItemModelCompatibility() && needsLegacyCompatibility() && hasLegacyModel) {
                            AbstractItemManager.this.modernItemModels1_21_2.compute(itemModel, (k, v) -> {
                                TreeSet<LegacyOverridesModel> set = v;
                                if (set == null) {
                                    set = new TreeSet<>();
                                }
                                set.addAll(legacyOverridesModels);
                                return set;
                            });
                        }
                    }
                } else {
                    // 原版物品的item model覆写
                    if (isModernFormatRequired()) {
                        AbstractItemManager.this.modernItemModels1_21_4.put(id, new ModernItemModel(
                                modernModel,
                                section.getBoolean(HAND_ANIMATION_ON_SWAP, true),
                                section.getBoolean(OVERSIZED_IN_GUI, true),
                                section.getFloat(SWAP_ANIMATION_SCALE, 1f)
                        ));
                    }
                }
            }, super.errorHandler), AbstractItemManager.this.plugin.scheduler().async()));
        }
    }

    protected void processModelRecursively(
            ItemModel currentModel,
            Map<String, Object> accumulatedPredicates,
            Collection<LegacyOverridesModel> resultList,
            Key materialId,
            int customModelData
    ) {
        if (currentModel instanceof ConditionItemModel conditionModel) {
            handleConditionModel(conditionModel, accumulatedPredicates, resultList, materialId, customModelData);
        } else if (currentModel instanceof RangeDispatchItemModel rangeModel) {
            handleRangeModel(rangeModel, accumulatedPredicates, resultList, materialId, customModelData);
        } else if (currentModel instanceof SelectItemModel selectModel) {
            handleSelectModel(selectModel, accumulatedPredicates, resultList, materialId, customModelData);
        } else if (currentModel instanceof BaseItemModel baseModel) {
            resultList.add(new LegacyOverridesModel(
                    new LinkedHashMap<>(accumulatedPredicates),
                    baseModel.path(),
                    customModelData,
                    null
            ));
        } else if (currentModel instanceof SpecialItemModel specialModel) {
            resultList.add(new LegacyOverridesModel(
                    new LinkedHashMap<>(accumulatedPredicates),
                    specialModel.base(),
                    customModelData,
                    null
            ));
        } else if (currentModel instanceof EmptyItemModel) {
            resultList.add(new LegacyOverridesModel(
                    new LinkedHashMap<>(accumulatedPredicates),
                    Key.of("item/air"),
                    customModelData,
                    null
            ));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void handleConditionModel(
            ConditionItemModel model,
            Map<String, Object> parentPredicates,
            Collection<LegacyOverridesModel> resultList,
            Key materialId,
            int customModelData
    ) {
        if (model.property() instanceof LegacyModelPredicate predicate) {
            String predicateId = predicate.legacyPredicateId(materialId);
            Map<String, Object> truePredicates = mergePredicates(
                    parentPredicates,
                    predicateId,
                    predicate.toLegacyValue(true)
            );
            processModelRecursively(
                    model.onTrue(),
                    truePredicates,
                    resultList,
                    materialId,
                    customModelData
            );
            Map<String, Object> falsePredicates = mergePredicates(
                    parentPredicates,
                    predicateId,
                    predicate.toLegacyValue(false)
            );
            processModelRecursively(
                    model.onFalse(),
                    falsePredicates,
                    resultList,
                    materialId,
                    customModelData
            );
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void handleRangeModel(
            RangeDispatchItemModel model,
            Map<String, Object> parentPredicates,
            Collection<LegacyOverridesModel> resultList,
            Key materialId,
            int customModelData
    ) {
        if (model.property() instanceof LegacyModelPredicate predicate) {
            String predicateId = predicate.legacyPredicateId(materialId);
            for (Map.Entry<Float, ItemModel> entry : model.entries().entrySet()) {
                Map<String, Object> merged = mergePredicates(
                        parentPredicates,
                        predicateId,
                        predicate.toLegacyValue(entry.getKey())
                );
                processModelRecursively(
                        entry.getValue(),
                        merged,
                        resultList,
                        materialId,
                        customModelData
                );
            }
            if (model.fallBack() != null) {
                Map<String, Object> merged = mergePredicates(
                        parentPredicates,
                        predicateId,
                        predicate.toLegacyValue(0f)
                );
                processModelRecursively(
                        model.fallBack(),
                        merged,
                        resultList,
                        materialId,
                        customModelData
                );
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void handleSelectModel(
            SelectItemModel model,
            Map<String, Object> parentPredicates,
            Collection<LegacyOverridesModel> resultList,
            Key materialId,
            int customModelData
    ) {
        if (model.property() instanceof LegacyModelPredicate predicate) {
            String predicateId = predicate.legacyPredicateId(materialId);
            for (Map.Entry<Either<JsonElement, List<JsonElement>>, ItemModel> entry : model.whenMap().entrySet()) {
                List<JsonElement> cases = entry.getKey().map(List::of, Function.identity());
                for (JsonElement caseValue : cases) {
                    if (caseValue instanceof JsonPrimitive primitive) {
                        Number legacyValue;
                        if (primitive.isBoolean()) {
                            legacyValue = predicate.toLegacyValue(primitive.getAsBoolean());
                        } else if (primitive.isString()) {
                            legacyValue = predicate.toLegacyValue(primitive.getAsString());
                        } else {
                            legacyValue = predicate.toLegacyValue(primitive.getAsNumber());
                        }
                        if (predicate instanceof TrimMaterialSelectProperty) {
                            if (legacyValue.floatValue() > 1f) {
                                continue;
                            }
                        }
                        Map<String, Object> merged = mergePredicates(
                                parentPredicates,
                                predicateId,
                                legacyValue
                        );
                        // Additional check for crossbow
                        if (predicate instanceof ChargeTypeSelectProperty && materialId.equals(ItemKeys.CROSSBOW)) {
                            merged = mergePredicates(
                                    merged,
                                    "charged",
                                    1
                            );
                        }
                        processModelRecursively(
                                entry.getValue(),
                                merged,
                                resultList,
                                materialId,
                                customModelData
                        );
                    }
                }
            }
            // Additional check for crossbow
            if (model.fallBack() != null) {
                if (predicate instanceof ChargeTypeSelectProperty && materialId.equals(ItemKeys.CROSSBOW)) {
                    processModelRecursively(
                            model.fallBack(),
                            mergePredicates(
                                    parentPredicates,
                                    "charged",
                                    0
                            ),
                            resultList,
                            materialId,
                            customModelData
                    );
                } else if (predicate instanceof TrimMaterialSelectProperty) {
                    processModelRecursively(
                            model.fallBack(),
                            mergePredicates(
                                    parentPredicates,
                                    "trim_type",
                                    0f
                            ),
                            resultList,
                            materialId,
                            customModelData
                    );
                }
            }
        }
    }

    private Map<String, Object> mergePredicates(
            Map<String, Object> existing,
            String newKey,
            Number newValue
    ) {
        Map<String, Object> merged = new LinkedHashMap<>(existing);
        if (newKey == null) return merged;
        merged.put(newKey, newValue);
        return merged;
    }
}
