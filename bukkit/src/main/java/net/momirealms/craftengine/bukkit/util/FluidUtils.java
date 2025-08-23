package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.world.FluidCollisionRule;
import org.bukkit.FluidCollisionMode;

public final class FluidUtils {

    private FluidUtils() {}

    public static FluidCollisionMode toCollisionRule(FluidCollisionRule rule) {
        switch (rule) {
            case NONE -> {
                return FluidCollisionMode.NEVER;
            }
            case ALWAYS -> {
                return FluidCollisionMode.ALWAYS;
            }
            case SOURCE_ONLY -> {
                return FluidCollisionMode.SOURCE_ONLY;
            }
        }
        return FluidCollisionMode.NEVER;
    }
}
