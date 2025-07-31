package net.momirealms.craftengine.core.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviors;
import net.momirealms.craftengine.core.item.equipment.*;
import net.momirealms.craftengine.core.item.modifier.*;
import net.momirealms.craftengine.core.pack.AbstractPackManager;
import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.ResourceLocation;
import net.momirealms.craftengine.core.pack.model.*;
import net.momirealms.craftengine.core.pack.model.generation.AbstractModelGenerator;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.pack.model.select.ChargeTypeSelectProperty;
import net.momirealms.craftengine.core.pack.model.select.TrimMaterialSelectProperty;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.event.EventFunctions;
import net.momirealms.craftengine.core.plugin.context.event.EventTrigger;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.*;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.type.Either;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public abstract class AbstractItemManager<I> extends AbstractModelGenerator implements ItemManager<I> {
    protected static final Map<Key, List<ItemBehavior>> VANILLA_ITEM_EXTRA_BEHAVIORS = new HashMap<>();
    protected static final Set<Key> VANILLA_ITEMS = new HashSet<>(1024);
    protected static final Map<Key, List<UniqueKey>> VANILLA_ITEM_TAGS = new HashMap<>();

    private final ItemParser itemParser;
    private final EquipmentParser equipmentParser;
    protected final Map<String, ExternalItemSource<I>> externalItemSources = new HashMap<>();
    protected final Map<Key, CustomItem<I>> customItems = new HashMap<>();
    protected final Map<Key, List<UniqueKey>> customItemTags = new HashMap<>();
    protected final Map<Key, Map<Integer, Key>> cmdConflictChecker = new HashMap<>();
    protected final Map<Key, ModernItemModel> modernItemModels1_21_4 = new HashMap<>();
    protected final Map<Key, TreeSet<LegacyOverridesModel>> modernItemModels1_21_2 = new HashMap<>();
    protected final Map<Key, TreeSet<LegacyOverridesModel>> legacyOverrides = new HashMap<>();
    protected final Map<Key, TreeMap<Integer, ModernItemModel>> modernOverrides = new HashMap<>();
    protected final Map<Key, Equipment> equipments = new HashMap<>();
    // Cached command suggestions
    protected final List<Suggestion> cachedSuggestions = new ArrayList<>();
    protected final List<Suggestion> cachedTotemSuggestions = new ArrayList<>();

    protected AbstractItemManager(CraftEngine plugin) {
        super(plugin);
        this.itemParser = new ItemParser();
        this.equipmentParser = new EquipmentParser();
        ItemDataModifiers.init();
    }

    protected static void registerVanillaItemExtraBehavior(ItemBehavior behavior, Key... items) {
        for (Key key : items) {
            VANILLA_ITEM_EXTRA_BEHAVIORS.computeIfAbsent(key, k -> new ArrayList<>()).add(behavior);
        }
    }

    @SuppressWarnings("unchecked")
    protected void applyDataModifiers(Map<String, Object> dataSection, Consumer<ItemDataModifier<I>> callback) {
        ExceptionCollector<LocalizedResourceConfigException> errorCollector = new ExceptionCollector<>();
        if (dataSection != null) {
            for (Map.Entry<String, Object> dataEntry : dataSection.entrySet()) {
                Object value = dataEntry.getValue();
                if (value == null) continue;
                Optional.ofNullable(BuiltInRegistries.ITEM_DATA_MODIFIER_FACTORY.getValue(Key.withDefaultNamespace(dataEntry.getKey(), Key.DEFAULT_NAMESPACE))).ifPresent(factory -> {
                    try {
                        callback.accept((ItemDataModifier<I>) factory.create(value));
                    } catch (LocalizedResourceConfigException e) {
                        errorCollector.add(e);
                    }
                });
            }
        }
        errorCollector.throwIfPresent();
    }

    @Override
    public ConfigParser[] parsers() {
        return new ConfigParser[]{this.itemParser, this.equipmentParser};
    }

    @Override
    public ExternalItemSource<I> getExternalItemSource(String name) {
        return this.externalItemSources.get(name);
    }

    @Override
    public boolean registerExternalItemSource(ExternalItemSource<I> externalItemSource) {
        if (!ResourceLocation.isValidNamespace(externalItemSource.plugin())) return false;
        if (this.externalItemSources.containsKey(externalItemSource.plugin())) return false;
        this.externalItemSources.put(externalItemSource.plugin(), externalItemSource);
        return true;
    }

    @Override
    public void unload() {
        super.clearModelsToGenerate();
        this.customItems.clear();
        this.cachedSuggestions.clear();
        this.cachedTotemSuggestions.clear();
        this.legacyOverrides.clear();
        this.modernOverrides.clear();
        this.customItemTags.clear();
        this.equipments.clear();
        this.cmdConflictChecker.clear();
        this.modernItemModels1_21_4.clear();
        this.modernItemModels1_21_2.clear();
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
        return Optional.ofNullable(this.customItems.get(key));
    }

    @Override
    public boolean addCustomItem(CustomItem<I> customItem) {
        Key id = customItem.id();
        if (this.customItems.containsKey(id)) return false;
        this.customItems.put(id, customItem);
        // cache command suggestions
        this.cachedSuggestions.add(Suggestion.suggestion(id.toString()));
        // totem animations
        if (VersionHelper.isOrAbove1_21_2()) {
            this.cachedTotemSuggestions.add(Suggestion.suggestion(id.toString()));
        } else if (customItem.material().equals(ItemKeys.TOTEM_OF_UNDYING)) {
            this.cachedTotemSuggestions.add(Suggestion.suggestion(id.toString()));
        }
        // tags
        Set<Key> tags = customItem.settings().tags();
        for (Key tag : tags) {
            this.customItemTags.computeIfAbsent(tag, k -> new ArrayList<>()).add(customItem.uniqueId());
        }
        return true;
    }

    @Override
    public List<UniqueKey> tagToItems(Key tag) {
        List<UniqueKey> items = new ArrayList<>();
        List<UniqueKey> holders = VANILLA_ITEM_TAGS.get(tag);
        if (holders != null) {
            items.addAll(holders);
        }
        List<UniqueKey> customItems = this.customItemTags.get(tag);
        if (customItems != null) {
            items.addAll(customItems);
        }
        return items;
    }

    @Override
    public List<UniqueKey> tagToVanillaItems(Key tag) {
        return Collections.unmodifiableList(VANILLA_ITEM_TAGS.getOrDefault(tag, List.of()));
    }

    @Override
    public List<UniqueKey> tagToCustomItems(Key tag) {
        return Collections.unmodifiableList(this.customItemTags.getOrDefault(tag, List.of()));
    }

    @Override
    public Collection<Suggestion> cachedSuggestions() {
        return Collections.unmodifiableCollection(this.cachedSuggestions);
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
    public Collection<Key> items() {
        return Collections.unmodifiableCollection(this.customItems.keySet());
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

    protected abstract CustomItem.Builder<I> createPlatformItemBuilder(UniqueKey id, Key material, Key clientBoundMaterial);

    protected abstract void registerArmorTrimPattern(Collection<Key> equipments);

    public class EquipmentParser implements ConfigParser {
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
        public void parseSection(Pack pack, Path path, Key id, Map<String, Object> section) {
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

    public class ItemParser implements ConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] {"items", "item"};

        private boolean isModernFormatRequired() {
            return Config.packMaxVersion().isAtOrAbove(MinecraftVersions.V1_21_4);
        }

        private boolean needsLegacyCompatibility() {
            return Config.packMinVersion().isBelow(MinecraftVersions.V1_21_4);
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
        public void parseSection(Pack pack, Path path, Key id, Map<String, Object> section) {
            if (AbstractItemManager.this.customItems.containsKey(id)) {
                throw new LocalizedResourceConfigException("warning.config.item.duplicate");
            }

            UniqueKey uniqueId = UniqueKey.create(id);
            // 判断是不是原版物品
            boolean isVanillaItem = isVanillaItem(id);
            Key material = Key.from(isVanillaItem ? id.value() : ResourceConfigUtils.requireNonEmptyStringOrThrow(section.get("material"), "warning.config.item.missing_material").toLowerCase(Locale.ENGLISH));
            Key clientBoundMaterial = section.containsKey("client-bound-material") ? Key.from(section.get("client-bound-material").toString().toLowerCase(Locale.ENGLISH)) : material;
            // 如果是原版物品，那么custom-model-data只能是0，即使用户设置了其他值
            int customModelData = isVanillaItem ? 0 : ResourceConfigUtils.getAsInt(section.getOrDefault("custom-model-data", 0), "custom-model-data");
            boolean clientBoundModel = section.containsKey("client-bound-model") ? ResourceConfigUtils.getAsBoolean(section.get("client-bound-data"), "client-bound-data") : Config.globalClientboundModel();
            if (customModelData < 0) {
                throw new LocalizedResourceConfigException("warning.config.item.invalid_custom_model_data", String.valueOf(customModelData));
            }
            if (customModelData > 16_777_216) {
                throw new LocalizedResourceConfigException("warning.config.item.bad_custom_model_data", String.valueOf(customModelData));
            }

            // item-model值
            Key itemModelKey = null;

            CustomItem.Builder<I> itemBuilder = createPlatformItemBuilder(uniqueId, material, clientBoundMaterial);
            boolean hasItemModelSection = section.containsKey("item-model");

            // 如果custom-model-data不为0
            if (customModelData > 0) {
                if (clientBoundModel) itemBuilder.clientBoundDataModifier(new CustomModelDataModifier<>(customModelData));
                else itemBuilder.dataModifier(new CustomModelDataModifier<>(customModelData));
            }
            // 如果没有item-model选项被配置，同时这个物品又含有 model 区域
            else if (!hasItemModelSection && section.containsKey("model") && VersionHelper.isOrAbove1_21_2()) {
                // 那么使用物品id当成item-model的值
                itemModelKey = Key.from(section.getOrDefault("item-model", id.toString()).toString());
                // 但是有个前提，id必须是有效的resource location
                if (ResourceLocation.isValid(itemModelKey.toString())) {
                    if (clientBoundModel) itemBuilder.clientBoundDataModifier(new ItemModelModifier<>(itemModelKey));
                    else itemBuilder.dataModifier(new ItemModelModifier<>(itemModelKey));
                } else {
                    itemModelKey = null;
                }
            }

            // 如果有item-model
            if (hasItemModelSection && VersionHelper.isOrAbove1_21_2()) {
                itemModelKey = Key.from(section.get("item-model").toString());
                if (clientBoundModel) itemBuilder.clientBoundDataModifier(new ItemModelModifier<>(itemModelKey));
                else itemBuilder.dataModifier(new ItemModelModifier<>(itemModelKey));
            }

            // 对于不重要的配置，可以仅警告，不返回
            ExceptionCollector<LocalizedResourceConfigException> collector = new ExceptionCollector<>();

            // 应用物品数据
            try {
                applyDataModifiers(MiscUtils.castToMap(section.get("data"), true), itemBuilder::dataModifier);
            } catch (LocalizedResourceConfigException e) {
                collector.add(e);
            }
            try {
                applyDataModifiers(MiscUtils.castToMap(section.get("client-bound-data"), true), itemBuilder::clientBoundDataModifier);
            } catch (LocalizedResourceConfigException e) {
                collector.add(e);
            }

            // 如果不是原版物品，那么加入ce的标识符
            if (!isVanillaItem)
                itemBuilder.dataModifier(new IdModifier<>(id));

            // 事件
            Map<EventTrigger, List<net.momirealms.craftengine.core.plugin.context.function.Function<PlayerOptionalContext>>> eventTriggerListMap;
            try {
                eventTriggerListMap = EventFunctions.parseEvents(ResourceConfigUtils.get(section, "events", "event"));
            } catch (LocalizedResourceConfigException e) {
                collector.add(e);
                eventTriggerListMap = Map.of();
            }

            // 设置
            ItemSettings settings;
            try {
                settings = Optional.ofNullable(ResourceConfigUtils.get(section, "settings"))
                        .map(map -> ItemSettings.fromMap(MiscUtils.castToMap(map, true)))
                        .map(it -> isVanillaItem ? it.canPlaceRelatedVanillaBlock(true) : it)
                        .orElse(ItemSettings.of().canPlaceRelatedVanillaBlock(isVanillaItem));
            } catch (LocalizedResourceConfigException e) {
                collector.add(e);
                settings = ItemSettings.of().canPlaceRelatedVanillaBlock(isVanillaItem);
            }

            // 行为
            List<ItemBehavior> behaviors;
            try {
                behaviors = ItemBehaviors.fromObj(pack, path, id, ResourceConfigUtils.get(section, "behavior", "behaviors"));
            } catch (LocalizedResourceConfigException e) {
                collector.add(e);
                behaviors = Collections.emptyList();
            }

            // 构建自定义物品
            CustomItem<I> customItem = itemBuilder
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

            // 模型配置区域，如果这里被配置了，那么用户必须要配置custom-model-data或item-model
            Map<String, Object> modelSection = MiscUtils.castToMap(section.get("model"), true);
            Map<String, Object> legacyModelSection = MiscUtils.castToMap(section.get("legacy-model"), true);
            if (modelSection == null && legacyModelSection == null) {
                collector.throwIfPresent();
                return;
            }

            boolean needsModelSection = isModernFormatRequired() || (needsLegacyCompatibility() && legacyModelSection == null);
            // 只对自定义物品有这个限制
            if (!isVanillaItem) {
                // 既没有模型值也没有item-model
                if (customModelData == 0 && itemModelKey == null) {
                    collector.addAndThrow(new LocalizedResourceConfigException("warning.config.item.missing_model_id"));
                }
            }

            // 新版格式
            ItemModel modernModel = null;
            // 旧版格式
            TreeSet<LegacyOverridesModel> legacyOverridesModels = null;
            // 如果需要支持新版item model 或者用户需要旧版本兼容，但是没配置legacy-model
            if (needsModelSection) {
                // 1.21.4+必须要配置model区域，如果不需要高版本兼容，则可以只写legacy-model
                if (modelSection == null) {
                    collector.addAndThrow(new LocalizedResourceConfigException("warning.config.item.missing_model"));
                    return;
                }
                try {
                    modernModel = ItemModels.fromMap(modelSection);
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
                        collector.add(new LocalizedResourceConfigException("warning.config.item.legacy_model.cannot_convert", path.toString(), id.asString()));
                    }
                }
            }

            // 自定义物品的model处理
            if (!isVanillaItem) {
                // 这个item-model是否存在，且是原版item-model
                boolean isVanillaItemModel = itemModelKey != null && AbstractPackManager.PRESET_ITEMS.containsKey(itemModelKey);
                // 使用了自定义模型值
                if (customModelData != 0) {
                    // 如果用户主动设置了item-model且为原版物品，则使用item-model为基础模型，否则使用其视觉材质对应的item-model
                    Key finalBaseModel = isVanillaItemModel ? itemModelKey : clientBoundMaterial;
                    // 检查cmd冲突
                    Map<Integer, Key> conflict = AbstractItemManager.this.cmdConflictChecker.computeIfAbsent(finalBaseModel, k -> new HashMap<>());
                    if (conflict.containsKey(customModelData)) {
                        collector.addAndThrow(new LocalizedResourceConfigException("warning.config.item.custom_model_data_conflict", String.valueOf(customModelData), conflict.get(customModelData).toString()));
                    }
                    conflict.put(customModelData, id);
                    // 添加新版item model
                    if (isModernFormatRequired() && modernModel != null) {
                        TreeMap<Integer, ModernItemModel> map = AbstractItemManager.this.modernOverrides.computeIfAbsent(finalBaseModel, k -> new TreeMap<>());
                        map.put(customModelData, new ModernItemModel(
                                modernModel,
                                ResourceConfigUtils.getAsBoolean(section.getOrDefault("oversized-in-gui", true), "oversized-in-gui"),
                                ResourceConfigUtils.getAsBoolean(section.getOrDefault("hand-animation-on-swap", true), "hand-animation-on-swap")
                        ));
                    }
                    // 添加旧版 overrides
                    if (needsLegacyCompatibility() && legacyOverridesModels != null && !legacyOverridesModels.isEmpty()) {
                        TreeSet<LegacyOverridesModel> lom = AbstractItemManager.this.legacyOverrides.computeIfAbsent(finalBaseModel, k -> new TreeSet<>());
                        lom.addAll(legacyOverridesModels);
                    }
                } else if (isVanillaItemModel) {
                    collector.addAndThrow(new LocalizedResourceConfigException("warning.config.item.item_model.conflict", itemModelKey.asString()));
                }

                // 使用了item-model组件，且不是原版物品的
                if (itemModelKey != null && !isVanillaItemModel) {
                    if (isModernFormatRequired() && modernModel != null) {
                        AbstractItemManager.this.modernItemModels1_21_4.put(itemModelKey, new ModernItemModel(
                                modernModel,
                                ResourceConfigUtils.getAsBoolean(section.getOrDefault("oversized-in-gui", true), "oversized-in-gui"),
                                ResourceConfigUtils.getAsBoolean(section.getOrDefault("hand-animation-on-swap", true), "hand-animation-on-swap")
                        ));
                    }
                    if (Config.packMaxVersion().isAtOrAbove(MinecraftVersions.V1_21_2) && needsLegacyCompatibility() && legacyOverridesModels != null && !legacyOverridesModels.isEmpty()) {
                        TreeSet<LegacyOverridesModel> lom = AbstractItemManager.this.modernItemModels1_21_2.computeIfAbsent(itemModelKey, k -> new TreeSet<>());
                        lom.addAll(legacyOverridesModels);
                    }
                }
            } else {
                // 原版物品的item model覆写
                if (isModernFormatRequired()) {
                    AbstractItemManager.this.modernItemModels1_21_4.put(id, new ModernItemModel(
                            modernModel,
                            ResourceConfigUtils.getAsBoolean(section.getOrDefault("oversized-in-gui", true), "oversized-in-gui"),
                            ResourceConfigUtils.getAsBoolean(section.getOrDefault("hand-animation-on-swap", true), "hand-animation-on-swap")
                    ));
                }
            }

            // 抛出异常
            collector.throwIfPresent();
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
