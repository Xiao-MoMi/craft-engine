package net.momirealms.craftengine.core.util;

public class DefaultStringReader implements StringReader {
    private static final char SYNTAX_ESCAPE = '\\';
    private static final char SYNTAX_DOUBLE_QUOTE = '"';
    private static final char SYNTAX_SINGLE_QUOTE = '\'';
    protected final String string;
    protected int cursor;
    protected int marker;

    public DefaultStringReader(DefaultStringReader other) {
        this.string = other.string;
        this.cursor = other.cursor;
        this.marker = other.marker;
    }

    public DefaultStringReader(String string) {
        this.string = string;
    }

    @Override
    public int getMarker() {
        return this.marker;
    }

    @Override
    public void setMarker(int marker) {
        this.marker = marker;
    }

    @Override
    public void restore() {
        this.cursor = this.marker;
    }

    @Override
    public String getString() {
        return this.string;
    }

    @Override
    public void setCursor(int cursor) {
        this.cursor = cursor;
    }

    @Override
    public int getRemainingLength() {
        return this.string.length() - this.cursor;
    }

    @Override
    public int getTotalLength() {
        return this.string.length();
    }

    @Override
    public int getCursor() {
        return this.cursor;
    }

    @Override
    public String getRead() {
        return this.string.substring(0, this.cursor);
    }

    @Override
    public String getRemaining() {
        return this.string.substring(this.cursor);
    }

    @Override
    public boolean canRead(int length) {
        return this.cursor + length <= this.string.length();
    }

    @Override
    public boolean canRead() {
        return this.canRead(1);
    }

    @Override
    public char peek() {
        return this.string.charAt(this.cursor);
    }

    @Override
    public char peek(int offset) {
        return this.string.charAt(this.cursor + offset);
    }

    @Override
    public char read() {
        if (!canRead()) {
            throw new RuntimeException("No more characters to read.");
        }
        return this.string.charAt(this.cursor++);
    }

    @Override
    public void skip() {
        if (!canRead()) {
            throw new RuntimeException("No more characters to skip.");
        }
        ++this.cursor;
    }

    @Override
    public void skip(int count) {
        if (!canRead()) {
            throw new RuntimeException("No more characters to skip.");
        }
        this.cursor += count;
    }

    @Override
    public char charAt(int index) {
        return this.string.charAt(index);
    }

    @Override
    public String substring(int start, int end) {
        return this.string.substring(start, end);
    }

    public static boolean isAllowedNumber(char c) {
        return c >= '0' && c <= '9' || c == '.' || c == '-';
    }

    public static boolean isNumber(char c) {
        return c >= '0' && c <= '9';
    }

    public static boolean isQuotedStringStart(char c) {
        return c == SYNTAX_DOUBLE_QUOTE || c == SYNTAX_SINGLE_QUOTE;
    }

    @Override
    public void skipWhitespace() {
        while (this.canRead() && Character.isWhitespace(this.peek())) {
            this.skip();
        }
    }

    @Override
    public void skipWhitespaceAndComment() {
        while (this.canRead()) {
            if (Character.isWhitespace(this.peek())) {
                this.skip();
            } else if (this.peek() == '/' && this.canRead(1)) {
                if (this.peek(1) == '/') {
                    this.skip(2);
                    while (this.canRead() && this.peek() != '\n' && this.peek() != '\r') {
                        this.skip();
                    }
                } else if (this.peek(1) == '*') {
                    this.skip(2);
                    while (this.canRead()) {
                        if (this.peek() == '*' && this.canRead(1) && this.peek(1) == '/') {
                            this.skip(2);
                            break;
                        }
                        this.skip();
                    }
                } else {
                    break;
                }
            } else {
                break;
            }
        }
    }

    @Override
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
        } catch (NumberFormatException e) {
            this.cursor = start;
            throw new RuntimeException("Failed to parse integer: " + number);
        }
    }

    @Override
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
        } catch (NumberFormatException e) {
            this.cursor = start;
            throw new RuntimeException("Failed to parse long: " + number);
        }
    }

    @Override
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
        } catch (NumberFormatException e) {
            this.cursor = start;
            throw new RuntimeException("Failed to parse double: " + number);
        }
    }

    @Override
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
        } catch (NumberFormatException e) {
            this.cursor = start;
            throw new RuntimeException("Failed to parse float: " + number);
        }
    }

    public static boolean isAllowedInUnquotedString(char c) {
        return c >= '0' && c <= '9' || c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c == '_' || c == '-' || c == '.' || c == '+';
    }

    @Override
    public String readUnquotedString() {
        int start = this.cursor;
        while (this.canRead() && isAllowedInUnquotedString(this.peek())) {
            this.skip();
        }
        return this.string.substring(start, this.cursor);
    }

    @Override
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

    @Override
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

    @Override
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
