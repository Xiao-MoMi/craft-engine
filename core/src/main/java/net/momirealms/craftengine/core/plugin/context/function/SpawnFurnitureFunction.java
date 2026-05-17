package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.furniture.FurniturePersistentData;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;

import java.util.List;
import java.util.Optional;

public final class SpawnFurnitureFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final Key furnitureId;
    private final NumberProvider x;
    private final NumberProvider y;
    private final NumberProvider z;
    private final NumberProvider pitch;
    private final NumberProvider yaw;
    private final String variant;
    private final boolean playSound;

    private SpawnFurnitureFunction(
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
        CraftEngine.instance().furnitureManager().furnitureById(furnitureId).ifPresent(furniture -> CraftEngine.instance().furnitureManager().place(position, furniture, FurniturePersistentData.ofVariant(Optional.ofNullable(variant).orElseGet(furniture::anyVariantName)), playSound));
    }

    public static <CTX extends Context> FunctionFactory<CTX, SpawnFurnitureFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, SpawnFurnitureFunction<CTX>> {
        private static final String[] VARIANT = new String[]{"variant", "anchor_type", "anchor-type"};
        private static final String[] PLAY_SOUND = new String[]{"play_sound", "play-sound"};
        private static final String[] FURNITURE_ID = new String[]{"furniture_id", "furniture-id", "furniture", "id"};

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public SpawnFurnitureFunction<CTX> create(ConfigSection section) {
            return new SpawnFurnitureFunction<>(
                    getPredicates(section),
                    section.getNumber("x", ConfigConstants.POSITION_X),
                    section.getNumber("y", ConfigConstants.POSITION_Y),
                    section.getNumber("z", ConfigConstants.POSITION_Z),
                    section.getNumber("pitch", ConfigConstants.POSITION_PITCH),
                    section.getNumber("yaw", ConfigConstants.POSITION_YAW),
                    section.getNonNullString(VARIANT),
                    section.getBoolean(PLAY_SOUND, true),
                    section.getNonNullIdentifier(FURNITURE_ID)
            );
        }
    }
}