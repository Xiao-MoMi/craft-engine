package net.momirealms.craftengine.bedrock.text;

import java.util.ArrayList;
import java.util.List;

public final class SidebarCodec {
    public static final String MARKER = "ce:sb/";
    public static final int PREFIX_BYTES = 18;
    public static final String SCORE_LAYERS = "§r~n";
    public static final int SCORE_HEADER_BYTES = 10;

    private SidebarCodec() {
    }

    public static String encode(int profile, String order, int width, ScoreColumn number, List<String> layers) {
        if (profile < 0 || profile > 9999 || width > 4096) {
            throw new UnsupportedOperationException("Sidebar exceeds layout capacity");
        }
        List<String> contents = new ArrayList<>(layers.size() + number.layers.size() + 2);
        String measure = spacing(Math.max(2, width));
        if (!number.layers.isEmpty()) {
            StringBuilder header = new StringBuilder(SCORE_LAYERS);
            ContainerTitleCodec.appendNumber(header, number.layers.size());
            measure = header.append(':').append(measure).toString();
        }
        contents.add(measure);
        contents.add(number.text.isEmpty() ? "§r" : number.text);
        contents.addAll(layers);
        contents.addAll(number.layers);
        int rowEntries = layers.size() + 2;
        if (profile == 0 && order == null) {
            return ContainerTitleCodec.encode(MARKER + "§r§rP0000:", contents, rowEntries);
        }
        StringBuilder prefix = new StringBuilder(PREFIX_BYTES).append(MARKER);
        prefix.append(order == null ? "§r" : order).append("§rP");
        ContainerTitleCodec.appendNumber(prefix, profile);
        return ContainerTitleCodec.encode(prefix.append(':').toString(), contents, rowEntries);
    }

    private static String spacing(int width) {
        StringBuilder result = new StringBuilder(2 + Math.ceilDiv(width, ContainerTitleLayout.MAX_SPACER)).append("§r");
        while (width > 0) {
            int step = Math.min(width, ContainerTitleLayout.MAX_SPACER);
            if (width - step == 1) {
                step--;
            }
            result.appendCodePoint(ContainerTitleLayout.SPACER_FIRST + step - 2);
            width -= step;
        }
        return result.toString();
    }

    public record ScoreColumn(String text, List<String> layers) {
        public static ScoreColumn plain(String text) {
            return new ScoreColumn(text, List.of());
        }

        public static ScoreColumn positioned(int width, List<String> layers) {
            if (width > 4096) {
                throw new UnsupportedOperationException("Sidebar score exceeds layout capacity");
            }
            return new ScoreColumn(spacing(Math.max(2, width)), layers);
        }
    }
}
