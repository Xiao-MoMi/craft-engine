package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.*;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;
import net.momirealms.craftengine.core.plugin.context.text.TextProvider;
import net.momirealms.craftengine.core.plugin.context.text.TextProviders;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class TeleportFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final PlayerSelector<CTX> selector;
    @Nullable
    private final TextProvider world;
    private final NumberProvider x;
    private final NumberProvider y;
    private final NumberProvider z;
    private final NumberProvider pitch;
    private final NumberProvider yaw;

    private TeleportFunction(List<Condition<CTX>> predicates,
                             @Nullable PlayerSelector<CTX> selector,
                             @Nullable TextProvider world,
                             NumberProvider x,
                             NumberProvider y,
                             NumberProvider z,
                             NumberProvider pitch,
                             NumberProvider yaw) {
        super(predicates);
        this.selector = selector;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.yaw = yaw;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public void runInternal(CTX ctx) {
        if (this.selector == null) {
            ctx.getOptionalParameter(DirectContextParameters.PLAYER).ifPresent(it -> it.teleport(new WorldPosition(
                    Optional.ofNullable(this.world).map(w -> w.get(ctx)).map(w -> CraftEngine.instance().platform().getWorld(w)).orElse(it.world()),
                    this.x.getDouble(ctx),
                    this.y.getDouble(ctx),
                    this.z.getDouble(ctx),
                    this.pitch.getFloat(ctx),
                    this.yaw.getFloat(ctx))
            ));
        } else {
            for (Player viewer : this.selector.get(ctx)) {
                RelationalContext relationalContext = ViewerContext.of(ctx, PlayerOptionalContext.of(viewer));
                viewer.teleport(new WorldPosition(
                        Optional.ofNullable(this.world).map(w -> w.get(relationalContext)).map(w -> CraftEngine.instance().platform().getWorld(w)).orElse(viewer.world()),
                        this.x.getDouble(relationalContext),
                        this.y.getDouble(relationalContext),
                        this.z.getDouble(relationalContext),
                        this.pitch.getFloat(relationalContext),
                        this.yaw.getFloat(relationalContext))
                );
            }
        }
    }

    public static <CTX extends Context> FunctionFactory<CTX, TeleportFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, TeleportFunction<CTX>> {

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public TeleportFunction<CTX> create(ConfigSection section) {
            return new TeleportFunction<>(
                    getPredicates(section),
                    getPlayerSelector(section),
                    section.getValue("world", v -> TextProviders.fromString(v.getAsString())),
                    section.getNonNullNumber("x"),
                    section.getNonNullNumber("y"),
                    section.getNonNullNumber("z"),
                    section.getNumber("pitch", ConfigConstants.POSITION_PITCH),
                    section.getNumber("yaw", ConfigConstants.POSITION_YAW)
            );
        }
    }
}