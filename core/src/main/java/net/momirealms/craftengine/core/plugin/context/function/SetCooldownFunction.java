package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.*;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;
import net.momirealms.craftengine.core.plugin.context.text.TextProvider;
import net.momirealms.craftengine.core.plugin.context.text.TextProviders;
import net.momirealms.craftengine.core.util.TimeUtils;

import java.util.List;
import java.util.Optional;

public final class SetCooldownFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final PlayerSelector<CTX> selector;
    private final TextProvider time;
    private final String id;
    private final boolean add;

    private SetCooldownFunction(List<Condition<CTX>> predicates,
                                PlayerSelector<CTX> selector,
                                boolean add,
                                String id,
                                TextProvider time) {
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
                RelationalContext relationalContext = ViewerContext.of(ctx, PlayerOptionalContext.of(target));
                long millis = TimeUtils.parseToMillis(this.time.get(relationalContext));
                CooldownData data = target.cooldown();
                if (this.add) data.addCooldown(this.id, millis);
                else data.setCooldown(this.id, millis);
            }
        }
    }

    public static <CTX extends Context> FunctionFactory<CTX, SetCooldownFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, SetCooldownFunction<CTX>> {

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public SetCooldownFunction<CTX> create(ConfigSection section) {
            return new SetCooldownFunction<>(
                    getPredicates(section),
                    getPlayerSelector(section),
                    section.getBoolean("add"),
                    section.getNonNullString("id"),
                    TextProviders.fromString(section.getNonNullString("time"))
            );
        }
    }
}