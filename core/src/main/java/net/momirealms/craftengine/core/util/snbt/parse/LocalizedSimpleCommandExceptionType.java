package net.momirealms.craftengine.core.util.snbt.parse;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

public class LocalizedSimpleCommandExceptionType extends SimpleCommandExceptionType {
    private final Message message;

    public LocalizedSimpleCommandExceptionType(Message message) {
        super(message);
        this.message = message;
    }

    @Override
    public CommandSyntaxException create() {
        return new LocalizedCommandSyntaxException(this, message);
    }

    @Override
    public CommandSyntaxException createWithContext(final ImmutableStringReader reader) {
        return new LocalizedCommandSyntaxException(this, message, reader.getString(), reader.getCursor());
    }
}
