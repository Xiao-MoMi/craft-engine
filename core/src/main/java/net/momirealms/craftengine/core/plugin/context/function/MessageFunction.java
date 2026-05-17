package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.*;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;
import net.momirealms.craftengine.core.util.AdventureHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class MessageFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final List<String> messages;
    private final PlayerSelector<CTX> selector;
    private final boolean overlay;

    private MessageFunction(List<Condition<CTX>> predicates,
                            @Nullable PlayerSelector<CTX> selector,
                            List<String> messages,
                            boolean overlay) {
        super(predicates);
        this.messages = messages;
        this.selector = selector;
        this.overlay = overlay;
    }

    @Override
    public void runInternal(CTX ctx) {
        if (this.selector == null) {
            ctx.getOptionalParameter(DirectContextParameters.PLAYER).ifPresent(it -> {
                for (String c : this.messages) {
                    it.sendMessage(AdventureHelper.miniMessage().deserialize(c, ctx.tagResolvers()), this.overlay);
                }
            });
        } else {
            for (Player viewer : this.selector.get(ctx)) {
                RelationalContext relationalContext = ViewerContext.of(ctx, PlayerOptionalContext.of(viewer));
                for (String c : this.messages) {
                    viewer.sendMessage(AdventureHelper.miniMessage().deserialize(c, relationalContext.tagResolvers()), this.overlay);
                }
            }
        }
    }

    public static <CTX extends Context> FunctionFactory<CTX, MessageFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, MessageFunction<CTX>> {
        private static final String[] MESSAGES = new String[] {"messages", "message"};

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public MessageFunction<CTX> create(ConfigSection section) {
            return new MessageFunction<>(
                    getPredicates(section),
                    getPlayerSelector(section),
                    section.getNonNullStringList(MESSAGES).stream().map(AdventureHelper::legacyToMiniMessage).toList(),
                    section.getBoolean("overlay"));
        }
    }
}