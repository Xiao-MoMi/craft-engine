package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.block.entity.SimpleParticleBlockEntityController;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.EntityBlock;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.world.particle.ParticleConfig;

public final class SimpleParticleBlockBehavior extends BukkitBlockBehavior implements EntityBlock {
    public static final BlockBehaviorFactory<SimpleParticleBlockBehavior> FACTORY = new Factory();
    public final ParticleConfig[] particles;
    public final int tickInterval;

    private SimpleParticleBlockBehavior(BlockDefinition blockDefinition,
                                        ParticleConfig[] particles,
                                        int tickInterval) {
        super(blockDefinition);
        this.particles = particles;
        this.tickInterval = tickInterval;
    }

    @Override
    public BlockEntityController createBlockEntityController(BlockEntity entity) {
        return new SimpleParticleBlockEntityController(entity, this);
    }

    @Override
    public void initControllerId(int id) {
    }

    private static class Factory implements BlockBehaviorFactory<SimpleParticleBlockBehavior> {
        private static final String[] PARTICLES = new String[] {"particles", "particle"};
        private static final String[] TICK_INTERVAL = new String[] {"tick_interval", "tick-interval"};

        @Override
        public SimpleParticleBlockBehavior create(BlockDefinition block, ConfigSection section) {
            return new SimpleParticleBlockBehavior(
                    block,
                    section.getSectionList(PARTICLES, ParticleConfig::fromConfig$blockEntity).toArray(new ParticleConfig[0]),
                    section.getInt(TICK_INTERVAL, 10)
            );
        }
    }
}
