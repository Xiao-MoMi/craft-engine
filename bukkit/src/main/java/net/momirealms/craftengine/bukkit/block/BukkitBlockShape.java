package net.momirealms.craftengine.bukkit.block;

import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.shared.block.BlockShape;

public class BukkitBlockShape implements BlockShape {
    private final Object rawBlockState;

    public BukkitBlockShape(Object rawBlockState) {
        this.rawBlockState = rawBlockState;
    }

    @Override
    public Object getShape(Object thisObj, Object[] args) throws Exception {
        return Reflections.method$BlockBehaviour$getShape.invoke(Reflections.field$StateHolder$owner.get(this.rawBlockState), this.rawBlockState, args[1], args[2], args[3]);
    }
}
