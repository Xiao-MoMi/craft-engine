package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;

public final class MAttributeHolders {
    private MAttributeHolders() {}

    public static final Object BLOCK_BREAK_SPEED = VersionHelper.isOrAbove1_20_5() ? getById(VersionHelper.isOrAbove1_21_2() ? "block_break_speed" : "player.block_break_speed") : null;
    public static final Object BLOCK_INTERACTION_RANGE = VersionHelper.isOrAbove1_20_5() ? getById(VersionHelper.isOrAbove1_21_2() ? "block_interaction_range" : "player.block_interaction_range") : null;
    public static final Object SCALE = VersionHelper.isOrAbove1_20_5() ? getById(VersionHelper.isOrAbove1_21_2() ? "scale" : "generic.scale") : null;

    private static Object getById(String path) {
        Object id = IdentifierProxy.INSTANCE.newInstance("minecraft", path);
        return RegistryProxy.INSTANCE.get$0(MBuiltInRegistries.ATTRIBUTE, id).orElseThrow();
    }
}
