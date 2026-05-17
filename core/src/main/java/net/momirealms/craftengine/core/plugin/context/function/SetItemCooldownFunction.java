package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.*;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;
import net.momirealms.craftengine.core.plugin.context.text.TextProvider;
import net.momirealms.craftengine.core.plugin.context.text.TextProviders;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.TimeUtils;

import java.util.List;
import java.util.Optional;

public final class SetItemCooldownFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final PlayerSelector<CTX> selector;
    private final TextProvider time;
    private final Key id;
    private final boolean add;

    private SetItemCooldownFunction(List<Condition<CTX>> predicates,
                                    PlayerSelector<CTX> selector,
                                    boolean add,
                                    Key id,
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
                if (this.add) {
                    player.setItemCooldown(this.id, MiscUtils.ceil(millis / 50.0) + player.getItemCooldown(this.id));
                } else {
                    player.setItemCooldown(this.id, MiscUtils.ceil(millis / 50.0));
                }
            });
        } else {
            for (Player target : this.selector.get(ctx)) {
                RelationalContext relationalContext = ViewerContext.of(ctx, PlayerOptionalContext.of(target));
                long millis = TimeUtils.parseToMillis(this.time.get(relationalContext));
                if (this.add) {
                    target.setItemCooldown(this.id, MiscUtils.ceil(millis / 50.0) + target.getItemCooldown(this.id));
                } else {
                    target.setItemCooldown(this.id, MiscUtils.ceil(millis / 50.0));
                }
            }
        }
    }

    public static <CTX extends Context> FunctionFactory<CTX, SetItemCooldownFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, SetItemCooldownFunction<CTX>> {

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public SetItemCooldownFunction<CTX> create(ConfigSection section) {
            return new SetItemCooldownFunction<>(
                    getPredicates(section),
                    getPlayerSelector(section),
                    section.getBoolean("add"),
                    section.getNonNullIdentifier("id"),
                    TextProviders.fromString(section.getNonNullString("time"))
            );
        }
    }
}