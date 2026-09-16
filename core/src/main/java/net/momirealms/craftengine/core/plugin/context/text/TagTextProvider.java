package net.momirealms.craftengine.core.plugin.context.text;

import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.util.Key;

public final class TagTextProvider implements TextProvider {
    private final StringTemplate template;

    public TagTextProvider(String text) {
        this.template = StringTemplate.of(text);
    }

    public TagTextProvider(StringTemplate template) {
        this.template = template;
    }

    public static TagTextProvider of(String text) {
        return new TagTextProvider(text);
    }

    public static TagTextProvider of(StringTemplate template) {
        return new TagTextProvider(template);
    }

    @Override
    public String get(Context context) {
        return this.template.render(context);
    }

    @Override
    public Key type() {
        return TextProviders.TAG;
    }

    @Override
    public boolean isConstant() {
        return !this.template.hasTags();
    }
}
