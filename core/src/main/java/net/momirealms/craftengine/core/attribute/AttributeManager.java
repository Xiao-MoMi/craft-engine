package net.momirealms.craftengine.core.attribute;

import net.momirealms.craftengine.core.attribute.damage.DamageEvent;
import net.momirealms.craftengine.core.attribute.damage.DamageRule;
import net.momirealms.craftengine.core.attribute.modifier.ItemAttributeModifiersProvider;
import net.momirealms.craftengine.core.attribute.modifier.SlotAttributeModifierConfig;
import net.momirealms.craftengine.core.entity.Entity;
import net.momirealms.craftengine.core.entity.LivingEntity;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.equipment.EquipmentSet;
import net.momirealms.craftengine.core.item.setting.value.AttributeModifiers;
import net.momirealms.craftengine.core.plugin.Manageable;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AttributeManager extends Manageable {
    String META_KEY = "ce:attr";

    Optional<Attribute> getAttribute(Key id);

    Collection<Attribute> getAttributes();

    List<Attribute> attributesByEntityType(Key entityType);

    double getAttributeValue(LivingEntity entity, Attribute attribute);

    List<SlotAttributeModifierConfig> getItemAttributeModifiers(Item item);

    void registerItemModifiersProvider(Key id, int priority, ItemAttributeModifiersProvider provider);

    void unregisterItemModifiersProvider(Key id);

    default double getWeaponAttributeValue(@Nullable Item weapon, Attribute attribute, Context context) {
        if (weapon == null || weapon.isEmpty()) return 0;
        return AttributeModifiers.weaponValue(getItemAttributeModifiers(weapon), attribute, context);
    }

    Optional<AttributeOperation> getOperation(Key id);

    Optional<EquipmentSet> equipmentSet(Key id);

    @Nullable
    DamageRule findDamageRule(DamageEvent event);

    ConfigParser[] parsers();

    void processDamageEvent(DamageEvent event);

    double vanillaAttributeDefaultBaseValue(Entity living, Key attribute, double fallback);
}
