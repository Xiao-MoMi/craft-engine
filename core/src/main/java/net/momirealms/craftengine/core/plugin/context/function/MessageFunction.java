package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.context.*;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelectors;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class MessageFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final List<String> messages;
    private final PlayerSelector<CTX> selector;
    private final boolean overlay;

    public MessageFunction(List<Condition<CTX>> predicates, @Nullable PlayerSelector<CTX> selector, List<String> messages, boolean overlay) {
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

    @Override
    public Key type() {
        return CommonFunctions.MESSAGE;
    }

    public static class FactoryImpl<CTX extends Context> extends AbstractFactory<CTX> {

        public FactoryImpl(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public Function<CTX> create(Map<String, Object> arguments) {
            Object message = ResourceConfigUtils.requireNonNullOrThrow(ResourceConfigUtils.get(arguments, "messages", "message"), "warning.config.function.command.missing_message");
            List<String> messages = MiscUtils.getAsStringList(message);
            boolean overlay = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("overlay", false), "overlay");
            return new MessageFunction<>(getPredicates(arguments), PlayerSelectors.fromObject(arguments.get("target"), conditionFactory()), messages, overlay);
        }
    }
}
