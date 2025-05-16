package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.context.*;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProviders;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelectors;
import net.momirealms.craftengine.core.plugin.context.text.TextProvider;
import net.momirealms.craftengine.core.plugin.context.text.TextProviders;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Key;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TitleFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final PlayerSelector<CTX> selector;
    private final TextProvider main;
    private final TextProvider sub;
    private final NumberProvider fadeIn;
    private final NumberProvider stay;
    private final NumberProvider fadeOut;

    public TitleFunction(List<Condition<CTX>> predicates, @Nullable PlayerSelector<CTX> selector,
                         TextProvider main, TextProvider sub, NumberProvider fadeIn, NumberProvider stay, NumberProvider fadeOut) {
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
        Optional<Player> owner = ctx.getOptionalParameter(DirectContextParameters.PLAYER);
        if (this.selector == null) {
            owner.ifPresent(it -> it.sendTitle(
                    AdventureHelper.miniMessage().deserialize(this.main.get(ctx), ctx.tagResolvers()),
                    AdventureHelper.miniMessage().deserialize(this.sub.get(ctx), ctx.tagResolvers()),
                    this.fadeIn.getInt(ctx), this.stay.getInt(ctx), this.fadeOut.getInt(ctx)
            ));
        } else {
            for (Player viewer : this.selector.get(ctx)) {
                RelationalContext relationalContext = ViewerContext.of(ctx, PlayerOptionalContext.of(viewer, ContextHolder.EMPTY));
                viewer.sendTitle(
                        AdventureHelper.miniMessage().deserialize(this.main.get(relationalContext), relationalContext.tagResolvers()),
                        AdventureHelper.miniMessage().deserialize(this.sub.get(relationalContext), relationalContext.tagResolvers()),
                        this.fadeIn.getInt(relationalContext), this.stay.getInt(relationalContext), this.fadeOut.getInt(relationalContext)
                );
            }
        }
    }

    @Override
    public Key type() {
        return CommonFunctions.TITLE;
    }

    public static class FactoryImpl<CTX extends Context> extends AbstractFactory<CTX> {

        public FactoryImpl(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public Function<CTX> create(Map<String, Object> arguments) {
            String title = arguments.getOrDefault("title", "").toString();
            String subtitle = arguments.getOrDefault("subtitle", "").toString();
            NumberProvider fadeIn = NumberProviders.fromObject(arguments.getOrDefault("fade-in", 10));
            NumberProvider stay = NumberProviders.fromObject(arguments.getOrDefault("stay", 20));
            NumberProvider fadeOut = NumberProviders.fromObject(arguments.getOrDefault("fade-out", 5));
            return new TitleFunction<>(getPredicates(arguments), PlayerSelectors.fromObject(arguments.get("target"), conditionFactory()),
                    TextProviders.fromString(title), TextProviders.fromString(subtitle), fadeIn, stay, fadeOut);
        }
    }
}
