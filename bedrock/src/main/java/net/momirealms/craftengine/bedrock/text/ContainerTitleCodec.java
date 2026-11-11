package net.momirealms.craftengine.bedrock.text;

import java.util.List;

public final class ContainerTitleCodec {
    public static final int FORMAT_BYTES = 7;
    public static final int ENTRY_BYTES = FORMAT_BYTES * 2;
    public static final int COUNT_BYTES = 6;
    public static final int MAX_BYTES = 9999;

    private ContainerTitleCodec() {
    }

    public static String encode(int rows, List<String> layers) {
        if (layers.size() > MAX_BYTES / ENTRY_BYTES) {
            throw new UnsupportedOperationException("Title table exceeds protocol capacity");
        }
        String marker = ContainerTitleLayout.MARKER + rows + '/';
        StringBuilder countBuilder = new StringBuilder(COUNT_BYTES).append('N');
        appendNumber(countBuilder, layers.size());
        String count = countBuilder.append(':').toString();
        int headerBytes = ENTRY_BYTES * layers.size();
        // 索引表是 ASCII，每层两个 %.0000s 分别记录正文的 UTF-8 字节偏移和长度。
        StringBuilder payload = new StringBuilder(headerBytes);
        int byteOffset = headerBytes;
        int characterCount = headerBytes;
        for (String layer : layers) {
            // 重置前缀让纯数字文本在客户端表达式中仍按字符串处理。
            if (!layer.isEmpty() && !layer.startsWith("§r")) {
                throw new UnsupportedOperationException("Title layer has no formatting prefix");
            }
            if (layer.contains(marker) || layer.contains(count)) {
                throw new UnsupportedOperationException("Title contains its layout marker or layer count");
            }
            int byteLength = layerByteLength(layer);
            if (byteOffset + byteLength > MAX_BYTES) {
                throw new UnsupportedOperationException("Title exceeds the four-digit layout table");
            }
            appendFormat(payload, byteOffset);
            appendFormat(payload, byteLength);
            byteOffset += byteLength;
            characterCount += layer.length();
        }
        // 复用索引表的缓冲区追加正文，按 UTF-16 字符数预留容量，避免按字节数过量分配。
        payload.ensureCapacity(characterCount);
        for (String layer : layers) {
            payload.append(layer);
        }
        String encoded = payload.toString();
        verifySlices(encoded, layers);
        return marker + count + encoded;
    }

    private static int layerByteLength(String layer) {
        // 只统计长度，不创建 UTF-8 字节数组；孤立代理字符按 Java 编码器的 '?' 替换计一字节。
        int bytes = 0;
        for (int i = 0, length = layer.length(); i < length; i++) {
            char character = layer.charAt(i);
            if (character < 0x80) {
                bytes++;
            } else if (character < 0x800) {
                bytes += 2;
            } else if (!Character.isSurrogate(character)) {
                bytes += 3;
            } else if (Character.isHighSurrogate(character) && i + 1 < length && Character.isLowSurrogate(layer.charAt(i + 1))) {
                bytes += 4;
                i++;
            } else {
                bytes++;
            }
            if (bytes > MAX_BYTES) {
                throw new UnsupportedOperationException("Title exceeds the four-digit layout table");
            }
        }
        return bytes;
    }

    private static void appendFormat(StringBuilder output, int value) {
        output.append("%.");
        appendNumber(output, value);
        output.append('s');
    }

    private static void appendNumber(StringBuilder output, int value) {
        for (int divisor = 1000; divisor > 0; divisor /= 10) {
            output.append((char) ('0' + value / divisor % 10));
        }
    }

    private static void verifySlices(String payload, List<String> layers) {
        // 这里模拟 Java 字符串操作，位置使用 UTF-16 下标，不是索引表中的 UTF-8 字节偏移。
        int characterOffset = ENTRY_BYTES * layers.size();
        for (int i = 0; i < layers.size(); i++) {
            // UI 字符串减法会删除所有匹配项；模拟删除后的前缀，不创建完整的替换结果。
            int entry = i * ENTRY_BYTES;
            int lengthEntry = entry + FORMAT_BYTES;
            if (!startsWithAfterRemovingPrefix(payload, entry, payload, entry, FORMAT_BYTES) || !startsWithAfterRemovingPrefix(payload, lengthEntry, payload, lengthEntry, FORMAT_BYTES)) {
                throw new UnsupportedOperationException("Title layout table conflicts with its contents");
            }
            String expected = layers.get(i);
            if (!startsWithAfterRemovingPrefix(payload, characterOffset, expected, 0, expected.length())) {
                throw new UnsupportedOperationException("Title layer conflicts with its prefix");
            }
            characterOffset += expected.length();
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean startsWithAfterRemovingPrefix(String source, int prefixLength, String expected, int expectedOffset, int expectedLength) {
        // 首次匹配必定在开头；后续按非重叠匹配跳过，与 String.replace 的语义一致。
        int position = prefixLength;
        int matched = 0;
        while (matched < expectedLength) {
            if (position >= source.length()) {
                return false;
            }
            if (prefixLength > 0 && source.regionMatches(position, source, 0, prefixLength)) {
                position += prefixLength;
                continue;
            }
            if (source.charAt(position) != expected.charAt(expectedOffset + matched)) {
                return false;
            }
            position++;
            matched++;
        }
        return true;
    }
}
