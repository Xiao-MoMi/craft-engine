package net.momirealms.craftengine.bedrock.text;

import java.util.List;
import java.util.function.Function;

public final class BossBarTitleCodec {
    public static final int LEFT_ANCHORED_VERSION = 2;
    public static final int VERTICAL_OFFSET_VERSION = 3;
    public static final int FONT_FACE_VERSION = 4;
    public static final int COMPACT_POSITION_VERSION = 5;
    public static final String MARKER = "§o§rce:b/";
    public static final String COMPACT_MARKER = "§o§rce:c/";
    public static final int MARKER_BYTES = ContainerTitleCodec.utf8Length(MARKER, 0);
    public static final int COUNT_BYTES = 2;
    public static final int POSITION_BYTES = 5;
    public static final int ENTRY_BYTES = POSITION_BYTES + 4;
    public static final int POSITION_BIAS = 50000;
    public static final int COMPACT_POSITION_BYTES = 3;
    public static final int COMPACT_POSITION_BIAS = 500;
    public static final int PERCENT_FLAG = 1000;
    public static final int MAX_NAME = 256;
    public static final int MAX_LAYERS = (MAX_NAME - 2 - MARKER.length() - COUNT_BYTES) / ENTRY_BYTES;
    public static final int PLAIN_PREFIX_BYTES = ContainerTitleCodec.utf8Length("§r", 0);
    public static final int STYLE_PREFIX_BYTES = ContainerTitleCodec.utf8Length("§r" + BossBarStyle.marker(0) + "§r", 0);

    private BossBarTitleCodec() {
    }

    public static String encode(ContainerTitleLayout.MeasuredLayout layout, boolean leftAnchored,
                                Function<List<ContainerTitleLayout.Run>, String> serialize) {
        return encode(layout, leftAnchored, 1, serialize);
    }

    public static int entryBytes(int styleCount) {
        int bytes = ENTRY_BYTES;
        for (long tag = 2L * styleCount - 1; tag >= 10; tag /= 10) {
            bytes++;
        }
        return bytes;
    }

    public static String encodePositioned(ContainerTitleLayout.MeasuredLayout layout, int baselineCount,
                                          Function<List<ContainerTitleLayout.Run>, String> serialize) {
        return encode(layout, true, baselineCount, true, fitsCompactCoordinates(layout), serialize);
    }

    private static boolean fitsCompactCoordinates(ContainerTitleLayout.MeasuredLayout layout) {
        if (layout.halfLeft() == null) {
            return false;
        }
        for (int halfLeft : layout.halfLeft()) {
            long offset = (long) halfLeft - layout.width();
            if (offset < -COMPACT_POSITION_BIAS || offset >= COMPACT_POSITION_BIAS) {
                return false;
            }
        }
        return true;
    }

    public static String encode(ContainerTitleLayout.MeasuredLayout layout, boolean leftAnchored, int baselineCount,
                                Function<List<ContainerTitleLayout.Run>, String> serialize) {
        return encode(layout, leftAnchored, baselineCount, false, serialize);
    }

    public static String encode(ContainerTitleLayout.MeasuredLayout layout, boolean leftAnchored, int baselineCount, boolean fontFaces,
                                Function<List<ContainerTitleLayout.Run>, String> serialize) {
        return encode(layout, leftAnchored, baselineCount, fontFaces, false, serialize);
    }

    private static String encode(ContainerTitleLayout.MeasuredLayout layout, boolean leftAnchored, int baselineCount, boolean fontFaces,
                                 boolean compact, Function<List<ContainerTitleLayout.Run>, String> serialize) {
        List<List<ContainerTitleLayout.Run>> layers = layout.layers();
        if (layers.isEmpty()) {
            return null;
        }
        if (layers.size() == 1 && layers.getFirst().isEmpty()) {
            return "";
        }
        String marker = compact ? COMPACT_MARKER : MARKER;
        int bias = compact ? COMPACT_POSITION_BIAS : POSITION_BIAS;
        int styles = fontFaces ? baselineCount * 2 : baselineCount;
        int entryBytes = entryBytes(styles);
        int tagDivisor = 1;
        for (int i = ENTRY_BYTES; i < entryBytes; i++) {
            tagDivisor *= 10;
        }
        if (compact) {
            entryBytes -= POSITION_BYTES - COMPACT_POSITION_BYTES;
        }
        int headerCapacity = marker.length() + COUNT_BYTES + Math.min(layers.size(), MAX_NAME / entryBytes) * entryBytes;
        StringBuilder header = new StringBuilder(headerCapacity).append(marker).append("00");
        StringBuilder body = new StringBuilder();
        int count = 0;
        int endBytes = 0;
        for (int index = 0; index < layers.size(); index++) {
            List<ContainerTitleLayout.Run> layer = layers.get(index);
            int first = !layer.isEmpty() && layer.getFirst().part() < 0 ? 1 : 0;
            int end = layer.size();
            if (first < end && layer.getLast().part() < 0) {
                end--;
            }
            long position = bias + layerOffset(layout, index, first, end, leftAnchored);
            if (position < 0 || position >= 2 * bias) {
                return null;
            }
            String text = serialize.apply(first == 0 && end == layer.size() ? layer : layer.subList(first, end));
            int textLength = leftAnchored ? compactTextLength(text) : text.length();
            if (leftAnchored && textLength == 0) {
                continue;
            }
            if (textLength > MAX_NAME - 2 - header.length() - entryBytes - body.length()) {
                return null;
            }
            int bodyStart = body.length();
            appendLayerText(body, text, textLength);
            endBytes += ContainerTitleCodec.utf8Length(body, bodyStart);
            int percent = body.indexOf("%", bodyStart);
            if (percent >= 0) {
                if (percent != body.length() - 1) {
                    return null;
                }
                // 用一个等长的 ASCII 占位字节隔开下一层的格式码，HUD 按表项恢复百分号。
                body.setCharAt(percent, '.');
            }
            count++;
            ContainerTitleCodec.appendNumber(header, (int) position, bias / 5);
            // 名称最多 256 个 UTF-16 单元，累计 UTF-8 长度只需三位。前一位同时容纳基线和百分号标志。
            int baseline = layout.baselines() == null ? 0 : layout.baselines()[index];
            int style = baseline + (fontFaces && layout.unicode(index) ? baselineCount : 0);
            ContainerTitleCodec.appendNumber(header, style + (percent < 0 ? 0 : styles), tagDivisor);
            ContainerTitleCodec.appendNumber(header, endBytes, 100);
        }
        if (count == 0) {
            return "";
        }
        header.setCharAt(marker.length(), (char) ('0' + count / 10));
        header.setCharAt(marker.length() + 1, (char) ('0' + count % 10));
        return header.append(body).toString();
    }

    private static long layerOffset(ContainerTitleLayout.MeasuredLayout layout, int index, int first, int end, boolean leftAnchored) {
        if (leftAnchored && layout.halfLeft() != null) {
            return (long) layout.halfLeft()[index] - layout.width();
        }
        // 旧布局把位置信息放在正文两端的间距段中。
        List<ContainerTitleLayout.Run> layer = layout.layers().get(index);
        int leading = first == 0 ? 0 : spacingWidth(layer.getFirst().text());
        if (leftAnchored) {
            return 2 * (leading - ContainerTitleLayout.ORIGIN) - (long) layout.width();
        }
        int trailing = end == layer.size() ? 0 : spacingWidth(layer.getLast().text());
        return leading - trailing;
    }

    private static int compactTextLength(String text) {
        // 每个新版 HUD 标签从白色开始。含其他颜色或装饰的层保留原样。
        boolean ink = false;
        int length = text.length();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§') {
                if (++i == text.length() || (text.charAt(i) != 'r' && text.charAt(i) != 'f')) {
                    return text.length();
                }
                length -= 2;
            } else if (c != ' ' && (c < ContainerTitleLayout.SPACER_FIRST
                    || c > ContainerTitleLayout.SPACER_FIRST + ContainerTitleLayout.MAX_SPACER - 2)) {
                ink = true;
            }
        }
        return ink ? length : 0;
    }

    private static void appendLayerText(StringBuilder output, String text, int textLength) {
        if (textLength == text.length()) {
            output.append(text);
            return;
        }
        int start = 0;
        for (int marker = text.indexOf('§'); marker >= 0; marker = text.indexOf('§', start)) {
            output.append(text, start, marker);
            start = marker + 2;
        }
        output.append(text, start, text.length());
    }

    public static boolean fits(String encoded, String style) {
        int prefix = style == null ? 2 : style.length() + 4;
        return prefix + encoded.length() <= MAX_NAME;
    }

    private static int spacingWidth(String spacers) {
        int width = 0;
        for (int i = 0; i < spacers.length(); i++) {
            width += spacers.charAt(i) - ContainerTitleLayout.SPACER_FIRST + 2;
        }
        return width;
    }
}
