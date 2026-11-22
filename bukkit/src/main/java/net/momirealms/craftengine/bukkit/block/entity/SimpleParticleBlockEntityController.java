package net.momirealms.craftengine.bukkit.block.entity;

import net.momirealms.craftengine.bukkit.block.behavior.SimpleParticleBlockBehavior;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.block.entity.tick.BlockEntityTicker;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.SimpleContext;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.particle.ParticleConfig;

public final class SimpleParticleBlockEntityController extends BlockEntityController {
    private final SimpleParticleBlockBehavior behavior;
    private int tickCount;

    public SimpleParticleBlockEntityController(BlockEntity blockEntity, SimpleParticleBlockBehavior behavior) {
        super(blockEntity);
        this.behavior = behavior;
    }

    @Override
    public <C extends BlockEntityController> BlockEntityTicker<C> createAsyncBlockEntityTicker(CEWorld world, ImmutableBlockState blockState) {
        return createTickerHelper(SimpleParticleBlockEntityController::tick);
    }

    public void animateTick(World level, BlockPos pos) {
        for (ParticleConfig particle : this.behavior.particles) {
            Vec3d location = new Vec3d(pos.x() + particle.x.getDouble(), pos.y() + particle.y.getDouble(), pos.z() + particle.z.getDouble());
            level.spawnParticle(
                    location,
                    particle.particleType,
                    particle.count.getInt(),
                    particle.xOffset.getDouble(),
                    particle.yOffset.getDouble(),
                    particle.zOffset.getDouble(),
                    particle.speed.getDouble(),
                    particle.particleData
            );
        }
    }

    public static void tick(CEWorld ceWorld, BlockPos blockPos, ImmutableBlockState state, SimpleParticleBlockEntityController particle) {
        particle.tickCount++;
        if (particle.tickCount % particle.behavior.tickInterval != 0) return;
        particle.animateTick(ceWorld.world(), blockPos);
    }
}
