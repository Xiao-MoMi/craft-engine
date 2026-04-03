package net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.parameters;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.storage.loot.parameters.LootContextParams")
public interface LootContextParamsProxy {
    LootContextParamsProxy INSTANCE = ASMProxyFactory.create(LootContextParamsProxy.class);
    Object THIS_ENTITY = INSTANCE.getThisEntity();
    Object INTERACTING_ENTITY = INSTANCE.getInteractingEntity();
    Object TARGET_ENTITY  = INSTANCE.getTargetEntity();
    Object LAST_DAMAGE_PLAYER = INSTANCE.getLastDamagePlayer();
    Object DAMAGE_SOURCE = INSTANCE.getDamageSource();
    Object ATTACKING_ENTITY = INSTANCE.getAttackingEntity();
    Object KILLER_ENTITY = INSTANCE.getKillerEntity();
    Object DIRECT_ATTACKING_ENTITY = INSTANCE.getDirectAttackingEntity();
    Object DIRECT_KILLER_ENTITY = INSTANCE.getDirectKillerEntity();
    Object ORIGIN = INSTANCE.getOrigin();
    Object BLOCK_STATE = INSTANCE.getBlockState();
    Object BLOCK_ENTITY = INSTANCE.getBlockEntity();
    Object TOOL = INSTANCE.getTool();
    Object EXPLOSION_RADIUS = INSTANCE.getExplosionRadius();
    Object LOOTING_MOD = INSTANCE.getLootMod();
    Object ENCHANTMENT_LEVEL = INSTANCE.getEnchantmentLevel();
    Object ENCHANTMENT_ACTIVE = INSTANCE.getEnchantmentActive();

    // 全版本
    @FieldGetter(name = "THIS_ENTITY", isStatic = true)
    Object getThisEntity();

    @FieldGetter(name = "INTERACTING_ENTITY", isStatic = true, activeIf = "min_version=1.21.9")
    default Object getInteractingEntity() {
        return null;
    }

    @FieldGetter(name = "TARGET_ENTITY", isStatic = true, activeIf = "min_version=1.21.9")
    default Object getTargetEntity() {
        return null;
    }

    // 全版本
    @FieldGetter(name = "LAST_DAMAGE_PLAYER", isStatic = true)
    Object getLastDamagePlayer();

    // 全版本
    @FieldGetter(name = "DAMAGE_SOURCE", isStatic = true)
    Object getDamageSource();

    @FieldGetter(name = "ATTACKING_ENTITY", isStatic = true, activeIf = "min_version=1.21")
    default Object getAttackingEntity() {
        return null;
    }

    @FieldGetter(name = "KILLER_ENTITY", isStatic = true, activeIf = "max_version=1.20.6")
    default Object getKillerEntity() {
        return null;
    }

    @FieldGetter(name = "DIRECT_ATTACKING_ENTITY", isStatic = true, activeIf = "min_version=1.21")
    default Object getDirectAttackingEntity() {
        return null;
    }

    @FieldGetter(name = "DIRECT_KILLER_ENTITY", isStatic = true, activeIf = "max_version=1.20.6")
    default Object getDirectKillerEntity() {
        return null;
    }

    // 全版本
    @FieldGetter(name = "ORIGIN", isStatic = true)
    Object getOrigin();

    // 全版本
    @FieldGetter(name = "BLOCK_STATE", isStatic = true)
    Object getBlockState();

    // 全版本
    @FieldGetter(name = "BLOCK_ENTITY", isStatic = true)
    Object getBlockEntity();

    // 全版本
    @FieldGetter(name = "TOOL", isStatic = true)
    Object getTool();

    // 全版本
    @FieldGetter(name = "EXPLOSION_RADIUS", isStatic = true)
    Object getExplosionRadius();

    @FieldGetter(name = "LOOTING_MOD", isStatic = true, activeIf = "max_version=1.20.6")
    default Object getLootMod() {
        return null;
    }

    @FieldGetter(name = "ENCHANTMENT_LEVEL", isStatic = true, activeIf = "min_version=1.21")
    default Object getEnchantmentLevel() {
        return null;
    }

    @FieldGetter(name = "ENCHANTMENT_ACTIVE", isStatic = true, activeIf = "min_version=1.21")
    default Object getEnchantmentActive() {
        return null;
    }
}
