package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.context.*;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProviders;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelectors;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.List;
import java.util.Map;

public class PotionEffectFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final PlayerSelector<CTX> selector;
    private final Key potionEffectType;
    private final NumberProvider duration;
    private final NumberProvider amplifier;
    private final boolean ambient;
    private final boolean particles;

    public PotionEffectFunction(Key potionEffectType, NumberProvider duration, NumberProvider amplifier, boolean ambient, boolean particles, PlayerSelector<CTX> selector, List<Condition<CTX>> predicates) {
        super(predicates);
        this.potionEffectType = potionEffectType;
        this.duration = duration;
        this.amplifier = amplifier;
        this.selector = selector;
        this.ambient = ambient;
        this.particles = particles;
    }

    @Override
    public void runInternal(CTX ctx) {
        if (this.selector == null) {
            ctx.getOptionalParameter(DirectContextParameters.PLAYER).ifPresent(it -> {
                it.addPotionEffect(this.potionEffectType, this.duration.getInt(ctx), this.amplifier.getInt(ctx), this.ambient, this.particles);
            });
        } else {
            for (Player target : this.selector.get(ctx)) {
                RelationalContext relationalContext = ViewerContext.of(ctx, PlayerOptionalContext.of(target, ContextHolder.EMPTY));
                target.addPotionEffect(this.potionEffectType, this.duration.getInt(relationalContext), this.amplifier.getInt(relationalContext), this.ambient, this.particles);
            }
        }
    }

    @Override
    public Key type() {
        return CommonFunctions.POTION_EFFECT;
    }

    public static class FactoryImpl<CTX extends Context> extends AbstractFactory<CTX> {

        public FactoryImpl(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public Function<CTX> create(Map<String, Object> arguments) {
            Key effectType = Key.of(ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("potion-effect"), "warning.config.function.potion_effect.missing_potion_effect"));
            NumberProvider duration = NumberProviders.fromObject(arguments.getOrDefault("duration", 20));
            NumberProvider amplifier = NumberProviders.fromObject(arguments.getOrDefault("amplifier", 0));
            boolean ambient = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("ambient", false), "ambient");
            boolean particles = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("particles", true), "particles");
            return new PotionEffectFunction<>(effectType, duration, amplifier, ambient, particles, PlayerSelectors.fromObject(arguments.get("target"), conditionFactory()), getPredicates(arguments));
        }
    }
}
