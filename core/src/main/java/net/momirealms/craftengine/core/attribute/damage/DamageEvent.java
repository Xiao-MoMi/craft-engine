package net.momirealms.craftengine.core.attribute.damage;

import net.momirealms.craftengine.core.attribute.Attribute;
import net.momirealms.craftengine.core.attribute.AttributeSide;
import net.momirealms.craftengine.core.entity.Entity;
import net.momirealms.craftengine.core.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface DamageEvent {

    EntityDamageContext context();

    double damage();

    default double finalDamage() {
        return damage();
    }

    void setDamage(double damage);

    DamageSource source();

    Entity victim();

    boolean isSweepAttack();

    default float attackStrength() {
        return 1.0F;
    }

    default boolean isAttackReady() {
        return attackStrength() > 0.9F;
    }

    double getAttributeValue(AttributeSide side, Attribute attribute);

    void recordDamagePart(String id, double amount);

    void initFinalDamage();

    Map<String, Double> damageParts();

    @Nullable
    Item activeWeapon();
}
