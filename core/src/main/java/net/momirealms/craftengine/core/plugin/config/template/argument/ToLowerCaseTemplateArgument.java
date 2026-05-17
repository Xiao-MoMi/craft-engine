package net.momirealms.craftengine.core.plugin.config.template.argument;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;

import java.util.Locale;
import java.util.Map;

public final class ToLowerCaseTemplateArgument implements TemplateArgument {
    public static final TemplateArgumentFactory<ToLowerCaseTemplateArgument> FACTORY = new Factory();
    private final String result;

    private ToLowerCaseTemplateArgument(String result) {
        this.result = result;
    }

    public static ToLowerCaseTemplateArgument toLowerCase(String result) {
        return new ToLowerCaseTemplateArgument(result.toLowerCase(Locale.ROOT));
    }

    public String result() {
        return this.result;
    }

    @Override
    public Object get(String node, Map<String, TemplateArgument> arguments) {
        return this.result;
    }

    private static class Factory implements TemplateArgumentFactory<ToLowerCaseTemplateArgument> {

        @Override
        public ToLowerCaseTemplateArgument create(ConfigSection section) {
            return new ToLowerCaseTemplateArgument(
                    section.getNonNullString("value").toLowerCase(section.getValue("locale", o -> TranslationManager.parseLocale(o.toString()), Locale.ROOT))
            );
        }
    }
}
