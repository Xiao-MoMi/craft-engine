package net.momirealms.craftengine.core.plugin.context.text;

import net.momirealms.craftengine.core.util.Key;

public final class TextProviders {
    public static final Key PLAIN = Key.ce("plain");
    public static final Key TAG = Key.ce("tag");

    public static TextProvider fromString(String string) {
        if (string.contains("<") && string.contains(">")) {
            return TagTextProvider.of(string);
        }
        return PlainTextProvider.of(string);
    }
}
