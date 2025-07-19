package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.attribute.AttributeModifier;
import net.momirealms.craftengine.core.attribute.Attributes;
import net.momirealms.craftengine.core.attribute.Attributes1_21;
import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.NetworkItemHandler;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.UUIDUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.ListTag;
import net.momirealms.sparrow.nbt.Tag;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class AttributeModifiersModifier<I> implements ItemDataModifier<I> {
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

    private final List<AttributeModifier> modifiers;

    public AttributeModifiersModifier(List<AttributeModifier> modifiers) {
        this.modifiers = modifiers;
    }

    public List<AttributeModifier> modifiers() {
        return this.modifiers;
    }

    @Override
    public String name() {
        return "attribute-modifiers";
    }

    private static Object previous;

    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        if (VersionHelper.isOrAbove1_21_5()) {
            ListTag modifiers = new ListTag();
            for (AttributeModifier modifier : this.modifiers) {
                CompoundTag modifierTag = new CompoundTag();
                modifierTag.putString("type", modifier.type());
                modifierTag.putString("slot", modifier.slot().name().toLowerCase(Locale.ENGLISH));
                modifierTag.putString("id", modifier.id().toString());
                modifierTag.putDouble("amount", modifier.amount());
                modifierTag.putString("operation", modifier.operation().id());
                AttributeModifier.Display display = modifier.display();
                if (VersionHelper.isOrAbove1_21_6() && display != null) {
                    CompoundTag displayTag = new CompoundTag();
                    AttributeModifier.Display.Type displayType = display.type();
                    displayTag.putString("type", displayType.name().toLowerCase(Locale.ENGLISH));
                    if (displayType == AttributeModifier.Display.Type.OVERRIDE) {
                        displayTag.put("value", AdventureHelper.componentToTag(AdventureHelper.miniMessage().deserialize(display.value(), context.tagResolvers())));
                    }
                    modifierTag.put("display", displayTag);
                }
                modifiers.add(modifierTag);
            }
            item.setNBTComponent(ComponentKeys.ATTRIBUTE_MODIFIERS, modifiers);
        } else if (VersionHelper.isOrAbove1_20_5()) {
            CompoundTag compoundTag = (CompoundTag) Optional.ofNullable(item.getSparrowNBTComponent(ComponentKeys.ATTRIBUTE_MODIFIERS)).orElseGet(CompoundTag::new);
            ListTag modifiers = new ListTag();
            compoundTag.put("modifiers", modifiers);
            for (AttributeModifier modifier : this.modifiers) {
                CompoundTag modifierTag = new CompoundTag();
                modifierTag.putString("type", modifier.type());
                modifierTag.putString("slot", modifier.slot().name().toLowerCase(Locale.ENGLISH));
                if (VersionHelper.isOrAbove1_21()) {
                    modifierTag.putString("id", modifier.id().toString());
                } else {
                    modifierTag.putIntArray("uuid", UUIDUtils.uuidToIntArray(UUID.nameUUIDFromBytes(modifier.id().toString().getBytes(StandardCharsets.UTF_8))));
                    modifierTag.putString("name", modifier.id().toString());
                }
                modifierTag.putDouble("amount", modifier.amount());
                modifierTag.putString("operation", modifier.operation().id());
                modifiers.add(modifierTag);
            }
            item.setNBTComponent(ComponentKeys.ATTRIBUTE_MODIFIERS, compoundTag);
        } else {
            ListTag listTag = new ListTag();
            for (AttributeModifier modifier : this.modifiers) {
                CompoundTag modifierTag = new CompoundTag();
                modifierTag.putString("AttributeName", modifier.type());
                modifierTag.putString("Name", modifier.id().toString());
                modifierTag.putString("Slot", modifier.slot().name().toLowerCase(Locale.ENGLISH));
                modifierTag.putInt("Operation", modifier.operation().ordinal());
                modifierTag.putDouble("Amount", modifier.amount());
                modifierTag.putIntArray("UUID", UUIDUtils.uuidToIntArray(UUID.nameUUIDFromBytes(modifier.id().toString().getBytes(StandardCharsets.UTF_8))));
                listTag.add(modifierTag);
            }
            item.setTag(listTag, "AttributeModifiers");
        }
        return item;
    }

    @Override
    public Item<I> prepareNetworkItem(Item<I> item, ItemBuildContext context, CompoundTag networkData) {
        if (VersionHelper.isOrAbove1_20_5()) {
            Tag previous = item.getSparrowNBTComponent(ComponentKeys.ATTRIBUTE_MODIFIERS);
            if (previous != null) {
                networkData.put(ComponentKeys.ATTRIBUTE_MODIFIERS.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
            } else {
                networkData.put(ComponentKeys.ATTRIBUTE_MODIFIERS.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
            }
        } else {
            Tag previous = item.getTag("AttributeModifiers");
            if (previous != null) {
                networkData.put("AttributeModifiers", NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
            } else {
                networkData.put("AttributeModifiers", NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
            }
        }
        return item;
    }
}
