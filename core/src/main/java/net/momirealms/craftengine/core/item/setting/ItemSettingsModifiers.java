package net.momirealms.craftengine.core.item.setting;

import net.momirealms.craftengine.core.attribute.equipment.EquipmentSetComponent;
import net.momirealms.craftengine.core.attribute.modifier.SlotAttributeModifierConfig;
import net.momirealms.craftengine.core.entity.projectile.ProjectileMeta;
import net.momirealms.craftengine.core.item.AbstractItemManager;
import net.momirealms.craftengine.core.item.equipment.ComponentBasedEquipment;
import net.momirealms.craftengine.core.item.equipment.Equipment;
import net.momirealms.craftengine.core.item.equipment.Equipments;
import net.momirealms.craftengine.core.item.equipment.SlotPotionEffect;
import net.momirealms.craftengine.core.item.recipe.remainder.CraftRemainder;
import net.momirealms.craftengine.core.item.recipe.remainder.CraftRemainders;
import net.momirealms.craftengine.core.item.setting.value.*;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.*;
import net.momirealms.craftengine.core.plugin.context.number.NumberProviders;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ItemSettingsModifiers {
    public static final ItemSettingsModifierType<ItemSettingsModifier> REPAIRABLE = register(Key.ce("repairable"), value -> {
        if (value.is(Map.class)) {
            Repairable repairable = Repairable.fromConfig(value.getAsSection());
            return settings -> settings.repairable(repairable);
        } else {
            return settings -> settings.repairable(value.getAsBoolean() ? Repairable.TRUE : Repairable.FALSE);
        }
    });
    public static final ItemSettingsModifierType<ItemSettingsModifier> ENCHANTABLE = register(Key.ce("enchantable"), (value -> {
        boolean canEnchant = value.getAsBoolean();
        return settings -> settings.canEnchant(canEnchant);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> KEEP_ON_DEATH_CHANCE = register(Key.ce("keep_on_death_chance"), (value -> {
        float chance = MiscUtils.clamp(value.getAsFloat(), 0, 1);
        return settings -> settings.keepOnDeathChance(chance);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> DESTROY_ON_DEATH_CHANCE = register(Key.ce("destroy_on_death_chance"), (value -> {
        float chance = MiscUtils.clamp(value.getAsFloat(), 0, 1);
        return settings -> settings.destroyOnDeathChance(chance);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> RENAMEABLE = register(Key.ce("renameable"), (value -> {
        boolean renameable = value.getAsBoolean();
        return settings -> settings.renameable(renameable);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> PREVENT_BREAK = register(Key.ce("prevent_break"), (value -> {
        boolean preventBreak = value.getAsBoolean();
        return settings -> settings.preventBreak(preventBreak);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> DROP_DISPLAY = register(Key.ce("drop_display"), (value -> {
        if (value.is(String.class)) {
            return settings -> settings.dropDisplay(value.getAsString());
        } else {
            return settings -> settings.dropDisplay(value.getAsBoolean() ? "" : null);
        }
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> GLOW_COLOR = register(Key.ce("glow_color"), (value -> {
        LegacyChatFormatter formatter = value.getAsEnum(LegacyChatFormatter.class);
        return settings -> settings.glowColor(formatter);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> ANVIL_REPAIR_ITEM = register(Key.ce("anvil_repair_item"), (value -> {
        List<AnvilRepairItem> anvilRepairItemList = value.getAsList(it -> {
            ConfigSection section = it.getAsSection();
            return new AnvilRepairItem(
                    section.getStringList("target"),
                    section.getValue("amount", NumberProviders::fromConfig, NumberProviders.direct(0)),
                    section.getValue("percent", NumberProviders::fromConfig, NumberProviders.direct(0))
            );
        });
        return settings -> settings.repairItems(anvilRepairItemList);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> DRAG_REPAIR_ITEM = register(Key.ce("drag_repair_item"), (value -> {
        List<DragRepairItem> dragRepairItemList = value.getAsList(it -> {
            ConfigSection section = it.getAsSection();
            return new DragRepairItem(
                    section.getStringList("target"),
                    section.getValue("amount", NumberProviders::fromConfig, NumberProviders.direct(0)),
                    section.getValue("percent", NumberProviders::fromConfig, NumberProviders.direct(0)),
                    section.getValue("sound", v -> SoundData.fromConfig(v, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.FIXED_1), (SoundData) null)
            );
        });
        return settings -> settings.dragRepairItems(dragRepairItemList);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> FUEL_TIME = register(Key.ce("fuel_time"), (value -> {
        int fuelTime = value.getAsInt();
        return settings -> settings.fuelTime(fuelTime);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> BREAK_POWER = register(Key.ce("break_power"), (value -> {
        int breakPower = value.getAsInt();
        return settings -> settings.breakPower(breakPower);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> CONSUME_REPLACEMENT = register(Key.ce("consume_replacement"), (value -> {
        Key itemId = value.getAsIdentifier();
        return settings -> settings.consumeReplacement(itemId);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> CRAFT_REMAINING_ITEM = register(Key.ce("craft_remaining_item"), (value -> {
        CraftRemainder craftRemainder = CraftRemainders.fromConfig(value);
        return settings -> settings.craftRemainder(craftRemainder);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> CRAFT_REMAINDER = register(Key.ce("craft_remainder"), (value -> {
        CraftRemainder craftRemainder = CraftRemainders.fromConfig(value);
        return settings -> settings.craftRemainder(craftRemainder);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> TAGS = register(Key.ce("tags"), (value -> {
        Set<Key> tags = Set.copyOf(value.getAsList(it -> {
            String asString = it.getAsString();
            if (asString.charAt(0) == '#') {
                return Key.of(asString.substring(1));
            } else {
                return Key.of(asString);
            }
        }));
        return settings -> settings.tags(tags);
    }));
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
        Tristate clientBoundModel = section.getValue(ConfigKeys.of("client_bound_model"), it -> Tristate.of(it.getAsBoolean()), Tristate.UNDEFINED);
        Key assetId = section.getNonNullIdentifier(ConfigKeys.of("asset_id"));
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
                copiedSection.put("asset-id", Config.sacrificedVanillaArmorType());
                EquipmentData data = EquipmentData.fromConfig(copiedSection);
                return settings -> settings.equipment(new ItemEquipment(clientBoundModel, data, optionalEquipment.get()));
            }
        } else {
            return settings -> settings.equipment(new ItemEquipment(clientBoundModel, null, optionalEquipment.get()));
        }
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> EQUIPMENT_LOD = register(Key.ce("equipment_lod"), (value -> {
        EquipmentFallback fallback = EquipmentFallback.fromConfig(value.getAsSection());
        return settings -> settings.equipmentFallback(fallback);
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
    public static final ItemSettingsModifierType<ItemSettingsModifier> DYE_COLOR = register(Key.ce("dye_color"), (value -> {
        Color color = value.getAsColor();
        return settings -> settings.dyeColor(color);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> FIREWORK_COLOR = register(Key.ce("firework_color"), (value -> {
        Color color = value.getAsColor();
        return settings -> settings.fireworkColor(color);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> FOOD = register(Key.ce("food"), (value -> {
        FoodData foodData = FoodData.fromConfig(value.getAsSection());
        return settings -> settings.foodData(foodData);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> INVULNERABLE = register(Key.ce("invulnerable"), (value -> {
        List<DamageSource> list = value.getAsList(it -> it.getAsEnum(DamageSource.class));
        return settings -> settings.invulnerable(list);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> INGREDIENT_SUBSTITUTE = register(Key.ce("ingredient_substitute"), (value -> {
        List<Key> list = value.getAsList(ConfigValue::getAsIdentifier);
        return settings -> settings.ingredientSubstitutes(list);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> FUEL_REMAINDER = register(Key.ce("fuel_remainder"), (value -> {
        Key itemId = value.getAsIdentifier();
        return settings -> settings.fuelRemainder(itemId);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> ALLOWED_PROJECTILES = register(Key.ce("allowed_projectiles"), (value -> {
        List<Key> list = value.getAsList(ConfigValue::getAsIdentifier);
        return settings -> settings.allowedProjectiles(Set.copyOf(list));
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> EQUIPMENT_SET_PART = register(Key.ce("equipment_set_part"), (value -> {
        EquipmentSetPart equipmentSetPart = new EquipmentSetPart(value.getAsList(v -> EquipmentSetComponent.fromConfig(v.getAsSection())));
        return settings -> settings.equipmentSetPart(equipmentSetPart);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> ATTRIBUTE_MODIFIERS = register(Key.ce("attribute_modifiers"), (value -> {
        AttributeModifiers attributeModifiers = new AttributeModifiers(value.getAsList(v -> SlotAttributeModifierConfig.fromConfig(v.getAsSection())));
        return settings -> settings.attributeModifiers(attributeModifiers);
    }));
    public static final ItemSettingsModifierType<ItemSettingsModifier> EQUIPMENT_POTION_EFFECTS = register(Key.ce("equipment_potion_effects"), (value -> {
        EquipmentPotionEffects potionEffects = new EquipmentPotionEffects(value.getAsList(v -> SlotPotionEffect.fromConfig(v.getAsSection())));
        return settings -> settings.equipmentPotionEffects(potionEffects);
    }));

    private ItemSettingsModifiers() {
    }

    public static void init() {
    }

    public static <M extends ItemSettingsModifier> ItemSettingsModifierType<M> register(Key id, ItemSettingsModifierFactory<M> factory) {
        ItemSettingsModifierType<M> type = new ItemSettingsModifierType<>(id, factory);
        ((WritableRegistry<ItemSettingsModifierType<? extends ItemSettingsModifier>>) BuiltInRegistries.ITEM_SETTINGS_TYPE)
                .register(ResourceKey.create(Registries.ITEM_SETTINGS_TYPE.location(), id), type);
        return type;
    }
}
