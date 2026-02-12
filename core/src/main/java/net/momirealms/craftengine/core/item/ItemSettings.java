package net.momirealms.craftengine.core.item;

import net.momirealms.craftengine.core.entity.display.Billboard;
import net.momirealms.craftengine.core.entity.display.ItemDisplayContext;
import net.momirealms.craftengine.core.entity.projectile.ProjectileMeta;
import net.momirealms.craftengine.core.item.equipment.ComponentBasedEquipment;
import net.momirealms.craftengine.core.item.equipment.Equipment;
import net.momirealms.craftengine.core.item.equipment.Equipments;
import net.momirealms.craftengine.core.item.processor.EquippableProcessor;
import net.momirealms.craftengine.core.item.processor.FoodProcessor;
import net.momirealms.craftengine.core.item.processor.ItemProcessor;
import net.momirealms.craftengine.core.item.recipe.remainder.CraftRemainder;
import net.momirealms.craftengine.core.item.recipe.remainder.CraftRemainders;
import net.momirealms.craftengine.core.item.setting.*;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.*;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.stream.Collectors;

public final class ItemSettings {
    int fuelTime;
    Set<Key> tags = Set.of();
    Repairable repairable = Repairable.UNDEFINED;
    List<AnvilRepairItem> anvilRepairItems = List.of();
    boolean renameable = true;
    boolean disableVanillaBehavior = true;
    ProjectileMeta projectileMeta;
    Tristate dyeable = Tristate.UNDEFINED;
    Helmet helmet = null;
    FoodData foodData = null;
    Key consumeReplacement = null;
    CraftRemainder craftRemainder = null;
    List<DamageSource> invulnerable = List.of();
    boolean canEnchant = true;
    float compostProbability= 0.5f;
    boolean respectRepairableComponent = false;
    List<Key> ingredientSubstitutes = List.of();
    @Nullable
    ItemEquipment equipment;
    @Nullable
    Color dyeColor;
    @Nullable
    Color fireworkColor;
    float keepOnDeathChance = 0f;
    float destroyOnDeathChance = 0f;
    @Nullable
    String dropDisplay = Config.defaultDropDisplayFormat();
    @Nullable
    LegacyChatFormatter glowColor = null;
    Map<CustomItemSettingType<?>, Object> customData = new IdentityHashMap<>(4);

    private ItemSettings() {}

    @SuppressWarnings("unchecked")
    public List<ItemProcessor> processors() {
        ArrayList<ItemProcessor> processors = new ArrayList<>();
        if (this.equipment != null) {
            EquipmentData data = this.equipment.equipmentData();
            if (data != null) {
                data.setAssetId(null);
                processors.add(new EquippableProcessor(data));
            }
            if (!this.equipment.clientBoundModel().asBoolean(Config.globalClientboundModel())) {
                processors.addAll(this.equipment.equipment().modifiers());
            }
        }
        if (VersionHelper.isOrAbove1_20_5() && this.foodData != null) {
            processors.add(new FoodProcessor(this.foodData.nutrition(), this.foodData.saturation(), false));
        }
        for (Map.Entry<CustomItemSettingType<?>, Object> entry : this.customData.entrySet()) {
            CustomItemSettingType<Object> type = (CustomItemSettingType<Object>) entry.getKey();
            Optional.ofNullable(type.dataProcessor()).ifPresent(it -> {
                it.accept(entry.getValue(), processors::add);
            });
        }
        return processors;
    }

    @SuppressWarnings("unchecked")
    public List<ItemProcessor> clientBoundProcessors() {
        ArrayList<ItemProcessor> processors = new ArrayList<>();
        if (this.equipment != null) {
            if (this.equipment.clientBoundModel().asBoolean(Config.globalClientboundModel())) {
                processors.addAll(this.equipment.equipment().modifiers());
            }
        }
        for (Map.Entry<CustomItemSettingType<?>, Object> entry : this.customData.entrySet()) {
            CustomItemSettingType<Object> type = (CustomItemSettingType<Object>) entry.getKey();
            Optional.ofNullable(type.clientBoundDataProcessor()).ifPresent(it -> {
                it.accept(entry.getValue(), processors::add);
            });
        }
        return processors;
    }

    public static ItemSettings of() {
        return new ItemSettings();
    }

    public static ItemSettings fromMap(Map<String, Object> map) {
        if (map == null) return ItemSettings.of();
        return applyModifiers(ItemSettings.of(), map);
    }

    public static ItemSettings ofFullCopy(ItemSettings settings) {
        ItemSettings newSettings = of();
        newSettings.fuelTime = settings.fuelTime;
        newSettings.tags = settings.tags;
        newSettings.equipment = settings.equipment;
        newSettings.repairable = settings.repairable;
        newSettings.anvilRepairItems = settings.anvilRepairItems;
        newSettings.renameable = settings.renameable;
        newSettings.disableVanillaBehavior = settings.disableVanillaBehavior;
        newSettings.projectileMeta = settings.projectileMeta;
        newSettings.dyeable = settings.dyeable;
        newSettings.helmet = settings.helmet;
        newSettings.foodData = settings.foodData;
        newSettings.consumeReplacement = settings.consumeReplacement;
        newSettings.craftRemainder = settings.craftRemainder;
        newSettings.invulnerable = settings.invulnerable;
        newSettings.canEnchant = settings.canEnchant;
        newSettings.compostProbability = settings.compostProbability;
        newSettings.respectRepairableComponent = settings.respectRepairableComponent;
        newSettings.dyeColor = settings.dyeColor;
        newSettings.fireworkColor = settings.fireworkColor;
        newSettings.ingredientSubstitutes = settings.ingredientSubstitutes;
        newSettings.keepOnDeathChance = settings.keepOnDeathChance;
        newSettings.destroyOnDeathChance = settings.destroyOnDeathChance;
        newSettings.glowColor = settings.glowColor;
        newSettings.dropDisplay = settings.dropDisplay;
        newSettings.customData = new IdentityHashMap<>(settings.customData);
        return newSettings;
    }

    public static ItemSettings applyModifiers(ItemSettings settings, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            ItemSettings.Modifier.Factory factory = ItemSettings.Modifiers.FACTORIES.get(entry.getKey());
            if (factory != null) {
                factory.createModifier(entry.getValue()).apply(settings);
            } else {
                throw new LocalizedResourceConfigException("warning.config.item.settings.unknown", entry.getKey());
            }
        }
        return settings;
    }

    @SuppressWarnings("unchecked")
    public <T> T getCustomData(CustomItemSettingType<T> type) {
        return (T) this.customData.get(type);
    }

    public void clearCustomData() {
        this.customData.clear();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T removeCustomData(CustomItemSettingType<?> type) {
        return (T) this.customData.remove(type);
    }

    public <T> void addCustomData(CustomItemSettingType<T> key, T value) {
        this.customData.put(key, value);
    }

    public ProjectileMeta projectileMeta() {
        return this.projectileMeta;
    }

    public boolean disableVanillaBehavior() {
        return this.disableVanillaBehavior;
    }

    public Repairable repairable() {
        return this.repairable;
    }

    public int fuelTime() {
        return this.fuelTime;
    }

    public boolean renameable() {
        return this.renameable;
    }

    public Set<Key> tags() {
        return this.tags;
    }

    public Tristate dyeable() {
        return this.dyeable;
    }

    public boolean canEnchant() {
        return this.canEnchant;
    }

    public List<AnvilRepairItem> repairItems() {
        return this.anvilRepairItems;
    }

    public boolean respectRepairableComponent() {
        return this.respectRepairableComponent;
    }

    public List<Key> ingredientSubstitutes() {
        return this.ingredientSubstitutes;
    }

    @Nullable
    public FoodData foodData() {
        return this.foodData;
    }

    @Nullable
    public Key consumeReplacement() {
        return this.consumeReplacement;
    }

    @Nullable
    public CraftRemainder craftRemainder() {
        return this.craftRemainder;
    }

    @Nullable
    public Helmet helmet() {
        return this.helmet;
    }

    @Nullable
    public ItemEquipment equipment() {
        return this.equipment;
    }

    @Nullable
    public Color dyeColor() {
        return this.dyeColor;
    }

    @Nullable
    public Color fireworkColor() {
        return this.fireworkColor;
    }

    public List<DamageSource> invulnerable() {
        return this.invulnerable;
    }

    public float compostProbability() {
        return this.compostProbability;
    }

    public float keepOnDeathChance() {
        return this.keepOnDeathChance;
    }

    public float destroyOnDeathChance() {
        return this.destroyOnDeathChance;
    }

    @Nullable
    public LegacyChatFormatter glowColor() {
        return this.glowColor;
    }

    @Nullable
    public String dropDisplay() {
        return this.dropDisplay;
    }

    public ItemSettings fireworkColor(Color color) {
        this.fireworkColor = color;
        return this;
    }

    public ItemSettings ingredientSubstitutes(List<Key> substitutes) {
        this.ingredientSubstitutes = substitutes;
        return this;
    }

    public ItemSettings dyeColor(Color color) {
        this.dyeColor = color;
        return this;
    }

    public ItemSettings repairItems(List<AnvilRepairItem> items) {
        this.anvilRepairItems = items;
        return this;
    }

    public ItemSettings consumeReplacement(Key key) {
        this.consumeReplacement = key;
        return this;
    }

    public ItemSettings craftRemainder(CraftRemainder craftRemainder) {
        this.craftRemainder = craftRemainder;
        return this;
    }

    public ItemSettings compostProbability(float chance) {
        this.compostProbability = chance;
        return this;
    }

    public ItemSettings repairable(Repairable repairable) {
        this.repairable = repairable;
        return this;
    }

    public ItemSettings canEnchant(boolean canEnchant) {
        this.canEnchant = canEnchant;
        return this;
    }

    public ItemSettings renameable(boolean renameable) {
        this.renameable = renameable;
        return this;
    }

    public ItemSettings dropDisplay(String showName) {
        this.dropDisplay = showName;
        return this;
    }

    public ItemSettings projectileMeta(ProjectileMeta projectileMeta) {
        this.projectileMeta = projectileMeta;
        return this;
    }

    public ItemSettings disableVanillaBehavior(boolean disableVanillaBehavior) {
        this.disableVanillaBehavior = disableVanillaBehavior;
        return this;
    }

    public ItemSettings fuelTime(int fuelTime) {
        this.fuelTime = fuelTime;
        return this;
    }

    public ItemSettings tags(Set<Key> tags) {
        this.tags = tags;
        return this;
    }

    public ItemSettings foodData(FoodData foodData) {
        this.foodData = foodData;
        return this;
    }

    public ItemSettings equipment(ItemEquipment equipment) {
        this.equipment = equipment;
        return this;
    }

    public ItemSettings dyeable(Tristate bool) {
        this.dyeable = bool;
        return this;
    }

    public ItemSettings helmet(Helmet helmet) {
        this.helmet = helmet;
        return this;
    }

    public ItemSettings respectRepairableComponent(boolean respectRepairableComponent) {
        this.respectRepairableComponent = respectRepairableComponent;
        return this;
    }

    public ItemSettings invulnerable(List<DamageSource> invulnerable) {
        this.invulnerable = invulnerable;
        return this;
    }

    public ItemSettings keepOnDeathChance(float keepChance) {
        this.keepOnDeathChance = keepChance;
        return this;
    }

    public ItemSettings destroyOnDeathChance(float destroyChance) {
        this.destroyOnDeathChance = destroyChance;
        return this;
    }

    public ItemSettings glowColor(LegacyChatFormatter chatFormatter) {
        this.glowColor = chatFormatter;
        return this;
    }

    @FunctionalInterface
    public interface Modifier {

        void apply(ItemSettings settings);

        @FunctionalInterface
        interface Factory {

            ItemSettings.Modifier createModifier(Object value);
        }
    }

    public static class Modifiers {
        private static final Map<String, ItemSettings.Modifier.Factory> FACTORIES = new HashMap<>();

        static {
            registerFactory("repairable", (value -> {
                if (value instanceof Map<?,?> mapValue) {
                    Map<String, Object> repairableData = ResourceConfigUtils.getAsMap(mapValue, "repairable");
                    Repairable repairable = Repairable.fromMap(repairableData);
                    return settings -> settings.repairable(repairable);
                } else {
                    boolean bool = ResourceConfigUtils.getAsBoolean(value, "repairable");
                    return settings -> settings.repairable(bool ? Repairable.TRUE : Repairable.FALSE);
                }
            }));
            registerFactory("enchantable", (value -> {
                boolean bool = ResourceConfigUtils.getAsBoolean(value, "enchantable");
                return settings -> settings.canEnchant(bool);
            }));
            registerFactory("keep-on-death-chance", (value -> {
                float chance = ResourceConfigUtils.getAsFloat(value, "keep-on-death-chance");
                return settings -> settings.keepOnDeathChance(MiscUtils.clamp(chance, 0, 1));
            }));
            registerFactory("destroy-on-death-chance", (value -> {
                float chance = ResourceConfigUtils.getAsFloat(value, "destroy-on-death-chance");
                return settings -> settings.destroyOnDeathChance(MiscUtils.clamp(chance, 0, 1));
            }));
            registerFactory("renameable", (value -> {
                boolean bool = ResourceConfigUtils.getAsBoolean(value, "renameable");
                return settings -> settings.renameable(bool);
            }));
            registerFactory("drop-display", (value -> {
                if (value instanceof String name) {
                    return settings -> settings.dropDisplay(name);
                } else {
                    boolean bool = ResourceConfigUtils.getAsBoolean(value, "drop-display");
                    return settings -> settings.dropDisplay(bool ? "" : null);
                }
            }));
            registerFactory("glow-color", (value -> {
                LegacyChatFormatter chatFormatter = ResourceConfigUtils.getAsEnum(value, LegacyChatFormatter.class, LegacyChatFormatter.WHITE);
                return settings -> settings.glowColor(chatFormatter);
            }));
            registerFactory("anvil-repair-item", (value -> {
                List<AnvilRepairItem> anvilRepairItemList = ResourceConfigUtils.parseConfigAsList(value, material -> {
                    int amount = ResourceConfigUtils.getAsInt(material.getOrDefault("amount", 0), "amount");
                    double percent = ResourceConfigUtils.getAsDouble(material.getOrDefault("percent", 0), "percent");
                    return new AnvilRepairItem(MiscUtils.getAsStringList(material.get("target")), amount, percent);
                });
                return settings -> settings.repairItems(anvilRepairItemList);
            }));
            registerFactory("fuel-time", (value -> {
                int intValue = ResourceConfigUtils.getAsInt(value, "fuel-time");
                return settings -> settings.fuelTime(intValue);
            }));
            registerFactory("consume-replacement", (value -> settings -> {
                if (value == null) settings.consumeReplacement(null);
                else settings.consumeReplacement(Key.of(value.toString()));
            }));
            registerFactory("craft-remaining-item", (value -> settings -> {
                if (value == null) settings.craftRemainder(null);
                else settings.craftRemainder(CraftRemainders.fromObject(value));
            }));
            registerFactory("craft-remainder", (value -> settings -> {
                if (value == null) settings.craftRemainder(null);
                else settings.craftRemainder(CraftRemainders.fromObject(value));
            }));
            registerFactory("tags", (value -> {
                List<String> tags = MiscUtils.getAsStringList(value);
                return settings -> settings.tags(tags.stream().map(it -> {
                    if (it.charAt(0) == '#') {
                        return Key.of(it.substring(1));
                    } else {
                        return Key.of(it);
                    }
                }).collect(Collectors.toSet()));
            }));
            registerFactory("equippable", (value -> {
                Map<String, Object> args = MiscUtils.castToMap(value, false);
                EquipmentData data = EquipmentData.fromMap(args);
                if (data.assetId() == null) {
                    throw new IllegalArgumentException("Please move 'equippable' option to 'data' section.");
                }
                ComponentBasedEquipment componentBasedEquipment = Equipments.COMPONENT.factory().create(data.assetId(), args);
                ((AbstractItemManager<?>) CraftEngine.instance().itemManager()).addOrMergeEquipment(componentBasedEquipment);
                ItemEquipment itemEquipment = new ItemEquipment(Tristate.FALSE, data, componentBasedEquipment);
                return settings -> settings.equipment(itemEquipment);
            }));
            registerFactory("equipment", (value -> {
                Map<String, Object> args = MiscUtils.castToMap(value, false);
                Tristate clientBoundModel = Tristate.of((Boolean) args.get("client-bound-model"));
                Key assetId = Key.of(ResourceConfigUtils.requireNonEmptyStringOrThrow(args.get("asset-id"), "warning.config.item.settings.equipment.missing_asset_id"));
                Optional<Equipment> optionalEquipment = CraftEngine.instance().itemManager().getEquipment(assetId);
                if (optionalEquipment.isEmpty()) {
                    throw new LocalizedResourceConfigException("warning.config.item.settings.equipment.invalid_asset_id");
                }
                if (VersionHelper.isOrAbove1_21_2() && args.containsKey("slot")) {
                    if (optionalEquipment.get() instanceof ComponentBasedEquipment) {
                        EquipmentData data = EquipmentData.fromMap(args);
                        return settings -> settings.equipment(new ItemEquipment(clientBoundModel, data, optionalEquipment.get()));
                    } else {
                        // trim based
                        Map<String, Object> copiedArgs = new HashMap<>(args);
                        copiedArgs.put("asset-id", Config.sacrificedVanillaArmorType());
                        EquipmentData data = EquipmentData.fromMap(copiedArgs);
                        return settings -> settings.equipment(new ItemEquipment(clientBoundModel, data, optionalEquipment.get()));
                    }
                } else {
                    return settings -> settings.equipment(new ItemEquipment(clientBoundModel, null, optionalEquipment.get()));
                }
            }));
            registerFactory("can-place", (value -> {
                boolean bool = ResourceConfigUtils.getAsBoolean(value, "can-place");
                return settings -> settings.disableVanillaBehavior(!bool);
            }));
            registerFactory("disable-vanilla-behavior", (value -> {
                boolean bool = ResourceConfigUtils.getAsBoolean(value, "disable-vanilla-behavior");
                return settings -> settings.disableVanillaBehavior(bool);
            }));
            registerFactory("projectile", (value -> {
                Map<String, Object> args = MiscUtils.castToMap(value, false);
                Key customTridentItemId = Key.of(ResourceConfigUtils.requireNonEmptyStringOrThrow(args.get("item"), "warning.config.item.settings.projectile.missing_item"));
                ItemDisplayContext displayType = ItemDisplayContext.valueOf(args.getOrDefault("display-transform", "NONE").toString().toUpperCase(Locale.ENGLISH));
                Billboard billboard = Billboard.valueOf(args.getOrDefault("billboard", "FIXED").toString().toUpperCase(Locale.ENGLISH));
                Vector3f translation = ResourceConfigUtils.getAsVector3f(args.getOrDefault("translation", 0), "translation");
                Vector3f scale = ResourceConfigUtils.getAsVector3f(args.getOrDefault("scale", 1), "scale");
                Quaternionf rotation = ResourceConfigUtils.getAsQuaternionf(ResourceConfigUtils.get(args, "rotation"), "rotation");
                double range = ResourceConfigUtils.getAsDouble(args.getOrDefault("range", 1), "range");
                return settings -> settings.projectileMeta(new ProjectileMeta(customTridentItemId, displayType, billboard, scale, translation, rotation, range));
            }));
            registerFactory("helmet", (value -> {
                Map<String, Object> args = MiscUtils.castToMap(value, false);
                return settings -> settings.helmet(new Helmet(SoundData.create(args.getOrDefault("equip-sound", "minecraft:intentionally_empty"), SoundData.SoundValue.FIXED_1, SoundData.SoundValue.FIXED_1)));
            }));
            registerFactory("compost-probability", (value -> {
                float chance = ResourceConfigUtils.getAsFloat(value, "compost-probability");
                return settings -> settings.compostProbability(chance);
            }));
            registerFactory("dyeable", (value -> {
                boolean bool = ResourceConfigUtils.getAsBoolean(value, "dyeable");
                return settings -> settings.dyeable(bool ? Tristate.TRUE : Tristate.FALSE);
            }));
            registerFactory("respect-repairable-component", (value -> {
                boolean bool = ResourceConfigUtils.getAsBoolean(value, "respect-repairable-component");
                return settings -> settings.respectRepairableComponent(bool);
            }));
            registerFactory("dye-color", (value -> {
                if (value instanceof Integer i) {
                    return settings -> settings.dyeColor(Color.fromDecimal(i));
                } else {
                    return settings -> settings.dyeColor(Color.fromVector3f(ResourceConfigUtils.getAsVector3f(value, "dye-color")));
                }
            }));
            registerFactory("firework-color", (value -> {
                if (value instanceof Integer i) {
                    return settings -> settings.fireworkColor(Color.fromDecimal(i));
                } else {
                    return settings -> settings.fireworkColor(Color.fromVector3f(ResourceConfigUtils.getAsVector3f(value, "firework-color")));
                }
            }));
            registerFactory("food", (value -> {
                Map<String, Object> args = MiscUtils.castToMap(value, false);
                FoodData data = new FoodData(
                        ResourceConfigUtils.getAsInt(args.get("nutrition"), "nutrition"),
                        ResourceConfigUtils.getAsFloat(args.get("saturation"), "saturation")
                );
                return settings -> settings.foodData(data);
            }));
            registerFactory("invulnerable", (value -> {
                List<DamageSource> list = MiscUtils.getAsStringList(value).stream().map(it -> {
                    DamageSource source = DamageSource.byName(it);
                    if (source == null) {
                        throw new LocalizedResourceConfigException("warning.config.item.settings.invulnerable.invalid_damage_source", it, EnumUtils.toString(DamageSource.values()));
                    }
                    return source;
                }).toList();
                return settings -> settings.invulnerable(list);
            }));
            registerFactory("ingredient-substitute", (value -> settings -> settings.ingredientSubstitutes(MiscUtils.getAsStringList(value).stream().map(Key::of).toList())));
        }

        public static void registerFactory(String id, ItemSettings.Modifier.Factory factory) {
            FACTORIES.put(id, factory);
        }
    }
}
