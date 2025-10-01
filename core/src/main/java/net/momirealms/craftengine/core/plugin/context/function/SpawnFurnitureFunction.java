package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.furniture.AnchorType;
import net.momirealms.craftengine.core.entity.furniture.FurnitureExtraData;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProviders;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SpawnFurnitureFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final Key furnitureId;
    private final NumberProvider x;
    private final NumberProvider y;
    private final NumberProvider z;
    private final NumberProvider pitch;
    private final NumberProvider yaw;
    private final AnchorType anchorType;
    private final boolean playSound;

    public SpawnFurnitureFunction(
            Key furnitureId,
            NumberProvider x,
            NumberProvider y,
            NumberProvider z,
            NumberProvider pitch,
            NumberProvider yaw,
            AnchorType anchorType,
            boolean playSound,
            List<Condition<CTX>> predicates
    ) {
        super(predicates);
        this.furnitureId = furnitureId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.yaw = yaw;
        this.anchorType = anchorType;
        this.playSound = playSound;
    }

    @Override
    public void runInternal(CTX ctx) {
        ctx.getOptionalParameter(DirectContextParameters.POSITION).ifPresent(worldPosition -> {
            World world = worldPosition.world();
            double xPos = this.x.getDouble(ctx);
            double yPos = this.y.getDouble(ctx);
            double zPos = this.z.getDouble(ctx);
            float pitchValue = this.pitch.getFloat(ctx);
            float yawValue = this.yaw.getFloat(ctx);
            WorldPosition position = new WorldPosition(world, xPos, yPos, zPos, pitchValue, yawValue);
            spawnFurniture(this.furnitureId, position, this.anchorType, this.playSound);
        });
    }

    public static void spawnFurniture(Key furnitureId, WorldPosition position, AnchorType anchorType, boolean playSound) {
        CraftEngine.instance().furnitureManager().furnitureById(furnitureId).ifPresent(furniture -> {
            AnchorType anchor = Optional.ofNullable(anchorType).orElse(furniture.getAnyAnchorType());
            FurnitureExtraData extraData = FurnitureExtraData.builder().anchorType(anchor).build();
            CraftEngine.instance().furnitureManager().place(position, furniture, extraData, playSound);
        });
    }

    @Override
    public Key type() {
        return CommonFunctions.SPAWN_FURNITURE;
    }

    public static class FactoryImpl<CTX extends Context> extends AbstractFactory<CTX> {

        public FactoryImpl(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public Function<CTX> create(Map<String, Object> arguments) {
            Key furnitureId = Key.of(ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("furniture-id"), "warning.config.function.spawn_furniture.missing_furniture_id"));
            NumberProvider x = NumberProviders.fromObject(arguments.getOrDefault("x", "<arg:position.x>"));
            NumberProvider y = NumberProviders.fromObject(arguments.getOrDefault("y", "<arg:position.y>"));
            NumberProvider z = NumberProviders.fromObject(arguments.getOrDefault("z", "<arg:position.z>"));
            NumberProvider pitch = NumberProviders.fromObject(arguments.getOrDefault("pitch", "<arg:position.pitch>"));
            NumberProvider yaw = NumberProviders.fromObject(arguments.getOrDefault("yaw", "<arg:position.yaw>"));
            AnchorType anchorType = ResourceConfigUtils.getAsEnum(arguments.get("anchor-type"), AnchorType.class, null);
            boolean playSound = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("play-sound", true), "play-sound");
            return new SpawnFurnitureFunction<>(furnitureId, x, y, z, pitch, yaw, anchorType, playSound, getPredicates(arguments));
        }
    }
}
