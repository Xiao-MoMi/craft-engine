package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProviders;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Color;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.Position;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.particle.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class ParticleFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    public static final Map<Key, java.util.function.Function<Map<String, Object>, ParticleData>> DATA_TYPES = new HashMap<>();

    static {
        registerParticleData(map -> new BlockStateData(
                        LazyReference.lazyReference(new Supplier<>() {
                            final String blockState = ResourceConfigUtils.requireNonEmptyStringOrThrow(map.get("block-state"), "warning.config.function.particle.missing_block_state");
                            @Override
                            public BlockStateWrapper get() {
                                return CraftEngine.instance().blockManager().createPackedBlockState(this.blockState);
                            }
                        })),
                ParticleTypes.BLOCK, ParticleTypes.FALLING_DUST, ParticleTypes.DUST_PILLAR, ParticleTypes.BLOCK_CRUMBLE, ParticleTypes.BLOCK_MARKER);
        registerParticleData(map -> new ColorData(
                        Color.fromString(ResourceConfigUtils.requireNonEmptyStringOrThrow(map.get("color"), "warning.config.function.particle.missing_color").split(","))),
                ParticleTypes.ENTITY_EFFECT, ParticleTypes.TINTED_LEAVES);
        registerParticleData(map -> new JavaTypeData(
                        ResourceConfigUtils.getAsFloat(map.get("charge"), "charge")),
                ParticleTypes.SCULK_CHARGE);
        registerParticleData(map -> new JavaTypeData(
                        ResourceConfigUtils.getAsInt(map.get("shriek"), "shriek")),
                ParticleTypes.SHRIEK);
        registerParticleData(map -> new DustData(
                        Color.fromString(ResourceConfigUtils.requireNonEmptyStringOrThrow(map.get("color"), "warning.config.function.particle.missing_color").split(",")),
                        ResourceConfigUtils.getAsFloat(map.getOrDefault("scale", 1), "scale")),
                ParticleTypes.DUST);
        registerParticleData(map -> new DustTransitionData(
                        Color.fromString(ResourceConfigUtils.requireNonEmptyStringOrThrow(map.get("from"), "warning.config.function.particle.missing_from").split(",")),
                        Color.fromString(ResourceConfigUtils.requireNonEmptyStringOrThrow(map.get("to"), "warning.config.function.particle.missing_to").split(",")),
                        ResourceConfigUtils.getAsFloat(map.getOrDefault("scale", 1), "scale")),
                ParticleTypes.DUST_COLOR_TRANSITION);
        registerParticleData(map -> new ItemStackData(
                        LazyReference.lazyReference(new Supplier<>() {
                            final Key itemId = Key.of(ResourceConfigUtils.requireNonEmptyStringOrThrow(map.get("item"), "warning.config.function.particle.missing_item"));
                            @Override
                            public Item<?> get() {
                                return CraftEngine.instance().itemManager().createWrappedItem(this.itemId, null);
                            }
                        })
                ),
                ParticleTypes.ITEM);
        registerParticleData(map -> new VibrationData(
                        NumberProviders.fromObject(map.getOrDefault("target-x", 0)),
                        NumberProviders.fromObject(map.getOrDefault("target-y", 0)),
                        NumberProviders.fromObject(map.getOrDefault("target-z", 0)),
                        NumberProviders.fromObject(map.getOrDefault("arrival-time", 10))),
                ParticleTypes.VIBRATION);
        registerParticleData(map -> new TrailData(
                        NumberProviders.fromObject(map.getOrDefault("target-x", 0)),
                        NumberProviders.fromObject(map.getOrDefault("target-y", 0)),
                        NumberProviders.fromObject(map.getOrDefault("target-z", 0)),
                        Color.fromString(ResourceConfigUtils.requireNonEmptyStringOrThrow(map.get("color"), "warning.config.function.particle.missing_color").split(",")),
                        NumberProviders.fromObject(map.getOrDefault("duration", 10))),
                ParticleTypes.TRAIL);
    }

    public static void registerParticleData(java.util.function.Function<Map<String, Object>, ParticleData> function, Key... types) {
        for (Key type : types) {
            DATA_TYPES.put(type, function);
        }
    }

    private final Key particleType;
    private final NumberProvider x;
    private final NumberProvider y;
    private final NumberProvider z;
    private final NumberProvider count;
    private final NumberProvider xOffset;
    private final NumberProvider yOffset;
    private final NumberProvider zOffset;
    private final NumberProvider speed;
    private final ParticleData particleData;

    public ParticleFunction(Key particleType, NumberProvider x, NumberProvider y, NumberProvider z, NumberProvider count,
                            NumberProvider xOffset, NumberProvider yOffset, NumberProvider zOffset, NumberProvider speed, ParticleData particleData, List<Condition<CTX>> predicates) {
        super(predicates);
        this.particleType = particleType;
        this.count = count;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.zOffset = zOffset;
        this.speed = speed;
        this.x = x;
        this.y = y;
        this.z = z;
        this.particleData = particleData;
    }

    @Override
    public void runInternal(CTX ctx) {
        Optional<WorldPosition> optionalWorldPosition = ctx.getOptionalParameter(DirectContextParameters.POSITION);
        if (optionalWorldPosition.isPresent()) {
            World world = optionalWorldPosition.get().world();
            Position position = new Vec3d(this.x.getDouble(ctx), this.y.getDouble(ctx), this.z.getDouble(ctx));
            world.spawnParticle(position, this.particleType, this.count.getInt(ctx), this.xOffset.getDouble(ctx), this.yOffset.getDouble(ctx), this.zOffset.getDouble(ctx), this.speed.getDouble(ctx), this.particleData, ctx);
        }
    }

    @Override
    public Key type() {
        return CommonFunctions.PARTICLE;
    }

    public static class FactoryImpl<CTX extends Context> extends AbstractFactory<CTX> {

        public FactoryImpl(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public Function<CTX> create(Map<String, Object> arguments) {
            Key particleType = Key.of(ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("particle"), "warning.config.function.particle.missing_particle"));
            NumberProvider x = NumberProviders.fromObject(arguments.getOrDefault("x", "<arg:position.x>"));
            NumberProvider y = NumberProviders.fromObject(arguments.getOrDefault("y", "<arg:position.y>"));
            NumberProvider z = NumberProviders.fromObject(arguments.getOrDefault("z", "<arg:position.z>"));
            NumberProvider count = NumberProviders.fromObject(arguments.getOrDefault("count", 1));
            NumberProvider xOffset = NumberProviders.fromObject(arguments.getOrDefault("offset-x", 0));
            NumberProvider yOffset = NumberProviders.fromObject(arguments.getOrDefault("offset-y", 0));
            NumberProvider zOffset = NumberProviders.fromObject(arguments.getOrDefault("offset-z", 0));
            NumberProvider speed = NumberProviders.fromObject(arguments.getOrDefault("speed", 0));
            return new ParticleFunction<>(particleType, x, y, z, count, xOffset, yOffset, zOffset, speed,
                    Optional.ofNullable(ParticleFunction.DATA_TYPES.get(particleType)).map(it -> it.apply(arguments)).orElse(null), getPredicates(arguments));
        }
    }
}
