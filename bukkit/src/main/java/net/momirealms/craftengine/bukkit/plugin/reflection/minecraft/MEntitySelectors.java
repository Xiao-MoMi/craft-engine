package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;

import java.util.function.Predicate;

public final class MEntitySelectors {
    private MEntitySelectors() {}

    public static final Predicate<Object> NO_SPECTATORS = entity -> !EntityProxy.INSTANCE.isSpectator(entity);

}
