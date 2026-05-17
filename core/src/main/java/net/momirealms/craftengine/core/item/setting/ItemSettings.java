package net.momirealms.craftengine.core.item.setting;

import net.momirealms.craftengine.core.entity.projectile.ProjectileMeta;
import net.momirealms.craftengine.core.item.processor.EquippableProcessor;
import net.momirealms.craftengine.core.item.processor.FoodProcessor;
import net.momirealms.craftengine.core.item.processor.ItemProcessor;
import net.momirealms.craftengine.core.item.recipe.remainder.CraftRemainder;
import net.momirealms.craftengine.core.item.setting.value.*;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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
    Key fuelRemainder = null;
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
    boolean triggerAdvancement = false;

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
        if (VersionHelper.isOrAbove1_20_5 && this.foodData != null) {
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

    public void lateInit() {
        if (VersionHelper.isOrAbove26_1 && this.dyeColor != null) {
            Key dyesTag = Key.of("minecraft:dyes");
            if (this.tags.isEmpty()) {
                this.tags = Set.of(dyesTag);
            } else {
                this.tags.add(dyesTag);
            }
        }
    }

    public static ItemSettings of() {
        return new ItemSettings();
    }

    public static ItemSettings fromConfig(@Nullable ConfigSection section) {
        ItemSettings itemSettings = ItemSettings.of();
        if (section == null) return itemSettings;
        applyModifiers(itemSettings, section);
        return itemSettings;
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
        newSettings.fuelRemainder = settings.fuelRemainder;
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
        newSettings.triggerAdvancement = settings.triggerAdvancement;
        newSettings.customData = new IdentityHashMap<>(settings.customData);
        return newSettings;
    }

    public static void applyModifiers(ItemSettings settings, ConfigSection section) {
        ExceptionCollector<KnownResourceException> collector = new ExceptionCollector<>(KnownResourceException.class);
        if (section != null) {
            for (String type : section.keySet()) {
                ConfigValue value = section.getValue(type);
                String key = StringUtils.normalizeSettingsType(type);
                collector.runCatching(() -> {
                    Optional.ofNullable(BuiltInRegistries.ITEM_SETTINGS_TYPE.getValue(Key.ce(key)))
                            .ifPresent(modifierType ->
                                    modifierType.factory().create(value).apply(settings));
                });
            }
        }
        collector.throwIfPresent();
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

    public boolean triggerAdvancement() {
        return triggerAdvancement;
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

    @Nullable
    public Key fuelRemainder() {
        return this.fuelRemainder;
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

    public ItemSettings triggerAdvancement(boolean triggerAdvancement) {
        this.triggerAdvancement = triggerAdvancement;
        return this;
    }

    public ItemSettings fuelRemainder(Key fuelRemainder) {
        this.fuelRemainder = fuelRemainder;
        return this;
    }
}
