package net.momirealms.craftengine.bukkit.item;

import net.momirealms.craftengine.bukkit.compatibility.item.MMOItemsProvider;
import net.momirealms.craftengine.bukkit.compatibility.item.NeigeItemsProvider;
import net.momirealms.craftengine.bukkit.item.behavior.AxeItemBehavior;
import net.momirealms.craftengine.bukkit.item.behavior.BoneMealItemBehavior;
import net.momirealms.craftengine.bukkit.item.behavior.BucketItemBehavior;
import net.momirealms.craftengine.bukkit.item.behavior.WaterBucketItemBehavior;
import net.momirealms.craftengine.bukkit.item.factory.BukkitItemFactory;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.*;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviors;
import net.momirealms.craftengine.core.item.modifier.CustomModelDataModifier;
import net.momirealms.craftengine.core.item.modifier.IdModifier;
import net.momirealms.craftengine.core.item.modifier.ItemModelModifier;
import net.momirealms.craftengine.core.pack.LegacyOverridesModel;
import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.ResourceLocation;
import net.momirealms.craftengine.core.pack.misc.EquipmentGeneration;
import net.momirealms.craftengine.core.pack.model.*;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.pack.model.select.ChargeTypeSelectProperty;
import net.momirealms.craftengine.core.pack.model.select.TrimMaterialSelectProperty;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.ConfigSectionParser;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.util.context.ContextHolder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.type.Either;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

public class BukkitItemManager extends AbstractItemManager<ItemStack> {
    static {
        registerVanillaItemExtraBehavior(AxeItemBehavior.INSTANCE, ItemKeys.AXES);
        registerVanillaItemExtraBehavior(WaterBucketItemBehavior.INSTANCE, ItemKeys.WATER_BUCKETS);
        registerVanillaItemExtraBehavior(BucketItemBehavior.INSTANCE, ItemKeys.BUCKET);
        registerVanillaItemExtraBehavior(BoneMealItemBehavior.INSTANCE, ItemKeys.BONE_MEAL);
    }

    private static BukkitItemManager instance;
    private final BukkitItemFactory factory;
    private final BukkitCraftEngine plugin;
    private final ItemEventListener itemEventListener;
    private final DebugStickListener debugStickListener;
    private final ItemParser itemParser;

    public BukkitItemManager(BukkitCraftEngine plugin) {
        super(plugin);
        instance = this;
        this.plugin = plugin;
        this.factory = BukkitItemFactory.create(plugin);
        this.itemEventListener = new ItemEventListener(plugin);
        this.debugStickListener = new DebugStickListener(plugin);
        this.itemParser = new ItemParser();
        this.registerAllVanillaItems();
        if (plugin.hasMod()) {
            Class<?> clazz$CustomStreamCodec = ReflectionUtils.getClazz("net.momirealms.craftengine.mod.item.CustomStreamCodec");
            if (clazz$CustomStreamCodec != null) {
                Field cProcessor = ReflectionUtils.getDeclaredField(clazz$CustomStreamCodec, Function.class, 0);
                Field sProcessor = ReflectionUtils.getDeclaredField(clazz$CustomStreamCodec, Function.class, 1);
                Function<Object, Object> c = (raw) -> {
                    ItemStack itemStack = FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(raw);
                    Item<ItemStack> wrapped = this.wrap(itemStack.clone());
                    Optional<CustomItem<ItemStack>> customItem = wrapped.getCustomItem();
                    if (customItem.isEmpty()) {
                        return processItem(wrapped);
                    }
                    CustomItem<ItemStack> custom = customItem.get();
                    if (!custom.hasClientBoundDataModifier()) {
                        return processItem(wrapped);
                    }
                    for (NetworkItemDataProcessor<ItemStack> processor : custom.networkItemDataProcessors()) {
                        processor.toClient(wrapped, ItemBuildContext.EMPTY);
                    }
                    return processItem(wrapped);
                };

                Function<Object, Object> s = (raw) -> {
                    ItemStack itemStack = FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(raw);
                    Item<ItemStack> wrapped = this.wrap(itemStack);
                    Optional<CustomItem<ItemStack>> customItem = wrapped.getCustomItem();
                    if (customItem.isEmpty()) {
                        return processItem(wrapped);
                    }
                    CustomItem<ItemStack> custom = customItem.get();
                    if (!custom.hasClientBoundDataModifier()) {
                        return processItem(wrapped);
                    }
                    for (NetworkItemDataProcessor<ItemStack> processor : custom.networkItemDataProcessors()) {
                        processor.toServer(wrapped, ItemBuildContext.EMPTY);
                    }

                    return processItem(wrapped);
                };
                try {
                    assert cProcessor != null;
                    cProcessor.set(null, c);
                    assert sProcessor != null;
                    sProcessor.set(null, s);
                } catch (ReflectiveOperationException e) {
                    plugin.logger().warn("Failed to load custom stream codec", e);
                }
            }
        }
    }

    private Object processItem(Item<?> item) {
        if (VersionHelper.isVersionNewerThan1_20_5()) {
            ComponentUtils.processComponent(item, ComponentKeys.CAN_BREAK, "predicates");
            ComponentUtils.processComponent(item, ComponentKeys.CAN_PLACE_ON, "predicates");
            ComponentUtils.processComponent(item, ComponentKeys.TOOL, "rules");
        }
        item.load();
        return item.getLiteralObject();
    }

    @Override
    public void delayedInit() {
        Bukkit.getPluginManager().registerEvents(this.itemEventListener, this.plugin.bootstrap());
        Bukkit.getPluginManager().registerEvents(this.debugStickListener, this.plugin.bootstrap());
        this.hookExternalPlugins();
    }

    private void hookExternalPlugins() {
        if (this.plugin.isPluginEnabled("NeigeItems")) {
            registerExternalItemProvider(new NeigeItemsProvider());
        }
        if (this.plugin.isPluginEnabled("MMOItems")) {
            registerExternalItemProvider(new MMOItemsProvider());
        }
    }

    @Override
    public Object encodeJava(Key componentType, @Nullable Object component) {
        return this.factory.encodeJava(componentType, component);
    }

    public static BukkitItemManager instance() {
        return instance;
    }

    @Override
    public Optional<BuildableItem<ItemStack>> getVanillaItem(Key key) {
        Material material = Registry.MATERIAL.get(Objects.requireNonNull(NamespacedKey.fromString(key.toString())));
        if (material == null) {
            return Optional.empty();
        }
        return Optional.of(new CloneableConstantItem(key, new ItemStack(material)));
    }

    @Override
    public int fuelTime(ItemStack itemStack) {
        if (ItemUtils.isEmpty(itemStack)) return 0;
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
    }

    @Override
    public ConfigSectionParser parser() {
        return this.itemParser;
    }

    @Override
    public ItemStack buildCustomItemStack(Key id, Player player) {
        return Optional.ofNullable(customItems.get(id)).map(it -> it.buildItemStack(new ItemBuildContext(player, ContextHolder.EMPTY), 1)).orElse(null);
    }

    @Override
    public ItemStack buildItemStack(Key id, @Nullable Player player) {
        return Optional.ofNullable(buildCustomItemStack(id, player)).orElseGet(() -> createVanillaItemStack(id));
    }

    @Override
    public Item<ItemStack> createCustomWrappedItem(Key id, Player player) {
        return Optional.ofNullable(customItems.get(id)).map(it -> it.buildItem(player)).orElse(null);
    }

    private ItemStack createVanillaItemStack(Key id) {
        NamespacedKey key = NamespacedKey.fromString(id.toString());
        if (key == null) {
            this.plugin.logger().warn(id + " is not a valid namespaced key");
            return new ItemStack(Material.AIR);
        }
        Material material = Registry.MATERIAL.get(key);
        if (material == null) {
            this.plugin.logger().warn(id + " is not a valid material");
            return new ItemStack(Material.AIR);
        }
        return new ItemStack(material);
    }

    @Override
    public Item<ItemStack> createWrappedItem(Key id, @Nullable Player player) {
        return Optional.ofNullable(customItems.get(id)).map(it -> it.buildItem(player)).orElseGet(() -> {
            ItemStack itemStack = createVanillaItemStack(id);
            return wrap(itemStack);
        });
    }

    @Override
    public Item<ItemStack> wrap(ItemStack itemStack) {
        if (ItemUtils.isEmpty(itemStack)) return null;
        return this.factory.wrap(itemStack);
    }

    @Override
    public Key itemId(ItemStack itemStack) {
        Item<ItemStack> wrapped = wrap(itemStack);
        return wrapped.id();
    }

    @Override
    public Key customItemId(ItemStack itemStack) {
        Item<ItemStack> wrapped = wrap(itemStack);
        if (!wrapped.hasTag(IdModifier.CRAFT_ENGINE_ID)) return null;
        return wrapped.id();
    }

    public class ItemParser implements ConfigSectionParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] {"items", "item"};

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
            if (customItems.containsKey(id)) {
                TranslationManager.instance().log("warning.config.item.duplicated", path.toString(), id.toString());
                return;
            }

            // just register for recipes
            Holder.Reference<Key> holder = BuiltInRegistries.OPTIMIZED_ITEM_ID.get(id)
                    .orElseGet(() -> ((WritableRegistry<Key>) BuiltInRegistries.OPTIMIZED_ITEM_ID)
                            .register(new ResourceKey<>(BuiltInRegistries.OPTIMIZED_ITEM_ID.key().location(), id), id));

            String materialStringId = (String) section.get("material");
            if (materialStringId == null) {
                TranslationManager.instance().log("warning.config.item.lack_material", path.toString(), id.toString());
                return;
            }

            Material material = MaterialUtils.getMaterial(materialStringId);
            if (material == null) {
                TranslationManager.instance().log("warning.config.item.invalid_material", path.toString(), id.toString(), materialStringId);
                return;
            }
            
            Key materialId = Key.of(material.getKey().namespace(), material.getKey().value());
            int customModelData = MiscUtils.getAsInt(section.getOrDefault("custom-model-data", 0));
            Key itemModelKey = null;

            CustomItem.Builder<ItemStack> itemBuilder = BukkitCustomItem.builder().id(id).material(materialId);
            itemBuilder.dataModifier(new IdModifier<>(id));

            boolean hasItemModelSection = section.containsKey("item-model");

            // To get at least one model provider
            // Sets some basic model info
            if (customModelData != 0) {
                itemBuilder.dataModifier(new CustomModelDataModifier<>(customModelData));
            }
            // Requires the item to have model before apply item-model
            else if (!hasItemModelSection && section.containsKey("model") && VersionHelper.isVersionNewerThan1_21_2()) {
                // check server version here because components require 1.21.2+
                // customize or use the id
                itemModelKey = Key.from(section.getOrDefault("item-model", id.toString()).toString());
                if (ResourceLocation.isValid(itemModelKey.toString())) {
                    itemBuilder.dataModifier(new ItemModelModifier<>(itemModelKey));
                } else {
                    itemModelKey = null;
                }
            }

            if (hasItemModelSection) {
                itemModelKey = Key.from(section.get("item-model").toString());
                itemBuilder.dataModifier(new ItemModelModifier<>(itemModelKey));
            }

            // Get item behaviors
            Object behaviorConfig = section.get("behavior");
            if (behaviorConfig instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> behavior = (List<Map<String, Object>>) behaviorConfig;
                List<ItemBehavior> behaviors = new ArrayList<>();
                for (Map<String, Object> behaviorMap : behavior) {
                    behaviors.add(ItemBehaviors.fromMap(pack, path, id, behaviorMap));
                }
                itemBuilder.behaviors(behaviors);
            } else if (behaviorConfig instanceof Map<?, ?>) {
                Map<String, Object> behaviorSection = MiscUtils.castToMap(section.get("behavior"), true);
                if (behaviorSection != null) {
                    itemBuilder.behavior(ItemBehaviors.fromMap(pack, path, id, behaviorSection));
                }
            }

            // Get item data
            Map<String, Object> dataSection = MiscUtils.castToMap(section.get("data"), true);
            if (dataSection != null) {
                for (Map.Entry<String, Object> dataEntry : dataSection.entrySet()) {
                    Optional.ofNullable(dataFunctions.get(dataEntry.getKey())).ifPresent(function -> {
                        try {
                            itemBuilder.dataModifier(function.apply(dataEntry.getValue()));
                        } catch (IllegalArgumentException e) {
                            plugin.logger().warn("Invalid data format", e);
                        }
                    });
                }
            }

            // Get item data
            Map<String, Object> clientSideDataSection = MiscUtils.castToMap(section.get("client-bound-data"), true);
            if (clientSideDataSection != null) {
                for (Map.Entry<String, Object> dataEntry : clientSideDataSection.entrySet()) {
                    Optional.ofNullable(dataFunctions.get(dataEntry.getKey())).ifPresent(function -> {
                        try {
                            itemBuilder.clientBoundDataModifier(function.apply(dataEntry.getValue()));
                        } catch (IllegalArgumentException e) {
                            plugin.logger().warn("Invalid client bound data format", e);
                        }
                    });
                }
            }

            if (section.containsKey("settings")) {
                Map<String, Object> settings = MiscUtils.castToMap(section.get("settings"), false);
                itemBuilder.settings(ItemSettings.fromMap(settings));
            }

            CustomItem<ItemStack> customItem = itemBuilder.build();
            customItems.put(id, customItem);

            // cache command suggestions
            cachedSuggestions.add(Suggestion.suggestion(id.toString()));
            if (material == Material.TOTEM_OF_UNDYING)
                cachedTotemSuggestions.add(Suggestion.suggestion(id.toString()));

            // post process
            // register tags
            Set<Key> tags = customItem.settings().tags();
            for (Key tag : tags) {
                customItemTags.computeIfAbsent(tag, k -> new ArrayList<>()).add(holder);
            }

            // create trims
            EquipmentGeneration equipment = customItem.settings().equipment();
            if (equipment != null) {
                EquipmentData modern = equipment.modernData();
                // 1.21.2+
                if (modern != null) {
                    equipmentsToGenerate.add(equipment);
                }
                // TODO 1.20
            }

            // add it to category
            if (section.containsKey("category")) {
                plugin.itemBrowserManager().addExternalCategoryMember(id, MiscUtils.getAsStringList(section.get("category")).stream().map(Key::of).toList());
            }

            // model part, can be null
            // but if it exists, either custom model data or item model should be configured
            Map<String, Object> modelSection = MiscUtils.castToMap(section.get("model"), true);
            if (modelSection == null) {
                return;
            }

            boolean hasModel = false;
            if (customModelData != 0) {
                hasModel= true;
                // use custom model data
                // check conflict
                Map<Integer, Key> conflict = cmdConflictChecker.computeIfAbsent(materialId, k -> new HashMap<>());
                if (conflict.containsKey(customModelData)) {
                    TranslationManager.instance().log("warning.config.item.custom_model_data_conflict", path.toString(), id.toString(), String.valueOf(customModelData), conflict.get(customModelData).toString());
                    return;
                }

                if (customModelData > 16_777_216) {
                    TranslationManager.instance().log("warning.config.item.bad_custom_model_data_value", path.toString(), id.toString(), String.valueOf(customModelData));
                }

                conflict.put(customModelData, id);

                // Parse models
                ItemModel model = ItemModels.fromMap(modelSection);
                for (ModelGeneration generation : model.modelsToGenerate()) {
                    prepareModelGeneration(path, id, generation);
                }

                if (Config.packMaxVersion() > 21.39f) {
                    TreeMap<Integer, ItemModel> map = modernOverrides.computeIfAbsent(materialId, k -> new TreeMap<>());
                    map.put(customModelData, model);
                }

                if (Config.packMinVersion() < 21.39f) {
                    List<LegacyOverridesModel> legacyOverridesModels = new ArrayList<>();
                    processModelRecursively(model, new LinkedHashMap<>(), legacyOverridesModels, materialId, customModelData);
                    TreeSet<LegacyOverridesModel> lom = legacyOverrides.computeIfAbsent(materialId, k -> new TreeSet<>());
                    lom.addAll(legacyOverridesModels);
                }
            }
            if (itemModelKey != null) {
                hasModel = true;
                // use components
                ItemModel model = ItemModels.fromMap(modelSection);
                for (ModelGeneration generation : model.modelsToGenerate()) {
                    prepareModelGeneration(path, id, generation);
                }

                if (Config.packMaxVersion() > 21.39f) {
                    modernItemModels1_21_4.put(itemModelKey, model);
                }

                if (Config.packMaxVersion() > 21.19f && Config.packMinVersion() < 21.39f) {
                    List<LegacyOverridesModel> legacyOverridesModels = new ArrayList<>();
                    processModelRecursively(model, new LinkedHashMap<>(), legacyOverridesModels, materialId, 0);
                    if (!legacyOverridesModels.isEmpty()) {
                        legacyOverridesModels.sort(LegacyOverridesModel::compareTo);
                        modernItemModels1_21_2.put(itemModelKey, legacyOverridesModels);
                    } else {
                        plugin.debug(() -> "Can't convert " + id + "'s model to legacy format.");
                    }
                }
            }
            if (!hasModel) {
                TranslationManager.instance().log("warning.config.item.lack_model_id", path.toString(), id.toString());
            }
        }
    }

    private void processModelRecursively(
            ItemModel currentModel,
            Map<String, Object> accumulatedPredicates,
            List<LegacyOverridesModel> resultList,
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
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void handleConditionModel(
            ConditionItemModel model,
            Map<String, Object> parentPredicates,
            List<LegacyOverridesModel> resultList,
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
            List<LegacyOverridesModel> resultList,
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
            List<LegacyOverridesModel> resultList,
            Key materialId,
            int customModelData
    ) {
        if (model.property() instanceof LegacyModelPredicate predicate) {
            String predicateId = predicate.legacyPredicateId(materialId);
            for (Map.Entry<Either<String, List<String>>, ItemModel> entry : model.whenMap().entrySet()) {
                List<String> cases = entry.getKey().fallbackOrMapPrimary(List::of);
                for (String caseValue : cases) {
                    Number legacyValue = predicate.toLegacyValue(caseValue);
                    if (predicate instanceof TrimMaterialSelectProperty property && property.isArmor(materialId)) {
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
                } else if (predicate instanceof TrimMaterialSelectProperty property && property.isArmor(materialId)) {
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

    @SuppressWarnings("unchecked")
    private void registerAllVanillaItems() {
        try {
            for (NamespacedKey item : FastNMS.INSTANCE.getAllVanillaItems()) {
                if (item.getNamespace().equals("minecraft")) {
                    Key id = KeyUtils.namespacedKey2Key(item);
                    VANILLA_ITEMS.add(id);
                    Holder.Reference<Key> holder =  BuiltInRegistries.OPTIMIZED_ITEM_ID.get(id)
                            .orElseGet(() -> ((WritableRegistry<Key>) BuiltInRegistries.OPTIMIZED_ITEM_ID)
                                    .register(new ResourceKey<>(BuiltInRegistries.OPTIMIZED_ITEM_ID.key().location(), id), id));
                    Object resourceLocation = KeyUtils.toResourceLocation(id.namespace(), id.value());
                    Object mcHolder = ((Optional<Object>) Reflections.method$Registry$getHolder1.invoke(Reflections.instance$BuiltInRegistries$ITEM, Reflections.method$ResourceKey$create.invoke(null, Reflections.instance$Registries$ITEM, resourceLocation))).get();
                    Set<Object> tags = (Set<Object>) Reflections.field$Holder$Reference$tags.get(mcHolder);
                    for (Object tag : tags) {
                        Key tagId = Key.of(Reflections.field$TagKey$location.get(tag).toString());
                        VANILLA_ITEM_TAGS.computeIfAbsent(tagId, (key) -> new ArrayList<>()).add(holder);
                    }
                }
            }
        } catch (ReflectiveOperationException e) {
            plugin.logger().warn("Failed to init vanilla items", e);
        }
    }
}
