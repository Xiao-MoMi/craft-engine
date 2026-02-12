package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelAccessorProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.ScheduledTickAccessProxy;

import javax.annotation.Nullable;

public final class LevelUtils {
    private LevelUtils() {}

    public static void scheduleBlockTick(Object level, Object blockPos, Object block, int ticks) {
        if (VersionHelper.isOrAbove1_21_2()) {
            ScheduledTickAccessProxy.INSTANCE.scheduleTick$0(level, blockPos, block, ticks);
        } else {
            LevelAccessorProxy.INSTANCE.scheduleTick$0(level, blockPos, block, ticks);
        }
    }

    public static void scheduleBlockTick(Object level, Object blockPos, Object block, int ticks, Object priority) {
        if (VersionHelper.isOrAbove1_21_2()) {
            ScheduledTickAccessProxy.INSTANCE.scheduleTick$0(level, blockPos, block, ticks, priority);
        } else {
            LevelAccessorProxy.INSTANCE.scheduleTick$0(level, blockPos, block, ticks, priority);
        }
    }

    public static void scheduleFluidTick(Object level, Object blockPos, Object fluid, int ticks) {
        if (VersionHelper.isOrAbove1_21_2()) {
            ScheduledTickAccessProxy.INSTANCE.scheduleTick$1(level, blockPos, fluid, ticks);
        } else {
            LevelAccessorProxy.INSTANCE.scheduleTick$1(level, blockPos, fluid, ticks);
        }
    }

    public static void scheduleFluidTick(Object level, Object blockPos, Object fluid, int ticks, Object priority) {
        if (VersionHelper.isOrAbove1_21_2()) {
            ScheduledTickAccessProxy.INSTANCE.scheduleTick$1(level, blockPos, fluid, ticks, priority);
        } else {
            LevelAccessorProxy.INSTANCE.scheduleTick$1(level, blockPos, fluid, ticks, priority);
        }
    }

    public static void levelEvent(Object target, @Nullable Object source, int eventId, Object pos, int data) {
        if (VersionHelper.isOrAbove1_21_5()) {
            LevelAccessorProxy.INSTANCE.levelEvent$0(target, source, eventId, pos, data);
        } else {
            LevelAccessorProxy.INSTANCE.levelEvent$1(target, source, eventId, pos, data);
        }
    }
}
