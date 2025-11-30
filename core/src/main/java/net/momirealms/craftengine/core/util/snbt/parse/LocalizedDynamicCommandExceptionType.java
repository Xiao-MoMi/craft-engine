package net.momirealms.craftengine.core.util.snbt.parse;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import java.util.function.Function;

public class LocalizedDynamicCommandExceptionType extends DynamicCommandExceptionType {
    private final Function<Object, Message> function;

    public LocalizedDynamicCommandExceptionType(Function<Object, Message> function) {
        super(function);
        this.function = function;
    }

    @Override
    public CommandSyntaxException create(final Object arg) {
        return new LocalizedCommandSyntaxException(this, function.apply(arg));
    }

    @Override
    public CommandSyntaxException createWithContext(final ImmutableStringReader reader, final Object arg) {
        return new LocalizedCommandSyntaxException(this, function.apply(arg), reader.getString(), reader.getCursor());
    }
}
