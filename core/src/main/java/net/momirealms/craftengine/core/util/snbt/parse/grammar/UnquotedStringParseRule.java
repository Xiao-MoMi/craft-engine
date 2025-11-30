package net.momirealms.craftengine.core.util.snbt.parse.grammar;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.momirealms.craftengine.core.util.snbt.parse.DelayedException;
import net.momirealms.craftengine.core.util.snbt.parse.ParseState;
import net.momirealms.craftengine.core.util.snbt.parse.Rule;

import javax.annotation.Nullable;

public class UnquotedStringParseRule implements Rule<StringReader, String> {
    private final int minSize;
    private final DelayedException<CommandSyntaxException> error;

    public UnquotedStringParseRule(int minSize, DelayedException<CommandSyntaxException> error) {
        this.minSize = minSize;
        this.error = error;
    }

    @Nullable
    @Override
    public String parse(ParseState<StringReader> parseState) {
        parseState.input().skipWhitespace();
        int i = parseState.mark();
        String unquotedString = parseState.input().readUnquotedString();
        if (unquotedString.length() < this.minSize) {
            parseState.errorCollector().store(i, this.error);
            return null;
        } else {
            return unquotedString;
        }
    }
}
