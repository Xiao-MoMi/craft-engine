package net.momirealms.craftengine.core.attribute;

import net.momirealms.craftengine.core.attribute.base.BaseValueSource;
import net.momirealms.craftengine.core.attribute.base.BaseValueSources;
import net.momirealms.craftengine.core.attribute.base.ConstantBaseValueSource;
import net.momirealms.craftengine.core.attribute.damage.DamageEvent;
import net.momirealms.craftengine.core.attribute.damage.DamageRule;
import net.momirealms.craftengine.core.attribute.damage.DamageRuleTable;
import net.momirealms.craftengine.core.attribute.damage.effect.DamageEffect;
import net.momirealms.craftengine.core.attribute.damage.effect.DamageEffects;
import net.momirealms.craftengine.core.attribute.derived.DerivedValue;
import net.momirealms.craftengine.core.attribute.derived.DerivedValues;
import net.momirealms.craftengine.core.attribute.formula.DamageFormula;
import net.momirealms.craftengine.core.attribute.formula.DamageFormulas;
import net.momirealms.craftengine.core.attribute.modifier.ItemAttributeModifier;
import net.momirealms.craftengine.core.attribute.modifier.ItemAttributeModifierStore;
import net.momirealms.craftengine.core.attribute.modifier.ItemAttributeModifiersProvider;
import net.momirealms.craftengine.core.attribute.modifier.SlotAttributeModifierConfig;
import net.momirealms.craftengine.core.attribute.sync.SyncTarget;
import net.momirealms.craftengine.core.attribute.sync.SyncValueProvider;
import net.momirealms.craftengine.core.attribute.sync.SyncValueProviders;
import net.momirealms.craftengine.core.attribute.sync.ValueSyncValueProvider;
import net.momirealms.craftengine.core.attribute.vanilla.VanillaAttributeModifier.Operation;
import net.momirealms.craftengine.core.entity.Entity;
import net.momirealms.craftengine.core.entity.EntityDefinition;
import net.momirealms.craftengine.core.entity.LivingEntity;
import net.momirealms.craftengine.core.entity.LivingEntityHolder;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.item.equipment.EquipmentSet;
import net.momirealms.craftengine.core.item.setting.value.AttributeModifiers;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.*;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStage;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStages;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;

public abstract class AbstractAttributeManager implements AttributeManager {
    protected final CraftEngine plugin;
    // API 注册
    protected final Map<Key, Attribute> apiAttributes = new HashMap<>();
    protected final Map<Key, AttributeOperation> apiOperations = new HashMap<>();
    // 配置注册
    protected final Map<Key, Attribute> configAttributes = new HashMap<>();
    protected final Map<Key, AttributeOperation> configOperations = new HashMap<>();
    // 套装定义
    protected final Map<Key, EquipmentSet> equipmentSets = new HashMap<>();
    // 物品修饰符动态来源注册表（按 id），变动时重建有序快照
    protected final Map<Key, RegisteredProvider> itemModifiersProviders = new LinkedHashMap<>();
    private final OperationParser operationParser = new OperationParser();
    private final AttributeParser attributeParser = new AttributeParser();
    private final DamageRuleParser damageRuleParser = new DamageRuleParser();
    private final EquipmentSetParser equipmentSetParser = new EquipmentSetParser();
    // 按优先级升序的快照，遍历时零排序开销
    protected volatile List<ItemAttributeModifiersProvider> sortedItemModifiersProviders = List.of();
    // API配置合并
    protected Map<Key, Attribute> mergedAttributes = Map.of();
    // 按实体类型分桶的受限属性
    protected List<Attribute> globalAttributes = List.of();
    protected Map<Key, List<Attribute>> attributesByEntityType = Map.of();
    private DamageRuleTable damageRules;

    protected AbstractAttributeManager(CraftEngine plugin) {
        this.plugin = plugin;
        this.apiOperations.put(AttributeOperations.ADD_VALUE_ID, AttributeOperations.ADD_VALUE);
        this.apiOperations.put(AttributeOperations.ADD_MULTIPLIED_BASE_ID, AttributeOperations.ADD_MULTIPLIED_BASE);
        this.apiOperations.put(AttributeOperations.ADD_MULTIPLIED_TOTAL_ID, AttributeOperations.ADD_MULTIPLIED_TOTAL);
        registerItemModifiersProvider(Key.ce("settings"), ItemAttributeModifiersProvider.PRIORITY_SETTINGS, item -> {
            Optional<ItemDefinition> definition = item.getDefinition();
            if (definition.isEmpty()) return List.of();
            AttributeModifiers modifiers = definition.get().settings().attributeModifiers();
            return modifiers == null ? List.of() : modifiers.modifiers();
        });
        registerItemModifiersProvider(Key.ce("persistent"), ItemAttributeModifiersProvider.PRIORITY_PERSISTENT, item -> {
            List<ItemAttributeModifier> persistent = ItemAttributeModifierStore.read(item);
            if (persistent.isEmpty()) return List.of();
            List<SlotAttributeModifierConfig> configs = new ArrayList<>(persistent.size());
            for (ItemAttributeModifier modifier : persistent) {
                configs.add(modifier.toConfig());
            }
            return configs;
        });
    }

    @Override
    public void unload() {
        this.configAttributes.clear();
        this.configOperations.clear();
        this.equipmentSets.clear();
        this.damageRules = null;
    }

    @Override
    public Optional<EquipmentSet> equipmentSet(Key id) {
        return Optional.ofNullable(this.equipmentSets.get(id));
    }

    @Override
    public Optional<AttributeOperation> getOperation(Key id) {
        AttributeOperation operation = this.configOperations.get(id);
        return operation != null ? Optional.of(operation) : Optional.ofNullable(this.apiOperations.get(id));
    }

    @Override
    public void registerItemModifiersProvider(Key id, int priority, ItemAttributeModifiersProvider provider) {
        this.itemModifiersProviders.put(id, new RegisteredProvider(priority, provider));
        rebuildSortedItemModifiersProviders();
    }

    @Override
    public void unregisterItemModifiersProvider(Key id) {
        if (this.itemModifiersProviders.remove(id) != null) {
            rebuildSortedItemModifiersProviders();
        }
    }

    private void rebuildSortedItemModifiersProviders() {
        List<RegisteredProvider> all = new ArrayList<>(this.itemModifiersProviders.values());
        all.sort(Comparator.comparingInt(RegisteredProvider::priority));
        List<ItemAttributeModifiersProvider> sorted = new ArrayList<>(all.size());
        for (RegisteredProvider registered : all) {
            sorted.add(registered.provider());
        }
        this.sortedItemModifiersProviders = List.copyOf(sorted);
    }

    @Override
    public List<SlotAttributeModifierConfig> getItemAttributeModifiers(Item item) {
        List<ItemAttributeModifiersProvider> providers = this.sortedItemModifiersProviders;
        if (providers.isEmpty()) return List.of();
        Map<ModifierMergeKey, SlotAttributeModifierConfig> merged = new LinkedHashMap<>();
        for (ItemAttributeModifiersProvider provider : providers) {
            for (SlotAttributeModifierConfig modifier : provider.getModifiers(item)) {
                if (modifier != null) {
                    merged.put(new ModifierMergeKey(modifier.attribute, modifier.id), modifier);
                }
            }
        }
        return merged.isEmpty() ? List.of() : List.copyOf(merged.values());
    }

    @Override
    public Optional<Attribute> getAttribute(Key id) {
        return Optional.ofNullable(this.mergedAttributes.get(id));
    }

    @Override
    public Collection<Attribute> getAttributes() {
        return this.mergedAttributes.values();
    }

    @Override
    public List<Attribute> attributesByEntityType(Key entityType) {
        return this.attributesByEntityType.getOrDefault(entityType, this.globalAttributes);
    }

    @Override
    public double getAttributeValue(LivingEntity entity, Attribute attribute) {
        LivingEntityHolder holder = this.plugin.entityManager().getEntityHolder(entity.uuid());
        if (holder == null) {
            return resolveUntrackedValue(entity, attribute);
        }
        return holder.attributes().getAttributeValue(attribute);
    }

    private double resolveUntrackedValue(LivingEntity entity, Attribute attribute) {
        if (attribute.derived() != null) {
            return attribute.derive(dependency -> resolveUntrackedValue(entity, dependency));
        }
        return attribute.currentValue(entity);
    }

    @Override
    public ConfigParser[] parsers() {
        return new ConfigParser[]{this.attributeParser, this.operationParser, this.damageRuleParser, this.equipmentSetParser};
    }

    @Override
    public DamageRule findDamageRule(DamageEvent event) {
        if (this.damageRules == null) return null;
        return this.damageRules.find(event);
    }

    @Override
    public void processDamageEvent(DamageEvent event) {
        DamageRule rule = findDamageRule(event);
        if (rule == null) {
            return;
        }
        DamageFormula formula = rule.formula();
        if (formula != null) {
            double newDamage = formula.getValue(event);
            if (event.damage() != newDamage) {
                event.setDamage(Math.max(0, newDamage));
            }
        }
        event.initFinalDamage();
        for (DamageEffect effect : rule.effects()) {
            effect.apply(event);
        }
    }

    @Override
    public double vanillaAttributeDefaultBaseValue(Entity living, Key attribute, double fallback) {
        EntityDefinition definition = this.plugin.entityManager().entityDefinition(living.id());
        if (definition != null) {
            Double value = definition.settings().attributeValue(attribute);
            if (value != null) return value;
        }
        return vanillaEntityTypeDefaultBaseValue(living.type(), attribute, fallback);
    }

    protected abstract double vanillaEntityTypeDefaultBaseValue(Key entityType, Key attribute, double fallback);

    private List<AttributeOperation> parseOperations(ConfigSection section) {
        List<String> operationIds = section.getStringList("operations");
        if (operationIds.isEmpty()) return AttributeOperations.DEFAULT_PIPELINE;
        List<AttributeOperation> operations = new ArrayList<>(operationIds.size());
        for (String operationId : operationIds) {
            Key operationKey = Key.of(operationId);
            operations.add(getOperation(operationKey)
                    .orElseThrow(() -> new KnownResourceException("attribute.unknown_operation", section.assemblePath("operations"), operationKey.asString())));
        }
        return List.copyOf(operations);
    }

    @Nullable
    private Set<Key> parseApplicableEntityTypes(ConfigSection section) {
        List<String> entities = section.getStringList("entities");
        if (entities.isEmpty()) return null;
        Set<Key> types = new HashSet<>();
        for (String entry : entities) {
            if (entry.isEmpty()) continue;
            if (entry.charAt(0) == '#') {
                types.addAll(AbstractAttributeManager.this.plugin.entityManager().entityIdsByTag(Key.of(entry.substring(1))));
            } else {
                types.add(Key.of(entry));
            }
        }
        return types.isEmpty() ? null : Set.copyOf(types);
    }

    protected record RegisteredProvider(int priority, ItemAttributeModifiersProvider provider) {
    }

    private record ModifierMergeKey(Key attribute, Key id) {
    }

    private static final class MutableDamageRule {
        @Nullable
        private DamageFormula formula;
        private List<DamageEffect> effects = List.of();
        private boolean hasEffects;

        private void merge(@Nullable DamageFormula formula, List<DamageEffect> effects, boolean hasEffects) {
            if (formula != null) {
                this.formula = formula;
            }
            if (hasEffects) {
                this.effects = effects;
                this.hasEffects = true;
            }
        }

        private DamageRule build(@Nullable DamageRule fallback) {
            DamageFormula resolvedFormula = this.formula != null
                    ? this.formula
                    : fallback == null ? null : fallback.formula();
            List<DamageEffect> resolvedEffects = this.hasEffects
                    ? this.effects
                    : fallback == null ? List.of() : fallback.effects();
            return new DamageRule(resolvedFormula, resolvedEffects);
        }
    }

    private final class DamageRuleParser extends SectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = ConfigKeys.of("damage_rule(s)");
        private static final String[] EFFECTS = ConfigKeys.of("effect(s)", "post_effect(s)");

        @Override
        public Key type() {
            return Key.ce("damage_rule");
        }

        @Override
        protected void parseSection(Pack pack, Path path, ConfigSection section) {
            Map<Key, DamageRuleTable.RulesByVictim> rulesByCause = new HashMap<>();
            for (String damageSourceType : section.keySet()) {
                Key source = Key.of(damageSourceType);
                List<ConfigSection> sections = section.getList(damageSourceType, ConfigValue::getAsSection);
                Map<Key, MutableDamageRule> rulesByVictim = new HashMap<>();
                MutableDamageRule defaultRule = null;
                for (int i = sections.size() - 1; i >= 0; i--) {
                    ConfigSection configSection = sections.get(i);
                    List<String> targets = configSection.getStringList("target");
                    DamageFormula formula = configSection.getValue("formula", DamageFormulas::fromConfig);
                    boolean hasEffects = configSection.containsKey(EFFECTS);
                    List<DamageEffect> ruleEffects = configSection.getList(EFFECTS, DamageEffects::fromConfig);
                    if (formula == null && !hasEffects) {
                        configSection.getNonNullValue("formula", ConfigConstants.ARGUMENT_STRING, DamageFormulas::fromConfig);
                    }
                    if (!targets.isEmpty()) {
                        for (String target : targets) {
                            if (target.isEmpty()) continue;
                            if (target.charAt(0) == '#') {
                                for (Key entity : AbstractAttributeManager.this.plugin.entityManager().entityIdsByTag(Key.of(target.substring(1)))) {
                                    rulesByVictim.computeIfAbsent(entity, ignored -> new MutableDamageRule())
                                            .merge(formula, ruleEffects, hasEffects);
                                }
                            } else {
                                Key entity = Key.of(target);
                                rulesByVictim.computeIfAbsent(entity, ignored -> new MutableDamageRule())
                                        .merge(formula, ruleEffects, hasEffects);
                            }
                        }
                    } else {
                        if (defaultRule == null) {
                            defaultRule = new MutableDamageRule();
                        }
                        defaultRule.merge(formula, ruleEffects, hasEffects);
                    }
                }
                DamageRule resolvedDefaultRule = defaultRule == null ? null : defaultRule.build(null);
                Map<Key, DamageRule> resolvedRulesByVictim = new HashMap<>(rulesByVictim.size());
                rulesByVictim.forEach((victim, rule) -> resolvedRulesByVictim.put(victim, rule.build(resolvedDefaultRule)));
                rulesByCause.put(source, new DamageRuleTable.RulesByVictim(resolvedDefaultRule, resolvedRulesByVictim));
            }
            AbstractAttributeManager.this.damageRules = new DamageRuleTable(rulesByCause);
        }

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public LoadingStage loadingStage() {
            return LoadingStages.ATTRIBUTE_RULES;
        }

        @Override
        public List<LoadingStage> dependencies() {
            return List.of(LoadingStages.ATTRIBUTE);
        }
    }

    private final class AttributeParser extends IdSectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = ConfigKeys.of("attribute(s)");

        @Override
        public Key type() {
            return Key.ce("attribute");
        }

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public LoadingStage loadingStage() {
            return LoadingStages.ATTRIBUTE;
        }

        @Override
        public List<LoadingStage> dependencies() {
            return List.of(LoadingStages.ENTITY, LoadingStages.ATTRIBUTE_OPERATION);
        }

        @Override
        public int count() {
            return AbstractAttributeManager.this.configAttributes.size();
        }

        @Override
        public void postProcess() {
            Map<Key, Attribute> attributes = new HashMap<>();
            attributes.putAll(AbstractAttributeManager.this.apiAttributes);
            attributes.putAll(AbstractAttributeManager.this.configAttributes);
            AbstractAttributeManager.this.mergedAttributes = attributes;
            List<Attribute> global = new ArrayList<>();
            Map<Key, List<Attribute>> byType = new HashMap<>();
            for (Attribute attribute : attributes.values()) {
                if (attribute.applicableEntityTypes == null) {
                    global.add(attribute);
                } else {
                    for (Key entityType : attribute.applicableEntityTypes) {
                        byType.computeIfAbsent(entityType, k -> new ArrayList<>()).add(attribute);
                    }
                }
            }
            AbstractAttributeManager.this.globalAttributes = List.copyOf(global);
            Map<Key, List<Attribute>> byTypeImmutable = new HashMap<>();
            byType.forEach((type, list) -> {
                List<Attribute> merged = new ArrayList<>(global.size() + list.size());
                merged.addAll(global);
                merged.addAll(list);
                byTypeImmutable.put(type, List.copyOf(merged));
            });
            AbstractAttributeManager.this.attributesByEntityType = Map.copyOf(byTypeImmutable);
            for (Attribute attribute : attributes.values()) {
                DerivedValue derived = attribute.derived();
                if (derived != null) {
                    derived.bind(attributes::get);
                }
            }
        }

        @Override
        protected void parseSection(@NotNull Pack pack, @NotNull Path path, @NotNull Key id, @NotNull ConfigSection section) {
            BaseValueSource baseValueSource = section.<BaseValueSource>getValue("base", BaseValueSources::fromConfig, () -> new ConstantBaseValueSource(0d));
            ConfigSection constraintSection = section.getSection("constraint");
            ValueConstraint constraint = ValueConstraint.noLimit();
            if (constraintSection != null) {
                constraint = ValueConstraint.clamp(
                        constraintSection.getDouble("min", Double.MIN_VALUE),
                        constraintSection.getDouble("max", Double.MAX_VALUE)
                );
            }
            List<SyncTarget> sync = section.getSectionList("sync", v -> new SyncTarget(
                    v.getNonNullKey("target"),
                    v.getNonNullEnum("operation", Operation.class),
                    v.<SyncValueProvider>getValue("value", SyncValueProviders::fromConfig, () -> ValueSyncValueProvider.INSTANCE)
            ));
            Set<Key> applicableEntityTypes = parseApplicableEntityTypes(section);
            List<AttributeOperation> operations = parseOperations(section);
            DerivedValue derived = section.getValue("derived", DerivedValues::fromConfig);
            Attribute attribute = new Attribute(id, baseValueSource, constraint, operations, applicableEntityTypes, sync, derived);
            AbstractAttributeManager.this.configAttributes.put(id, attribute);
        }
    }

    private final class EquipmentSetParser extends IdSectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = ConfigKeys.of("equipment_set(s)");

        @Override
        public Key type() {
            return Key.ce("equipment_set");
        }

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public LoadingStage loadingStage() {
            return LoadingStages.EQUIPMENT_SET;
        }

        @Override
        public List<LoadingStage> dependencies() {
            return List.of(LoadingStages.ATTRIBUTE);
        }

        @Override
        public int count() {
            return AbstractAttributeManager.this.equipmentSets.size();
        }

        @Override
        protected void parseSection(@NotNull Pack pack, @NotNull Path path, @NotNull Key id, @NotNull ConfigSection section) {
            AbstractAttributeManager.this.equipmentSets.put(id, EquipmentSet.fromConfig(section));
        }

    }

    private final class OperationParser extends IdSectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = ConfigKeys.of("attribute_operation(s)");

        @Override
        public Key type() {
            return Key.ce("attribute_operation");
        }

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public LoadingStage loadingStage() {
            return LoadingStages.ATTRIBUTE_OPERATION;
        }

        @Override
        public int count() {
            return AbstractAttributeManager.this.configOperations.size();
        }

        @Override
        public boolean supportSearch() {
            return false;
        }

        @Override
        protected void parseSection(@NotNull Pack pack, @NotNull Path path, @NotNull Key id, @NotNull ConfigSection section) {
            String expression = section.getNonNullString("expression");
            AbstractAttributeManager.this.configOperations.put(
                    id,
                    AttributeOperation.expression(id, section.assemblePath("expression"), expression)
            );
        }
    }
}
