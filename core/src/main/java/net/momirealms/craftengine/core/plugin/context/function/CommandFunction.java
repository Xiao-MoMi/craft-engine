package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.Platform;
import net.momirealms.craftengine.core.plugin.context.*;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelectors;
import net.momirealms.craftengine.core.plugin.context.text.TextProvider;
import net.momirealms.craftengine.core.plugin.context.text.TextProviders;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class CommandFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final List<TextProvider> command;
    private final boolean asPlayer;
    private final PlayerSelector<CTX> selector;

    public CommandFunction(List<Condition<CTX>> predicates, @Nullable PlayerSelector<CTX> selector, List<TextProvider> command, boolean asPlayer) {
        super(predicates);
        this.asPlayer = asPlayer;
        this.command = command;
        this.selector = selector;
    }

    @Override
    public void runInternal(CTX ctx) {
        if (this.asPlayer) {
            if (this.selector == null) {
                ctx.getOptionalParameter(DirectContextParameters.PLAYER).ifPresent(it -> {
                    for (TextProvider c : this.command) {
                        it.performCommand(c.get(ctx));
                    }
                });
            } else {
                for (Player viewer : this.selector.get(ctx)) {
                    RelationalContext relationalContext = ViewerContext.of(ctx, PlayerOptionalContext.of(viewer, ContextHolder.EMPTY));
                    for (TextProvider c : this.command) {
                        viewer.performCommand(c.get(relationalContext));
                    }
                }
            }
        } else {
            Platform platform = CraftEngine.instance().platform();
            for (TextProvider c : this.command) {
                platform.dispatchCommand(c.get(ctx));
            }
        }
    }

    @Override
    public Key type() {
        return CommonFunctions.COMMAND;
    }

    public static class FactoryImpl<CTX extends Context> extends AbstractFactory<CTX> {

        public FactoryImpl(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public Function<CTX> create(Map<String, Object> arguments) {
            Object command = ResourceConfigUtils.requireNonNullOrThrow(ResourceConfigUtils.get(arguments, "command", "commands"), "warning.config.function.command.missing_command");
            List<TextProvider> commands = MiscUtils.getAsStringList(command).stream().map(TextProviders::fromString).toList();
            boolean asPlayer = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("as-player", false), "as-player");
            return new CommandFunction<>(getPredicates(arguments), PlayerSelectors.fromObject(arguments.get("target"), conditionFactory()), commands, asPlayer);
        }
    }
}
