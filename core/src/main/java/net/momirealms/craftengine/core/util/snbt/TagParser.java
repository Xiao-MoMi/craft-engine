package net.momirealms.craftengine.core.util.snbt;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.util.snbt.parse.LocalizedMessage;
import net.momirealms.craftengine.core.util.snbt.parse.LocalizedSimpleCommandExceptionType;
import net.momirealms.craftengine.core.util.snbt.parse.grammar.Grammar;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;
import net.momirealms.sparrow.nbt.codec.LegacyJavaOps;
import net.momirealms.sparrow.nbt.codec.LegacyNBTOps;
import net.momirealms.sparrow.nbt.codec.NBTOps;

public class TagParser<T> {
    public static final SimpleCommandExceptionType ERROR_TRAILING_DATA = new LocalizedSimpleCommandExceptionType(
            new LocalizedMessage("warning.config.type.snbt.parser.trailing")
    );
    public static final SimpleCommandExceptionType ERROR_EXPECTED_COMPOUND = new LocalizedSimpleCommandExceptionType(
            new LocalizedMessage("warning.config.type.snbt.parser.expected.compound")
    );
    public static final char ELEMENT_SEPARATOR = ',';
    public static final char NAME_VALUE_SEPARATOR = ':';
    private static final TagParser<Tag> NBT_OPS_PARSER = create(VersionHelper.isOrAbove1_20_5() ? NBTOps.INSTANCE : LegacyNBTOps.INSTANCE);
    private static final TagParser<Object> JAVA_OPS_PARSER = create(VersionHelper.isOrAbove1_20_5() ? JavaOps.INSTANCE : LegacyJavaOps.INSTANCE);
    private final DynamicOps<T> ops;
    private final Grammar<T> grammar;

    private TagParser(DynamicOps<T> ops, Grammar<T> grammar) {
        this.ops = ops;
        this.grammar = grammar;
    }

    public DynamicOps<T> ops() {
        return this.ops;
    }

    public static <T> TagParser<T> create(DynamicOps<T> ops) {
        return new TagParser<>(ops, SnbtGrammar.createParser(ops));
    }

    private static CompoundTag castToCompoundOrThrow(StringReader reader, Tag tag) throws CommandSyntaxException {
        if (tag instanceof CompoundTag compoundTag) {
            return compoundTag;
        }
        throw ERROR_EXPECTED_COMPOUND.createWithContext(reader);
    }

    public static CompoundTag parseCompoundFully(String data) throws CommandSyntaxException {
        StringReader stringReader = new StringReader(data);
        return parseCompoundAsArgument(stringReader);
    }

    public static Object parseObjectFully(String data) throws CommandSyntaxException {
        StringReader stringReader = new StringReader(data);
        return parseObjectAsArgument(stringReader);
    }

    public T parseFully(String text) throws CommandSyntaxException {
        return this.parseFully(new StringReader(text));
    }

    public T parseFully(StringReader reader) throws CommandSyntaxException {
        T object = this.grammar.parse(reader);
        reader.skipWhitespace();
        if (reader.canRead()) {
            throw ERROR_TRAILING_DATA.createWithContext(reader);
        }
        return object;
    }

    public T parseAsArgument(StringReader reader) throws CommandSyntaxException {
        return this.grammar.parse(reader);
    }

    public static CompoundTag parseCompoundAsArgument(StringReader reader) throws CommandSyntaxException {
        Tag tag = NBT_OPS_PARSER.parseAsArgument(reader);
        return castToCompoundOrThrow(reader, tag);
    }

    public static Object parseObjectAsArgument(StringReader reader) throws CommandSyntaxException {
        return JAVA_OPS_PARSER.parseAsArgument(reader);
    }
}
