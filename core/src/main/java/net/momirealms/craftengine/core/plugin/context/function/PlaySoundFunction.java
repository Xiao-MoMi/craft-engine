package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;
import net.momirealms.craftengine.core.sound.SoundSource;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;

import java.util.List;
import java.util.Optional;

public final class PlaySoundFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final Key soundEvent;
    private final NumberProvider x;
    private final NumberProvider y;
    private final NumberProvider z;
    private final NumberProvider volume;
    private final NumberProvider pitch;
    private final SoundSource source;
    private final PlayerSelector<CTX> selector;

    private PlaySoundFunction(List<Condition<CTX>> predicates,
                              PlayerSelector<CTX> selector,
                              NumberProvider x,
                              NumberProvider y,
                              NumberProvider z,
                              NumberProvider volume,
                              NumberProvider pitch,
                              SoundSource source,
                              Key soundEvent) {
        super(predicates);
        this.soundEvent = soundEvent;
        this.x = x;
        this.y = y;
        this.z = z;
        this.volume = volume;
        this.pitch = pitch;
        this.source = source;
        this.selector = selector;
    }

    @Override
    public void runInternal(CTX ctx) {
        if (this.selector == null) {
            Optional<WorldPosition> optionalWorldPosition = ctx.getOptionalParameter(DirectContextParameters.POSITION);
            if (optionalWorldPosition.isPresent()) {
                World world = optionalWorldPosition.get().world();
                world.playSound(new Vec3d(this.x.getDouble(ctx), this.y.getDouble(ctx), this.z.getDouble(ctx)),
                        this.soundEvent, this.volume.getFloat(ctx), this.pitch.getFloat(ctx), this.source);
            }
        } else {
            for (Player player : selector.get(ctx)) {
                player.playSound(this.soundEvent, this.source, this.volume.getFloat(ctx), this.pitch.getFloat(ctx));
            }
        }
    }

    public static <CTX extends Context> FunctionFactory<CTX, PlaySoundFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, PlaySoundFunction<CTX>> {

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public PlaySoundFunction<CTX> create(ConfigSection section) {
            return new PlaySoundFunction<>(
                    getPredicates(section),
                    getPlayerSelector(section),
                    section.getNumber("x", ConfigConstants.POSITION_X),
                    section.getNumber("y", ConfigConstants.POSITION_Y),
                    section.getNumber("z", ConfigConstants.POSITION_Z),
                    section.getNumber("volume", ConfigConstants.CONSTANT_ONE),
                    section.getNumber("pitch", ConfigConstants.CONSTANT_ONE),
                    section.getEnum("source", SoundSource.class, SoundSource.MASTER),
                    section.getNonNullIdentifier("sound")
            );
        }
    }
}