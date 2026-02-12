package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.context.*;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelectors;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class ActionBarFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final String message;
    private final PlayerSelector<CTX> selector;

    public ActionBarFunction(List<Condition<CTX>> predicates, @Nullable PlayerSelector<CTX> selector, String message) {
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

    public static <CTX extends Context> FunctionFactory<CTX, ActionBarFunction<CTX>> factory(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, ActionBarFunction<CTX>> {

        public Factory(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public ActionBarFunction<CTX> create(Map<String, Object> arguments) {
            String message = ResourceConfigUtils.requireNonEmptyStringOrThrow(ResourceConfigUtils.get(arguments, "actionbar", "message"), "warning.config.function.actionbar.missing_actionbar");
            return new ActionBarFunction<>(getPredicates(arguments), PlayerSelectors.fromObject(arguments.get("target"), conditionFactory()), AdventureHelper.legacyToMiniMessage(message));
        }
    }
}