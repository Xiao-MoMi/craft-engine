package net.momirealms.craftengine.core.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviors;
import net.momirealms.craftengine.core.item.equipment.*;
import net.momirealms.craftengine.core.item.processor.*;
import net.momirealms.craftengine.core.item.updater.ItemUpdateConfig;
import net.momirealms.craftengine.core.item.updater.ItemUpdateResult;
import net.momirealms.craftengine.core.item.updater.ItemUpdater;
import net.momirealms.craftengine.core.item.updater.ItemUpdaters;
import net.momirealms.craftengine.core.pack.AbstractPackManager;
import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.allocator.IdAllocator;
import net.momirealms.craftengine.core.pack.model.definition.*;
import net.momirealms.craftengine.core.pack.model.definition.select.ChargeTypeSelectProperty;
import net.momirealms.craftengine.core.pack.model.definition.select.TrimMaterialSelectProperty;
import net.momirealms.craftengine.core.pack.model.generation.AbstractModelGenerator;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.pack.model.legacy.LegacyItemModel;
import net.momirealms.craftengine.core.pack.model.legacy.LegacyModelPredicate;
import net.momirealms.craftengine.core.pack.model.legacy.LegacyOverridesModel;
import net.momirealms.craftengine.core.pack.model.simplified.SimplifiedModelReader;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.plugin.config.IdSectionConfigParser;
import net.momirealms.craftengine.core.plugin.context.CommonFunctions;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.EventTrigger;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.plugin.logger.Debugger;
import net.momirealms.craftengine.core.util.*;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.type.Either;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class AbstractItemManager<I> extends AbstractModelGenerator implements ItemManager<I> {
    protected static final Map<Key, List<ItemBehavior>> VANILLA_ITEM_EXTRA_BEHAVIORS = new HashMap<>();
    protected static final Set<Key> VANILLA_ITEMS = new HashSet<>(1024);
    protected static final Map<Key, List<UniqueKey>> VANILLA_ITEM_TAGS = new HashMap<>();

    private final ItemParser itemParser;
    private final EquipmentParser equipmentParser;
    protected final Map<Key, CustomItem<I>> customItemsById = new LinkedHashMap<>();
    protected final Map<String, CustomItem<I>> customItemsByPath = new HashMap<>();
    protected final Map<Key, List<UniqueKey>> customItemTags = new HashMap<>();
    protected final Map<Key, ModernItemModel> modernItemModels1_21_4 = new HashMap<>();
    protected final Map<Key, TreeSet<LegacyOverridesModel>> modernItemModels1_21_2 = new HashMap<>();
    protected final Map<Key, TreeSet<LegacyOverridesModel>> legacyOverrides = new HashMap<>();
    protected final Map<Key, TreeMap<Integer, ModernItemModel>> modernOverrides = new HashMap<>();
    protected final Map<Key, Equipment> equipments = new HashMap<>();
    // Cached command suggestions
    protected final List<Suggestion> cachedCustomItemSuggestions = new ArrayList<>();
    protected final List<Suggestion> cachedAllItemSuggestions = new ArrayList<>();
    protected final List<Suggestion> cachedVanillaItemSuggestions = new ArrayList<>();
    protected final List<Suggestion> cachedTotemSuggestions = new ArrayList<>();
    // 替代配方材料
    protected final Map<Key, List<UniqueKey>> ingredientSubstitutes = new HashMap<>();

    protected boolean featureFlag$keepOnDeathChance = false;
    protected boolean featureFlag$destroyOnDeathChance = false;

    protected AbstractItemManager(CraftEngine plugin) {
        super(plugin);
        this.itemParser = new ItemParser();
        this.equipmentParser = new EquipmentParser();
        ItemProcessors.init();
    }

    public ItemParser itemParser() {
        return itemParser;
    }

    public EquipmentParser equipmentParser() {
        return equipmentParser;
    }

    protected static void registerVanillaItemExtraBehavior(ItemBehavior behavior, Key... items) {
        for (Key key : items) {
            VANILLA_ITEM_EXTRA_BEHAVIORS.computeIfAbsent(key, k -> new ArrayList<>()).add(behavior);
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
        this.customItemsById.clear();
        this.customItemsByPath.clear();
        this.cachedCustomItemSuggestions.clear();
        this.cachedAllItemSuggestions.clear();
        this.cachedTotemSuggestions.clear();
        this.legacyOverrides.clear();
        this.modernOverrides.clear();
        this.customItemTags.clear();
        this.equipments.clear();
        this.modernItemModels1_21_4.clear();
        this.modernItemModels1_21_2.clear();
        this.ingredientSubstitutes.clear();
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
    public Optional<CustomItem<I>> getCustomItem(Key key) {
        return Optional.ofNullable(this.customItemsById.get(key));
    }

    @Override
    public Optional<CustomItem<I>> getCustomItemByPathOnly(String path) {
        return Optional.ofNullable(this.customItemsByPath.get(path));
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
    public ItemUpdateResult updateItem(Item<I> item, Supplier<ItemBuildContext> contextSupplier) {
        Optional<CustomItem<I>> optionalCustomItem = item.getCustomItem();
        if (optionalCustomItem.isPresent()) {
            CustomItem<I> customItem = optionalCustomItem.get();
            Optional<ItemUpdateConfig> updater = customItem.updater();
            if (updater.isPresent()) {
                return updater.get().update(item, contextSupplier);
            }
        }
        return new ItemUpdateResult(item, false, false);
    }

    @Override
    public boolean addCustomItem(CustomItem<I> customItem) {
        Key id = customItem.id();
        if (this.customItemsById.containsKey(id)) return false;
        this.customItemsById.put(id, customItem);
        this.customItemsByPath.put(id.value(), customItem);
        if (!customItem.isVanillaItem()) {
            // cache command suggestions
            this.cachedCustomItemSuggestions.add(Suggestion.suggestion(id.toString()));
            // totem animations
            if (VersionHelper.isOrAbove1_21_2()) {
                this.cachedTotemSuggestions.add(Suggestion.suggestion(id.toString()));
            } else if (customItem.material().equals(ItemKeys.TOTEM_OF_UNDYING)) {
                this.cachedTotemSuggestions.add(Suggestion.suggestion(id.toString()));
            }
            // tags
            ItemSettings settings = customItem.settings();
            Set<Key> tags = settings.tags();
            for (Key tag : tags) {
                this.customItemTags.computeIfAbsent(tag, k -> new ArrayList<>()).add(customItem.uniqueId());
            }
            // ingredient substitutes
            List<Key> substitutes = settings.ingredientSubstitutes();
            if (!substitutes.isEmpty()) {
                for (Key key : substitutes) {
                    if (VANILLA_ITEMS.contains(key)) {
                        AbstractItemManager.this.ingredientSubstitutes.computeIfAbsent(key, k -> new ArrayList<>()).add(customItem.uniqueId());
                    }
                }
            }
            if (settings.keepOnDeathChance != 0) {
                this.featureFlag$keepOnDeathChance = true;
            }
            if (settings.destroyOnDeathChance != 0) {
                this.featureFlag$destroyOnDeathChance = true;
            }
        }
        return true;
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
    public Collection<Key> itemTags() {
        Set<Key> tags = new HashSet<>(VANILLA_ITEM_TAGS.keySet());
        tags.addAll(this.customItemTags.keySet());
        return tags;
    }

    @Override
    public Collection<Suggestion> cachedCustomItemSuggestions() {
        return Collections.unmodifiableCollection(this.cachedCustomItemSuggestions);
    }

    @Override
    public Collection<Suggestion> cachedAllItemSuggestions() {
        return Collections.unmodifiableCollection(this.cachedAllItemSuggestions);
    }

    @Override
    public Collection<Suggestion> cachedTotemSuggestions() {
        return Collections.unmodifiableCollection(this.cachedTotemSuggestions);
    }

    @Override
    public Optional<List<ItemBehavior>> getItemBehavior(Key key) {
        Optional<CustomItem<I>> customItemOptional = getCustomItem(key);
        if (customItemOptional.isPresent()) {
            CustomItem<I> customItem = customItemOptional.get();
            Key vanillaMaterial = customItem.material();
            List<ItemBehavior> behavior = VANILLA_ITEM_EXTRA_BEHAVIORS.get(vanillaMaterial);
            if (behavior != null) {
                return Optional.of(Stream.concat(customItem.behaviors().stream(), behavior.stream()).toList());
            } else {
                return Optional.of(List.copyOf(customItem.behaviors()));
            }
        } else {
            List<ItemBehavior> behavior = VANILLA_ITEM_EXTRA_BEHAVIORS.get(key);
            if (behavior != null) {
                return Optional.of(List.copyOf(behavior));
            } else {
                return Optional.empty();
            }
        }
    }

    @Override
    public void delayedLoad() {
        this.cachedAllItemSuggestions.addAll(this.cachedVanillaItemSuggestions);
        this.cachedAllItemSuggestions.addAll(this.cachedCustomItemSuggestions);
    }

    @Override
    public Map<Key, CustomItem<I>> loadedItems() {
        return Collections.unmodifiableMap(this.customItemsById);
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

    protected abstract CustomItem.Builder<I> createPlatformItemBuilder(UniqueKey id, Key material, Key clientBoundMaterial);

    protected abstract void registerArmorTrimPattern(Collection<Key> equipments);

    public class EquipmentParser extends IdSectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] {"equipments", "equipment"};

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public int loadingSequence() {
            return LoadingSequence.EQUIPMENT;
        }

        @Override
        public void parseSection(Pack pack, Path path, String node, Key id, Map<String, Object> section) {
            if (AbstractItemManager.this.equipments.containsKey(id)) {
                throw new LocalizedResourceConfigException("warning.config.equipment.duplicate");
            }
            Equipment equipment = Equipments.fromMap(id, section);
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

    public class ItemParser extends IdSectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] {"items", "item"};
        private final Map<Key, IdAllocator> idAllocators = new HashMap<>();

        @Override
        public int count() {
            return AbstractItemManager.this.customItemsById.size();
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
            return Config.packMaxVersion().isAtOrAbove(MinecraftVersion.V1_21_2) && VersionHelper.isOrAbove1_21_2(); //todo 能否通过客户端包解决问题
        }

        public Map<Key, IdAllocator> idAllocators() {
            return this.idAllocators;
        }

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public int loadingSequence() {
            return LoadingSequence.ITEM;
        }

        @Override
        public void preProcess() {
            this.idAllocators.clear();
        }

        @Override
        public void postProcess() {
            for (Map.Entry<Key, IdAllocator> entry : this.idAllocators.entrySet()) {
                entry.getValue().processPendingAllocations();
                try {
                    entry.getValue().saveToCache();
                } catch (IOException e) {
                    AbstractItemManager.this.plugin.logger().warn("Error while saving custom model data allocation for material " + entry.getKey().asString(), e);
                }
            }
        }

        // 创建或获取已有的自动分配器
        private IdAllocator getOrCreateIdAllocator(Key key) {
            return this.idAllocators.computeIfAbsent(key, k -> {
                IdAllocator newAllocator = new IdAllocator(plugin.dataFolderPath().resolve("cache").resolve("custom-model-data").resolve(k.value() + ".json"));
                newAllocator.reset(Config.customModelDataStartingValue(k), 16_777_216);
                try {
                    newAllocator.loadFromCache();
                } catch (IOException e) {
                    AbstractItemManager.this.plugin.logger().warn("Error while loading custom model data from cache for material " + k.asString(), e);
                }
                return newAllocator;
            });
        }

        @Override
        public void parseSection(Pack pack, Path path, String node, Key id, Map<String, Object> section) {
            if (AbstractItemManager.this.customItemsById.containsKey(id)) {
                throw new LocalizedResourceConfigException("warning.config.item.duplicate");
            }
            // 创建UniqueKey，仅缓存用
            UniqueKey uniqueId = UniqueKey.create(id);
            // 判断是不是原版物品
            boolean isVanillaItem = isVanillaItem(id);

            // 读取服务端侧材质
            Key material = isVanillaItem ? id : Key.from(ResourceConfigUtils.requireNonEmptyStringOrThrow(section.getOrDefault("material", Config.defaultMaterial()), "warning.config.item.missing_material").toLowerCase(Locale.ROOT));
            // 读取客户端侧材质
            Key clientBoundMaterial = VersionHelper.PREMIUM && section.containsKey("client-bound-material") ? Key.from(section.get("client-bound-material").toString().toLowerCase(Locale.ROOT)) : material;

            // custom model data
            CompletableFuture<Integer> customModelDataFuture;
            boolean forceCustomModelData;

            if (!isVanillaItem) {
                // 如果用户指定了，说明要手动分配，不管他是什么版本，都强制设置模型值
                Object rawCustomModelData = section.get("custom-model-data");
                if (rawCustomModelData != null) {
                    int customModelData = ResourceConfigUtils.getAsInt(rawCustomModelData, "custom-model-data");
                    if (customModelData > 0) {
                        if (customModelData > 16_777_216) {
                            throw new LocalizedResourceConfigException("warning.config.item.bad_custom_model_data", String.valueOf(customModelData));
                        }
                        forceCustomModelData = true;
                        customModelDataFuture = getOrCreateIdAllocator(clientBoundMaterial).assignFixedId(id.asString(), customModelData);
                    } else {
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
            customModelDataFuture.whenComplete((cmd, throwable) -> ResourceConfigUtils.runCatching(path, node, () -> {
                int customModelData;
                if (throwable != null) {
                    // 检测custom model data 冲突
                    if (throwable instanceof IdAllocator.IdConflictException exception) {
                        if (section.containsKey("model") || section.containsKey("models") || section.containsKey("texture") || section.containsKey("textures") || section.containsKey("legacy-model")) {
                            throw new LocalizedResourceConfigException("warning.config.item.custom_model_data.conflict", String.valueOf(exception.id()), exception.previousOwner());
                        }
                        customModelData = exception.id();
                    }
                    // custom model data 已被用尽，不太可能
                    else if (throwable instanceof IdAllocator.IdExhaustedException) {
                        throw new LocalizedResourceConfigException("warning.config.item.custom_model_data.exhausted", clientBoundMaterial.asString());
                    }
                    // 未知错误
                    else {
                        Debugger.ITEM.warn(() -> "Unknown error while allocating custom model data.", throwable);
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
                    if (section.containsKey("item-model")) {
                        itemModel = Key.from(section.get("item-model").toString());
                        forceItemModel = true;
                    }
                    // 用户没设置item model也没设置custom model data，那么为他生成一个基于物品id的item model
                    else if (customModelData == 0 || Config.alwaysUseItemModel()) {
                        itemModel = id;
                    }
                    // 用户没设置item model但是有custom model data，那么就使用custom model data
                }

                // 是否使用客户端侧模型
                boolean clientBoundModel = VersionHelper.PREMIUM && (section.containsKey("client-bound-model") ? ResourceConfigUtils.getAsBoolean(section.get("client-bound-model"), "client-bound-model") : Config.globalClientboundModel());

                CustomItem.Builder<I> itemBuilder = createPlatformItemBuilder(uniqueId, material, clientBoundMaterial);

                // 对于不重要的配置，可以仅警告，不返回
                ExceptionCollector<LocalizedResourceConfigException> collector = new ExceptionCollector<>();

                // 模型配置区域，如果这里被配置了，那么用户可以配置custom-model-data或item-model
                // model可以是一个string也可以是一个区域
                Object modelSection = ResourceConfigUtils.get(section, "model", "models");
                Map<String, Object> legacyModelSection = MiscUtils.castToMap(section.get("legacy-model"), true);
                // model可以是一个map，也可以是一个string或list
                boolean hasModelSection = modelSection instanceof Map<?,?> || legacyModelSection != null;
                if (!hasModelSection) {
                    Object texture = ResourceConfigUtils.get(section, "texture", "textures");
                    // 如果使用的是textures，那么model指的是
                    if (texture != null) {
                        // 获取textures列表
                        List<String> textures = texture instanceof List<?> ? MiscUtils.getAsStringList(texture) : List.of(texture.toString());
                        if (!textures.isEmpty()) {
                            // 获取可选的模型列表，此时的model不可能是map了
                            List<String> modelPath = modelSection != null ? MiscUtils.getAsStringList(modelSection) : List.of();
                            // 根据父item model选择处理方案
                            Key templateModel = itemModel != null && AbstractPackManager.PRESET_MODERN_MODELS_ITEM.containsKey(itemModel) ? itemModel : clientBoundMaterial;
                            SimplifiedModelReader simplifiedModelReader = AbstractPackManager.SIMPLIFIED_MODEL_READERS.get(templateModel);
                            if (simplifiedModelReader != null) {
                                try {
                                    modelSection = simplifiedModelReader.convert(textures, modelPath, id);
                                    if (modelSection != null) {
                                        hasModelSection = true;
                                    }
                                } catch (LocalizedResourceConfigException e) {
                                    collector.add(e);
                                }
                            }
                        }
                    }
                    // 如果没有配贴图，且model为string或list，直接生成相应类型的模型
                    else if (modelSection != null) {
                        List<String> models = modelSection instanceof List<?> ? MiscUtils.getAsStringList(modelSection) : List.of(modelSection.toString());
                        if (!models.isEmpty()) {
                            Key templateModel = itemModel != null && AbstractPackManager.PRESET_MODERN_MODELS_ITEM.containsKey(itemModel) ? itemModel : clientBoundMaterial;
                            SimplifiedModelReader simplifiedModelReader = AbstractPackManager.SIMPLIFIED_MODEL_READERS.get(templateModel);
                            if (simplifiedModelReader != null) {
                                try {
                                    modelSection = simplifiedModelReader.convert(models);
                                    if (modelSection != null) {
                                        hasModelSection = true;
                                    }
                                } catch (LocalizedResourceConfigException e) {
                                    collector.add(e);
                                }
                            }
                        }
                    }
                }

                if (customModelData > 0 && (hasModelSection || forceCustomModelData)) {
                    if (clientBoundModel) itemBuilder.clientBoundDataModifier(new OverwritableCustomModelDataProcessor(customModelData));
                    else itemBuilder.dataModifier(new CustomModelDataProcessor(customModelData));
                }
                if (itemModel != null && (hasModelSection || forceItemModel)) {
                    if (clientBoundModel) itemBuilder.clientBoundDataModifier(new OverwritableItemModelProcessor(itemModel));
                    else itemBuilder.dataModifier(new ItemModelProcessor(itemModel));
                }

                // 应用物品数据
                try {
                    ItemProcessors.applyDataModifiers(MiscUtils.castToMap(section.get("data"), true), itemBuilder::dataModifier);
                } catch (LocalizedResourceConfigException e) {
                    collector.add(e);
                }

                // 应用客户端侧数据
                try {
                    if (VersionHelper.PREMIUM) {
                        ItemProcessors.applyDataModifiers(MiscUtils.castToMap(section.get("client-bound-data"), true), itemBuilder::clientBoundDataModifier);
                    }
                } catch (LocalizedResourceConfigException e) {
                    collector.add(e);
                }

                // 如果不是原版物品，那么加入ce的标识符
                if (!isVanillaItem)
                    itemBuilder.dataModifier(new IdProcessor(id));

                // 事件
                Map<EventTrigger, List<net.momirealms.craftengine.core.plugin.context.function.Function<Context>>> eventTriggerListMap;
                try {
                    eventTriggerListMap = CommonFunctions.parseEvents(ResourceConfigUtils.get(section, "event", "events"));
                } catch (LocalizedResourceConfigException e) {
                    collector.add(e);
                    eventTriggerListMap = Map.of();
                }

                // 设置
                ItemSettings settings;
                try {
                    settings = Optional.ofNullable(ResourceConfigUtils.get(section, "settings"))
                            .map(map -> ItemSettings.fromMap(MiscUtils.castToMap(map, true)))
                            .map(it -> isVanillaItem ? it.disableVanillaBehavior(false) : it)
                            .orElse(ItemSettings.of().disableVanillaBehavior(!isVanillaItem));
                } catch (LocalizedResourceConfigException e) {
                    collector.add(e);
                    settings = ItemSettings.of().disableVanillaBehavior(!isVanillaItem);
                }

                // 行为
                List<ItemBehavior> behaviors;
                try {
                    behaviors = ResourceConfigUtils.parseConfigAsList(ResourceConfigUtils.get(section, "behavior", "behaviors"), map -> ItemBehaviors.fromMap(pack, path, node, id, map));
                } catch (LocalizedResourceConfigException e) {
                    collector.add(e);
                    behaviors = Collections.emptyList();
                }

                // 如果有物品更新器
                if (section.containsKey("updater")) {
                    Map<String, Object> updater = ResourceConfigUtils.getAsMap(section.get("updater"), "updater");
                    List<ItemUpdateConfig.Version> versions = new ArrayList<>(2);
                    for (Map.Entry<String, Object> entry : updater.entrySet()) {
                        try {
                            int version = Integer.parseInt(entry.getKey());
                            versions.add(new ItemUpdateConfig.Version(
                                    version,
                                    ResourceConfigUtils.parseConfigAsList(entry.getValue(), map -> ItemUpdaters.fromMap(id, map)).toArray(new ItemUpdater[0])
                            ));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    ItemUpdateConfig config = new ItemUpdateConfig(versions);
                    itemBuilder.updater(config);
                    itemBuilder.dataModifier(new ItemVersionProcessor(config.maxVersion()));
                }

                // 构建自定义物品
                CustomItem<I> customItem = itemBuilder
                        .isVanillaItem(isVanillaItem)
                        .behaviors(behaviors)
                        .settings(settings)
                        .events(eventTriggerListMap)
                        .build();

                // 添加到缓存
                addCustomItem(customItem);

                // 如果有类别，则添加
                if (section.containsKey("category")) {
                    AbstractItemManager.this.plugin.itemBrowserManager().addExternalCategoryMember(id, MiscUtils.getAsStringList(section.get("category")).stream().map(Key::of).toList());
                }

                if (!hasModelSection) {
                    collector.throwIfPresent();
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
                    collector.addAndThrow(new LocalizedResourceConfigException("warning.config.item.missing_model_id"));
                }

                // 新版格式
                ItemModel modernModel = null;
                // 旧版格式
                TreeSet<LegacyOverridesModel> legacyOverridesModels = null;
                // 如果需要支持新版item model 或者用户需要旧版本兼容，但是没配置legacy-model
                if (isModernFormatRequired() || (needsLegacyCompatibility() && legacyModelSection == null)) {
                    // 1.21.4+必须要配置model区域，如果不需要高版本兼容，则可以只写legacy-model
                    if (modelSection == null) {
                        collector.addAndThrow(new LocalizedResourceConfigException("warning.config.item.missing_model"));
                        return;
                    }
                    try {
                        modernModel = ItemModels.fromObj(modelSection);
                        for (ModelGeneration generation : modernModel.modelsToGenerate()) {
                            prepareModelGeneration(generation);
                        }
                    } catch (LocalizedResourceConfigException e) {
                        collector.addAndThrow(e);
                    }
                }
                // 如果需要旧版本兼容
                if (needsLegacyCompatibility()) {
                    if (legacyModelSection != null) {
                        try {
                            LegacyItemModel legacyItemModel = LegacyItemModel.fromMap(legacyModelSection, customModelData);
                            for (ModelGeneration generation : legacyItemModel.modelsToGenerate()) {
                                prepareModelGeneration(generation);
                            }
                            legacyOverridesModels = new TreeSet<>(legacyItemModel.overrides());
                        } catch (LocalizedResourceConfigException e) {
                            collector.addAndThrow(e);
                        }
                    } else {
                        legacyOverridesModels = new TreeSet<>();
                        processModelRecursively(modernModel, new LinkedHashMap<>(), legacyOverridesModels, clientBoundMaterial, customModelData);
                        if (legacyOverridesModels.isEmpty()) {
                            collector.add(new LocalizedResourceConfigException("warning.config.item.legacy_model.cannot_convert"));
                        }
                    }
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
                            TreeMap<Integer, ModernItemModel> map = AbstractItemManager.this.modernOverrides.computeIfAbsent(finalBaseModel, k -> new TreeMap<>());
                            map.put(customModelData, new ModernItemModel(
                                    modernModel,
                                    ResourceConfigUtils.getAsBoolean(section.getOrDefault("oversized-in-gui", true), "oversized-in-gui"),
                                    ResourceConfigUtils.getAsBoolean(section.getOrDefault("hand-animation-on-swap", true), "hand-animation-on-swap"),
                                    ResourceConfigUtils.getAsFloat(section.getOrDefault("swap-animation-scale", 1f), "swap-animation-scale")
                            ));
                        }
                        // 添加旧版 overrides
                        if (needsLegacyCompatibility() && hasLegacyModel) {
                            TreeSet<LegacyOverridesModel> lom = AbstractItemManager.this.legacyOverrides.computeIfAbsent(finalBaseModel, k -> new TreeSet<>());
                            lom.addAll(legacyOverridesModels);
                        }
                    } else if (isVanillaItemModel) {
                        collector.addAndThrow(new LocalizedResourceConfigException("warning.config.item.item_model.conflict", itemModel.asString()));
                    }

                    // 使用了item-model组件，且不是原版物品的
                    if (itemModel != null && !isVanillaItemModel) {
                        if (isModernFormatRequired() && hasModernModel) {
                            AbstractItemManager.this.modernItemModels1_21_4.put(itemModel, new ModernItemModel(
                                    modernModel,
                                    ResourceConfigUtils.getAsBoolean(section.getOrDefault("oversized-in-gui", true), "oversized-in-gui"),
                                    ResourceConfigUtils.getAsBoolean(section.getOrDefault("hand-animation-on-swap", true), "hand-animation-on-swap"),
                                    ResourceConfigUtils.getAsFloat(section.getOrDefault("swap-animation-scale", 1f), "swap-animation-scale")
                            ));
                        }
                        if (needsItemModelCompatibility() && needsLegacyCompatibility() && hasLegacyModel) {
                            TreeSet<LegacyOverridesModel> lom = AbstractItemManager.this.modernItemModels1_21_2.computeIfAbsent(itemModel, k -> new TreeSet<>());
                            lom.addAll(legacyOverridesModels);
                        }
                    }
                } else {
                    // 原版物品的item model覆写
                    if (isModernFormatRequired()) {
                        AbstractItemManager.this.modernItemModels1_21_4.put(id, new ModernItemModel(
                                modernModel,
                                ResourceConfigUtils.getAsBoolean(section.getOrDefault("oversized-in-gui", true), "oversized-in-gui"),
                                ResourceConfigUtils.getAsBoolean(section.getOrDefault("hand-animation-on-swap", true), "hand-animation-on-swap"),
                                ResourceConfigUtils.getAsFloat(section.getOrDefault("swap-animation-scale", 1f), "swap-animation-scale")
                        ));
                    }
                }

                // 抛出异常
                collector.throwIfPresent();

            }, () -> GsonHelper.get().toJson(section)));
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
                    customModelData
            ));
        } else if (currentModel instanceof SpecialItemModel specialModel) {
            resultList.add(new LegacyOverridesModel(
                    new LinkedHashMap<>(accumulatedPredicates),
                    specialModel.base(),
                    customModelData
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
                List<JsonElement> cases = entry.getKey().fallbackOrMapPrimary(List::of);
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
