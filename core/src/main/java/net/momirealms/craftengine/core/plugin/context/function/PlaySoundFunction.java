package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProviders;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelectors;
import net.momirealms.craftengine.core.sound.SoundSource;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class PlaySoundFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final Key soundEvent;
    private final NumberProvider x;
    private final NumberProvider y;
    private final NumberProvider z;
    private final NumberProvider volume;
    private final NumberProvider pitch;
    private final SoundSource source;
    private final PlayerSelector<CTX> selector;

    public PlaySoundFunction(
            Key soundEvent,
            NumberProvider x,
            NumberProvider y,
            NumberProvider z,
            NumberProvider volume,
            NumberProvider pitch,
            SoundSource source,
            PlayerSelector<CTX> selector,
            List<Condition<CTX>> predicates
    ) {
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

    @Override
    public Key type() {
        return CommonFunctions.PLAY_SOUND;
    }

    public static class FactoryImpl<CTX extends Context> extends AbstractFactory<CTX> {

        public FactoryImpl(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public Function<CTX> create(Map<String, Object> arguments) {
            Key soundEvent = Key.of(ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("sound"), "warning.config.function.play_sound.missing_sound"));
            NumberProvider x = NumberProviders.fromObject(arguments.getOrDefault("x", "<arg:position.x>"));
            NumberProvider y = NumberProviders.fromObject(arguments.getOrDefault("y", "<arg:position.y>"));
            NumberProvider z = NumberProviders.fromObject(arguments.getOrDefault("z", "<arg:position.z>"));
            NumberProvider volume = NumberProviders.fromObject(arguments.getOrDefault("volume", 1));
            NumberProvider pitch = NumberProviders.fromObject(arguments.getOrDefault("pitch", 1));
            SoundSource source = Optional.ofNullable(arguments.get("source")).map(String::valueOf).map(it -> SoundSource.valueOf(it.toUpperCase(Locale.ENGLISH))).orElse(SoundSource.MASTER);
            PlayerSelector<CTX> selector = PlayerSelectors.fromObject(arguments.get("target"), conditionFactory());
            return new PlaySoundFunction<>(soundEvent, x, y, z, volume, pitch, source, selector, getPredicates(arguments));
        }
    }
}
