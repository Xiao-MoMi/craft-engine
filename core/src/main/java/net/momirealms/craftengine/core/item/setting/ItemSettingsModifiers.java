package net.momirealms.craftengine.core.item.setting;

import net.momirealms.craftengine.core.entity.projectile.ProjectileMeta;
import net.momirealms.craftengine.core.item.AbstractItemManager;
import net.momirealms.craftengine.core.item.equipment.ComponentBasedEquipment;
import net.momirealms.craftengine.core.item.equipment.Equipment;
import net.momirealms.craftengine.core.item.equipment.Equipments;
import net.momirealms.craftengine.core.item.recipe.remainder.CraftRemainders;
import net.momirealms.craftengine.core.item.recipe.remainder.EmptyCraftRemainder;
import net.momirealms.craftengine.core.item.setting.value.*;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ItemSettingsModifiers {
    public static final ItemSettingsModifierType<ItemSettingsModifier> REPAIRABLE = register(Key.ce("repairable"), value -> {
        if (value.is(Map.class)) {
            Repairable repairable = Repairable.fromConfig(value.getAsSection());
            return settings -> settings.repairable(repairable);
        } else {
            return settings -> settings.repairable(value.getAsBoolean() ? Repairable.TRUE : Repairable.FALSE);
        }
    });
    public static final ItemSettingsModifierType<ItemSettingsModifier> ENCHANTABLE = register(Key.ce("enchantable"), (value -> settings -> {
        boolean canEnchant = value.getAsBoolean();
        settings.canEnchant(canEnchant);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> KEEP_ON_DEATH_CHANCE = register(Key.ce("keep_on_death_chance"), (value -> settings -> {
        float chance = MiscUtils.clamp(value.getAsFloat(), 0, 1);
        settings.keepOnDeathChance(chance);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> DESTROY_ON_DEATH_CHANCE = register(Key.ce("destroy_on_death_chance"), (value -> settings -> {
        float chance = MiscUtils.clamp(value.getAsFloat(), 0, 1);
        settings.destroyOnDeathChance(chance);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> RENAMEABLE = register(Key.ce("renameable"), (value -> settings -> {
        boolean renameable = value.getAsBoolean();
        settings.renameable(renameable);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> DROP_DISPLAY = register(Key.ce("drop_display"), (value -> {
        if (value.is(String.class)) {
            return settings -> settings.dropDisplay(value.getAsString());
        } else {
            return settings -> settings.dropDisplay(value.getAsBoolean() ? "" : null);
        }
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> GLOW_COLOR = register(Key.ce("glow_color"), (value -> settings -> {
        LegacyChatFormatter formatter = value.getAsEnum(LegacyChatFormatter.class);
        settings.glowColor(formatter);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> ANVIL_REPAIR_ITEM = register(Key.ce("anvil_repair_item"), (value -> {
        List<AnvilRepairItem> anvilRepairItemList = value.getAsList(it -> {
            ConfigSection section = it.getAsSection();
            return new AnvilRepairItem(
                    section.getStringList("target"),
                    section.getInt("amount"),
                    section.getDouble("percent")
            );
        });
        return settings -> settings.repairItems(anvilRepairItemList);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> FUEL_TIME = register(Key.ce("fuel_time"), (value -> settings -> {
        int fuelTime = value.getAsInt();
        settings.fuelTime(fuelTime);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> CONSUME_REPLACEMENT = register(Key.ce("consume_replacement"), (value -> settings -> {
        Key itemId = value.getAsIdentifier();
        settings.consumeReplacement(itemId);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> CRAFT_REMAINING_ITEM = register(Key.ce("craft_remaining_item"), (value -> settings -> {
        settings.craftRemainder(CraftRemainders.fromConfig(value));
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> CRAFT_REMAINDER = register(Key.ce("craft_remainder"), (value -> settings -> {
        if (value == null) {
            settings.craftRemainder(EmptyCraftRemainder.INSTANCE);
        } else {
            settings.craftRemainder(CraftRemainders.fromConfig(value));
        }
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> TAGS = register(Key.ce("tags"), (value -> settings -> settings.tags(new HashSet<>(value.getAsList(it -> {
        String asString = it.getAsString();
        if (asString.charAt(0) == '#') {
            return Key.of(asString.substring(1));
        } else {
            return Key.of(asString);
        }
    })))));
    public static final ItemSettingsModifierType<ItemSettingsModifier> EQUIPPABLE = register(Key.ce("equippable"), (value -> {
        ConfigSection section = value.getAsSection();
        EquipmentData data = EquipmentData.fromConfig(section);
        if (data.assetId() == null) {
            throw new KnownResourceException("resource.item.settings.equippable", value.path());
        }
        // 旧版本兼容写法
        ComponentBasedEquipment componentBasedEquipment = Equipments.COMPONENT.factory().create(data.assetId(), value.getAsSection());
        ((AbstractItemManager) CraftEngine.instance().itemManager()).addOrMergeEquipment(componentBasedEquipment);
        ItemEquipment itemEquipment = new ItemEquipment(Tristate.FALSE, data, componentBasedEquipment);
        return settings -> settings.equipment(itemEquipment);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> EQUIPMENT = register(Key.ce("equipment"), (value -> {
        ConfigSection section = value.getAsSection();
        Tristate clientBoundModel = section.getValue(new String[] {"client_bound_model", "client-bound-model"}, it -> Tristate.of(it.getAsBoolean()), Tristate.UNDEFINED);
        Key assetId = section.getNonNullIdentifier(new String[] {"asset_id", "asset-id"});
        Optional<Equipment> optionalEquipment = CraftEngine.instance().itemManager().getEquipment(assetId);
        if (optionalEquipment.isEmpty()) {
            throw new KnownResourceException("resource.item.settings.equipment.invalid_asset_id", value.assemblePath("asset_id"), assetId.asString());
        }
        if (VersionHelper.isOrAbove1_21_2 && section.containsKey("slot")) {
            if (optionalEquipment.get() instanceof ComponentBasedEquipment) {
                // 基于组件
                EquipmentData data = EquipmentData.fromConfig(section);
                return settings -> settings.equipment(new ItemEquipment(clientBoundModel, data, optionalEquipment.get()));
            } else {
                // 基于盔甲纹饰
                ConfigSection copiedSection = section.copy();
                copiedSection.put("asset_id", Config.sacrificedVanillaArmorType());
                EquipmentData data = EquipmentData.fromConfig(copiedSection);
                return settings -> settings.equipment(new ItemEquipment(clientBoundModel, data, optionalEquipment.get()));
            }
        } else {
            return settings -> settings.equipment(new ItemEquipment(clientBoundModel, null, optionalEquipment.get()));
        }
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> CAN_PLACE = register(Key.ce("can_place"), (value -> {
        boolean bool = value.getAsBoolean();
        return settings -> settings.disableVanillaBehavior(!bool);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> TRIGGER_ADVANCEMENT = register(Key.ce("trigger_advancement"), (value -> {
        boolean bool = value.getAsBoolean();
        return settings -> settings.triggerAdvancement(bool);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> DISABLE_VANILLA_BEHAVIOR = register(Key.ce("disable_vanilla_behavior"), (value -> {
        boolean bool = value.getAsBoolean();
        return settings -> settings.disableVanillaBehavior(bool);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> PROJECTILE = register(Key.ce("projectile"), (value -> settings -> {
        settings.projectileMeta(ProjectileMeta.fromConfig(value.getAsSection()));
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> COMPOST_PROBABILITY = register(Key.ce("compost_probability"), (value -> {
        float chance = value.getAsFloat();
        return settings -> settings.compostProbability(chance);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> DYEABLE = register(Key.ce("dyeable"), (value -> {
        boolean bool = value.getAsBoolean();
        return settings -> settings.dyeable(Tristate.of(bool));
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> RESPECT_REPAIRABLE_COMPONENT = register(Key.ce("respect_repairable_component"), (value -> {
        boolean bool = value.getAsBoolean();
        return settings -> settings.respectRepairableComponent(bool);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> DYE_COLOR = register(Key.ce("dye_color"), (value -> settings -> {
        Color color = value.getAsColor();
        settings.dyeColor(color);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> FIREWORK_COLOR = register(Key.ce("firework_color"), (value -> settings -> {
        Color color = value.getAsColor();
        settings.fireworkColor(color);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> FOOD = register(Key.ce("food"), (value -> {
        return settings -> settings.foodData(FoodData.fromConfig(value.getAsSection()));
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> INVULNERABLE = register(Key.ce("invulnerable"), (value -> {
        List<DamageSource> list = value.getAsList(it -> it.getAsEnum(DamageSource.class));
        return settings -> settings.invulnerable(list);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> INGREDIENT_SUBSTITUTE = register(Key.ce("ingredient_substitute"), (value -> settings -> {
        List<Key> list = value.getAsList(ConfigValue::getAsIdentifier);
        settings.ingredientSubstitutes(list);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> FUEL_REMAINDER = register(Key.ce("fuel_remainder"), (value -> settings -> {
        Key itemId = value.getAsIdentifier();
        settings.fuelRemainder(itemId);
    }));

    private ItemSettingsModifiers() {}

    public static void init() {}

    public static <M extends ItemSettingsModifier> ItemSettingsModifierType<M> register(Key id, ItemSettingsModifierFactory<M> factory) {
        ItemSettingsModifierType<M> type = new ItemSettingsModifierType<>(id, factory);
        ((WritableRegistry<ItemSettingsModifierType<? extends ItemSettingsModifier>>) BuiltInRegistries.ITEM_SETTINGS_TYPE)
                .register(ResourceKey.create(Registries.ITEM_SETTINGS_TYPE.location(), id), type);
        return type;
    }
}
