package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelAccessorProxy;

import javax.annotation.Nullable;

public final class LevelUtils {
    private LevelUtils() {}

    public static void levelEvent(Object target, @Nullable Object source, int eventId, Object pos, int data) {
        if (VersionHelper.isOrAbove1_21_5) {
            LevelAccessorProxy.INSTANCE.levelEvent$0(target, source, eventId, pos, data);
        } else {
            LevelAccessorProxy.INSTANCE.levelEvent$1(target, source, eventId, pos, data);
        }
    }
}
