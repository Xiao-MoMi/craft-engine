package net.momirealms.craftengine.bukkit.util;

import org.bukkit.ExplosionResult;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

@SuppressWarnings("UnstableApiUsage")
public final class ExplosionUtils {

    public static boolean isDroppingItems(BlockExplodeEvent event) {
        return event.getExplosionResult() != ExplosionResult.KEEP && event.getExplosionResult() != ExplosionResult.TRIGGER_BLOCK;
    }

    public static boolean isDroppingItems(EntityExplodeEvent event) {
        return event.getExplosionResult() != ExplosionResult.KEEP && event.getExplosionResult() != ExplosionResult.TRIGGER_BLOCK;
    }
}
