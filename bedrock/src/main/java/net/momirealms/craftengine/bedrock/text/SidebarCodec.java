package net.momirealms.craftengine.bedrock.text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class SidebarCodec {
    public static final String MARKER = "ce:sb/";
    public static final int PREFIX_BYTES = 18;

    private SidebarCodec() {
    }

    public static String encode(int profile, String order, int width, String number, List<String> layers) {
        return encode(profile, order, width, number, layers, Function.identity());
    }

    public static <T> String encode(int profile, String order, int width, String number, List<T> layers, Function<T, String> serializeLayer) {
        if (profile < 0 || profile > 9999 || width > 4096) {
            throw new UnsupportedOperationException("Sidebar exceeds layout capacity");
        }
        List<String> contents = new ArrayList<>(layers.size() + 2);
        contents.add(spacing(Math.max(2, width)));
        contents.add(number.isEmpty() ? "§r" : number);
        for (T layer : layers) {
            contents.add(serializeLayer.apply(layer));
        }
        if (profile == 0 && order == null) {
            return ContainerTitleCodec.encode(MARKER + "§r§rP0000:", contents);
        }
        StringBuilder prefix = new StringBuilder(PREFIX_BYTES).append(MARKER);
        prefix.append(order == null ? "§r" : order).append("§rP");
        ContainerTitleCodec.appendNumber(prefix, profile);
        return ContainerTitleCodec.encode(prefix.append(':').toString(), contents);
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
}
