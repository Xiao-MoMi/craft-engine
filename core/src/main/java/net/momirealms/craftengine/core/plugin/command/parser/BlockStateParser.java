package net.momirealms.craftengine.core.plugin.command.parser;

import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCaptionKeys;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.caption.CaptionVariable;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.exception.parsing.ParserException;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;

public class BlockStateParser<C> implements ArgumentParser<C, ImmutableBlockState>, BlockingSuggestionProvider.Strings<C> {

    public static <C> @NonNull ParserDescriptor<C, ImmutableBlockState> blockStateParser() {
        return ParserDescriptor.of(new BlockStateParser<>(), ImmutableBlockState.class);
    }

    public static <C> CommandComponent.@NonNull Builder<C, ImmutableBlockState> blockStateComponent() {
        return CommandComponent.<C, ImmutableBlockState>builder().parser(blockStateParser());
    }

    @Override
    public @NonNull ArgumentParseResult<@NonNull ImmutableBlockState> parse(@NonNull CommandContext<@NonNull C> commandContext, @NonNull CommandInput commandInput) {
        String input = commandInput.readString();
        ImmutableBlockState state = net.momirealms.craftengine.core.block.BlockStateParser.deserialize(input);
        if (state == null) {
            return ArgumentParseResult.failure(new BlockStateParseException(input, commandContext));
        }
        return ArgumentParseResult.success(state);
    }

    @Override
    public @NonNull Iterable<@NonNull String> stringSuggestions(@NonNull CommandContext<C> commandContext, @NonNull CommandInput input) {
        return CraftEngine.instance().blockManager().cachedSuggestions().stream().map(it -> {
            return it.suggestion();
        }).toList();
    }

    public static final class BlockStateParseException extends ParserException {

        private final String input;

        public BlockStateParseException(
                final @NonNull String input,
                final @NonNull CommandContext<?> context
        ) {
            super(
                    BlockStateParser.class,
                    context,
                    CraftEngineCaptionKeys.ARGUMENT_PARSE_FAILURE_BLOCK_STATE,
                    CaptionVariable.of("input", input)
            );
            this.input = input;
        }

        public @NonNull String input() {
            return this.input;
        }
    }
}
