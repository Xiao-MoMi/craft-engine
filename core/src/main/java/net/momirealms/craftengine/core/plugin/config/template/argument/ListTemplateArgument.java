package net.momirealms.craftengine.core.plugin.config.template.argument;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;

import java.util.List;
import java.util.Map;

public final class ListTemplateArgument implements TemplateArgument {
    public static final TemplateArgumentFactory<ListTemplateArgument> FACTORY = new Factory();
    private final List<Object> value;

    private ListTemplateArgument(List<Object> value) {
        this.value = value;
    }

    public static ListTemplateArgument list(List<Object> value) {
        return new ListTemplateArgument(value);
    }

    @Override
    public List<Object> get(String node, Map<String, TemplateArgument> arguments) {
        return this.value;
    }

    private static class Factory implements TemplateArgumentFactory<ListTemplateArgument> {
        private static final String[] LIST = new String[] {"list", "value"};

        @Override
        public ListTemplateArgument create(ConfigSection section) {
            return new ListTemplateArgument(section.getNonNullList(LIST));
        }
    }
}
