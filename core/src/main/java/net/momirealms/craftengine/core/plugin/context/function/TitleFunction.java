package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.context.*;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProviders;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelectors;
import net.momirealms.craftengine.core.util.AdventureHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class TitleFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final PlayerSelector<CTX> selector;
    private final String main;
    private final String sub;
    private final NumberProvider fadeIn;
    private final NumberProvider stay;
    private final NumberProvider fadeOut;

    public TitleFunction(List<Condition<CTX>> predicates, @Nullable PlayerSelector<CTX> selector,
                         String main, String sub, NumberProvider fadeIn, NumberProvider stay, NumberProvider fadeOut) {
        super(predicates);
        this.selector = selector;
        this.main = main;
        this.sub = sub;
        this.fadeIn = fadeIn;
        this.stay = stay;
        this.fadeOut = fadeOut;
    }

    @Override
    public void runInternal(CTX ctx) {
        if (this.selector == null) {
            ctx.getOptionalParameter(DirectContextParameters.PLAYER).ifPresent(it -> it.sendTitle(
                    AdventureHelper.miniMessage().deserialize(this.main, ctx.tagResolvers()),
                    AdventureHelper.miniMessage().deserialize(this.sub, ctx.tagResolvers()),
                    this.fadeIn.getInt(ctx), this.stay.getInt(ctx), this.fadeOut.getInt(ctx)
            ));
        } else {
            for (Player viewer : this.selector.get(ctx)) {
                RelationalContext relationalContext = ViewerContext.of(ctx, PlayerOptionalContext.of(viewer));
                viewer.sendTitle(
                        AdventureHelper.miniMessage().deserialize(this.main, relationalContext.tagResolvers()),
                        AdventureHelper.miniMessage().deserialize(this.sub, relationalContext.tagResolvers()),
                        this.fadeIn.getInt(relationalContext), this.stay.getInt(relationalContext), this.fadeOut.getInt(relationalContext)
                );
            }
        }
    }

    public static <CTX extends Context> FunctionFactory<CTX, TitleFunction<CTX>> factory(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, TitleFunction<CTX>> {

        public Factory(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public TitleFunction<CTX> create(Map<String, Object> arguments) {
            String title = AdventureHelper.legacyToMiniMessage(arguments.getOrDefault("title", "").toString());
            String subtitle = AdventureHelper.legacyToMiniMessage(arguments.getOrDefault("subtitle", "").toString());
            NumberProvider fadeIn = NumberProviders.fromObject(arguments.getOrDefault("fade-in", 10));
            NumberProvider stay = NumberProviders.fromObject(arguments.getOrDefault("stay", 20));
            NumberProvider fadeOut = NumberProviders.fromObject(arguments.getOrDefault("fade-out", 5));
            return new TitleFunction<>(getPredicates(arguments), PlayerSelectors.fromObject(arguments.get("target"), conditionFactory()), title, subtitle, fadeIn, stay, fadeOut);
        }
    }
}