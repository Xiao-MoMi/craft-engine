package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.block.entity.WallTorchParticleBlockEntityController;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.EntityBlock;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.world.particle.ParticleConfig;

public final class WallTorchParticleBlockBehavior extends BukkitBlockBehavior implements EntityBlock {
    public static final BlockBehaviorFactory<WallTorchParticleBlockBehavior> FACTORY = new Factory();
    public final ParticleConfig[] particles;
    public final int tickInterval;
    public final Property<Direction> facingProperty;

    private WallTorchParticleBlockBehavior(BlockDefinition blockDefinition,
                                           ParticleConfig[] particles,
                                           int tickInterval,
                                           Property<Direction> facingProperty) {
        super(blockDefinition);
        this.particles = particles;
        this.tickInterval = tickInterval;
        this.facingProperty = facingProperty;
    }

    @Override
    public BlockEntityController createBlockEntityController(BlockEntity blockEntity) {
        return new WallTorchParticleBlockEntityController(blockEntity, this);
    }

    @Override
    public void initControllerId(int id) {
    }

    private static class Factory implements BlockBehaviorFactory<WallTorchParticleBlockBehavior> {
        private static final String[] PARTICLES = new String[] {"particles", "particle"};
        private static final String[] TICK_INTERVAL = new String[] {"tick_interval", "tick-interval"};

        @Override
        public WallTorchParticleBlockBehavior create(BlockDefinition block, ConfigSection section) {
            return new WallTorchParticleBlockBehavior(
                    block,
                    section.getSectionList(PARTICLES, ParticleConfig::fromConfig$blockEntity).toArray(new ParticleConfig[0]),
                    section.getInt(TICK_INTERVAL, 10),
                    BlockBehaviorFactory.getProperty(section.path(), block, "facing", Direction.class)
            );
        }
    }
}
