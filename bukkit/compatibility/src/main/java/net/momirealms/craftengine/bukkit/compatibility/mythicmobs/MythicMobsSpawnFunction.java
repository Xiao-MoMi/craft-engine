package net.momirealms.craftengine.bukkit.compatibility.mythicmobs;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.function.AbstractConditionalFunction;
import net.momirealms.craftengine.core.plugin.context.function.FunctionFactory;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.text.TextProvider;
import net.momirealms.craftengine.core.plugin.context.text.TextProviders;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class MythicMobsSpawnFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final TextProvider mobId;
    @Nullable
    private final NumberProvider level;
    @Nullable
    private final TextProvider world;
    private final NumberProvider x;
    private final NumberProvider y;
    private final NumberProvider z;
    private final NumberProvider pitch;
    private final NumberProvider yaw;

    private MythicMobsSpawnFunction(List<Condition<CTX>> predicates,
                                    TextProvider mobId,
                                    @Nullable NumberProvider level,
                                    @Nullable TextProvider world,
                                    NumberProvider x,
                                    NumberProvider y,
                                    NumberProvider z,
                                    NumberProvider pitch,
                                    NumberProvider yaw
    ) {
        super(predicates);
        this.mobId = mobId;
        this.level = level;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.yaw = yaw;
    }

    @Override
    protected void runInternal(CTX ctx) {
        WorldPosition worldPosition = new WorldPosition(
                Optional.ofNullable(this.world)
                        .map(w -> w.get(ctx))
                        .map(w -> CraftEngine.instance().platform().getWorld(w))
                        .orElseGet(() -> {
                            Optional<WorldPosition> worldPositionOpt = ctx.getOptionalParameter(DirectContextParameters.POSITION);
                            if (worldPositionOpt.isPresent()) {
                                return worldPositionOpt.get().world();
                            }
                            Optional<Player> playerOptionalOpt = ctx.getOptionalParameter(DirectContextParameters.PLAYER);
                            if (playerOptionalOpt.isPresent()) {
                                return playerOptionalOpt.get().world();
                            }
                            throw new IllegalArgumentException("Cann't found world parameter.");
                        }),
                this.x.getDouble(ctx),
                this.y.getDouble(ctx),
                this.z.getDouble(ctx),
                this.pitch.getFloat(ctx),
                this.yaw.getFloat(ctx)
        );
        double level = this.level == null ? 1.0 : this.level.getDouble(ctx);
        MythicMobsHelper.summonMob(this.mobId.get(ctx), worldPosition, Math.max(level, 1.0));
    }

    public static <CTX extends Context> FunctionFactory<CTX, MythicMobsSpawnFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, MythicMobsSpawnFunction<CTX>> {

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public MythicMobsSpawnFunction<CTX> create(ConfigSection section) {
            return new MythicMobsSpawnFunction<>(
                    getPredicates(section),
                    section.getNonNullValue("mob", ConfigConstants.ARGUMENT_STRING, v -> TextProviders.fromString(v.getAsString())),
                    section.getNumber("level"),
                    section.getValue("world", v -> TextProviders.fromString(v.getAsString())),
                    section.getNumber("x", ConfigConstants.POSITION_X),
                    section.getNumber("y", ConfigConstants.POSITION_Y),
                    section.getNumber("z", ConfigConstants.POSITION_Z),
                    section.getNumber("pitch", ConfigConstants.POSITION_PITCH),
                    section.getNumber("yaw", ConfigConstants.POSITION_YAW)
            );
        }
    }
}