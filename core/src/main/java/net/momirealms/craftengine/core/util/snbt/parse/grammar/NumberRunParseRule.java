package net.momirealms.craftengine.core.util.snbt.parse.grammar;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.momirealms.craftengine.core.util.snbt.parse.DelayedException;
import net.momirealms.craftengine.core.util.snbt.parse.ParseState;
import net.momirealms.craftengine.core.util.snbt.parse.Rule;

import javax.annotation.Nullable;

public abstract class NumberRunParseRule implements Rule<StringReader, String> {
    private final DelayedException<CommandSyntaxException> noValueError;
    private final DelayedException<CommandSyntaxException> underscoreNotAllowedError;

    public NumberRunParseRule(DelayedException<CommandSyntaxException> noValueError, DelayedException<CommandSyntaxException> underscoreNotAllowedError) {
        this.noValueError = noValueError;
        this.underscoreNotAllowedError = underscoreNotAllowedError;
    }

    @Nullable
    @Override
    public String parse(ParseState<StringReader> parseState) {
        StringReader stringReader = parseState.input();
        stringReader.skipWhitespace();
        String string = stringReader.getString();
        int cursor = stringReader.getCursor();
        int i = cursor;

        while (i < string.length() && this.isAccepted(string.charAt(i))) {
            i++;
        }

        int i1 = i - cursor;
        if (i1 == 0) {
            parseState.errorCollector().store(parseState.mark(), this.noValueError);
            return null;
        } else if (string.charAt(cursor) != '_' && string.charAt(i - 1) != '_') {
            stringReader.setCursor(i);
            return string.substring(cursor, i);
        } else {
            parseState.errorCollector().store(parseState.mark(), this.underscoreNotAllowedError);
            return null;
        }
    }

    protected abstract boolean isAccepted(char c);
}
