package net.momirealms.craftengine.core.plugin.config.template.argument;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;

import java.util.Locale;
import java.util.Map;

public final class ToUpperCaseTemplateArgument implements TemplateArgument {
    public static final TemplateArgumentFactory<ToUpperCaseTemplateArgument> FACTORY = new Factory();
    private final String result;

    private ToUpperCaseTemplateArgument(String result) {
        this.result = result;
    }

    public static ToUpperCaseTemplateArgument toUpperCase(String result) {
        return new ToUpperCaseTemplateArgument(result.toUpperCase(Locale.ROOT));
    }

    public String result() {
        return this.result;
    }

    @Override
    public Object get(String node, Map<String, TemplateArgument> arguments) {
        return this.result;
    }

    private static class Factory implements TemplateArgumentFactory<ToUpperCaseTemplateArgument> {

        @Override
        public ToUpperCaseTemplateArgument create(ConfigSection section) {
            return new ToUpperCaseTemplateArgument(
                    section.getNonNullString("value").toUpperCase(section.getValue("locale", o -> TranslationManager.parseLocale(o.toString()), Locale.ROOT))
            );
        }
    }
}
