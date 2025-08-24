package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.attribute.AttributeModifier;
import net.momirealms.craftengine.core.attribute.Attributes;
import net.momirealms.craftengine.core.attribute.Attributes1_21;
import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemDataModifierFactory;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProviders;
import net.momirealms.craftengine.core.util.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AttributeModifiersModifier<I> implements SimpleNetworkItemDataModifier<I> {
    public static final Factory<?> FACTORY = new Factory<>();
    public static final Map<Key, Key> CONVERTOR = new HashMap<>();

    static {
        if (VersionHelper.isOrAbove1_21_2()) {
            CONVERTOR.put(Attributes1_21.BURNING_TIME, Attributes.BURNING_TIME);
            CONVERTOR.put(Attributes1_21.ARMOR, Attributes.ARMOR);
            CONVERTOR.put(Attributes1_21.ARMOR_TOUGHNESS, Attributes.ARMOR_TOUGHNESS);
            CONVERTOR.put(Attributes1_21.ATTACK_KNOCKBACK, Attributes.ATTACK_KNOCKBACK);
            CONVERTOR.put(Attributes1_21.ATTACK_DAMAGE, Attributes.ATTACK_DAMAGE);
            CONVERTOR.put(Attributes1_21.ATTACK_SPEED, Attributes.ATTACK_SPEED);
            CONVERTOR.put(Attributes1_21.FLYING_SPEED, Attributes.FLYING_SPEED);
            CONVERTOR.put(Attributes1_21.FOLLOW_RANGE, Attributes.FOLLOW_RANGE);
            CONVERTOR.put(Attributes1_21.KNOCKBACK_RESISTANCE, Attributes.KNOCKBACK_RESISTANCE);
            CONVERTOR.put(Attributes1_21.LUCK, Attributes.LUCK);
            CONVERTOR.put(Attributes1_21.MAX_ABSORPTION, Attributes.MAX_ABSORPTION);
            CONVERTOR.put(Attributes1_21.MAX_HEALTH, Attributes.MAX_HEALTH);
            CONVERTOR.put(Attributes1_21.MOVEMENT_EFFICIENCY, Attributes.MOVEMENT_EFFICIENCY);
            CONVERTOR.put(Attributes1_21.SCALE, Attributes.SCALE);
            CONVERTOR.put(Attributes1_21.STEP_HEIGHT, Attributes.STEP_HEIGHT);
            CONVERTOR.put(Attributes1_21.JUMP_STRENGTH, Attributes.JUMP_STRENGTH);
            CONVERTOR.put(Attributes1_21.ENTITY_INTERACTION_RANGE, Attributes.ENTITY_INTERACTION_RANGE);
            CONVERTOR.put(Attributes1_21.BLOCK_INTERACTION_RANGE, Attributes.BLOCK_INTERACTION_RANGE);
            CONVERTOR.put(Attributes1_21.SPAWN_REINFORCEMENT, Attributes.SPAWN_REINFORCEMENT);
            CONVERTOR.put(Attributes1_21.BLOCK_BREAK_SPEED, Attributes.BLOCK_BREAK_SPEED);
            CONVERTOR.put(Attributes1_21.GRAVITY, Attributes.GRAVITY);
            CONVERTOR.put(Attributes1_21.SAFE_FALL_DISTANCE, Attributes.SAFE_FALL_DISTANCE);
            CONVERTOR.put(Attributes1_21.FALL_DAMAGE_MULTIPLIER, Attributes.FALL_DAMAGE_MULTIPLIER);
            CONVERTOR.put(Attributes1_21.EXPLOSION_KNOCKBACK_RESISTANCE, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE);
            CONVERTOR.put(Attributes1_21.MINING_EFFICIENCY, Attributes.MINING_EFFICIENCY);
            CONVERTOR.put(Attributes1_21.OXYGEN_BONUS, Attributes.OXYGEN_BONUS);
            CONVERTOR.put(Attributes1_21.SNEAKING_SPEED, Attributes.SNEAKING_SPEED);
            CONVERTOR.put(Attributes1_21.SUBMERGED_MINING_SPEED, Attributes.SUBMERGED_MINING_SPEED);
            CONVERTOR.put(Attributes1_21.SWEEPING_DAMAGE_RATIO, Attributes.SWEEPING_DAMAGE_RATIO);
            CONVERTOR.put(Attributes1_21.WATER_MOVEMENT_EFFICIENCY, Attributes.WATER_MOVEMENT_EFFICIENCY);
        } else {
            CONVERTOR.put(Attributes.BURNING_TIME, Attributes1_21.BURNING_TIME);
            CONVERTOR.put(Attributes.ARMOR, Attributes1_21.ARMOR);
            CONVERTOR.put(Attributes.ARMOR_TOUGHNESS, Attributes1_21.ARMOR_TOUGHNESS);
            CONVERTOR.put(Attributes.ATTACK_KNOCKBACK, Attributes1_21.ATTACK_KNOCKBACK);
            CONVERTOR.put(Attributes.ATTACK_DAMAGE, Attributes1_21.ATTACK_DAMAGE);
            CONVERTOR.put(Attributes.ATTACK_SPEED, Attributes1_21.ATTACK_SPEED);
            CONVERTOR.put(Attributes.FLYING_SPEED, Attributes1_21.FLYING_SPEED);
            CONVERTOR.put(Attributes.FOLLOW_RANGE, Attributes1_21.FOLLOW_RANGE);
            CONVERTOR.put(Attributes.KNOCKBACK_RESISTANCE, Attributes1_21.KNOCKBACK_RESISTANCE);
            CONVERTOR.put(Attributes.LUCK, Attributes1_21.LUCK);
            CONVERTOR.put(Attributes.MAX_ABSORPTION, Attributes1_21.MAX_ABSORPTION);
            CONVERTOR.put(Attributes.MAX_HEALTH, Attributes1_21.MAX_HEALTH);
            CONVERTOR.put(Attributes.MOVEMENT_EFFICIENCY, Attributes1_21.MOVEMENT_EFFICIENCY);
            CONVERTOR.put(Attributes.SCALE, Attributes1_21.SCALE);
            CONVERTOR.put(Attributes.STEP_HEIGHT, Attributes1_21.STEP_HEIGHT);
            CONVERTOR.put(Attributes.JUMP_STRENGTH, Attributes1_21.JUMP_STRENGTH);
            CONVERTOR.put(Attributes.ENTITY_INTERACTION_RANGE, Attributes1_21.ENTITY_INTERACTION_RANGE);
            CONVERTOR.put(Attributes.BLOCK_INTERACTION_RANGE, Attributes1_21.BLOCK_INTERACTION_RANGE);
            CONVERTOR.put(Attributes.SPAWN_REINFORCEMENT, Attributes1_21.SPAWN_REINFORCEMENT);
            CONVERTOR.put(Attributes.BLOCK_BREAK_SPEED, Attributes1_21.BLOCK_BREAK_SPEED);
            CONVERTOR.put(Attributes.GRAVITY, Attributes1_21.GRAVITY);
            CONVERTOR.put(Attributes.SAFE_FALL_DISTANCE, Attributes1_21.SAFE_FALL_DISTANCE);
            CONVERTOR.put(Attributes.FALL_DAMAGE_MULTIPLIER, Attributes1_21.FALL_DAMAGE_MULTIPLIER);
            CONVERTOR.put(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, Attributes1_21.EXPLOSION_KNOCKBACK_RESISTANCE);
            CONVERTOR.put(Attributes.MINING_EFFICIENCY, Attributes1_21.MINING_EFFICIENCY);
            CONVERTOR.put(Attributes.OXYGEN_BONUS, Attributes1_21.OXYGEN_BONUS);
            CONVERTOR.put(Attributes.SNEAKING_SPEED, Attributes1_21.SNEAKING_SPEED);
            CONVERTOR.put(Attributes.SUBMERGED_MINING_SPEED, Attributes1_21.SUBMERGED_MINING_SPEED);
            CONVERTOR.put(Attributes.SWEEPING_DAMAGE_RATIO, Attributes1_21.SWEEPING_DAMAGE_RATIO);
            CONVERTOR.put(Attributes.WATER_MOVEMENT_EFFICIENCY, Attributes1_21.WATER_MOVEMENT_EFFICIENCY);
        }
    }

    public static Key getNativeAttributeName(final Key attributeName) {
        return CONVERTOR.getOrDefault(attributeName, attributeName);
    }

    private final List<PreModifier> modifiers;

    public AttributeModifiersModifier(List<PreModifier> modifiers) {
        this.modifiers = modifiers;
    }

    public List<PreModifier> modifiers() {
        return this.modifiers;
    }

    @Override
    public Key type() {
        return ItemDataModifiers.ATTRIBUTE_MODIFIERS;
    }

    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        return item.attributeModifiers(this.modifiers.stream().map(it -> it.toAttributeModifier(context)).toList());
    }

    @Override
    public @Nullable Key componentType(Item<I> item, ItemBuildContext context) {
        return ComponentKeys.ATTRIBUTE_MODIFIERS;
    }

    @Override
    public @Nullable Object[] nbtPath(Item<I> item, ItemBuildContext context) {
        return new Object[]{"AttributeModifiers"};
    }

    @Override
    public String nbtPathString(Item<I> item, ItemBuildContext context) {
        return "AttributeModifiers";
    }

    public record PreModifier(String type,
                              AttributeModifier.Slot slot,
                              Key id,
                              NumberProvider amount,
                              AttributeModifier.Operation operation,
                              AttributeModifiersModifier.PreModifier.@Nullable PreDisplay display) {

        public PreModifier(String type, AttributeModifier.Slot slot, Key id, NumberProvider amount, AttributeModifier.Operation operation, @Nullable PreDisplay display) {
            this.amount = amount;
            this.type = type;
            this.slot = slot;
            this.id = id;
            this.operation = operation;
            this.display = display;
        }

        public AttributeModifier toAttributeModifier(ItemBuildContext context) {
            return new AttributeModifier(type, slot, id, amount.getDouble(context), operation, display == null ? null : display.toDisplay(context));
        }

        public record PreDisplay(AttributeModifier.Display.Type type, String value) {

            public AttributeModifier.Display toDisplay(ItemBuildContext context) {
                return new AttributeModifier.Display(type, AdventureHelper.miniMessage().deserialize(value, context.tagResolvers()));
            }
        }
    }

    public static class Factory<I> implements ItemDataModifierFactory<I> {

        @Override
        public ItemDataModifier<I> create(Object arg) {
            MutableInt mutableInt = new MutableInt(0);
            List<PreModifier> attributeModifiers = ResourceConfigUtils.parseConfigAsList(arg, (map) -> {
                String type = ResourceConfigUtils.requireNonEmptyStringOrThrow(map.get("type"), "warning.config.item.data.attribute_modifiers.missing_type");
                Key nativeType = AttributeModifiersModifier.getNativeAttributeName(Key.of(type));
                AttributeModifier.Slot slot = AttributeModifier.Slot.valueOf(map.getOrDefault("slot", "any").toString().toUpperCase(Locale.ENGLISH));
                Key id = Optional.ofNullable(map.get("id")).map(String::valueOf).map(Key::of).orElseGet(() -> {
                    mutableInt.add(1);
                    return Key.of("craftengine", "modifier_" + mutableInt.intValue());
                });
                NumberProvider amount = NumberProviders.fromObject(ResourceConfigUtils.requireNonNullOrThrow(map.get("amount"), "warning.config.item.data.attribute_modifiers.missing_amount"));
                AttributeModifier.Operation operation = AttributeModifier.Operation.valueOf(
                        ResourceConfigUtils.requireNonEmptyStringOrThrow(map.get("operation"), "warning.config.item.data.attribute_modifiers.missing_operation").toUpperCase(Locale.ENGLISH)
                );
                PreModifier.PreDisplay display = null;
                if (VersionHelper.isOrAbove1_21_6() && map.containsKey("display")) {
                    Map<String, Object> displayMap = MiscUtils.castToMap(map.get("display"), false);
                    AttributeModifier.Display.Type displayType = AttributeModifier.Display.Type.valueOf(ResourceConfigUtils.requireNonEmptyStringOrThrow(displayMap.get("type"), "warning.config.item.data.attribute_modifiers.display.missing_type").toUpperCase(Locale.ENGLISH));
                    if (displayType == AttributeModifier.Display.Type.OVERRIDE) {
                        String miniMessageValue = ResourceConfigUtils.requireNonEmptyStringOrThrow(displayMap.get("value"), "warning.config.item.data.attribute_modifiers.display.missing_value");
                        display = new PreModifier.PreDisplay(displayType, miniMessageValue);
                    } else {
                        display = new PreModifier.PreDisplay(displayType, null);
                    }
                }
                return new PreModifier(nativeType.value(), slot, id,
                        amount, operation, display);
            });
            return new AttributeModifiersModifier<>(attributeModifiers);
        }
    }
}
