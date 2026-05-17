package net.momirealms.craftengine.bukkit.block.entity;

import net.momirealms.craftengine.bukkit.block.behavior.WallTorchParticleBlockBehavior;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.block.entity.tick.BlockEntityTicker;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.SimpleContext;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.particle.ParticleConfig;

public final class WallTorchParticleBlockEntityController extends BlockEntityController {
    private final WallTorchParticleBlockBehavior behavior;
    private final Context context = SimpleContext.of(ContextHolder.empty());
    private int tickCount;

    public WallTorchParticleBlockEntityController(BlockEntity blockEntity, WallTorchParticleBlockBehavior behavior) {
        super(blockEntity);
        this.behavior = behavior;
    }

    @Override
    public <C extends BlockEntityController> BlockEntityTicker<C> createAsyncBlockEntityTicker(CEWorld world, ImmutableBlockState blockState) {
        return createTickerHelper(WallTorchParticleBlockEntityController::tick);
    }

    public void animateTick(ImmutableBlockState state, World level, BlockPos pos) {
        Direction direction = state.get(this.behavior.facingProperty);
        Vec3d center = Vec3d.atCenterOf(pos);
        Direction opposite = direction.opposite();
        for (ParticleConfig particle : this.behavior.particles) {
            Vec3d location = new Vec3d(
                    center.x() + particle.x.getDouble(context) * opposite.stepX(),
                    center.y() + particle.y.getDouble(context),
                    center.z() + particle.z.getDouble(context) * opposite.stepZ()
            );
            level.spawnParticle(
                    location,
                    particle.particleType,
                    particle.count.getInt(context),
                    particle.xOffset.getDouble(context),
                    particle.yOffset.getDouble(context),
                    particle.zOffset.getDouble(context),
                    particle.speed.getDouble(context),
                    particle.particleData,
                    context
            );
        }
    }

    public static void tick(CEWorld ceWorld, BlockPos blockPos, ImmutableBlockState state, WallTorchParticleBlockEntityController particle) {
        particle.tickCount++;
        if (particle.tickCount % particle.behavior.tickInterval != 0) return;
        particle.animateTick(state, ceWorld.world(), blockPos);
    }
}
