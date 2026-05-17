package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehavior;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.world.damagesource.DamageSourcesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockStateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;

import java.util.Optional;

public class BukkitBlockBehavior extends BlockBehavior {

    public BukkitBlockBehavior(BlockDefinition blockDefinition) {
        super(blockDefinition);
    }

    protected static final int updateShape$level = VersionHelper.isOrAbove1_21_2 ? 1 : 3;
    protected static final int updateShape$blockPos = VersionHelper.isOrAbove1_21_2 ? 3 : 4;
    protected static final int updateShape$neighborState = VersionHelper.isOrAbove1_21_2 ? 6 : 2;
    protected static final int updateShape$direction = VersionHelper.isOrAbove1_21_2 ? 4 : 1;

    protected static final int isPathFindable$type = VersionHelper.isOrAbove1_20_5 ? 1 : 3;

    @Override
    public boolean isPathFindable(Object thisBlock, Object[] args) {
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(args[0]);
        if (optionalCustomState.isEmpty()) return false;
        BlockStateWrapper vanillaState = optionalCustomState.get().visualBlockState();
        if (vanillaState == null) return false;
        if (VersionHelper.isOrAbove1_20_5) {
            return BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.isPathfindable(vanillaState.minecraftState(), args[isPathFindable$type]);
        } else {
            return BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.isPathfindable(vanillaState.minecraftState(), args[1], args[2], args[isPathFindable$type]);
        }
    }

    @Override
    public void fallOn(Object thisBlock, Object[] args) {
        Object sources = EntityProxy.INSTANCE.damageSources(args[3]);
        if (VersionHelper.isOrAbove1_21_5) {
            EntityProxy.INSTANCE.causeFallDamage(args[3], (double) args[4], 1.0f, DamageSourcesProxy.INSTANCE.fall(sources));
        } else {
            EntityProxy.INSTANCE.causeFallDamage(args[3], (float) args[4], 1.0f, DamageSourcesProxy.INSTANCE.fall(sources));
        }
    }

    @Override
    public void updateEntityMovementAfterFallOn(Object thisBlock, Object[] args) {
        Object deltaMovement = EntityProxy.INSTANCE.getDeltaMovement(args[1]);
        Object multiplied = Vec3Proxy.INSTANCE.multiply(deltaMovement, 1.0, 0, 1.0);
        EntityProxy.INSTANCE.setDeltaMovement(args[1], multiplied);
    }

    @Override
    public void affectNeighborsAfterRemoval(Object thisBlock, Object[] args) {
        if (!VersionHelper.isOrAbove1_21_5) {
            if (BlockStateProxy.INSTANCE.hasBlockEntity(args[0]) && !BlockStateProxy.INSTANCE.is$0(args[0], BlockStateProxy.INSTANCE.getBlock(args[3]))) {
                LevelProxy.INSTANCE.removeBlockEntity(args[1], args[2]);
            }
        }
    }
}
