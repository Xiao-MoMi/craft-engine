package net.momirealms.craftengine.bedrock.text;

public final class TitleFrame {
    public static final String DISPLAY = "ce:title/display";
    public static final int ALPHA_END = 4;
    public static final int TITLE_CAPACITY_END = ALPHA_END + 5;
    public static final int SUBTITLE_CAPACITY_END = TITLE_CAPACITY_END + 5;
    public static final int HEADER_BYTES = SUBTITLE_CAPACITY_END + 2;
    public static final int FORMAT_BYTES = 13;
    public static final int ENTRY_BYTES = FORMAT_BYTES * 2;
    public static final Content EMPTY = new Content("", 0, 0, null);
    private static final String PREFIX = ContainerTitleCodec.TITLE_MARKER + 'N';
    private static final String RESET_PREFIX = "§r" + PREFIX;
    private final StringBuilder buffer = new StringBuilder();

    public static Content content(String text) {
        if (text.isEmpty()) {
            return EMPTY;
        }
        int start = text.startsWith(PREFIX) ? PREFIX.length() : (text.startsWith(RESET_PREFIX) ? RESET_PREFIX.length() : -1);
        if (start >= 0 && start + 5 <= text.length() && text.charAt(start + 4) == ':') {
            Content layered = readLayers(text, start);
            if (layered != null) {
                return layered;
            }
        }
        return new Content(text, 0, ContainerTitleCodec.utf8Length(text, 0), null);
    }

    private static Content readLayers(String text, int start) {
        int count = digits(text, start);
        int tableBytes = count * ContainerTitleCodec.ENTRY_BYTES;
        int entry = start + 5;
        int body = entry + tableBytes;
        if (count <= 0 || tableBytes > ContainerTitleCodec.MAX_BYTES || body > text.length()) {
            return null;
        }

        int[] lengths = new int[count];
        int offset = tableBytes;
        for (int i = 0; i < count; i++) {
            int length = format(text, entry + ContainerTitleCodec.FORMAT_BYTES);
            if (length < 0 || format(text, entry) != offset) {
                return null;
            }
            lengths[i] = length;
            offset += length;
            entry += ContainerTitleCodec.ENTRY_BYTES;
        }
        int bodyBytes = offset - tableBytes;
        if (bodyBytes != ContainerTitleCodec.utf8Length(text, body)) {
            return null;
        }
        return new Content(text, body, bodyBytes, lengths);
    }

    private static int format(String text, int index) {
        return text.startsWith("%.", index) && text.charAt(index + 6) == 's' ? digits(text, index + 2) : -1;
    }

    private static int digits(String text, int index) {
        int value = 0;
        for (int i = index; i < index + 4; i++) {
            int digit = text.charAt(i) - '0';
            if (digit < 0 || digit > 9) {
                return -1;
            }
            value = value * 10 + digit;
        }
        return value;
    }

    public static String encode(int alpha, int titleCapacity, int subtitleCapacity, Content title, Content subtitle) {
        TitleFrame frame = new TitleFrame();
        frame.setContent(titleCapacity, subtitleCapacity, title, subtitle);
        return frame.encode(alpha);
    }

    public void setContent(int titleCapacity, int subtitleCapacity, Content title, Content subtitle) {
        int bodyStart = HEADER_BYTES + (titleCapacity + subtitleCapacity) * ENTRY_BYTES;
        this.buffer.setLength(0);
        this.buffer.ensureCapacity(bodyStart + title.source.length() - title.body + subtitle.source.length() - subtitle.body);
        this.buffer.append("A000T");
        ContainerTitleCodec.appendNumber(this.buffer, titleCapacity);
        this.buffer.append('S');
        ContainerTitleCodec.appendNumber(this.buffer, subtitleCapacity);
        this.buffer.append(title.backgroundFlag()).append(subtitle.backgroundFlag());
        // Absolute UTF-8 endpoints let a reused label slice the current frame with no dependency on the previous frame.
        title.appendTable(this.buffer, titleCapacity, bodyStart);
        subtitle.appendTable(this.buffer, subtitleCapacity, bodyStart + title.bytes);
        this.buffer.append(title.source, title.body, title.source.length());
        this.buffer.append(subtitle.source, subtitle.body, subtitle.source.length());
    }

    public String encode(int alpha) {
        this.buffer.setCharAt(1, (char) ('0' + alpha / 100 % 10));
        this.buffer.setCharAt(2, (char) ('0' + alpha / 10 % 10));
        this.buffer.setCharAt(3, (char) ('0' + alpha % 10));
        return this.buffer.toString();
    }

    private static void appendEndpoint(StringBuilder frame, int offset) {
        frame.append("%.0000000000s");
        for (int digit = frame.length() - 2; offset > 0; digit--) {
            frame.setCharAt(digit, (char) ('0' + offset % 10));
            offset /= 10;
        }
    }

    public static final class Content {
        private final String source;
        private final int body;
        private final int bytes;
        private final int[] lengths;

        private Content(String source, int body, int bytes, int[] lengths) {
            this.source = source;
            this.body = body;
            this.bytes = bytes;
            this.lengths = lengths;
        }

        public String source() {
            return this.source;
        }

        public boolean layered() {
            return this.lengths != null;
        }

        public int size() {
            return this.lengths == null ? (this.bytes == 0 ? 0 : 1) : this.lengths.length;
        }

        private char backgroundFlag() {
            return !this.layered() && this.bytes > 0 ? 'P' : 'L';
        }

        private void appendTable(StringBuilder frame, int capacity, int offset) {
            int size = this.size();
            for (int i = 0; i < capacity; i++) {
                appendEndpoint(frame, offset);
                if (i < size) {
                    offset += this.lengths == null ? this.bytes : this.lengths[i];
                }
                appendEndpoint(frame, offset);
            }
        }
    }
}
