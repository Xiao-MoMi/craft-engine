package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.context.*;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelectors;
import net.momirealms.craftengine.core.plugin.context.text.TextProvider;
import net.momirealms.craftengine.core.plugin.context.text.TextProviders;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ActionBarFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final TextProvider message;
    private final PlayerSelector<CTX> selector;

    public ActionBarFunction(List<Condition<CTX>> predicates, @Nullable PlayerSelector<CTX> selector, TextProvider messages) {
        super(predicates);
        this.message = messages;
        this.selector = selector;
    }

    @Override
    public void runInternal(CTX ctx) {
        Optional<Player> owner = ctx.getOptionalParameter(DirectContextParameters.PLAYER);
        if (this.selector == null) {
            owner.ifPresent(it -> {
                it.sendActionBar(AdventureHelper.miniMessage().deserialize(this.message.get(ctx), ctx.tagResolvers()));
            });
        } else {
            for (Player viewer : this.selector.get(ctx)) {
                RelationalContext relationalContext = ViewerContext.of(ctx, PlayerOptionalContext.of(viewer, ContextHolder.EMPTY));
                viewer.sendActionBar(AdventureHelper.miniMessage().deserialize(this.message.get(relationalContext), relationalContext.tagResolvers()));
            }
        }
    }

    @Override
    public Key type() {
        return CommonFunctions.ACTIONBAR;
    }

    public static class FactoryImpl<CTX extends Context> extends AbstractFactory<CTX> {

        public FactoryImpl(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public Function<CTX> create(Map<String, Object> arguments) {
            String message = ResourceConfigUtils.requireNonEmptyStringOrThrow(ResourceConfigUtils.get(arguments, "actionbar", "message"), "warning.config.function.actionbar.missing_actionbar");
            return new ActionBarFunction<>(getPredicates(arguments), PlayerSelectors.fromObject(arguments.get("target"), conditionFactory()), TextProviders.fromString(message));
        }
    }
}
