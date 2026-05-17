package net.momirealms.craftengine.core.plugin.context.text;

import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.util.Key;

public final class PlainTextProvider implements TextProvider {
    private final String text;

    public PlainTextProvider(String text) {
        this.text = text;
    }

    public static PlainTextProvider of(String text) {
        return new PlainTextProvider(text);
    }

    @Override
    public String get(Context context) {
        return this.text;
    }

    @Override
    public Key type() {
        return TextProviders.PLAIN;
    }
}
