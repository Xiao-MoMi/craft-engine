package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.Platform;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.*;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;
import net.momirealms.craftengine.core.plugin.context.text.TextProvider;
import net.momirealms.craftengine.core.plugin.context.text.TextProviders;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public final class CommandFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final List<TextProvider> command;
    private final PlayerSelector<CTX> selector;
    private final boolean asPlayer;
    private final boolean asEvent;
    private final boolean asOp;

    private CommandFunction(List<Condition<CTX>> predicates,
                            @Nullable PlayerSelector<CTX> selector,
                            List<TextProvider> command,
                            boolean asPlayer,
                            boolean asEvent,
                            boolean asOp) {
        super(predicates);
        this.command = command;
        this.selector = selector;
        this.asPlayer = asPlayer;
        this.asEvent = asEvent;
        this.asOp = asOp;
    }

    @Override
    public void runInternal(CTX ctx) {
        if (this.asPlayer || this.asOp) {
            if (this.selector == null) {
                ctx.getOptionalParameter(DirectContextParameters.PLAYER)
                        .ifPresent(player -> executeCommands(
                                ctx, this.asEvent ? player::performCommandAsEvent : command1 -> player.performCommand(command1, this.asOp)
                        ));
            } else {
                for (Player viewer : this.selector.get(ctx)) {
                    RelationalContext relationalContext = ViewerContext.of(ctx, PlayerOptionalContext.of(viewer));
                    executeCommands(relationalContext, this.asEvent ? viewer::performCommandAsEvent : command1 -> viewer.performCommand(command1, this.asOp));
                }
            }
        } else {
            Platform platform = CraftEngine.instance().platform();
            for (TextProvider c : this.command) {
                platform.dispatchCommand(c.get(ctx));
            }
        }
    }

    private void executeCommands(Context ctx, Consumer<String> executor) {
        for (TextProvider c : this.command) {
            executor.accept(c.get(ctx));
        }
    }

    public static <CTX extends Context> FunctionFactory<CTX, CommandFunction<CTX>> factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
        return new Factory<>(factory);
    }

    private static class Factory<CTX extends Context> extends AbstractFactory<CTX, CommandFunction<CTX>> {
        private static final String[] COMMAND = new String[]{"command", "commands"};
        private static final String[] AS_PLAYER = new String[]{"as_player", "as-player"};
        private static final String[] AS_EVENT = new String[]{"as_event", "as-event"};
        private static final String[] AS_OP = new String[]{"as_op", "as-op"};

        public Factory(java.util.function.Function<ConfigSection, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public CommandFunction<CTX> create(ConfigSection section) {
            return new CommandFunction<>(
                    getPredicates(section),
                    getPlayerSelector(section),
                    section.getNonEmptyList(COMMAND, v -> TextProviders.fromString(v.getAsString())),
                    section.getBoolean(AS_PLAYER),
                    section.getBoolean(AS_EVENT),
                    section.getBoolean(AS_OP)
            );
        }
    }
}
