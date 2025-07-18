package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.core.block.BlockBehavior;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldPosition;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

public class FallingBlockBehavior extends BukkitBlockBehavior {
    public static final Factory FACTORY = new Factory();
    private final float hurtAmount;
    private final int maxHurt;

    public FallingBlockBehavior(CustomBlock block, float hurtAmount, int maxHurt) {
        super(block);
        this.hurtAmount = hurtAmount;
        this.maxHurt = maxHurt;
    }

    @Override
    public void onPlace(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        Object world = args[1];
        Object blockPos = args[2];
        FastNMS.INSTANCE.method$ScheduledTickAccess$scheduleBlockTick(world, blockPos, thisBlock, 2);
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        Object world = args[updateShape$level];
        Object blockPos = args[updateShape$blockPos];
        FastNMS.INSTANCE.method$ScheduledTickAccess$scheduleBlockTick(world, blockPos, thisBlock, 2);
        return args[0];
    }

    @Override
    public void tick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object blockPos = args[2];
        int y = FastNMS.INSTANCE.field$Vec3i$y(blockPos);
        Object world = args[1];
        Object dimension = CoreReflections.method$$LevelReader$dimensionType.invoke(world);
        int minY = CoreReflections.field$DimensionType$minY.getInt(dimension);
        if (y < minY) {
            return;
        }
        int x = FastNMS.INSTANCE.field$Vec3i$x(blockPos);
        int z = FastNMS.INSTANCE.field$Vec3i$z(blockPos);
        Object belowPos = LocationUtils.toBlockPos(x, y - 1, z);
        Object belowState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(world, belowPos);
        boolean isFree = (boolean) CoreReflections.method$FallingBlock$isFree.invoke(null, belowState);
        if (!isFree) {
            return;
        }
        Object blockState = args[0];
        Object fallingBlockEntity = CoreReflections.method$FallingBlockEntity$fall.invoke(null, world, blockPos, blockState);
        if (this.hurtAmount > 0 && this.maxHurt > 0) {
            CoreReflections.method$FallingBlockEntity$setHurtsEntities.invoke(fallingBlockEntity, this.hurtAmount, this.maxHurt);
        }
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public void onBrokenAfterFall(Object thisBlock, Object[] args) throws Exception {
        // Use EntityRemoveEvent for 1.20.3+
        if (VersionHelper.isOrAbove1_20_3()) return;
        Object level = args[0];
        Object fallingBlockEntity = args[2];
        boolean cancelDrop = (boolean) CoreReflections.field$FallingBlockEntity$cancelDrop.get(fallingBlockEntity);
        if (cancelDrop) return;
        Object blockState = CoreReflections.field$FallingBlockEntity$blockState.get(fallingBlockEntity);
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) return;
        ImmutableBlockState customState = optionalCustomState.get();
        net.momirealms.craftengine.core.world.World world = new BukkitWorld(FastNMS.INSTANCE.method$Level$getCraftWorld(level));
        WorldPosition position = new WorldPosition(world, CoreReflections.field$Entity$xo.getDouble(fallingBlockEntity), CoreReflections.field$Entity$yo.getDouble(fallingBlockEntity), CoreReflections.field$Entity$zo.getDouble(fallingBlockEntity));
        ContextHolder.Builder builder = ContextHolder.builder()
                .withParameter(DirectContextParameters.FALLING_BLOCK, true)
                .withParameter(DirectContextParameters.POSITION, position);
        for (Item<Object> item : customState.getDrops(builder, world, null)) {
            world.dropItemNaturally(position, item);
        }
        Object entityData = CoreReflections.field$Entity$entityData.get(fallingBlockEntity);
        boolean isSilent = (boolean) CoreReflections.method$SynchedEntityData$get.invoke(entityData, CoreReflections.instance$Entity$DATA_SILENT);
        if (!isSilent) {
            world.playBlockSound(position, customState.settings().sounds().destroySound());
        }
    }

    @Override
    public void onLand(Object thisBlock, Object[] args) throws Exception {
        Object fallingBlock = args[4];
        Object level = args[0];
        Object pos = args[1];
        Object entityData = CoreReflections.field$Entity$entityData.get(fallingBlock);
        boolean isSilent = (boolean) CoreReflections.method$SynchedEntityData$get.invoke(entityData, CoreReflections.instance$Entity$DATA_SILENT);
        Object blockState = args[2];
        int stateId = BlockStateUtils.blockStateToId(blockState);
        ImmutableBlockState immutableBlockState = BukkitBlockManager.instance().getImmutableBlockState(stateId);
        if (immutableBlockState == null || immutableBlockState.isEmpty()) return;
        if (!isSilent) {
            net.momirealms.craftengine.core.world.World world = new BukkitWorld(FastNMS.INSTANCE.method$Level$getCraftWorld(level));
            world.playBlockSound(Vec3d.atCenterOf(LocationUtils.fromBlockPos(pos)), immutableBlockState.settings().sounds().landSound());
        }
    }

    public static class Factory implements BlockBehaviorFactory {
        @Override
        public BlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            float hurtAmount = ResourceConfigUtils.getAsFloat(arguments.getOrDefault("hurt-amount", -1f), "hurt-amount");
            int hurtMax = ResourceConfigUtils.getAsInt(arguments.getOrDefault("max-hurt", -1), "max-hurt");
            return new FallingBlockBehavior(block, hurtAmount, hurtMax);
        }
    }
}
