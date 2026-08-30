package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.attribute.vanilla.LegacyVanillaAttributes;
import net.momirealms.craftengine.core.attribute.vanilla.VanillaAttributeModifier;
import net.momirealms.craftengine.core.attribute.vanilla.VanillaAttributes;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class AttributeModifiersProcessor implements SimpleNetworkItemProcessor {
    public static final ItemProcessorFactory<AttributeModifiersProcessor> FACTORY = new Factory();
    public static final Map<Key, Key> CONVERTOR = new HashMap<>();
    private static final Object[] NBT_PATH = new Object[]{"AttributeModifiers"};

    static {
        if (VersionHelper.isOrAbove1_21_2) {
            CONVERTOR.put(LegacyVanillaAttributes.BURNING_TIME, VanillaAttributes.BURNING_TIME);
            CONVERTOR.put(LegacyVanillaAttributes.ARMOR, VanillaAttributes.ARMOR);
            CONVERTOR.put(LegacyVanillaAttributes.ARMOR_TOUGHNESS, VanillaAttributes.ARMOR_TOUGHNESS);
            CONVERTOR.put(LegacyVanillaAttributes.ATTACK_KNOCKBACK, VanillaAttributes.ATTACK_KNOCKBACK);
            CONVERTOR.put(LegacyVanillaAttributes.ATTACK_DAMAGE, VanillaAttributes.ATTACK_DAMAGE);
            CONVERTOR.put(LegacyVanillaAttributes.ATTACK_SPEED, VanillaAttributes.ATTACK_SPEED);
            CONVERTOR.put(LegacyVanillaAttributes.FLYING_SPEED, VanillaAttributes.FLYING_SPEED);
            CONVERTOR.put(LegacyVanillaAttributes.FOLLOW_RANGE, VanillaAttributes.FOLLOW_RANGE);
            CONVERTOR.put(LegacyVanillaAttributes.KNOCKBACK_RESISTANCE, VanillaAttributes.KNOCKBACK_RESISTANCE);
            CONVERTOR.put(LegacyVanillaAttributes.LUCK, VanillaAttributes.LUCK);
            CONVERTOR.put(LegacyVanillaAttributes.MAX_ABSORPTION, VanillaAttributes.MAX_ABSORPTION);
            CONVERTOR.put(LegacyVanillaAttributes.MAX_HEALTH, VanillaAttributes.MAX_HEALTH);
            CONVERTOR.put(LegacyVanillaAttributes.MOVEMENT_EFFICIENCY, VanillaAttributes.MOVEMENT_EFFICIENCY);
            CONVERTOR.put(LegacyVanillaAttributes.SCALE, VanillaAttributes.SCALE);
            CONVERTOR.put(LegacyVanillaAttributes.STEP_HEIGHT, VanillaAttributes.STEP_HEIGHT);
            CONVERTOR.put(LegacyVanillaAttributes.JUMP_STRENGTH, VanillaAttributes.JUMP_STRENGTH);
            CONVERTOR.put(LegacyVanillaAttributes.ENTITY_INTERACTION_RANGE, VanillaAttributes.ENTITY_INTERACTION_RANGE);
            CONVERTOR.put(LegacyVanillaAttributes.BLOCK_INTERACTION_RANGE, VanillaAttributes.BLOCK_INTERACTION_RANGE);
            CONVERTOR.put(LegacyVanillaAttributes.SPAWN_REINFORCEMENT, VanillaAttributes.SPAWN_REINFORCEMENT);
            CONVERTOR.put(LegacyVanillaAttributes.BLOCK_BREAK_SPEED, VanillaAttributes.BLOCK_BREAK_SPEED);
            CONVERTOR.put(LegacyVanillaAttributes.GRAVITY, VanillaAttributes.GRAVITY);
            CONVERTOR.put(LegacyVanillaAttributes.SAFE_FALL_DISTANCE, VanillaAttributes.SAFE_FALL_DISTANCE);
            CONVERTOR.put(LegacyVanillaAttributes.FALL_DAMAGE_MULTIPLIER, VanillaAttributes.FALL_DAMAGE_MULTIPLIER);
            CONVERTOR.put(LegacyVanillaAttributes.EXPLOSION_KNOCKBACK_RESISTANCE, VanillaAttributes.EXPLOSION_KNOCKBACK_RESISTANCE);
            CONVERTOR.put(LegacyVanillaAttributes.MINING_EFFICIENCY, VanillaAttributes.MINING_EFFICIENCY);
            CONVERTOR.put(LegacyVanillaAttributes.OXYGEN_BONUS, VanillaAttributes.OXYGEN_BONUS);
            CONVERTOR.put(LegacyVanillaAttributes.SNEAKING_SPEED, VanillaAttributes.SNEAKING_SPEED);
            CONVERTOR.put(LegacyVanillaAttributes.SUBMERGED_MINING_SPEED, VanillaAttributes.SUBMERGED_MINING_SPEED);
            CONVERTOR.put(LegacyVanillaAttributes.SWEEPING_DAMAGE_RATIO, VanillaAttributes.SWEEPING_DAMAGE_RATIO);
            CONVERTOR.put(LegacyVanillaAttributes.WATER_MOVEMENT_EFFICIENCY, VanillaAttributes.WATER_MOVEMENT_EFFICIENCY);
        } else {
            CONVERTOR.put(VanillaAttributes.BURNING_TIME, LegacyVanillaAttributes.BURNING_TIME);
            CONVERTOR.put(VanillaAttributes.ARMOR, LegacyVanillaAttributes.ARMOR);
            CONVERTOR.put(VanillaAttributes.ARMOR_TOUGHNESS, LegacyVanillaAttributes.ARMOR_TOUGHNESS);
            CONVERTOR.put(VanillaAttributes.ATTACK_KNOCKBACK, LegacyVanillaAttributes.ATTACK_KNOCKBACK);
            CONVERTOR.put(VanillaAttributes.ATTACK_DAMAGE, LegacyVanillaAttributes.ATTACK_DAMAGE);
            CONVERTOR.put(VanillaAttributes.ATTACK_SPEED, LegacyVanillaAttributes.ATTACK_SPEED);
            CONVERTOR.put(VanillaAttributes.FLYING_SPEED, LegacyVanillaAttributes.FLYING_SPEED);
            CONVERTOR.put(VanillaAttributes.FOLLOW_RANGE, LegacyVanillaAttributes.FOLLOW_RANGE);
            CONVERTOR.put(VanillaAttributes.KNOCKBACK_RESISTANCE, LegacyVanillaAttributes.KNOCKBACK_RESISTANCE);
            CONVERTOR.put(VanillaAttributes.LUCK, LegacyVanillaAttributes.LUCK);
            CONVERTOR.put(VanillaAttributes.MAX_ABSORPTION, LegacyVanillaAttributes.MAX_ABSORPTION);
            CONVERTOR.put(VanillaAttributes.MAX_HEALTH, LegacyVanillaAttributes.MAX_HEALTH);
            CONVERTOR.put(VanillaAttributes.MOVEMENT_EFFICIENCY, LegacyVanillaAttributes.MOVEMENT_EFFICIENCY);
            CONVERTOR.put(VanillaAttributes.SCALE, LegacyVanillaAttributes.SCALE);
            CONVERTOR.put(VanillaAttributes.STEP_HEIGHT, LegacyVanillaAttributes.STEP_HEIGHT);
            CONVERTOR.put(VanillaAttributes.JUMP_STRENGTH, LegacyVanillaAttributes.JUMP_STRENGTH);
            CONVERTOR.put(VanillaAttributes.ENTITY_INTERACTION_RANGE, LegacyVanillaAttributes.ENTITY_INTERACTION_RANGE);
            CONVERTOR.put(VanillaAttributes.BLOCK_INTERACTION_RANGE, LegacyVanillaAttributes.BLOCK_INTERACTION_RANGE);
            CONVERTOR.put(VanillaAttributes.SPAWN_REINFORCEMENT, LegacyVanillaAttributes.SPAWN_REINFORCEMENT);
            CONVERTOR.put(VanillaAttributes.BLOCK_BREAK_SPEED, LegacyVanillaAttributes.BLOCK_BREAK_SPEED);
            CONVERTOR.put(VanillaAttributes.GRAVITY, LegacyVanillaAttributes.GRAVITY);
            CONVERTOR.put(VanillaAttributes.SAFE_FALL_DISTANCE, LegacyVanillaAttributes.SAFE_FALL_DISTANCE);
            CONVERTOR.put(VanillaAttributes.FALL_DAMAGE_MULTIPLIER, LegacyVanillaAttributes.FALL_DAMAGE_MULTIPLIER);
            CONVERTOR.put(VanillaAttributes.EXPLOSION_KNOCKBACK_RESISTANCE, LegacyVanillaAttributes.EXPLOSION_KNOCKBACK_RESISTANCE);
            CONVERTOR.put(VanillaAttributes.MINING_EFFICIENCY, LegacyVanillaAttributes.MINING_EFFICIENCY);
            CONVERTOR.put(VanillaAttributes.OXYGEN_BONUS, LegacyVanillaAttributes.OXYGEN_BONUS);
            CONVERTOR.put(VanillaAttributes.SNEAKING_SPEED, LegacyVanillaAttributes.SNEAKING_SPEED);
            CONVERTOR.put(VanillaAttributes.SUBMERGED_MINING_SPEED, LegacyVanillaAttributes.SUBMERGED_MINING_SPEED);
            CONVERTOR.put(VanillaAttributes.SWEEPING_DAMAGE_RATIO, LegacyVanillaAttributes.SWEEPING_DAMAGE_RATIO);
            CONVERTOR.put(VanillaAttributes.WATER_MOVEMENT_EFFICIENCY, LegacyVanillaAttributes.WATER_MOVEMENT_EFFICIENCY);
        }
    }

    public static Key getNativeAttributeName(final Key attributeName) {
        return CONVERTOR.getOrDefault(attributeName, attributeName);
    }

    private final List<PreModifier> modifiers;

    public AttributeModifiersProcessor(List<PreModifier> modifiers) {
        this.modifiers = modifiers;
    }

    public List<PreModifier> modifiers() {
        return this.modifiers;
    }

    @Override
    public void apply(ItemBuildContext context) {
        Item item = context.item();
        List<VanillaAttributeModifier> results = new ArrayList<>(this.modifiers.size());
        for (PreModifier modifier : this.modifiers) {
            results.add(modifier.toAttributeModifier(item, context));
        }
        item.attributeModifiers(results);
    }

    @Override
    public Key componentType(Item item, ItemBuildContext context) {
        return DataComponentKeys.ATTRIBUTE_MODIFIERS;
    }

    @Override
    public Object[] nbtPath(Item item, ItemBuildContext context) {
        return NBT_PATH;
    }

    @Override
    public String nbtPathString(Item item, ItemBuildContext context) {
        return "AttributeModifiers";
    }

    public record PreModifier(String type,
                              VanillaAttributeModifier.Slot slot,
                              Optional<Key> id,
                              NumberProvider amount,
                              VanillaAttributeModifier.Operation operation,
                              AttributeModifiersProcessor.PreModifier.@Nullable PreDisplay display) {

        public PreModifier(String type, VanillaAttributeModifier.Slot slot, Optional<Key> id, NumberProvider amount, VanillaAttributeModifier.Operation operation, @Nullable PreDisplay display) {
            this.amount = amount;
            this.type = type;
            this.slot = slot;
            this.id = id;
            this.operation = operation;
            this.display = display;
        }

        public VanillaAttributeModifier toAttributeModifier(Item item, ItemBuildContext context) {
            return new VanillaAttributeModifier(this.type, this.slot, this.id.orElseGet(() -> Key.of("craftengine", UUID.randomUUID().toString())),
                    this.amount.getDouble(context), this.operation, this.display == null ? null : this.display.toDisplay(context));
        }

        public record PreDisplay(VanillaAttributeModifier.Display.Type type, String value) {

            public VanillaAttributeModifier.Display toDisplay(ItemBuildContext context) {
                return new VanillaAttributeModifier.Display(type, AdventureHelper.deserialize(value, context));
            }
        }
    }

    private static class Factory implements ItemProcessorFactory<AttributeModifiersProcessor> {

        @Override
        public AttributeModifiersProcessor create(ConfigValue value) {
            List<PreModifier> preModifiers = value.getAsList(v -> {
                ConfigSection section = v.getAsSection();
                Key nativeType = AttributeModifiersProcessor.getNativeAttributeName(section.getIdentifier("type"));
                VanillaAttributeModifier.Slot slot = section.getNonNullEnum("slot", VanillaAttributeModifier.Slot.class, s -> VanillaAttributeModifier.Slot.byId(s, VanillaAttributeModifier.Slot.ANY));
                VanillaAttributeModifier.Operation operation = section.getEnum("operation", VanillaAttributeModifier.Operation.class, VanillaAttributeModifier.Operation.ADD_VALUE);
                Optional<Key> id = Optional.ofNullable(section.getIdentifier("id"));
                NumberProvider amount = section.getNonNullNumber("amount");
                PreModifier.PreDisplay display = null;
                if (VersionHelper.isOrAbove1_21_6 && section.containsKey("display")) {
                    ConfigSection displaySection = section.getNonNullSection("display");
                    VanillaAttributeModifier.Display.Type displayType = displaySection.getNonNullEnum("type", VanillaAttributeModifier.Display.Type.class);
                    if (displayType == VanillaAttributeModifier.Display.Type.OVERRIDE) {
                        display = new PreModifier.PreDisplay(displayType, displaySection.getNonNullString("value"));
                    } else {
                        display = new PreModifier.PreDisplay(displayType, null);
                    }
                }
                return new PreModifier(nativeType.value(), slot, id,
                        amount, operation, display);
            });
            return new AttributeModifiersProcessor(preModifiers);
        }
    }
}
