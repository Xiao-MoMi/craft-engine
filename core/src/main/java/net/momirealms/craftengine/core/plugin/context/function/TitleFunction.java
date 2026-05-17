package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.*;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;
import net.momirealms.craftengine.core.util.AdventureHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class TitleFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final PlayerSelector<CTX> selector;
    private final String main;
    private final String sub;
    private final NumberProvider fadeIn;
    private final NumberProvider stay;
    private final NumberProvider fadeOut;

    private TitleFunction(List<Condition<CTX>> predicates,
                          @Nullable PlayerSelector<CTX> selector,
                          String main,
                          String sub,
                          NumberProvider fadeIn,
                          NumberProvider stay,
                          NumberProvider fadeOut) {
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

    public static <CTX extends Context> FunctionFactory<CTX, TitleFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, TitleFunction<CTX>> {
        private static final String[] FADE_IN = new String[] {"fade_in", "fade-in"};
        private static final String[] FADE_OUT = new String[] {"fade_out", "fade-out"};

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public TitleFunction<CTX> create(ConfigSection section) {
            return new TitleFunction<>(
                    getPredicates(section),
                    getPlayerSelector(section),
                    section.getString("title", ""),
                    section.getString("subtitle", ""),
                    section.getNumber(FADE_IN, ConfigConstants.CONSTANT_TEN),
                    section.getNumber("stay", ConfigConstants.CONSTANT_TWENTY),
                    section.getNumber(FADE_OUT, ConfigConstants.CONSTANT_FIVE)
            );
        }
    }
}