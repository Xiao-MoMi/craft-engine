package net.momirealms.craftengine.core.util.snbt.parse.grammar;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import it.unimi.dsi.fastutil.chars.CharList;
import net.momirealms.craftengine.core.util.snbt.parse.*;

import java.util.stream.Collectors;

public interface StringReaderTerms {
    DynamicCommandExceptionType LITERAL_INCORRECT = new LocalizedDynamicCommandExceptionType(
            expected -> new LocalizedMessage("warning.config.type.snbt.parser.incorrect", String.valueOf(expected))
    );

    static Term<StringReader> character(final char value) {
        return new TerminalCharacters(CharList.of(value)) {
            @Override
            protected boolean isAccepted(char c) {
                return value == c;
            }
        };
    }

    static Term<StringReader> characters(final char value1, final char value2) {
        return new TerminalCharacters(CharList.of(value1, value2)) {
            @Override
            protected boolean isAccepted(char c) {
                return c == value1 || c == value2;
            }
        };
    }

    static StringReader createReader(String input, int cursor) {
        StringReader stringReader = new StringReader(input);
        stringReader.setCursor(cursor);
        return stringReader;
    }

    abstract class TerminalCharacters implements Term<StringReader> {
        private final DelayedException<CommandSyntaxException> error;
        private final SuggestionSupplier<StringReader> suggestions;

        public TerminalCharacters(CharList characters) {
            String string = characters.intStream().mapToObj(Character::toString).collect(Collectors.joining("|"));
            this.error = DelayedException.create(LITERAL_INCORRECT, string);
            this.suggestions = parseState -> characters.intStream().mapToObj(Character::toString);
        }

        @Override
        public boolean parse(ParseState<StringReader> parseState, Scope scope, Control control) {
            parseState.input().skipWhitespace();
            int i = parseState.mark();
            if (parseState.input().canRead() && this.isAccepted(parseState.input().read())) {
                return true;
            } else {
                parseState.errorCollector().store(i, this.suggestions, this.error);
                return false;
            }
        }

        protected abstract boolean isAccepted(char c);
    }

}
