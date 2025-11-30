package net.momirealms.craftengine.core.util.snbt;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.chars.CharList;
import net.momirealms.craftengine.core.util.snbt.parse.*;
import net.momirealms.craftengine.core.util.snbt.parse.Dictionary;
import net.momirealms.craftengine.core.util.snbt.parse.grammar.*;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class SnbtGrammar {
    private static final DynamicCommandExceptionType ERROR_NUMBER_PARSE_FAILURE = new LocalizedDynamicCommandExceptionType(
            number -> new LocalizedMessage("warning.config.type.snbt.parser.number_parse_failure", String.valueOf(number))
    );
    static final DynamicCommandExceptionType ERROR_EXPECTED_HEX_ESCAPE = new LocalizedDynamicCommandExceptionType(
            length -> new LocalizedMessage("warning.config.type.snbt.parser.expected_hex_escape", String.valueOf(length))
    );
    private static final DynamicCommandExceptionType ERROR_INVALID_CODEPOINT = new LocalizedDynamicCommandExceptionType(
            codepoint -> new LocalizedMessage("warning.config.type.snbt.parser.invalid_codepoint", String.valueOf(codepoint))
    );
    private static final DynamicCommandExceptionType ERROR_NO_SUCH_OPERATION = new LocalizedDynamicCommandExceptionType(
            operation -> new LocalizedMessage("warning.config.type.snbt.parser.no_such_operation", String.valueOf(operation))
    );
    static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_INTEGER_TYPE = DelayedException.create(
            new LocalizedSimpleCommandExceptionType(new LocalizedMessage("warning.config.type.snbt.parser.expected_integer_type"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_FLOAT_TYPE = DelayedException.create(
            new LocalizedSimpleCommandExceptionType(new LocalizedMessage("warning.config.type.snbt.parser.expected_float_type"))
    );
    static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_NON_NEGATIVE_NUMBER = DelayedException.create(
            new LocalizedSimpleCommandExceptionType(new LocalizedMessage("warning.config.type.snbt.parser.expected_non_negative_number"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_INVALID_CHARACTER_NAME = DelayedException.create(
            new LocalizedSimpleCommandExceptionType(new LocalizedMessage("warning.config.type.snbt.parser.invalid_character_name"))
    );
    static final DelayedException<CommandSyntaxException> ERROR_INVALID_ARRAY_ELEMENT_TYPE = DelayedException.create(
            new LocalizedSimpleCommandExceptionType(new LocalizedMessage("warning.config.type.snbt.parser.invalid_array_element_type"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_INVALID_UNQUOTED_START = DelayedException.create(
            new LocalizedSimpleCommandExceptionType(new LocalizedMessage("warning.config.type.snbt.parser.invalid_unquoted_start"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_UNQUOTED_STRING = DelayedException.create(
            new LocalizedSimpleCommandExceptionType(new LocalizedMessage("warning.config.type.snbt.parser.expected_unquoted_string"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_INVALID_STRING_CONTENTS = DelayedException.create(
            new LocalizedSimpleCommandExceptionType(new LocalizedMessage("warning.config.type.snbt.parser.invalid_string_contents"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_BINARY_NUMERAL = DelayedException.create(
            new LocalizedSimpleCommandExceptionType(new LocalizedMessage("warning.config.type.snbt.parser.expected_binary_numeral"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_UNDERSCORE_NOT_ALLOWED = DelayedException.create(
            new LocalizedSimpleCommandExceptionType(new LocalizedMessage("warning.config.type.snbt.parser.underscore_not_allowed"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_DECIMAL_NUMERAL = DelayedException.create(
            new LocalizedSimpleCommandExceptionType(new LocalizedMessage("warning.config.type.snbt.parser.expected_decimal_numeral"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_HEX_NUMERAL = DelayedException.create(
            new LocalizedSimpleCommandExceptionType(new LocalizedMessage("Expected a hexadecimal number"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_EMPTY_KEY = DelayedException.create(
            new LocalizedSimpleCommandExceptionType(new LocalizedMessage("warning.config.type.snbt.parser.empty_key"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_LEADING_ZERO_NOT_ALLOWED = DelayedException.create(
            new LocalizedSimpleCommandExceptionType(new LocalizedMessage("warning.config.type.snbt.parser.leading_zero_not_allowed"))
    );
    private static final DelayedException<CommandSyntaxException> ERROR_INFINITY_NOT_ALLOWED = DelayedException.create(
            new LocalizedSimpleCommandExceptionType(new LocalizedMessage("warning.config.type.snbt.parser.infinity_not_allowed"))
    );
    private static final NumberRunParseRule BINARY_NUMERAL = new NumberRunParseRule(ERROR_EXPECTED_BINARY_NUMERAL, ERROR_UNDERSCORE_NOT_ALLOWED) {
        @Override
        protected boolean isAccepted(char c) {
            return switch (c) {
                case '0', '1', '_' -> true;
                default -> false;
            };
        }
    };
    private static final NumberRunParseRule DECIMAL_NUMERAL = new NumberRunParseRule(ERROR_EXPECTED_DECIMAL_NUMERAL, ERROR_UNDERSCORE_NOT_ALLOWED) {
        @Override
        protected boolean isAccepted(char c) {
            return switch (c) {
                case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '_' -> true;
                default -> false;
            };
        }
    };
    private static final NumberRunParseRule HEX_NUMERAL = new NumberRunParseRule(ERROR_EXPECTED_HEX_NUMERAL, ERROR_UNDERSCORE_NOT_ALLOWED) {
        @Override
        protected boolean isAccepted(char c) {
            return switch (c) {
                case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', '_', 'a', 'b', 'c', 'd', 'e', 'f' -> true;
                default -> false;
            };
        }
    };
    private static final GreedyPredicateParseRule PLAIN_STRING_CHUNK = new GreedyPredicateParseRule(1, ERROR_INVALID_STRING_CONTENTS) {
        @Override
        protected boolean isAccepted(char c) {
            return switch (c) {
                case '"', '\'', '\\' -> false;
                default -> true;
            };
        }
    };
    private static final StringReaderTerms.TerminalCharacters NUMBER_LOOKEAHEAD = new StringReaderTerms.TerminalCharacters(CharList.of()) {
        @Override
        protected boolean isAccepted(char c) {
            return canStartNumber(c);
        }
    };
    private static final Pattern UNICODE_NAME = Pattern.compile("[-a-zA-Z0-9 ]+");

    static DelayedException<CommandSyntaxException> createNumberParseError(NumberFormatException numberFormatException) {
        return DelayedException.create(ERROR_NUMBER_PARSE_FAILURE, numberFormatException.getMessage());
    }

    private static boolean isAllowedToStartUnquotedString(char c) {
        return !canStartNumber(c);
    }

    static boolean canStartNumber(char c) {
        return switch (c) {
            case '+', '-', '.', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> true;
            default -> false;
        };
    }

    static boolean needsUnderscoreRemoval(String text) {
        return text.indexOf(95) != -1;
    }

    private static void cleanAndAppend(StringBuilder stringBuilder, String text) {
        cleanAndAppend(stringBuilder, text, needsUnderscoreRemoval(text));
    }

    static void cleanAndAppend(StringBuilder stringBuilder, String text, boolean removeUnderscores) {
        if (removeUnderscores) {
            for (char c : text.toCharArray()) {
                if (c != '_') {
                    stringBuilder.append(c);
                }
            }
        }
        stringBuilder.append(text);
    }

    static short parseUnsignedShort(String text, int radix) {
        int i = Integer.parseInt(text, radix);
        if (i >> 16 == 0) {
            return (short)i;
        }
        throw new NumberFormatException("out of range: " + i);
    }

    @Nullable
    private static <T> T createFloat(
        DynamicOps<T> ops,
        Sign sign,
        @Nullable String wholePart,
        @Nullable String fractionPart,
        @Nullable Signed<String> exponentPart,
        @Nullable TypeSuffix suffix,
        ParseState<?> parseState
    ) {
        StringBuilder stringBuilder = new StringBuilder();
        sign.append(stringBuilder);
        if (wholePart != null) {
            cleanAndAppend(stringBuilder, wholePart);
        }

        if (fractionPart != null) {
            stringBuilder.append('.');
            cleanAndAppend(stringBuilder, fractionPart);
        }

        if (exponentPart != null) {
            stringBuilder.append('e');
            exponentPart.sign().append(stringBuilder);
            cleanAndAppend(stringBuilder, exponentPart.value);
        }

        try {
            String string = stringBuilder.toString();

            return switch (suffix) {
                case null -> convertDouble(ops, parseState, string);
                case FLOAT -> convertFloat(ops, parseState, string);
                case DOUBLE -> convertDouble(ops, parseState, string);
                default -> {
                    parseState.errorCollector().store(parseState.mark(), ERROR_EXPECTED_FLOAT_TYPE);
                    yield null;
                }
            };
        } catch (NumberFormatException var11) {
            parseState.errorCollector().store(parseState.mark(), createNumberParseError(var11));
            return null;
        }
    }

    @Nullable
    private static <T> T convertFloat(DynamicOps<T> ops, ParseState<?> parseState, String value) {
        float f = Float.parseFloat(value);
        if (!Float.isFinite(f)) {
            parseState.errorCollector().store(parseState.mark(), ERROR_INFINITY_NOT_ALLOWED);
            return null;
        }
        return ops.createFloat(f);
    }

    @Nullable
    private static <T> T convertDouble(DynamicOps<T> ops, ParseState<?> parseState, String value) {
        double d = Double.parseDouble(value);
        if (!Double.isFinite(d)) {
            parseState.errorCollector().store(parseState.mark(), ERROR_INFINITY_NOT_ALLOWED);
            return null;
        }
        return ops.createDouble(d);
    }

    private static String joinList(List<String> list) {
        return switch (list.size()) {
            case 0 -> "";
            case 1 -> list.getFirst();
            default -> String.join("", list);
        };
    }

    @SuppressWarnings("unchecked")
    public static <T> Grammar<T> createParser(DynamicOps<T> ops) {
        T object = ops.createBoolean(true);
        T object1 = ops.createBoolean(false);
        T object2 = ops.emptyMap();
        T object3 = ops.emptyList();
        Dictionary<StringReader> dictionary = new Dictionary<>();
        Atom<Sign> atom = Atom.of("sign");
        dictionary.put(
            atom,
            Term.alternative(
                Term.sequence(StringReaderTerms.character('+'), Term.marker(atom, Sign.PLUS)),
                Term.sequence(StringReaderTerms.character('-'), Term.marker(atom, Sign.MINUS))
            ),
            scope -> scope.getOrThrow(atom)
        );
        Atom<IntegerSuffix> atom1 = Atom.of("integer_suffix");
        dictionary.put(
            atom1,
            Term.alternative(
                Term.sequence(
                    StringReaderTerms.characters('u', 'U'),
                    Term.alternative(
                        Term.sequence(
                            StringReaderTerms.characters('b', 'B'),
                            Term.marker(atom1, new IntegerSuffix(SignedPrefix.UNSIGNED, TypeSuffix.BYTE))
                        ),
                        Term.sequence(
                            StringReaderTerms.characters('s', 'S'),
                            Term.marker(atom1, new IntegerSuffix(SignedPrefix.UNSIGNED, TypeSuffix.SHORT))
                        ),
                        Term.sequence(
                            StringReaderTerms.characters('i', 'I'),
                            Term.marker(atom1, new IntegerSuffix(SignedPrefix.UNSIGNED, TypeSuffix.INT))
                        ),
                        Term.sequence(
                            StringReaderTerms.characters('l', 'L'),
                            Term.marker(atom1, new IntegerSuffix(SignedPrefix.UNSIGNED, TypeSuffix.LONG))
                        )
                    )
                ),
                Term.sequence(
                    StringReaderTerms.characters('s', 'S'),
                    Term.alternative(
                        Term.sequence(
                            StringReaderTerms.characters('b', 'B'),
                            Term.marker(atom1, new IntegerSuffix(SignedPrefix.SIGNED, TypeSuffix.BYTE))
                        ),
                        Term.sequence(
                            StringReaderTerms.characters('s', 'S'),
                            Term.marker(atom1, new IntegerSuffix(SignedPrefix.SIGNED, TypeSuffix.SHORT))
                        ),
                        Term.sequence(
                            StringReaderTerms.characters('i', 'I'),
                            Term.marker(atom1, new IntegerSuffix(SignedPrefix.SIGNED, TypeSuffix.INT))
                        ),
                        Term.sequence(
                            StringReaderTerms.characters('l', 'L'),
                            Term.marker(atom1, new IntegerSuffix(SignedPrefix.SIGNED, TypeSuffix.LONG))
                        )
                    )
                ),
                Term.sequence(StringReaderTerms.characters('b', 'B'), Term.marker(atom1, new IntegerSuffix(null, TypeSuffix.BYTE))),
                Term.sequence(StringReaderTerms.characters('s', 'S'), Term.marker(atom1, new IntegerSuffix(null, TypeSuffix.SHORT))),
                Term.sequence(StringReaderTerms.characters('i', 'I'), Term.marker(atom1, new IntegerSuffix(null, TypeSuffix.INT))),
                Term.sequence(StringReaderTerms.characters('l', 'L'), Term.marker(atom1, new IntegerSuffix(null, TypeSuffix.LONG)))
            ),
            scope -> scope.getOrThrow(atom1)
        );
        Atom<String> atom2 = Atom.of("binary_numeral");
        dictionary.put(atom2, BINARY_NUMERAL);
        Atom<String> atom3 = Atom.of("decimal_numeral");
        dictionary.put(atom3, DECIMAL_NUMERAL);
        Atom<String> atom4 = Atom.of("hex_numeral");
        dictionary.put(atom4, HEX_NUMERAL);
        Atom<IntegerLiteral> atom5 = Atom.of("integer_literal");
        NamedRule<StringReader, IntegerLiteral> namedRule = dictionary.put(
            atom5,
            Term.sequence(
                Term.optional(dictionary.named(atom)),
                Term.alternative(
                    Term.sequence(
                        StringReaderTerms.character('0'),
                        Term.cut(),
                        Term.alternative(
                            Term.sequence(StringReaderTerms.characters('x', 'X'), Term.cut(), dictionary.named(atom4)),
                            Term.sequence(StringReaderTerms.characters('b', 'B'), dictionary.named(atom2)),
                            Term.sequence(dictionary.named(atom3), Term.cut(), Term.fail(ERROR_LEADING_ZERO_NOT_ALLOWED)),
                            Term.marker(atom3, "0")
                        )
                    ),
                    dictionary.named(atom3)
                ),
                Term.optional(dictionary.named(atom1))
            ),
            scope -> {
                IntegerSuffix integerSuffix = scope.getOrDefault(atom1, IntegerSuffix.EMPTY);
                Sign sign = scope.getOrDefault(atom, Sign.PLUS);
                String string = scope.get(atom3);
                if (string != null) {
                    return new IntegerLiteral(sign, Base.DECIMAL, string, integerSuffix);
                }
                String string1 = scope.get(atom4);
                if (string1 != null) {
                    return new IntegerLiteral(sign, Base.HEX, string1, integerSuffix);
                }
                String string2 = scope.getOrThrow(atom2);
                return new IntegerLiteral(sign, Base.BINARY, string2, integerSuffix);
            }
        );
        Atom<TypeSuffix> atom6 = Atom.of("float_type_suffix");
        dictionary.put(
            atom6,
            Term.alternative(
                Term.sequence(StringReaderTerms.characters('f', 'F'), Term.marker(atom6, TypeSuffix.FLOAT)),
                Term.sequence(StringReaderTerms.characters('d', 'D'), Term.marker(atom6, TypeSuffix.DOUBLE))
            ),
            scope -> scope.getOrThrow(atom6)
        );
        Atom<Signed<String>> atom7 = Atom.of("float_exponent_part");
        dictionary.put(
            atom7,
            Term.sequence(StringReaderTerms.characters('e', 'E'), Term.optional(dictionary.named(atom)), dictionary.named(atom3)),
            scope -> new Signed<>(scope.getOrDefault(atom, Sign.PLUS), scope.getOrThrow(atom3))
        );
        Atom<String> atom8 = Atom.of("float_whole_part");
        Atom<String> atom9 = Atom.of("float_fraction_part");
        Atom<T> atom10 = Atom.of("float_literal");
        dictionary.putComplex(
            atom10,
            Term.sequence(
                Term.optional(dictionary.named(atom)),
                Term.alternative(
                    Term.sequence(
                        dictionary.namedWithAlias(atom3, atom8),
                        StringReaderTerms.character('.'),
                        Term.cut(),
                        Term.optional(dictionary.namedWithAlias(atom3, atom9)),
                        Term.optional(dictionary.named(atom7)),
                        Term.optional(dictionary.named(atom6))
                    ),
                    Term.sequence(
                        StringReaderTerms.character('.'),
                        Term.cut(),
                        dictionary.namedWithAlias(atom3, atom9),
                        Term.optional(dictionary.named(atom7)),
                        Term.optional(dictionary.named(atom6))
                    ),
                    Term.sequence(dictionary.namedWithAlias(atom3, atom8), dictionary.named(atom7), Term.cut(), Term.optional(dictionary.named(atom6))),
                    Term.sequence(dictionary.namedWithAlias(atom3, atom8), Term.optional(dictionary.named(atom7)), dictionary.named(atom6))
                )
            ),
            parseState -> {
                Scope scope = parseState.scope();
                Sign sign = scope.getOrDefault(atom, Sign.PLUS);
                String string = scope.get(atom8);
                String string1 = scope.get(atom9);
                Signed<String> signed = scope.get(atom7);
                TypeSuffix typeSuffix = scope.get(atom6);
                return createFloat(ops, sign, string, string1, signed, typeSuffix, parseState);
            }
        );
        Atom<String> atom11 = Atom.of("string_hex_2");
        dictionary.put(atom11, new SimpleHexLiteralParseRule(2));
        Atom<String> atom12 = Atom.of("string_hex_4");
        dictionary.put(atom12, new SimpleHexLiteralParseRule(4));
        Atom<String> atom13 = Atom.of("string_hex_8");
        dictionary.put(atom13, new SimpleHexLiteralParseRule(8));
        Atom<String> atom14 = Atom.of("string_unicode_name");
        dictionary.put(atom14, new GreedyPatternParseRule(UNICODE_NAME, ERROR_INVALID_CHARACTER_NAME));
        Atom<String> atom15 = Atom.of("string_escape_sequence");
        dictionary.putComplex(
            atom15,
            Term.alternative(
                Term.sequence(StringReaderTerms.character('b'), Term.marker(atom15, "\b")),
                Term.sequence(StringReaderTerms.character('s'), Term.marker(atom15, " ")),
                Term.sequence(StringReaderTerms.character('t'), Term.marker(atom15, "\t")),
                Term.sequence(StringReaderTerms.character('n'), Term.marker(atom15, "\n")),
                Term.sequence(StringReaderTerms.character('f'), Term.marker(atom15, "\f")),
                Term.sequence(StringReaderTerms.character('r'), Term.marker(atom15, "\r")),
                Term.sequence(StringReaderTerms.character('\\'), Term.marker(atom15, "\\")),
                Term.sequence(StringReaderTerms.character('\''), Term.marker(atom15, "'")),
                Term.sequence(StringReaderTerms.character('"'), Term.marker(atom15, "\"")),
                Term.sequence(StringReaderTerms.character('x'), dictionary.named(atom11)),
                Term.sequence(StringReaderTerms.character('u'), dictionary.named(atom12)),
                Term.sequence(StringReaderTerms.character('U'), dictionary.named(atom13)),
                Term.sequence(StringReaderTerms.character('N'), StringReaderTerms.character('{'), dictionary.named(atom14), StringReaderTerms.character('}'))
            ),
            parseState -> {
                Scope scope = parseState.scope();
                String string = scope.getAny(atom15);
                if (string != null) {
                    return string;
                }
                String string1 = scope.getAny(atom11, atom12, atom13);
                if (string1 != null) {
                    int i = HexFormat.fromHexDigits(string1);
                    if (!Character.isValidCodePoint(i)) {
                        parseState.errorCollector()
                            .store(parseState.mark(), DelayedException.create(ERROR_INVALID_CODEPOINT, String.format(Locale.ROOT, "U+%08X", i)));
                        return null;
                    }
                    return Character.toString(i);
                }
                String string2 = scope.getOrThrow(atom14);

                int i1;
                try {
                    i1 = Character.codePointOf(string2);
                } catch (IllegalArgumentException var12x) {
                    parseState.errorCollector().store(parseState.mark(), ERROR_INVALID_CHARACTER_NAME);
                    return null;
                }

                return Character.toString(i1);
            }
        );
        Atom<String> atom16 = Atom.of("string_plain_contents");
        dictionary.put(atom16, PLAIN_STRING_CHUNK);
        Atom<List<String>> atom17 = Atom.of("string_chunks");
        Atom<String> atom18 = Atom.of("string_contents");
        Atom<String> atom19 = Atom.of("single_quoted_string_chunk");
        NamedRule<StringReader, String> namedRule1 = dictionary.put(
            atom19,
            Term.alternative(
                dictionary.namedWithAlias(atom16, atom18),
                Term.sequence(StringReaderTerms.character('\\'), dictionary.namedWithAlias(atom15, atom18)),
                Term.sequence(StringReaderTerms.character('"'), Term.marker(atom18, "\""))
            ),
            scope -> scope.getOrThrow(atom18)
        );
        Atom<String> atom20 = Atom.of("single_quoted_string_contents");
        dictionary.put(atom20, Term.repeated(namedRule1, atom17), scope -> joinList(scope.getOrThrow(atom17)));
        Atom<String> atom21 = Atom.of("double_quoted_string_chunk");
        NamedRule<StringReader, String> namedRule2 = dictionary.put(
            atom21,
            Term.alternative(
                dictionary.namedWithAlias(atom16, atom18),
                Term.sequence(StringReaderTerms.character('\\'), dictionary.namedWithAlias(atom15, atom18)),
                Term.sequence(StringReaderTerms.character('\''), Term.marker(atom18, "'"))
            ),
            scope -> scope.getOrThrow(atom18)
        );
        Atom<String> atom22 = Atom.of("double_quoted_string_contents");
        dictionary.put(atom22, Term.repeated(namedRule2, atom17), scope -> joinList(scope.getOrThrow(atom17)));
        Atom<String> atom23 = Atom.of("quoted_string_literal");
        dictionary.put(
            atom23,
            Term.alternative(
                Term.sequence(
                    StringReaderTerms.character('"'), Term.cut(), Term.optional(dictionary.namedWithAlias(atom22, atom18)), StringReaderTerms.character('"')
                ),
                Term.sequence(StringReaderTerms.character('\''), Term.optional(dictionary.namedWithAlias(atom20, atom18)), StringReaderTerms.character('\''))
            ),
            scope -> scope.getOrThrow(atom18)
        );
        Atom<String> atom24 = Atom.of("unquoted_string");
        dictionary.put(atom24, new UnquotedStringParseRule(1, ERROR_EXPECTED_UNQUOTED_STRING));
        Atom<T> atom25 = Atom.of("literal");
        Atom<List<T>> atom26 = Atom.of("arguments");
        dictionary.put(
            atom26, Term.repeatedWithTrailingSeparator(dictionary.forward(atom25), atom26, StringReaderTerms.character(TagParser.ELEMENT_SEPARATOR)), scope -> scope.getOrThrow(atom26)
        );
        Atom<T> atom27 = Atom.of("unquoted_string_or_builtin");
        dictionary.putComplex(
            atom27,
            Term.sequence(
                dictionary.named(atom24),
                Term.optional(Term.sequence(StringReaderTerms.character('('), dictionary.named(atom26), StringReaderTerms.character(')')))
            ),
            parseState -> {
                Scope scope = parseState.scope();
                String string = scope.getOrThrow(atom24);
                if (!string.isEmpty() && isAllowedToStartUnquotedString(string.charAt(0))) {
                    List<T> list = scope.get(atom26);
                    if (list != null) {
                        SnbtOperations.BuiltinKey builtinKey = new SnbtOperations.BuiltinKey(string, list.size());
                        SnbtOperations.BuiltinOperation builtinOperation = SnbtOperations.BUILTIN_OPERATIONS.get(builtinKey);
                        if (builtinOperation != null) {
                            return builtinOperation.run(ops, list, parseState);
                        }
                        parseState.errorCollector().store(parseState.mark(), DelayedException.create(ERROR_NO_SUCH_OPERATION, builtinKey.toString()));
                        return null;
                    } else if (string.equalsIgnoreCase("true")) {
                        return object;
                    } else if (string.equalsIgnoreCase("false")) {
                        return object1;
                    } else if (string.equalsIgnoreCase("null")) {
                        return Objects.requireNonNullElseGet(ops.empty(), () -> {
                            T nullString = ops.createString("null");
                            if ("null".equals(nullString)) { // 确定是 Java 类型的
                                return (T) CachedParseState.JAVA_NULL_VALUE_MARKER;
                            }
                            return nullString;
                        });
                    }
                    return ops.createString(string);
                }
                parseState.errorCollector().store(parseState.mark(), SnbtOperations.BUILTIN_IDS, ERROR_INVALID_UNQUOTED_START);
                return null;
            }
        );
        Atom<String> atom28 = Atom.of("map_key");
        dictionary.put(atom28, Term.alternative(dictionary.named(atom23), dictionary.named(atom24)), scope -> scope.getAnyOrThrow(atom23, atom24));
        Atom<Entry<String, T>> atom29 = Atom.of("map_entry");
        NamedRule<StringReader, Entry<String, T>> namedRule3 = dictionary.putComplex(
            atom29, Term.sequence(dictionary.named(atom28), StringReaderTerms.character(TagParser.NAME_VALUE_SEPARATOR), dictionary.named(atom25)), parseState -> {
                Scope scope = parseState.scope();
                String string = scope.getOrThrow(atom28);
                if (string.isEmpty()) {
                    parseState.errorCollector().store(parseState.mark(), ERROR_EMPTY_KEY);
                    return null;
                }
                T orThrow = scope.getOrThrow(atom25);
                return Map.entry(string, orThrow);
            }
        );
        Atom<List<Entry<String, T>>> atom30 = Atom.of("map_entries");
        dictionary.put(atom30, Term.repeatedWithTrailingSeparator(namedRule3, atom30, StringReaderTerms.character(TagParser.ELEMENT_SEPARATOR)), scope -> scope.getOrThrow(atom30));
        Atom<T> atom31 = Atom.of("map_literal");
        dictionary.put(atom31, Term.sequence(StringReaderTerms.character('{'), Scope.increaseDepth(), dictionary.named(atom30), Scope.decreaseDepth(), StringReaderTerms.character('}')), scope -> { // Paper - track depth
            List<Entry<String, T>> list = scope.getOrThrow(atom30);
            if (list.isEmpty()) {
                return object2;
            } else {
                Builder<T, T> builder = ImmutableMap.builderWithExpectedSize(list.size());

                for (Entry<String, T> entry : list) {
                    builder.put(ops.createString(entry.getKey()), entry.getValue());
                }

                return ops.createMap(builder.buildKeepingLast());
            }
        });
        Atom<List<T>> atom32 = Atom.of("list_entries");
        dictionary.put(
            atom32, Term.repeatedWithTrailingSeparator(dictionary.forward(atom25), atom32, StringReaderTerms.character(TagParser.ELEMENT_SEPARATOR)), scope -> scope.getOrThrow(atom32)
        );
        Atom<ArrayPrefix> atom33 = Atom.of("array_prefix");
        dictionary.put(
            atom33,
            Term.alternative(
                Term.sequence(StringReaderTerms.character('B'), Term.marker(atom33, ArrayPrefix.BYTE)),
                Term.sequence(StringReaderTerms.character('L'), Term.marker(atom33, ArrayPrefix.LONG)),
                Term.sequence(StringReaderTerms.character('I'), Term.marker(atom33, ArrayPrefix.INT))
            ),
            scope -> scope.getOrThrow(atom33)
        );
        Atom<List<IntegerLiteral>> atom34 = Atom.of("int_array_entries");
        dictionary.put(atom34, Term.repeatedWithTrailingSeparator(namedRule, atom34, StringReaderTerms.character(TagParser.ELEMENT_SEPARATOR)), scope -> scope.getOrThrow(atom34));
        Atom<T> atom35 = Atom.of("list_literal");
        dictionary.putComplex(
            atom35,
            Term.sequence(
                StringReaderTerms.character('['),
                Scope.increaseDepth(),
                Term.alternative(Term.sequence(dictionary.named(atom33), StringReaderTerms.character(';'), dictionary.named(atom34)), dictionary.named(atom32)),
                Scope.decreaseDepth(),
                StringReaderTerms.character(']')
            ),
            parseState -> {
                Scope scope = parseState.scope();
                ArrayPrefix arrayPrefix = scope.get(atom33);
                if (arrayPrefix != null) {
                    List<IntegerLiteral> list = scope.getOrThrow(atom34);
                    return list.isEmpty() ? arrayPrefix.create(ops) : arrayPrefix.create(ops, list, parseState);
                }
                List<T> list = scope.getOrThrow(atom32);
                return list.isEmpty() ? object3 : ops.createList(list.stream());
            }
        );
        NamedRule<StringReader, T> namedRule4 = dictionary.putComplex(
            atom25,
            Term.alternative(
                Term.sequence(Term.positiveLookahead(NUMBER_LOOKEAHEAD), Term.alternative(dictionary.namedWithAlias(atom10, atom25), dictionary.named(atom5))),
                Term.sequence(Term.positiveLookahead(StringReaderTerms.characters('"', '\'')), Term.cut(), dictionary.named(atom23)),
                Term.sequence(Term.positiveLookahead(StringReaderTerms.character('{')), Term.cut(), dictionary.namedWithAlias(atom31, atom25)),
                Term.sequence(Term.positiveLookahead(StringReaderTerms.character('[')), Term.cut(), dictionary.namedWithAlias(atom35, atom25)),
                dictionary.namedWithAlias(atom27, atom25)
            ),
            parseState -> {
                Scope scope = parseState.scope();
                String string = scope.get(atom23);
                if (string != null) {
                    return ops.createString(string);
                }
                IntegerLiteral integerLiteral = scope.get(atom5);
                return integerLiteral != null ? integerLiteral.create(ops, parseState) : scope.getOrThrow(atom25);
            }
        );
        return new Grammar<>(dictionary, namedRule4);
    }

    enum ArrayPrefix {
        BYTE(TypeSuffix.BYTE) {
            private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.wrap(new byte[0]);

            @Override
            public <T> T create(DynamicOps<T> ops) {
                return ops.createByteList(EMPTY_BUFFER);
            }

            @Nullable
            @Override
            public <T> T create(DynamicOps<T> ops, List<IntegerLiteral> values, ParseState<?> parseState) {
                ByteList list = new ByteArrayList();

                for (IntegerLiteral integerLiteral : values) {
                    Number number = this.buildNumber(integerLiteral, parseState);
                    if (number == null) {
                        return null;
                    }

                    list.add(number.byteValue());
                }

                return ops.createByteList(ByteBuffer.wrap(list.toByteArray()));
            }
        },
        INT(TypeSuffix.INT, TypeSuffix.BYTE, TypeSuffix.SHORT) {
            @Override
            public <T> T create(DynamicOps<T> ops) {
                return ops.createIntList(IntStream.empty());
            }

            @Nullable
            @Override
            public <T> T create(DynamicOps<T> ops, List<IntegerLiteral> values, ParseState<?> parseState) {
                IntStream.Builder builder = IntStream.builder();

                for (IntegerLiteral integerLiteral : values) {
                    Number number = this.buildNumber(integerLiteral, parseState);
                    if (number == null) {
                        return null;
                    }

                    builder.add(number.intValue());
                }

                return ops.createIntList(builder.build());
            }
        },
        LONG(TypeSuffix.LONG, TypeSuffix.BYTE, TypeSuffix.SHORT, TypeSuffix.INT) {
            @Override
            public <T> T create(DynamicOps<T> ops) {
                return ops.createLongList(LongStream.empty());
            }

            @Nullable
            @Override
            public <T> T create(DynamicOps<T> ops, List<IntegerLiteral> values, ParseState<?> parseState) {
                LongStream.Builder builder = LongStream.builder();

                for (IntegerLiteral integerLiteral : values) {
                    Number number = this.buildNumber(integerLiteral, parseState);
                    if (number == null) {
                        return null;
                    }

                    builder.add(number.longValue());
                }

                return ops.createLongList(builder.build());
            }
        };

        private final TypeSuffix defaultType;
        private final Set<TypeSuffix> additionalTypes;

        ArrayPrefix(final TypeSuffix defaultType, final TypeSuffix... additionalTypes) {
            this.additionalTypes = Set.of(additionalTypes);
            this.defaultType = defaultType;
        }

        public boolean isAllowed(TypeSuffix suffix) {
            return suffix == this.defaultType || this.additionalTypes.contains(suffix);
        }

        public abstract <T> T create(DynamicOps<T> ops);

        @Nullable
        public abstract <T> T create(DynamicOps<T> ops, List<IntegerLiteral> values, ParseState<?> parseState);

        @Nullable
        protected Number buildNumber(IntegerLiteral value, ParseState<?> parseState) {
            TypeSuffix typeSuffix = this.computeType(value.suffix);
            if (typeSuffix == null) {
                parseState.errorCollector().store(parseState.mark(), ERROR_INVALID_ARRAY_ELEMENT_TYPE);
                return null;
            }
            return (Number)value.create(JavaOps.INSTANCE, typeSuffix, parseState);
        }

        @Nullable
        private TypeSuffix computeType(IntegerSuffix suffix) {
            TypeSuffix typeSuffix = suffix.type();
            if (typeSuffix == null) {
                return this.defaultType;
            }
            return !this.isAllowed(typeSuffix) ? null : typeSuffix;
        }
    }

    enum Base {
        BINARY,
        DECIMAL,
        HEX
    }

    record IntegerLiteral(Sign sign, Base base, String digits, IntegerSuffix suffix) {
        private SignedPrefix signedOrDefault() {
            return Objects.requireNonNullElseGet(this.suffix.signed, () -> switch (this.base) {
                case BINARY, HEX -> SignedPrefix.UNSIGNED;
                case DECIMAL -> SignedPrefix.SIGNED;
            });
        }

        private String cleanupDigits(Sign sign) {
            boolean flag = needsUnderscoreRemoval(this.digits);
            if (sign != Sign.MINUS && !flag) {
                return this.digits;
            }
            StringBuilder stringBuilder = new StringBuilder();
            sign.append(stringBuilder);
            cleanAndAppend(stringBuilder, this.digits, flag);
            return stringBuilder.toString();
        }

        @Nullable
        public <T> T create(DynamicOps<T> ops, ParseState<?> parseState) {
            return this.create(ops, Objects.requireNonNullElse(this.suffix.type, TypeSuffix.INT), parseState);
        }

        @Nullable
        public <T> T create(DynamicOps<T> ops, TypeSuffix typeSuffix, ParseState<?> parseState) {
            boolean flag = this.signedOrDefault() == SignedPrefix.SIGNED;
            if (!flag && this.sign == Sign.MINUS) {
                parseState.errorCollector().store(parseState.mark(), ERROR_EXPECTED_NON_NEGATIVE_NUMBER);
                return null;
            }
            String string = this.cleanupDigits(this.sign);

            int i = switch (this.base) {
                case BINARY -> 2;
                case DECIMAL -> 10;
                case HEX -> 16;
            };

            try {
                if (flag) {
                    return switch (typeSuffix) {
                        case BYTE -> ops.createByte(Byte.parseByte(string, i));
                        case SHORT -> ops.createShort(Short.parseShort(string, i));
                        case INT -> ops.createInt(Integer.parseInt(string, i));
                        case LONG -> ops.createLong(Long.parseLong(string, i));
                        default -> {
                            parseState.errorCollector().store(parseState.mark(), ERROR_EXPECTED_INTEGER_TYPE);
                            yield null;
                        }
                    };
                }
                return switch (typeSuffix) {
                    case BYTE -> ops.createByte(com.google.common.primitives.UnsignedBytes.parseUnsignedByte(string, i));
                    case SHORT -> ops.createShort(parseUnsignedShort(string, i));
                    case INT -> ops.createInt(Integer.parseUnsignedInt(string, i));
                    case LONG -> ops.createLong(Long.parseUnsignedLong(string, i));
                    default -> {
                        parseState.errorCollector().store(parseState.mark(), ERROR_EXPECTED_INTEGER_TYPE);
                        yield null;
                    }
                };
            } catch (NumberFormatException var8) {
                parseState.errorCollector().store(parseState.mark(), createNumberParseError(var8));
                return null;
            }
        }
    }

    record IntegerSuffix(@Nullable SignedPrefix signed, @Nullable TypeSuffix type) {
        public static final IntegerSuffix EMPTY = new IntegerSuffix(null, null);
    }

    enum Sign {
        PLUS,
        MINUS;

        public void append(StringBuilder stringBuilder) {
            if (this == MINUS) {
                stringBuilder.append("-");
            }
        }
    }

    record Signed<T>(Sign sign, T value) {
    }

    enum SignedPrefix {
        SIGNED,
        UNSIGNED
    }

    static class SimpleHexLiteralParseRule extends GreedyPredicateParseRule {
        public SimpleHexLiteralParseRule(int minSize) {
            super(minSize, minSize, DelayedException.create(ERROR_EXPECTED_HEX_ESCAPE, String.valueOf(minSize)));
        }

        @Override
        protected boolean isAccepted(char c) {
            return switch (c) {
                case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'a', 'b', 'c', 'd', 'e', 'f' -> true;
                default -> false;
            };
        }
    }

    enum TypeSuffix {
        FLOAT,
        DOUBLE,
        BYTE,
        SHORT,
        INT,
        LONG
    }
}
