package net.momirealms.craftengine.core.util.snbt;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.DynamicOps;
import net.momirealms.craftengine.core.util.snbt.parse.*;
import net.momirealms.sparrow.nbt.util.UUIDUtil;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SnbtOperations {
    static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_STRING_UUID = DelayedException.create(
            new LocalizedSimpleCommandExceptionType(new LocalizedMessage("warning.config.type.snbt.parser.expected_string_uuid"))
    );
    static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_NUMBER_OR_BOOLEAN = DelayedException.create(
            new LocalizedSimpleCommandExceptionType(new LocalizedMessage("warning.config.type.snbt.parser.expected_number_or_boolean"))
    );
    public static final String BUILTIN_TRUE = "true";
    public static final String BUILTIN_FALSE = "false";
    public static final Map<BuiltinKey, BuiltinOperation> BUILTIN_OPERATIONS = Map.of(
            new BuiltinKey("bool", 1), new BuiltinOperation() {
                @Override
                public <T> T run(DynamicOps<T> ops, List<T> args, ParseState<StringReader> parseState) {
                    Boolean bool = convert(ops, args.getFirst());
                    if (bool == null) {
                        parseState.errorCollector().store(parseState.mark(), SnbtOperations.ERROR_EXPECTED_NUMBER_OR_BOOLEAN);
                        return null;
                    } else {
                        return ops.createBoolean(bool);
                    }
                }

                @Nullable
                private static <T> Boolean convert(DynamicOps<T> ops, T value) {
                    Optional<Boolean> optional = ops.getBooleanValue(value).result();
                    if (optional.isPresent()) {
                        return optional.get();
                    } else {
                        Optional<Number> optional1 = ops.getNumberValue(value).result();
                        return optional1.isPresent() ? optional1.get().doubleValue() != 0.0 : null;
                    }
                }
            }, new BuiltinKey("uuid", 1), new BuiltinOperation() {
                @Override
                public <T> T run(DynamicOps<T> ops, List<T> args, ParseState<StringReader> parseState) {
                    Optional<String> optional = ops.getStringValue(args.getFirst()).result();
                    if (optional.isEmpty()) {
                        parseState.errorCollector().store(parseState.mark(), SnbtOperations.ERROR_EXPECTED_STRING_UUID);
                        return null;
                    } else {
                        UUID uuid;
                        try {
                            uuid = UUID.fromString(optional.get());
                        } catch (IllegalArgumentException var7) {
                            parseState.errorCollector().store(parseState.mark(), SnbtOperations.ERROR_EXPECTED_STRING_UUID);
                            return null;
                        }

                        return ops.createIntList(IntStream.of(UUIDUtil.uuidToIntArray(uuid)));
                    }
                }
            }
    );
    public static final SuggestionSupplier<StringReader> BUILTIN_IDS = new SuggestionSupplier<>() {
        private final Set<String> keys = Stream.concat(
                        Stream.of(BUILTIN_FALSE, BUILTIN_TRUE), SnbtOperations.BUILTIN_OPERATIONS.keySet().stream().map(BuiltinKey::id)
                )
                .collect(Collectors.toSet());

        @Override
        public Stream<String> possibleValues(ParseState<StringReader> parseState) {
            return this.keys.stream();
        }
    };

    public record BuiltinKey(String id, int argCount) {
        @Override
        public @NotNull String toString() {
            return this.id + "/" + this.argCount;
        }
    }

    public interface BuiltinOperation {
        @Nullable
        <T> T run(DynamicOps<T> ops, List<T> args, ParseState<StringReader> parseState);
    }
}
