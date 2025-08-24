package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.util.DamageSource;
import org.bukkit.event.entity.EntityDamageEvent;

public final class DamageCauseUtils {
    
    private DamageCauseUtils() {}

    public static EntityDamageEvent.DamageCause toBukkit(DamageSource cause) {
        return switch (cause) {
            case BLOCK_EXPLOSION -> EntityDamageEvent.DamageCause.BLOCK_EXPLOSION;
            case CAMPFIRE -> EntityDamageEvent.DamageCause.CAMPFIRE;
            case CONTACT -> EntityDamageEvent.DamageCause.CONTACT;
            case CRAMMING -> EntityDamageEvent.DamageCause.CRAMMING;
            case CUSTOM -> EntityDamageEvent.DamageCause.CUSTOM;
            case DROWNING -> EntityDamageEvent.DamageCause.DROWNING;
            case DRYOUT -> EntityDamageEvent.DamageCause.DRYOUT;
            case ENTITY_ATTACK -> EntityDamageEvent.DamageCause.ENTITY_ATTACK;
            case ENTITY_EXPLOSION -> EntityDamageEvent.DamageCause.ENTITY_EXPLOSION;
            case ENTITY_SWEEP_ATTACK -> EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK;
            case FALL -> EntityDamageEvent.DamageCause.FALL;
            case FALLING_BLOCK -> EntityDamageEvent.DamageCause.FALLING_BLOCK;
            case FIRE -> EntityDamageEvent.DamageCause.FIRE;
            case FIRE_TICK -> EntityDamageEvent.DamageCause.FIRE_TICK;
            case FLY_INTO_WALL -> EntityDamageEvent.DamageCause.FLY_INTO_WALL;
            case FREEZE -> EntityDamageEvent.DamageCause.FREEZE;
            case HOT_FLOOR -> EntityDamageEvent.DamageCause.HOT_FLOOR;
            case KILL -> EntityDamageEvent.DamageCause.KILL;
            case LAVA -> EntityDamageEvent.DamageCause.LAVA;
            case LIGHTNING -> EntityDamageEvent.DamageCause.LIGHTNING;
            case MAGIC -> EntityDamageEvent.DamageCause.MAGIC;
            case MELTING -> EntityDamageEvent.DamageCause.MELTING;
            case POISON -> EntityDamageEvent.DamageCause.POISON;
            case PROJECTILE -> EntityDamageEvent.DamageCause.PROJECTILE;
            case SONIC_BOOM -> EntityDamageEvent.DamageCause.SONIC_BOOM;
            case STARVATION -> EntityDamageEvent.DamageCause.STARVATION;
            case SUFFOCATION -> EntityDamageEvent.DamageCause.SUFFOCATION;
            case SUICIDE -> EntityDamageEvent.DamageCause.SUICIDE;
            case THORNS -> EntityDamageEvent.DamageCause.THORNS;
            case VOID -> EntityDamageEvent.DamageCause.VOID;
            case WITHER -> EntityDamageEvent.DamageCause.WITHER;
            case WORLD_BORDER -> EntityDamageEvent.DamageCause.WORLD_BORDER;
            default -> null;
        };
    }

    public static DamageSource fromBukkit(EntityDamageEvent.DamageCause cause) {
        return switch (cause) {
            case BLOCK_EXPLOSION -> DamageSource.BLOCK_EXPLOSION;
            case CAMPFIRE -> DamageSource.CAMPFIRE;
            case CONTACT -> DamageSource.CONTACT;
            case CRAMMING -> DamageSource.CRAMMING;
            case CUSTOM -> DamageSource.CUSTOM;
            case DROWNING -> DamageSource.DROWNING;
            case DRYOUT -> DamageSource.DRYOUT;
            case ENTITY_ATTACK -> DamageSource.ENTITY_ATTACK;
            case ENTITY_EXPLOSION -> DamageSource.ENTITY_EXPLOSION;
            case ENTITY_SWEEP_ATTACK -> DamageSource.ENTITY_SWEEP_ATTACK;
            case FALL -> DamageSource.FALL;
            case FALLING_BLOCK -> DamageSource.FALLING_BLOCK;
            case FIRE -> DamageSource.FIRE;
            case FIRE_TICK -> DamageSource.FIRE_TICK;
            case FLY_INTO_WALL -> DamageSource.FLY_INTO_WALL;
            case FREEZE -> DamageSource.FREEZE;
            case HOT_FLOOR -> DamageSource.HOT_FLOOR;
            case KILL -> DamageSource.KILL;
            case LAVA -> DamageSource.LAVA;
            case LIGHTNING -> DamageSource.LIGHTNING;
            case MAGIC -> DamageSource.MAGIC;
            case MELTING -> DamageSource.MELTING;
            case POISON -> DamageSource.POISON;
            case PROJECTILE -> DamageSource.PROJECTILE;
            case SONIC_BOOM -> DamageSource.SONIC_BOOM;
            case STARVATION -> DamageSource.STARVATION;
            case SUFFOCATION -> DamageSource.SUFFOCATION;
            case SUICIDE -> DamageSource.SUICIDE;
            case THORNS -> DamageSource.THORNS;
            case VOID -> DamageSource.VOID;
            case WITHER -> DamageSource.WITHER;
            case WORLD_BORDER -> DamageSource.WORLD_BORDER;
            default -> null;
        };
    }
}
