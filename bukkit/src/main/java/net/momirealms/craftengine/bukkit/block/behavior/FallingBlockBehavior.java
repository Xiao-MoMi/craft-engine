package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.entity.BukkitEntity;
import net.momirealms.craftengine.bukkit.entity.data.BaseEntityData;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.proxy.minecraft.core.Vec3iProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.item.FallingBlockEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelAccessorProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelReaderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.FallingBlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.dimension.DimensionTypeProxy;

import java.util.Optional;

public final class FallingBlockBehavior extends BukkitBlockBehavior implements BukkitFallableBlock {
    public static final BlockBehaviorFactory<FallingBlockBehavior> FACTORY = new Factory();
    public final float hurtAmount;
    public final int maxHurt;
    public final SoundData landSound;
    public final SoundData destroySound;

    public FallingBlockBehavior(BlockDefinition block,
                                float hurtAmount,
                                int maxHurt,
                                SoundData landSound,
                                SoundData destroySound) {
        super(block);
        this.hurtAmount = hurtAmount;
        this.maxHurt = maxHurt;
        this.landSound = landSound;
        this.destroySound = destroySound;
    }

    @Override
    public void onPlace(Object thisBlock, Object[] args) {
        Object world = args[1];
        Object blockPos = args[2];
        LevelAccessorProxy.INSTANCE.scheduleTick$0(world, blockPos, thisBlock, 2);
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args) {
        Object world = args[updateShape$level];
        Object blockPos = args[updateShape$blockPos];
        LevelAccessorProxy.INSTANCE.scheduleTick$0(world, blockPos, thisBlock, 2);
        return args[0];
    }

    @Override
    public void tick(Object thisBlock, Object[] args) {
        Object blockPos = args[2];
        int y = Vec3iProxy.INSTANCE.getY(blockPos);
        Object world = args[1];
        Object dimension = LevelReaderProxy.INSTANCE.dimensionType(world);
        int minY = DimensionTypeProxy.INSTANCE.getMinY(dimension);
        if (y < minY) {
            return;
        }

        int x = Vec3iProxy.INSTANCE.getX(blockPos);
        int z = Vec3iProxy.INSTANCE.getZ(blockPos);
        Object belowPos = LocationUtils.toBlockPos(x, y - 1, z);
        Object belowState = BlockGetterProxy.INSTANCE.getBlockState(world, belowPos);
        boolean isFree = FallingBlockProxy.INSTANCE.isFree(belowState);
        if (!isFree) {
            return;
        }
        Object blockState = args[0];
        Object fallingBlockEntity = FastNMS.INSTANCE.createInjectedFallingBlockEntity(world, blockPos, blockState);
        if (this.hurtAmount > 0 && this.maxHurt > 0) {
            FallingBlockEntityProxy.INSTANCE.setHurtsEntities(fallingBlockEntity, this.hurtAmount, this.maxHurt);
        }
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public void onBrokenAfterFall(Object thisBlock, Object[] args) {
        Object level = args[0];
        Object fallingBlockEntity = args[2];
        BukkitEntity entity = BukkitAdaptor.adapt(EntityProxy.INSTANCE.getBukkitEntity(fallingBlockEntity));
        if (!entity.getEntityData(BaseEntityData.Silent)) {
            Object blockState = FallingBlockEntityProxy.INSTANCE.getBlockState(fallingBlockEntity);
            Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
            if (optionalCustomState.isEmpty()) return;
            net.momirealms.craftengine.core.world.World world = BukkitAdaptor.adapt(LevelProxy.INSTANCE.getWorld(level));
            WorldPosition position = new WorldPosition(world, EntityProxy.INSTANCE.getXo(fallingBlockEntity), EntityProxy.INSTANCE.getYo(fallingBlockEntity), EntityProxy.INSTANCE.getZo(fallingBlockEntity));
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
        Object blockState = args[2];
        int stateId = BlockStateUtils.blockStateToId(blockState);
        ImmutableBlockState immutableBlockState = BukkitBlockManager.instance().getImmutableBlockState(stateId);
        if (immutableBlockState == null || immutableBlockState.isEmpty()) return;
        if (!EntityProxy.INSTANCE.isSilent(fallingBlock)) {
            net.momirealms.craftengine.core.world.World world = BukkitAdaptor.adapt(LevelProxy.INSTANCE.getWorld(level));
            if (this.landSound != null) {
                world.playBlockSound(Vec3d.atCenterOf(LocationUtils.fromBlockPos(pos)), this.landSound);
            }
        }
    }

    private static class Factory implements BlockBehaviorFactory<FallingBlockBehavior> {
        private static final String[] HURT_AMOUNT = new String[] {"hurt_amount", "hurt-amount"};
        private static final String[] MAX_HURT = new String[] {"max_hurt", "max-hurt"};

        @Override
        public FallingBlockBehavior create(BlockDefinition block, ConfigSection section) {
            ConfigSection soundSection = section.getSection("sounds");
            SoundData landSound = null;
            SoundData destroySound = null;
            if (soundSection != null) {
                landSound = soundSection.getValue("land", v -> SoundData.fromConfig(v, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.RANGED_0_9_1));
                destroySound = soundSection.getValue("destroy", v -> SoundData.fromConfig(v, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.RANGED_0_9_1));
            }
            return new FallingBlockBehavior(
                    block,
                    section.getFloat(HURT_AMOUNT, -1f),
                    section.getInt(MAX_HURT, -1),
                    landSound,
                    destroySound
            );
        }
    }
}
