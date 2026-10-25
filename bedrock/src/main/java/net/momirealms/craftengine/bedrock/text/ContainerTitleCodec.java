package net.momirealms.craftengine.bedrock.text;

import java.nio.charset.StandardCharsets;
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
        StringBuilder header = new StringBuilder(headerBytes);
        int offset = headerBytes;
        for (String layer : layers) {
            // A reset keeps numeric text as a string in the client's expression evaluator.
            if (!layer.isEmpty() && !layer.startsWith("§r")) {
                throw new UnsupportedOperationException("Title layer has no formatting prefix");
            }
            if (layer.contains(marker) || layer.contains(count)) {
                throw new UnsupportedOperationException("Title contains its layout marker or layer count");
            }
            int length = layer.getBytes(StandardCharsets.UTF_8).length;
            if (offset + length > MAX_BYTES) {
                throw new UnsupportedOperationException("Title exceeds the four-digit layout table");
            }
            appendFormat(header, offset);
            appendFormat(header, length);
            offset += length;
        }
        StringBuilder payload = new StringBuilder(offset).append(header);
        for (String layer : layers) {
            payload.append(layer);
        }
        String encoded = payload.toString();
        verifySlices(encoded, layers);
        return marker + count + encoded;
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
        int position = ENTRY_BYTES * layers.size();
        for (int i = 0; i < layers.size(); i++) {
            // UI subtraction removes every match, so validate the actual staged operations before sending.
            int entry = i * ENTRY_BYTES;
            String skip = payload.replace(payload.substring(0, entry), "");
            String take = payload.replace(payload.substring(0, entry + FORMAT_BYTES), "");
            if (!skip.startsWith(payload.substring(entry, entry + FORMAT_BYTES))
                    || !take.startsWith(payload.substring(entry + FORMAT_BYTES, entry + ENTRY_BYTES))) {
                throw new UnsupportedOperationException("Title layout table conflicts with its contents");
            }
            String expected = layers.get(i);
            String remaining = payload.replace(payload.substring(0, position), "");
            if (!remaining.startsWith(expected)) {
                throw new UnsupportedOperationException("Title layer conflicts with its prefix");
            }
            position += expected.length();
        }
    }
}
