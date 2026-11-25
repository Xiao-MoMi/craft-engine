package net.momirealms.craftengine.bedrock.text;

import java.util.List;
import java.util.function.Function;

public final class BossBarTitleCodec {
    public static final int LEFT_ANCHORED_VERSION = 2;
    public static final String MARKER = "§o§rce:b/";
    public static final int MARKER_BYTES = ContainerTitleCodec.utf8Length(MARKER, 0);
    public static final int COUNT_BYTES = 2;
    public static final int POSITION_BYTES = 5;
    public static final int ENTRY_BYTES = POSITION_BYTES + 4;
    public static final int POSITION_BIAS = 50000;
    public static final int PERCENT_FLAG = 1000;
    public static final int MAX_NAME = 256;
    public static final int MAX_LAYERS = (MAX_NAME - 2 - MARKER.length() - COUNT_BYTES) / ENTRY_BYTES;
    public static final int PLAIN_PREFIX_BYTES = ContainerTitleCodec.utf8Length("§r", 0);
    public static final int STYLE_PREFIX_BYTES = ContainerTitleCodec.utf8Length("§r" + BossBarStyle.marker(0) + "§r", 0);

    private BossBarTitleCodec() {
    }

    public static String encode(ContainerTitleLayout.MeasuredLayout layout, boolean leftAnchored,
                                Function<List<ContainerTitleLayout.Run>, String> serialize) {
        List<List<ContainerTitleLayout.Run>> layers = layout.layers();
        if (layers.isEmpty()) {
            return null;
        }
        if (layers.size() == 1 && layers.getFirst().isEmpty()) {
            return "";
        }
        StringBuilder header = new StringBuilder().append(MARKER).append("00");
        StringBuilder body = new StringBuilder();
        int count = 0;
        int endBytes = 0;
        for (List<ContainerTitleLayout.Run> layer : layers) {
            int first = 0;
            int end = layer.size();
            int leading = 0;
            int trailing = 0;
            if (first < end && layer.get(first).part() < 0) {
                leading = spacingWidth(layer.get(first++).text());
            }
            if (first < end && layer.get(end - 1).part() < 0) {
                trailing = spacingWidth(layer.get(--end).text());
            }
            // 新 HUD 从整条标题的 Java 中心测量各层左边缘，旧 HUD 仍按每层自身宽度居中。
            int offset = leftAnchored ? 2 * (leading - ContainerTitleLayout.ORIGIN) - layout.width() : leading - trailing;
            int position = POSITION_BIAS + offset;
            if (position < 0 || position >= 100000) {
                return null;
            }
            String text = serialize.apply(layer.subList(first, end));
            if (leftAnchored) {
                text = compactWhiteText(text);
                if (text.isEmpty()) {
                    continue;
                }
            }
            count++;
            if (2 + header.length() + ENTRY_BYTES + body.length() + text.length() > MAX_NAME) {
                return null;
            }
            endBytes += ContainerTitleCodec.utf8Length(text, 0);
            int percent = text.indexOf('%');
            if (percent >= 0) {
                if (percent != text.length() - 1) {
                    return null;
                }
                // 用一个等长的 ASCII 占位字节隔开下一层的格式码，HUD 按表项恢复百分号。
                body.append(text, 0, percent).append('.');
            } else {
                body.append(text);
            }
            ContainerTitleCodec.appendNumber(header, position, 10000);
            ContainerTitleCodec.appendNumber(header, endBytes + (percent < 0 ? 0 : PERCENT_FLAG));
        }
        if (count == 0) {
            return "";
        }
        header.setCharAt(MARKER.length(), (char) ('0' + count / 10));
        header.setCharAt(MARKER.length() + 1, (char) ('0' + count % 10));
        return header.append(body).toString();
    }

    private static String compactWhiteText(String text) {
        // 每个新版 HUD 标签从白色开始。含其他颜色或装饰的层保留原样。
        boolean ink = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§') {
                if (++i == text.length() || (text.charAt(i) != 'r' && text.charAt(i) != 'f')) {
                    return text;
                }
            } else if (c != ' ') {
                ink = true;
            }
        }
        return ink ? text.replace("§r", "").replace("§f", "") : "";
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
