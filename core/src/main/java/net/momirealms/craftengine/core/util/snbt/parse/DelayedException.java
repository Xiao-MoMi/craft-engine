package net.momirealms.craftengine.core.util.snbt.parse;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.momirealms.craftengine.core.util.snbt.parse.grammar.StringReaderTerms;

@FunctionalInterface
public interface DelayedException<T extends Exception> {
    T create(String message, int cursor);

    static DelayedException<CommandSyntaxException> create(SimpleCommandExceptionType exception) {
        return (message, cursor) -> exception.createWithContext(StringReaderTerms.createReader(message, cursor));
    }

    static DelayedException<CommandSyntaxException> create(DynamicCommandExceptionType exception, String argument) {
        return (message, cursor) -> exception.createWithContext(StringReaderTerms.createReader(message, cursor), argument);
    }
}
