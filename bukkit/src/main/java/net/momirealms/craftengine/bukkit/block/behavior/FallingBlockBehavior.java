package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.entity.BukkitEntity;
import net.momirealms.craftengine.bukkit.entity.data.BaseEntityData;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldPosition;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

public class FallingBlockBehavior extends BukkitBlockBehavior {
    public static final BlockBehaviorFactory<FallingBlockBehavior> FACTORY = new Factory();
    private final float hurtAmount;
    private final int maxHurt;
    private final SoundData landSound;
    private final SoundData destroySound;

    public FallingBlockBehavior(CustomBlock block, float hurtAmount, int maxHurt, SoundData landSound, SoundData destroySound) {
        super(block);
        this.hurtAmount = hurtAmount;
        this.maxHurt = maxHurt;
        this.landSound = landSound;
        this.destroySound = destroySound;
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
        Object dimension = CoreReflections.method$LevelReader$dimensionType.invoke(world);
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
        Object fallingBlockEntity = FastNMS.INSTANCE.createInjectedFallingBlockEntity(world, blockPos, blockState);
        if (this.hurtAmount > 0 && this.maxHurt > 0) {
            CoreReflections.method$FallingBlockEntity$setHurtsEntities.invoke(fallingBlockEntity, this.hurtAmount, this.maxHurt);
        }
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public void onBrokenAfterFall(Object thisBlock, Object[] args) throws Exception {
        Object level = args[0];
        Object fallingBlockEntity = args[2];
        BukkitEntity entity = BukkitAdaptors.adapt(FastNMS.INSTANCE.method$Entity$getBukkitEntity(fallingBlockEntity));
        if (!entity.getEntityData(BaseEntityData.Silent)) {
            Object blockState = CoreReflections.field$FallingBlockEntity$blockState.get(fallingBlockEntity);
            Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
            if (optionalCustomState.isEmpty()) return;
            net.momirealms.craftengine.core.world.World world = BukkitAdaptors.adapt(FastNMS.INSTANCE.method$Level$getCraftWorld(level));
            WorldPosition position = new WorldPosition(world, CoreReflections.field$Entity$xo.getDouble(fallingBlockEntity), CoreReflections.field$Entity$yo.getDouble(fallingBlockEntity), CoreReflections.field$Entity$zo.getDouble(fallingBlockEntity));
            if (this.destroySound != null) {
                world.playBlockSound(position, this.destroySound);
            }
        }
    }

    @Override
    public void onLand(Object thisBlock, Object[] args) {
        Object fallingBlock = args[4];
        Object level = args[0];
        Object pos = args[1];
        BukkitEntity entity = BukkitAdaptors.adapt(FastNMS.INSTANCE.method$Entity$getBukkitEntity(fallingBlock));
        Object blockState = args[2];
        int stateId = BlockStateUtils.blockStateToId(blockState);
        ImmutableBlockState immutableBlockState = BukkitBlockManager.instance().getImmutableBlockState(stateId);
        if (immutableBlockState == null || immutableBlockState.isEmpty()) return;
        if (!entity.getEntityData(BaseEntityData.Silent)) {
            net.momirealms.craftengine.core.world.World world = BukkitAdaptors.adapt(FastNMS.INSTANCE.method$Level$getCraftWorld(level));
            if (this.landSound != null) {
                world.playBlockSound(Vec3d.atCenterOf(LocationUtils.fromBlockPos(pos)), this.landSound);
            }
        }
    }

    private static class Factory implements BlockBehaviorFactory<FallingBlockBehavior> {

        @SuppressWarnings("unchecked")
        @Override
        public FallingBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            float hurtAmount = ResourceConfigUtils.getAsFloat(arguments.getOrDefault("hurt-amount", -1f), "hurt-amount");
            int hurtMax = ResourceConfigUtils.getAsInt(arguments.getOrDefault("max-hurt", -1), "max-hurt");
            Map<String, Object> sounds = (Map<String, Object>) arguments.get("sounds");
            SoundData fallSound = null;
            SoundData destroySound = null;
            if (sounds != null) {
                fallSound = Optional.ofNullable(sounds.get("land")).map(obj -> SoundData.create(obj, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.ranged(0.9f, 1f))).orElse(null);
                destroySound = Optional.ofNullable(sounds.get("destroy")).map(obj -> SoundData.create(obj, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.ranged(0.9f, 1f))).orElse(null);
            }
            return new FallingBlockBehavior(block, hurtAmount, hurtMax, fallSound, destroySound);
        }
    }
}
