package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.context.*;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelectors;
import net.momirealms.craftengine.core.plugin.context.text.TextProvider;
import net.momirealms.craftengine.core.plugin.context.text.TextProviders;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.TimeUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SetCooldownFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final PlayerSelector<CTX> selector;
    private final TextProvider time;
    private final String id;
    private final boolean add;

    public SetCooldownFunction(TextProvider time, String id, boolean add, PlayerSelector<CTX> selector, List<Condition<CTX>> predicates) {
        super(predicates);
        this.time = time;
        this.add = add;
        this.selector = selector;
        this.id = id;
    }

    @Override
    public void runInternal(CTX ctx) {
        if (this.selector == null) {
            Optional<Player> optionalPlayer = ctx.getOptionalParameter(DirectContextParameters.PLAYER);
            optionalPlayer.ifPresent(player -> {
                long millis = TimeUtils.parseToMillis(this.time.get(ctx));
                CooldownData data = player.cooldown();
                if (this.add) data.addCooldown(this.id, millis);
                else data.setCooldown(this.id, millis);
            });
        } else {
            for (Player target : this.selector.get(ctx)) {
                RelationalContext relationalContext = ViewerContext.of(ctx, PlayerOptionalContext.of(target, ContextHolder.EMPTY));
                long millis = TimeUtils.parseToMillis(this.time.get(relationalContext));
                CooldownData data = target.cooldown();
                if (this.add) data.addCooldown(this.id, millis);
                else data.setCooldown(this.id, millis);
            }
        }
    }

    @Override
    public Key type() {
        return CommonFunctions.SET_COOLDOWN;
    }

    public static class FactoryImpl<CTX extends Context> extends AbstractFactory<CTX> {

        public FactoryImpl(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public Function<CTX> create(Map<String, Object> arguments) {
            String id = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("id"), "warning.config.function.set_cooldown.missing_id");
            String time = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("time"), "warning.config.function.set_cooldown.missing_time");
            boolean add = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("add", false), "add");
            return new SetCooldownFunction<>(TextProviders.fromString(time), id, add, PlayerSelectors.fromObject(arguments.get("target"), conditionFactory()), getPredicates(arguments));
        }
    }
}
