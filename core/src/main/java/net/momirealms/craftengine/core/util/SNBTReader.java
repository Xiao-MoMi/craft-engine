package net.momirealms.craftengine.core.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class SNBTReader extends DefaultStringReader {
    private static final char COMPOUND_START = '{';
    private static final char COMPOUND_END = '}';
    private static final char LIST_START = '[';
    private static final char LIST_END = ']';
    private static final char STRING_DELIMITER = '"';
    private static final char SINGLE_QUOTES = '\'';
    private static final char DOUBLE_QUOTES = '"';
    private static final char KEY_VALUE_SEPARATOR = ':';
    private static final char ELEMENT_SEPARATOR = ',';

    private static final char ARRAY_DELIMITER = ';';
    private static final char BYTE_ARRAY = 'b';
    private static final char INT_ARRAY = 'i';
    private static final char LONG_ARRAY = 'l';

    public SNBTReader(String content) {
        super(content);
    }

    public Object deserializeAsJava() {
        Object result = this.parseValue();
        this.skipWhitespace();
        if (getCursor() != getTotalLength())
            throw new IllegalArgumentException("Extra content at end: " + substring(getCursor(), getTotalLength()));
        return result;
    }

    // 开始解析, 步进字符.
    private Object parseValue() {
        skipWhitespace();
        return switch (peek()) {
            case COMPOUND_START -> parseCompound();
            case LIST_START -> parseList();
            case DOUBLE_QUOTES -> {
                skip();
                yield readStringUntil(DOUBLE_QUOTES);
            }
            case SINGLE_QUOTES -> {
                skip();
                yield readStringUntil(SINGLE_QUOTES);
            }
            default -> parsePrimitive();
        };
    }

    // 解析包小肠 {}
    private Map<String, Object> parseCompound() {
        skip(); // 跳过 '{'
        skipWhitespace();

        Map<String, Object> compoundMap = new LinkedHashMap<>();

        if (canRead() && peek() != COMPOUND_END) {
            do {
                String key = parseKey();
                if (!canRead() || peek() != KEY_VALUE_SEPARATOR) {
                    throw new IllegalArgumentException("Expected ':' at position " + getCursor());
                }
                skip(); // 跳过 ':'
                Object value = parseValue();
                compoundMap.put(key, value);
                skipWhitespace();
            } while (canRead() && peek() == ELEMENT_SEPARATOR && ++super.cursor > 0 /* 跳过 ',' */);
        }

        if (!canRead() || peek() != COMPOUND_END) {
            throw new IllegalArgumentException("Expected '}' at position " + getCursor());
        }
        skip(); // 跳过 '}'
        return compoundMap;
    }

    // 解析列表值 [1, 2, 3]
    private Object parseList() {
        skip(); // 跳过 '['
        skipWhitespace();

        // 检查接下来的2个非空格字符, 确认是否要走数组解析.
        if (canRead()) {
            setMarker(cursor); // 记录指针, 尝试解析数组.
            char typeChar = Character.toLowerCase(peek());
            if (typeChar == BYTE_ARRAY || typeChar == INT_ARRAY || typeChar == LONG_ARRAY) {
                skip();
                skipWhitespace();
                if (canRead() && peek() == ARRAY_DELIMITER) {  // 下一个必须是 ';'
                    skip();
                    switch (typeChar) { // 解析并返回数组喵
                        case BYTE_ARRAY -> {
                            return parseArray(list -> {
                                byte[] bytes = new byte[list.size()];
                                for (int i = 0; i < bytes.length; i++) {
                                    bytes[i] = list.get(i).byteValue();
                                }
                                return bytes;
                            });
                        }
                        case INT_ARRAY -> {
                            return parseArray(list -> {
                                int[] ints = new int[list.size()];
                                for (int i = 0; i < ints.length; i++) {
                                    ints[i] = list.get(i).intValue();
                                }
                                return ints;
                            });
                        }
                        case LONG_ARRAY -> {
                            return parseArray(list -> {
                                long[] longs = new long[list.size()];
                                for (int i = 0; i < longs.length; i++) {
                                    longs[i] = list.get(i).longValue();
                                }
                                return longs;
                            });
                        }
                    }
                }
            }
            restore(); // 复原指针.
        }

        List<Object> elementList = new ArrayList<>();

        if (canRead() && peek() != LIST_END) {
            do {
                elementList.add(parseValue());
                skipWhitespace();
            } while (canRead() && peek() == ELEMENT_SEPARATOR && ++super.cursor > 0 /* 跳过 ',' */);
        }

        if (!canRead() || peek() != LIST_END) {
            throw new IllegalArgumentException("Expected ']' at position " + getCursor());
        }
        skip(); // 跳过 ']'
        return elementList;
    }

    // 解析数组 [I; 11, 41, 54]
    // ArrayType -> B, I, L.
    private Object parseArray(Function<List<Number>, Object> convertor) {
        skipWhitespace();
        // 用来暂存解析出的数字
        List<Number> elements = new ArrayList<>();
        if (canRead() && peek() != LIST_END) {
            do {
                Object element = parseValue();

                // 1.21.6的SNBT原版是支持 {key:[B;1,2b,0xFF]} 这种奇葩写法的, 越界部分会被自动舍弃, 如0xff的byte值为-1.
                // 如果需要和原版对齐, 那么只需要判断是否是数字就行了.
                // if (!(element instanceof Number number))
                //    throw new IllegalArgumentException("Error element type at pos " + getCursor());
                if (!(element instanceof Number number))
                    throw new IllegalArgumentException("Error parsing number at pos " + getCursor());

                elements.add(number); // 校验通过后加入
                skipWhitespace();
            } while (canRead() && peek() == ELEMENT_SEPARATOR && ++cursor > 0 /* 跳过 ',' */);
        }

        if (!canRead() || peek() != LIST_END)
            throw new IllegalArgumentException("Expected ']' at position " + getCursor());
        skip(); // 跳过 ']'
        return convertor.apply(elements);
    }

    // 解析Key值
    private String parseKey() {
        skipWhitespace();
        if (!canRead()) {
            throw new IllegalArgumentException("Unterminated key at " + getCursor());
        }

        // 如果有双引号就委托给string解析处理.
        char peek = peek();
        if (peek == STRING_DELIMITER) {
            skip();
            return readStringUntil(STRING_DELIMITER);
        } else if (peek == SINGLE_QUOTES) {
            skip();
            return readStringUntil(SINGLE_QUOTES);
        }

        int start = getCursor();
        while (canRead()) {
            char c = peek();
            if (c == ' ') break; // 忽略 key 后面的空格, { a :1} 应当解析成 {a:1}
            if (Character.isJavaIdentifierPart(c)) skip(); else break;
        }

        String key = substring(start, getCursor());
        skipWhitespace(); // 跳过 key 后面的空格.
        return key;
    }

    // 解析原生值
    private Object parsePrimitive() {
        // 先解析获取值的长度
        int tokenStart = getCursor();
        int lastWhitespace = 0; // 记录值末尾的空格数量,{a:炒鸡 大保健} 和 {a:  炒鸡 大保健  } 都应解析成 "炒鸡 大保健".
        boolean contentHasWhitespace = false; // 记录值中有没有空格.
        while (canRead()) {
            char c = peek();
            if (c == ',' || c == ']' || c == '}') break;
            skip();
            if (c == ' ') {
                lastWhitespace++; // 遇到空格先增加值, 代表值尾部空格数量.
                continue;
            }
            if (lastWhitespace > 0) {
                lastWhitespace = 0; // 遇到正常字符时清空记录的尾部空格数.
                contentHasWhitespace = true;
            }
        }
        int tokenLength = getCursor() - tokenStart - lastWhitespace; // 计算值长度需要再减去尾部空格.
        if (tokenLength == 0) return null; // 如果值长度为0则返回null.
        if (contentHasWhitespace) return substring(tokenStart, tokenStart + tokenLength); // 如果值的中间有空格, 一定是字符串, 可直接返回.

        // 布尔值检查
        if (tokenLength == 4) {
            if (matchesAt(tokenStart, "true")) return Boolean.TRUE;
            if (matchesAt(tokenStart, "null")) return null; // 支持 {key:null}.
        } else if (tokenLength == 5) {
            if (matchesAt(tokenStart, "false")) return Boolean.FALSE;
        }
        if (tokenLength > 1) {
            // 至少有1个字符，给了后缀的可能性
            char lastChar = charAt(tokenStart + tokenLength - 1);
            try {
                switch (lastChar) {
                    case 'b', 'B' -> {
                        return Byte.parseByte(substring(tokenStart, tokenStart + tokenLength - 1));
                    }
                    case 's', 'S' -> {
                        return Short.parseShort(substring(tokenStart, tokenStart + tokenLength - 1));
                    }
                    case 'l', 'L' -> {
                        return Long.parseLong(substring(tokenStart, tokenStart + tokenLength - 1));
                    }
                    case 'f', 'F' -> {
                        return Float.parseFloat(substring(tokenStart, tokenStart + tokenLength));
                    }
                    case 'd', 'D' -> {
                        return Double.parseDouble(substring(tokenStart, tokenStart + tokenLength));
                    }
                    default -> {
                        String fullString = substring(tokenStart, tokenStart + tokenLength);
                        try {
                            double d = Double.parseDouble(fullString);
                            if (d % 1 != 0 || fullString.contains(".") || fullString.contains("e")) {
                                return d;
                            } else {
                                return (int) d;
                            }
                        } catch (NumberFormatException e) {
                            return fullString;
                        }
                    }
                }
            } catch (NumberFormatException e) {
                return substring(tokenStart, tokenStart + tokenLength);
            }
        } else {
            char onlyChar = charAt(tokenStart);
            if (isNumber(onlyChar)) {
                return onlyChar - '0';
            } else {
                return String.valueOf(onlyChar);
            }
        }
    }

    // 工具函数: 快速检查布尔值字符串匹配, 忽略大小写.
    private boolean matchesAt(int start, String target) {
        for (int i = 0; i < target.length(); i++) {
            char c1 = charAt(start + i);
            char c2 = target.charAt(i);
            if (c1 != c2 && c1 != (c2 ^ 32)) return false; // 忽略大小写比较
        }
        return true;
    }
}
