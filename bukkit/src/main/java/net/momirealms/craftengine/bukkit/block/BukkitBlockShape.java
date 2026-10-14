package net.momirealms.craftengine.bukkit.block;

import net.momirealms.craftengine.core.block.BlockShape;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlocksProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import org.jetbrains.annotations.Nullable;

public final class BukkitBlockShape implements BlockShape {
    public static final BukkitBlockShape STONE = new BukkitBlockShape(BlocksProxy.STONE$defaultState, null);
    private final Object rawBlockState;
    private final Object supportBlockState;

    public BukkitBlockShape(Object rawBlockState, @Nullable Object supportBlockState) {
        this.rawBlockState = rawBlockState;
        this.supportBlockState = supportBlockState == null ? rawBlockState : supportBlockState;
    }

    @Override
    public Object getShape(Object thisObj, Object blockState, Object level, Object blockPos, Object collisionContext) {
        return BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getShape(this.rawBlockState, level, blockPos, collisionContext);
    }

    @Override
    public Object getCollisionShape(Object thisObj, Object blockState, Object level, Object blockPos, Object collisionContext) {
        return BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getCollisionShape(this.rawBlockState, level, blockPos, collisionContext);
    }

    @Override
    public Object getSupportShape(Object thisObj, Object blockState, Object level, Object blockPos) {
        return BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getBlockSupportShape(this.supportBlockState, level, blockPos);
    }
}
