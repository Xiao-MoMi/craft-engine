package net.momirealms.craftengine.bukkit.item;

import net.momirealms.craftengine.bukkit.item.behavior.AxeItemBehavior;
import net.momirealms.craftengine.bukkit.item.behavior.BoneMealBehavior;
import net.momirealms.craftengine.bukkit.item.behavior.BucketItemBehavior;
import net.momirealms.craftengine.bukkit.item.behavior.WaterBucketItemBehavior;
import net.momirealms.craftengine.bukkit.item.factory.BukkitItemFactory;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.ItemUtils;
import net.momirealms.craftengine.bukkit.util.MaterialUtils;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.*;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviors;
import net.momirealms.craftengine.core.item.modifier.CustomModelDataModifier;
import net.momirealms.craftengine.core.item.modifier.IdModifier;
import net.momirealms.craftengine.core.item.modifier.ItemModelModifier;
import net.momirealms.craftengine.core.pack.LegacyOverridesModel;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.generator.ModelGeneration;
import net.momirealms.craftengine.core.pack.model.*;
import net.momirealms.craftengine.core.plugin.config.ConfigManager;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceKey;
import net.momirealms.craftengine.core.util.VersionHelper;
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

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class BukkitItemManager extends AbstractItemManager<ItemStack> {
    private static final Map<Key, List<ItemBehavior>> VANILLA_ITEM_EXTRA_BEHAVIORS = new HashMap<>();

    static {
        registerVanillaItemExtraBehavior(AxeItemBehavior.INSTANCE, ItemKeys.AXES);
        registerVanillaItemExtraBehavior(WaterBucketItemBehavior.INSTANCE, ItemKeys.WATER_BUCKETS);
        registerVanillaItemExtraBehavior(BucketItemBehavior.INSTANCE, ItemKeys.BUCKET);
        registerVanillaItemExtraBehavior(BoneMealBehavior.INSTANCE, ItemKeys.BONE_MEAL);
    }

    private static void registerVanillaItemExtraBehavior(ItemBehavior behavior, Key... items) {
        for (Key key : items) {
            VANILLA_ITEM_EXTRA_BEHAVIORS.computeIfAbsent(key, k -> new ArrayList<>()).add(behavior);
        }
    }

    private static BukkitItemManager instance;
    private final BukkitItemFactory factory;
    private final Map<Key, TreeSet<LegacyOverridesModel>> legacyOverrides;
    private final Map<Key, TreeMap<Integer, ItemModel>> modernOverrides;
    private final BukkitCraftEngine plugin;
    private final ItemEventListener itemEventListener;
    private final DebugStickListener debugStickListener;
    private final Map<Key, List<Holder<Key>>> vanillaItemTags;
    private final Map<Key, List<Holder<Key>>> customItemTags;
    private final Map<Key, Map<Integer, Key>> cmdConflictChecker;
    private final Map<Key, ItemModel> modernItemModels1_21_4;
    private final Map<Key, List<LegacyOverridesModel>> modernItemModels1_21_2;
    // Cached command suggestions
    private final List<Suggestion> cachedSuggestions = new ArrayList<>();

    public BukkitItemManager(BukkitCraftEngine plugin) {
        super(plugin);
        this.plugin = plugin;
        this.factory = BukkitItemFactory.create(plugin);
        this.legacyOverrides = new HashMap<>();
        this.modernOverrides = new HashMap<>();
        this.vanillaItemTags = new HashMap<>();
        this.customItemTags = new HashMap<>();
        this.cmdConflictChecker = new HashMap<>();
        this.modernItemModels1_21_4 = new HashMap<>();
        this.modernItemModels1_21_2 = new HashMap<>();
        this.itemEventListener = new ItemEventListener(plugin);
        this.debugStickListener = new DebugStickListener(plugin);
        this.registerAllVanillaItems();
        instance = this;
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
    public List<Holder<Key>> tagToItems(Key tag) {
        List<Holder<Key>> items = new ArrayList<>();
        List<Holder<Key>> holders = vanillaItemTags.get(tag);
        if (holders != null) {
            items.addAll(holders);
        }
        List<Holder<Key>> customItems = customItemTags.get(tag);
        if (customItems != null) {
            items.addAll(customItems);
        }
        return items;
    }

    @Override
    public List<Holder<Key>> tagToVanillaItems(Key tag) {
        return this.vanillaItemTags.getOrDefault(tag, List.of());
    }

    @Override
    public List<Holder<Key>> tagToCustomItems(Key tag) {
        return this.customItemTags.getOrDefault(tag, List.of());
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
    public Collection<Suggestion> cachedSuggestions() {
        return this.cachedSuggestions;
    }

    @Override
    public void load() {
        super.load();
        Bukkit.getPluginManager().registerEvents(this.itemEventListener, plugin.bootstrap());
        Bukkit.getPluginManager().registerEvents(this.debugStickListener, plugin.bootstrap());
    }

    @Override
    public void unload() {
        super.unload();
        this.cachedSuggestions.clear();
        this.legacyOverrides.clear();
        this.modernOverrides.clear();
        this.customItemTags.clear();
        HandlerList.unregisterAll(this.itemEventListener);
        HandlerList.unregisterAll(this.debugStickListener);
        this.cmdConflictChecker.clear();
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

    @Override
    public Optional<List<ItemBehavior>> getItemBehavior(Key key) {
        Optional<CustomItem<ItemStack>> customItemOptional = getCustomItem(key);
        if (customItemOptional.isPresent()) {
            CustomItem<ItemStack> customItem = customItemOptional.get();
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
        return new ArrayList<>(customItems.keySet());
    }

    @Override
    public void parseSection(Pack pack, Path path, Key id, Map<String, Object> section) {
        // just register and recipes
        Holder.Reference<Key> holder = BuiltInRegistries.OPTIMIZED_ITEM_ID.get(id)
                .orElseGet(() -> ((WritableRegistry<Key>) BuiltInRegistries.OPTIMIZED_ITEM_ID)
                        .register(new ResourceKey<>(BuiltInRegistries.OPTIMIZED_ITEM_ID.key().location(), id), id));

        String materialStringId = (String) section.get("material");
        Material material = MaterialUtils.getMaterial(materialStringId);
        if (material == null) {
            plugin.logger().warn(path, "material " + Optional.ofNullable(materialStringId).map(it -> it + " ").orElse("") + "does not exist for item " + id);
            return;
        }
        Key materialId = Key.of(material.getKey().namespace(), material.getKey().value());

        int customModelData = MiscUtils.getAsInt(section.getOrDefault("custom-model-data", 0));
        Key itemModelKey = null;

        CustomItem.Builder<ItemStack> itemBuilder = BukkitCustomItem.builder().id(id).material(materialId);
        itemBuilder.modifier(new IdModifier<>(id));

        // Sets some basic model info
        if (customModelData != 0) {
            itemBuilder.modifier(new CustomModelDataModifier<>(customModelData));
        } else if (section.containsKey("model") && VersionHelper.isVersionNewerThan1_21_2()) {
            // check server version here because components require 1.21.2+
            // customize or use the id
            itemModelKey = Key.from(section.getOrDefault("item-model", id.toString()).toString());
            itemBuilder.modifier(new ItemModelModifier<>(itemModelKey));
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
            itemBuilder.behavior(behaviors);
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
                        itemBuilder.modifier(function.apply(dataEntry.getValue()));
                    } catch (IllegalArgumentException e) {
                        plugin.logger().warn("Invalid data format", e);
                    }
                });
            }
        }

        if (section.containsKey("settings")) {
            Map<String, Object> settings = MiscUtils.castToMap(section.get("settings"), false);
            itemBuilder.settings(ItemSettings.fromMap(settings));
        }

        CustomItem<ItemStack> customItem = itemBuilder.build();
        this.customItems.put(id, customItem);
        this.cachedSuggestions.add(Suggestion.suggestion(id.toString()));

        // regitser tags
        Set<Key> tags = customItem.settings().tags();
        for (Key tag : tags) {
            this.customItemTags.computeIfAbsent(tag, k -> new ArrayList<>()).add(holder);
        }

        // model part, can be null
        // but if it exists, either custom model data or item model should be configured
        Map<String, Object> modelSection = MiscUtils.castToMap(section.get("model"), true);
        if (modelSection == null) {
            return;
        }

        if (customModelData != 0) {
            // use custom model data
            // check conflict
            Map<Integer, Key> conflict = this.cmdConflictChecker.computeIfAbsent(materialId, k -> new HashMap<>());
            if (conflict.containsKey(customModelData)) {
                plugin.logger().warn(path, "Failed to create model for " + id + " because custom-model-data " + customModelData + " already occupied by item " + conflict.get(customModelData).toString());
                return;
            }

            conflict.put(customModelData, id);

            // Parse models
            ItemModel model = ItemModels.fromMap(modelSection);
            for (ModelGeneration generation : model.modelsToGenerate()) {
                prepareModelGeneration(generation);
            }

            if (ConfigManager.packMaxVersion() > 21.39f) {
                TreeMap<Integer, ItemModel> map = this.modernOverrides.computeIfAbsent(materialId, k -> new TreeMap<>());
                map.put(customModelData, model);
            }

            if (ConfigManager.packMinVersion() < 21.39f) {
                List<LegacyOverridesModel> legacyOverridesModels = new ArrayList<>();
                processModelRecursively(model, new LinkedHashMap<>(), legacyOverridesModels, materialId, customModelData);
                TreeSet<LegacyOverridesModel> lom = this.legacyOverrides.computeIfAbsent(materialId, k -> new TreeSet<>());
                lom.addAll(legacyOverridesModels);
            }
        } else if (itemModelKey != null) {
            // use components
            ItemModel model = ItemModels.fromMap(modelSection);
            for (ModelGeneration generation : model.modelsToGenerate()) {
                prepareModelGeneration(generation);
            }

            if (ConfigManager.packMaxVersion() > 21.39f) {
                this.modernItemModels1_21_4.put(itemModelKey, model);
            }

            if (ConfigManager.packMaxVersion() > 21.19f && ConfigManager.packMinVersion() < 21.39f) {
                List<LegacyOverridesModel> legacyOverridesModels = new ArrayList<>();
                processModelRecursively(model, new LinkedHashMap<>(), legacyOverridesModels, materialId, 0);
                if (legacyOverridesModels.isEmpty()) {
                    plugin.logger().warn(path, "Can't convert " + id + "'s model to legacy format.");
                    return;
                }
                legacyOverridesModels.sort(LegacyOverridesModel::compareTo);
                this.modernItemModels1_21_2.put(itemModelKey, legacyOverridesModels);
            }
        } else {
            if (!VersionHelper.isVersionNewerThan1_21_2()) {
                plugin.logger().warn(path, "No custom-model-data configured for " + id);
            }
        }
    }

    @Override
    public Map<Key, ItemModel> modernItemModels1_21_4() {
        return this.modernItemModels1_21_4;
    }

    @Override
    public Map<Key, List<LegacyOverridesModel>> modernItemModels1_21_2() {
        return this.modernItemModels1_21_2;
    }

    @Override
    public Map<Key, TreeSet<LegacyOverridesModel>> legacyItemOverrides() {
        return this.legacyOverrides;
    }

    @Override
    public Map<Key, TreeMap<Integer, ItemModel>> modernItemOverrides() {
        return this.modernOverrides;
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
                    Map<String, Object> merged = mergePredicates(
                            parentPredicates,
                            predicateId,
                            predicate.toLegacyValue(caseValue)
                    );
                    // Additional check for crossbow
                    if (materialId.equals(ItemKeys.CROSSBOW)) {
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
            if (model.fallBack() != null && materialId.equals(ItemKeys.CROSSBOW)) {
                Map<String, Object> merged = mergePredicates(
                        parentPredicates,
                        "charged",
                        0
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
            for (Material material : Registry.MATERIAL) {
                if (!material.isLegacy() && material.isItem()) {
                    Key id = Key.from(material.getKey().asString());
                    Holder.Reference<Key> holder =  BuiltInRegistries.OPTIMIZED_ITEM_ID.get(id)
                            .orElseGet(() -> ((WritableRegistry<Key>) BuiltInRegistries.OPTIMIZED_ITEM_ID)
                                    .register(new ResourceKey<>(BuiltInRegistries.OPTIMIZED_ITEM_ID.key().location(), id), id));

                    Object resourceLocation = Reflections.method$ResourceLocation$fromNamespaceAndPath.invoke(null, id.namespace(), id.value());
                    Object mcHolder = ((Optional<Object>) Reflections.method$Registry$getHolder1.invoke(Reflections.instance$BuiltInRegistries$ITEM, Reflections.method$ResourceKey$create.invoke(null, Reflections.instance$Registries$ITEM, resourceLocation))).get();
                    Set<Object> tags = (Set<Object>) Reflections.field$Holder$Reference$tags.get(mcHolder);
                    for (Object tag : tags) {
                        Key tagId = Key.of(Reflections.field$TagKey$location.get(tag).toString());
                        this.vanillaItemTags.computeIfAbsent(tagId, (key) -> new ArrayList<>()).add(holder);
                    }
                }
            }
        } catch (ReflectiveOperationException e) {
            plugin.logger().warn("Failed to init vanilla items", e);
        }
    }
}
