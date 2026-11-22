package net.momirealms.craftengine.bukkit.entity.furniture.behavior;

import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.FurnitureDefinition;
import net.momirealms.craftengine.core.entity.furniture.FurniturePlacement;
import net.momirealms.craftengine.core.entity.furniture.FurnitureVariant;
import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureBehaviorFactory;
import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureBehaviorTemplate;
import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureController;
import net.momirealms.craftengine.core.entity.furniture.tick.FurnitureTicker;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.particle.ParticleConfig;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class SimpleParticleFurnitureBehaviorTemplate extends FurnitureBehaviorTemplate {
    public static final FurnitureBehaviorFactory<SimpleParticleFurnitureBehaviorTemplate> FACTORY = new Factory();
    private static final Emitter[] EMPTY = new Emitter[0];
    private final Emitter[] defaultParticles;
    private final Map<String, Emitter[]> variants;

    private SimpleParticleFurnitureBehaviorTemplate(FurnitureDefinition furniture, Emitter[] defaultParticles, Map<String, Emitter[]> variants) {
        super(furniture);
        this.defaultParticles = defaultParticles;
        this.variants = variants;
    }

    @Override
    public FurnitureController createController(Furniture furniture) {
        return new Controller(furniture, this);
    }

    private record Emitter(ParticleConfig particle, NumberProvider firstTick, NumberProvider tickInterval) {
    }

    private record Playback(Emitter[] particles, int generation) {
    }

    private static final class Controller extends FurnitureController {
        private static final FurnitureTicker<Controller> TICKER = (furniture, controller) -> controller.tick();
        private final SimpleParticleFurnitureBehaviorTemplate behavior;
        private volatile @Nullable Playback playback;
        private int publishedGeneration;
        private int activeGeneration;
        private long[] scheduledTicks = new long[0];
        private long tick;
        private long nextTick = Long.MAX_VALUE;

        private Controller(Furniture furniture, SimpleParticleFurnitureBehaviorTemplate behavior) {
            super(furniture);
            this.behavior = behavior;
        }

        @Override
        public <T extends FurnitureController> FurnitureTicker<T> createAsyncFurnitureTicker() {
            return createTickerHelper(TICKER);
        }

        @Override
        public void onLoad() {
            this.publishPlayback();
        }

        @Override
        public void onUnload() {
            this.playback = null;
        }

        @Override
        public void onVariantChange(FurnitureVariant previousVariant) {
            this.publishPlayback();
        }

        private void publishPlayback() {
            Emitter[] particles = this.behavior.variants.getOrDefault(super.furniture.currentVariant().name(), this.behavior.defaultParticles);
            if (particles.length == 0) {
                this.playback = null;
                return;
            }
            Playback previous = this.playback;
            if (previous != null && previous.particles() == particles) {
                return;
            }
            this.playback = new Playback(particles, ++this.publishedGeneration);
        }

        private void resetSchedule(Emitter[] particles) {
            this.tick = 0;
            this.nextTick = Long.MAX_VALUE;
            if (this.scheduledTicks.length < particles.length) {
                this.scheduledTicks = new long[particles.length];
            }
            for (int i = 0; i < particles.length; i++) {
                int firstTick = Math.max(1, particles[i].firstTick().getInt());
                this.scheduledTicks[i] = firstTick;
                this.nextTick = Math.min(this.nextTick, firstTick);
            }
        }

        private void tick() {
            Playback current = this.playback;
            if (current == null) {
                return;
            }
            Emitter[] particles = current.particles();
            if (this.activeGeneration != current.generation()) {
                this.activeGeneration = current.generation();
                this.resetSchedule(particles);
            }
            long now = ++this.tick;
            if (now < this.nextTick) {
                return;
            }
            FurniturePlacement placement = super.furniture.placement();
            World world = placement.origin.world;
            long next = Long.MAX_VALUE;
            for (int i = 0; i < particles.length; i++) {
                long scheduled = this.scheduledTicks[i];
                if (scheduled <= now) {
                    Emitter emitter = particles[i];
                    ParticleConfig particle = emitter.particle();
                    Vec3d position = placement.relativePosition(particle.x.getFloat(), particle.y.getFloat(), particle.z.getFloat());
                    if (this.playback != current) {
                        return;
                    }
                    world.spawnParticle(
                            position,
                            particle.particleType,
                            particle.count.getInt(),
                            particle.xOffset.getDouble(),
                            particle.yOffset.getDouble(),
                            particle.zOffset.getDouble(),
                            particle.speed.getDouble(),
                            particle.particleData
                    );
                    scheduled = now + Math.max(1, emitter.tickInterval().getInt());
                    this.scheduledTicks[i] = scheduled;
                }
                next = Math.min(next, scheduled);
            }
            this.nextTick = next;
        }
    }

    private static final class Factory implements FurnitureBehaviorFactory<SimpleParticleFurnitureBehaviorTemplate> {
        private final static String[] TICK_DELAY = ConfigKeys.of("tick_delay");
        private final static String[] TICK_INTERVAL = ConfigKeys.of("tick_interval");

        @Override
        public SimpleParticleFurnitureBehaviorTemplate create(FurnitureDefinition furniture, ConfigSection section) {
            NumberProvider interval = section.getNumber(TICK_INTERVAL, ConfigConstants.CONSTANT_TEN);
            Emitter[] defaults = this.particles(section, "particles", interval);
            ConfigSection variantsSection = section.getSection("variants");
            Map<String, Emitter[]> variants = new HashMap<>();
            if (variantsSection != null) {
                for (String name : variantsSection.keySet()) {
                    variants.put(name, this.particles(variantsSection, name, interval));
                }
            }
            return new SimpleParticleFurnitureBehaviorTemplate(furniture, defaults, variants);
        }

        private Emitter[] particles(ConfigSection section, String key, NumberProvider defaultInterval) {
            return section.getSectionList(key, particle -> {
                NumberProvider interval = section.getNumber(TICK_INTERVAL, defaultInterval);
                NumberProvider delay = particle.getNumber(TICK_DELAY, interval);
                return new Emitter(
                        ParticleConfig.fromConfig$blockEntity(particle),
                        delay,
                        interval
                );
            }).toArray(EMPTY);
        }
    }
}