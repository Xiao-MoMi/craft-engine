package net.momirealms.craftengine.core.util;

public interface StringReader {

    static StringReader simple(String input) {
        return new DefaultStringReader(input);
    }

    int getMarker();

    void setMarker(int marker);

    void restore();

    String getString();

    void setCursor(int cursor);

    int getRemainingLength();

    int getTotalLength();

    int getCursor();

    String getRead();

    String getRemaining();

    boolean canRead(int length);

    boolean canRead();

    char peek();

    char peek(int offset);

    char read();

    void skip();

    void skip(int count);

    char charAt(int index);

    String substring(int start, int end);

    void skipWhitespace();

    void skipWhitespaceAndComment();

    int readInt();

    long readLong();

    double readDouble();

    float readFloat();

    String readUnquotedString();

    String readQuotedString();

    String readString();

    boolean readBoolean();
}
