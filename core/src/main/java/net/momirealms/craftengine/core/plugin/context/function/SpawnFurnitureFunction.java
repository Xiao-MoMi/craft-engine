package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.FurnitureDataAccessor;
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
    private final String variant;
    private final boolean playSound;

    public SpawnFurnitureFunction(
            List<Condition<CTX>> predicates, NumberProvider x, NumberProvider y, NumberProvider z, NumberProvider pitch, NumberProvider yaw, String variant, boolean playSound, Key furnitureId
    ) {
        super(predicates);
        this.furnitureId = furnitureId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.yaw = yaw;
        this.variant = variant;
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
            spawnFurniture(this.furnitureId, position, this.variant, this.playSound);
        });
    }

    public static void spawnFurniture(Key furnitureId, WorldPosition position, String variant, boolean playSound) {
        CraftEngine.instance().furnitureManager().furnitureById(furnitureId).ifPresent(furniture -> CraftEngine.instance().furnitureManager().place(position, furniture, FurnitureDataAccessor.ofVariant(Optional.ofNullable(variant).orElseGet(furniture::anyVariantName)), playSound));
    }

    public static <CTX extends Context> FunctionFactory<CTX, SpawnFurnitureFunction<CTX>> factory(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, SpawnFurnitureFunction<CTX>> {

        public Factory(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public SpawnFurnitureFunction<CTX> create(Map<String, Object> arguments) {
            Key furnitureId = Key.of(ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("furniture-id"), "warning.config.function.spawn_furniture.missing_furniture_id"));
            NumberProvider x = NumberProviders.fromObject(arguments.getOrDefault("x", "<arg:position.x>"));
            NumberProvider y = NumberProviders.fromObject(arguments.getOrDefault("y", "<arg:position.y>"));
            NumberProvider z = NumberProviders.fromObject(arguments.getOrDefault("z", "<arg:position.z>"));
            NumberProvider pitch = NumberProviders.fromObject(arguments.getOrDefault("pitch", "<arg:position.pitch>"));
            NumberProvider yaw = NumberProviders.fromObject(arguments.getOrDefault("yaw", "<arg:position.yaw>"));
            String variant = ResourceConfigUtils.getAsStringOrNull(ResourceConfigUtils.get(arguments, "variant", "anchor-type"));
            boolean playSound = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("play-sound", true), "play-sound");
            return new SpawnFurnitureFunction<>(getPredicates(arguments), x, y, z, pitch, yaw, variant, playSound, furnitureId);
        }
    }
}