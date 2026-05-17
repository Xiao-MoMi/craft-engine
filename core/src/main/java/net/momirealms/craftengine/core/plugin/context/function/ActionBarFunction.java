package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.*;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;
import net.momirealms.craftengine.core.util.AdventureHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ActionBarFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final String message;
    private final PlayerSelector<CTX> selector;

    private ActionBarFunction(List<Condition<CTX>> predicates,
                              @Nullable PlayerSelector<CTX> selector,
                              String message) {
        super(predicates);
        this.message = message;
        this.selector = selector;
    }

    @Override
    public void runInternal(CTX ctx) {
        if (this.selector == null) {
            ctx.getOptionalParameter(DirectContextParameters.PLAYER).ifPresent(it -> {
                it.sendActionBar(AdventureHelper.miniMessage().deserialize(this.message, ctx.tagResolvers()));
            });
        } else {
            for (Player viewer : this.selector.get(ctx)) {
                RelationalContext relationalContext = ViewerContext.of(ctx, PlayerOptionalContext.of(viewer));
                viewer.sendActionBar(AdventureHelper.miniMessage().deserialize(this.message, relationalContext.tagResolvers()));
            }
        }
    }

    public static <CTX extends Context> FunctionFactory<CTX, ActionBarFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, ActionBarFunction<CTX>> {
        private static final String[] ACTIONBAR = new String[] {"actionbar", "message"};

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public ActionBarFunction<CTX> create(ConfigSection section) {
            return new ActionBarFunction<>(
                    getPredicates(section),
                    getPlayerSelector(section),
                    AdventureHelper.legacyToMiniMessage(section.getNonNullString(ACTIONBAR))
            );
        }
    }
}