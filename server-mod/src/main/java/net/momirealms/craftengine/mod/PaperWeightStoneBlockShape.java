package net.momirealms.craftengine.mod;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.momirealms.craftengine.shared.block.BlockShape;

public class PaperWeightStoneBlockShape implements BlockShape {
    private final BlockState rawBlockState;

    public PaperWeightStoneBlockShape(BlockState rawBlockState) {
        this.rawBlockState = rawBlockState;
    }

    @Override
    public Object getShape(Object thisObj, Object[] args) {
        return rawBlockState.getShape((BlockGetter) args[1], (BlockPos) args[2], (CollisionContext) args[3]);
    }
}
