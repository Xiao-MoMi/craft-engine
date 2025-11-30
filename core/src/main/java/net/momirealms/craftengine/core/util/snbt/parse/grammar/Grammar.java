package net.momirealms.craftengine.core.util.snbt.parse.grammar;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.momirealms.craftengine.core.util.snbt.parse.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record Grammar<T>(Dictionary<StringReader> rules, NamedRule<StringReader, T> top) {
    public Grammar {
        rules.checkAllBound();
    }

    public Optional<T> parse(ParseState<StringReader> parseState) {
        return parseState.parseTopRule(this.top);
    }

    public T parse(StringReader reader) throws CommandSyntaxException {
        ErrorCollector.LongestOnly<StringReader> longestOnly = new ErrorCollector.LongestOnly<>();
        StringReaderParserState stringReaderParserState = new StringReaderParserState(longestOnly, reader);
        Optional<T> optional = this.parse(stringReaderParserState);
        if (optional.isPresent()) {
            T result = optional.get();
            if (CachedParseState.JAVA_NULL_VALUE_MARKER.equals(result)) {
                result = null;
            }
            return result;
        } else {
            List<ErrorEntry<StringReader>> list = longestOnly.entries();
            List<Exception> list1 = list.stream().<Exception>mapMulti((errorEntry, consumer) -> {
                if (errorEntry.reason() instanceof DelayedException<?> delayedException) {
                    consumer.accept(delayedException.create(reader.getString(), errorEntry.cursor()));
                } else if (errorEntry.reason() instanceof Exception exception1) {
                    consumer.accept(exception1);
                }
            }).toList();

            for (Exception exception : list1) {
                if (exception instanceof CommandSyntaxException commandSyntaxException) {
                    throw commandSyntaxException;
                }
            }

            if (list1.size() == 1 && list1.getFirst() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            } else {
                throw new IllegalStateException("Failed to parse: " + list.stream().map(ErrorEntry::toString).collect(Collectors.joining(", ")));
            }
        }
    }
}
