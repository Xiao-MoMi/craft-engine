package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MFluids;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.PlaceLiquidBlockBehavior;
import net.momirealms.craftengine.core.world.WorldEvents;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidStateProxy;

import java.util.Map;
import java.util.concurrent.Callable;

public class LiquidFlowableBlockBehavior extends BukkitBlockBehavior implements PlaceLiquidBlockBehavior {
    public static final BlockBehaviorFactory<LiquidFlowableBlockBehavior> FACTORY = new Factory();

    public LiquidFlowableBlockBehavior(CustomBlock customBlock) {
        super(customBlock);
    }

    @Override
    public boolean canPlaceLiquid(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        return true;
    }

    @Override
    public boolean placeLiquid(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        Object level = args[0];
        Object pos = args[1];
        Object blockState = args[2];
        Object fluidState = args[3];
        Object fluidType = FastNMS.INSTANCE.method$FluidState$getType(fluidState);
        if (fluidType == MFluids.LAVA || fluidType == MFluids.FLOWING_LAVA) {
            FastNMS.INSTANCE.method$LevelAccessor$levelEvent(level, WorldEvents.LAVA_CONVERTS_BLOCK, pos, 0);
        } else {
            FastNMS.INSTANCE.method$Block$dropResources(blockState, level, pos);
        }
        FastNMS.INSTANCE.method$LevelWriter$setBlock(level, pos, FluidStateProxy.INSTANCE.createLegacyBlock(fluidState), UpdateOption.UPDATE_ALL.flags());
        return true;
    }

    private static class Factory implements BlockBehaviorFactory<LiquidFlowableBlockBehavior> {

        @Override
        public LiquidFlowableBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            return new LiquidFlowableBlockBehavior(block);
        }
    }
}
