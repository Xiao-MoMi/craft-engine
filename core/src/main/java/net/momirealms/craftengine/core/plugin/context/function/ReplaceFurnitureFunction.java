package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.WorldPosition;

import java.util.List;
import java.util.Optional;

public final class ReplaceFurnitureFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final Key newFurnitureId;
    private final NumberProvider x;
    private final NumberProvider y;
    private final NumberProvider z;
    private final NumberProvider pitch;
    private final NumberProvider yaw;
    private final String variant;
    private final boolean dropLoot;
    private final boolean playSound;

    private ReplaceFurnitureFunction(List<Condition<CTX>> predicates,
                                     NumberProvider x,
                                     NumberProvider y,
                                     NumberProvider z,
                                     NumberProvider pitch,
                                     NumberProvider yaw,
                                     String variant,
                                     boolean dropLoot,
                                     boolean playSound,
                                     Key newFurnitureId) {
        super(predicates);
        this.newFurnitureId = newFurnitureId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.yaw = yaw;
        this.variant = variant;
        this.dropLoot = dropLoot;
        this.playSound = playSound;
    }

    @Override
    public void runInternal(CTX ctx) {
        Optional<WorldPosition> optionalWorldPosition = ctx.getOptionalParameter(DirectContextParameters.POSITION);
        Optional<Furniture> optionalOldFurniture = ctx.getOptionalParameter(DirectContextParameters.FURNITURE);

        if (optionalWorldPosition.isPresent() && optionalOldFurniture.isPresent()) {
            Furniture oldFurniture = optionalOldFurniture.get();
            WorldPosition worldPosition = optionalWorldPosition.get();

            // Get the new position or use the current furniture position
            double xPos = this.x.getDouble(ctx);
            double yPos = this.y.getDouble(ctx);
            double zPos = this.z.getDouble(ctx);
            float pitchValue = this.pitch.getFloat(ctx);
            float yawValue = this.yaw.getFloat(ctx);
            WorldPosition newPosition = new WorldPosition(worldPosition.world(), xPos, yPos, zPos, pitchValue, yawValue);

            // Remove the old furniture
            RemoveFurnitureFunction.removeFurniture(ctx, oldFurniture, dropLoot, playSound);

            // Place the new furniture
            SpawnFurnitureFunction.spawnFurniture(this.newFurnitureId, newPosition, this.variant, this.playSound);
        }
    }

    public static <CTX extends Context> FunctionFactory<CTX, ReplaceFurnitureFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, ReplaceFurnitureFunction<CTX>> {
        private static final String[] VARIANT = new String[] {"variant", "anchor_type", "anchor-type"};
        private static final String[] DROP_LOOT = new String[] {"drop_loot", "drop-loot"};
        private static final String[] PLAY_SOUND = new String[] {"play_sound", "play-sound"};
        private static final String[] FURNITURE_ID = new String[] {"furniture_id", "furniture-id", "furniture", "id"};

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public ReplaceFurnitureFunction<CTX> create(ConfigSection section) {

            return new ReplaceFurnitureFunction<>(
                    getPredicates(section),
                    section.getNumber("x", ConfigConstants.FURNITURE_X),
                    section.getNumber("y", ConfigConstants.FURNITURE_Y),
                    section.getNumber("z", ConfigConstants.FURNITURE_Z),
                    section.getNumber("pitch", ConfigConstants.FURNITURE_PITCH),
                    section.getNumber("yaw", ConfigConstants.FURNITURE_YAW),
                    section.getNonNullString(VARIANT),
                    section.getBoolean(DROP_LOOT, true),
                    section.getBoolean(PLAY_SOUND, true),
                    section.getNonNullIdentifier(FURNITURE_ID)
            );
        }
    }
}