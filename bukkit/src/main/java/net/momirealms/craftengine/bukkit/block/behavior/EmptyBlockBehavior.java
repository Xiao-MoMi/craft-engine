package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.core.block.EmptyBlockDefinition;

public final class EmptyBlockBehavior extends BukkitBlockBehavior {
    public static final EmptyBlockBehavior INSTANCE = new EmptyBlockBehavior();

    private EmptyBlockBehavior() {
        super(EmptyBlockDefinition.INSTANCE);
    }
}
