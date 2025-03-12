package net.momirealms.craftengine.core.util;

public class StringReader {
    private static final char SYNTAX_ESCAPE = '\\';
    private static final char SYNTAX_DOUBLE_QUOTE = '"';
    private static final char SYNTAX_SINGLE_QUOTE = '\'';
    private final String string;
    private int cursor;

    public StringReader(StringReader other) {
        this.string = other.string;
        this.cursor = other.cursor;
    }

    public StringReader(String string) {
        this.string = string;
    }

    public String getString() {
        return this.string;
    }

    public void setCursor(int cursor) {
        this.cursor = cursor;
    }

    public int getRemainingLength() {
        return this.string.length() - this.cursor;
    }

    public int getTotalLength() {
        return this.string.length();
    }

    public int getCursor() {
        return this.cursor;
    }

    public String getRead() {
        return this.string.substring(0, this.cursor);
    }

    public String getRemaining() {
        return this.string.substring(this.cursor);
    }

    public boolean canRead(int length) {
        return this.cursor + length <= this.string.length();
    }

    public boolean canRead() {
        return this.canRead(1);
    }

    public char peek() {
        return this.string.charAt(this.cursor);
    }

    public char peek(int offset) {
        return this.string.charAt(this.cursor + offset);
    }

    public char read() {
        if (!canRead()) {
            throw new RuntimeException("No more characters to read.");
        }
        return this.string.charAt(this.cursor++);
    }

    public void skip() {
        if (!canRead()) {
            throw new RuntimeException("No more characters to skip.");
        }
        ++this.cursor;
    }

    public static boolean isAllowedNumber(char c) {
        return c >= '0' && c <= '9' || c == '.' || c == '-';
    }

    public static boolean isQuotedStringStart(char c) {
        return c == SYNTAX_DOUBLE_QUOTE || c == SYNTAX_SINGLE_QUOTE;
    }

    public void skipWhitespace() {
        while (this.canRead() && Character.isWhitespace(this.peek())) {
            this.skip();
        }
    }

    public int readInt() {
        int start = this.cursor;
        while (this.canRead() && isAllowedNumber(this.peek())) {
            this.skip();
        }
        String number = this.string.substring(start, this.cursor);
        if (number.isEmpty()) {
            throw new RuntimeException("Expected integer, but found empty string.");
        }
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException var4) {
            this.cursor = start;
            throw new RuntimeException("Failed to parse integer: " + number);
        }
    }

    public long readLong() {
        int start = this.cursor;
        while (this.canRead() && isAllowedNumber(this.peek())) {
            this.skip();
        }
        String number = this.string.substring(start, this.cursor);
        if (number.isEmpty()) {
            throw new RuntimeException("Expected long, but found empty string.");
        }
        try {
            return Long.parseLong(number);
        } catch (NumberFormatException var4) {
            this.cursor = start;
            throw new RuntimeException("Failed to parse long: " + number);
        }
    }

    public double readDouble() {
        int start = this.cursor;
        while (this.canRead() && isAllowedNumber(this.peek())) {
            this.skip();
        }
        String number = this.string.substring(start, this.cursor);
        if (number.isEmpty()) {
            throw new RuntimeException("Expected double, but found empty string.");
        }
        try {
            return Double.parseDouble(number);
        } catch (NumberFormatException var4) {
            this.cursor = start;
            throw new RuntimeException("Failed to parse double: " + number);
        }
    }

    public float readFloat() {
        int start = this.cursor;
        while (this.canRead() && isAllowedNumber(this.peek())) {
            this.skip();
        }
        String number = this.string.substring(start, this.cursor);
        if (number.isEmpty()) {
            throw new RuntimeException("Expected float, but found empty string.");
        }
        try {
            return Float.parseFloat(number);
        } catch (NumberFormatException var4) {
            this.cursor = start;
            throw new RuntimeException("Failed to parse float: " + number);
        }
    }

    public static boolean isAllowedInUnquotedString(char c) {
        return c >= '0' && c <= '9' || c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c == '_' || c == '-' || c == '.' || c == '+';
    }

    public String readUnquotedString() {
        int start = this.cursor;
        while (this.canRead() && isAllowedInUnquotedString(this.peek())) {
            this.skip();
        }
        return this.string.substring(start, this.cursor);
    }

    public String readQuotedString() {
        if (!this.canRead()) {
            return "";
        } else {
            char next = this.peek();
            if (!isQuotedStringStart(next)) {
                throw new RuntimeException("Expected quoted string, but found: " + next);
            } else {
                this.skip();
                return this.readStringUntil(next);
            }
        }
    }

    public String readStringUntil(char terminator) {
        StringBuilder result = new StringBuilder();
        boolean escaped = false;

        while (this.canRead()) {
            char c = this.read();
            if (escaped) {
                if (c != terminator && c != SYNTAX_ESCAPE) {
                    this.setCursor(this.getCursor() - 1);
                    throw new RuntimeException("Invalid escape sequence.");
                }
                result.append(c);
                escaped = false;
            } else if (c == SYNTAX_ESCAPE) {
                escaped = true;
            } else {
                if (c == terminator) {
                    return result.toString();
                }
                result.append(c);
            }
        }
        throw new RuntimeException("Unexpected end of input while reading string.");
    }

    public String readString() {
        if (!this.canRead()) {
            return "";
        } else {
            char next = this.peek();
            if (isQuotedStringStart(next)) {
                this.skip();
                return this.readStringUntil(next);
            } else {
                return this.readUnquotedString();
            }
        }
    }

    public boolean readBoolean() {
        int start = this.cursor;
        String value = this.readString();
        if (value.isEmpty()) {
            throw new RuntimeException("Expected boolean, but found empty string.");
        } else if (value.equals("true")) {
            return true;
        } else if (value.equals("false")) {
            return false;
        } else {
            this.cursor = start;
            throw new RuntimeException("Failed to parse boolean: " + value);
        }
    }
}
