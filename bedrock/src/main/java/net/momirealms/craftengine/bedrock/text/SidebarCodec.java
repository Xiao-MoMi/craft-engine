package net.momirealms.craftengine.bedrock.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SidebarCodec {
    public static final String MARKER = "ce:sb/";
    public static final int PREFIX_BYTES = 18;

    private SidebarCodec() {
    }

    public static String encode(int profile, String order, int width, String number, List<String> layers) {
        if (profile < 0 || profile > 9999 || width > 4096) {
            throw new UnsupportedOperationException("Sidebar exceeds layout capacity");
        }
        List<String> contents = new ArrayList<>(layers.size() + 2);
        contents.add("§r" + spacing(Math.max(2, width)));
        contents.add(number.isEmpty() ? "§r" : number);
        contents.addAll(layers);
        String prefix = MARKER + (order == null ? "§r§r" : order + "§r") + String.format(Locale.ROOT, "P%04d:", profile);
        return ContainerTitleCodec.encode(prefix, contents);
    }

    private static String spacing(int width) {
        StringBuilder result = new StringBuilder();
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
