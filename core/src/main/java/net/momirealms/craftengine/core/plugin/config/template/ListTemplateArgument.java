package net.momirealms.craftengine.core.plugin.config.template;

import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.List;
import java.util.Map;

public class ListTemplateArgument implements TemplateArgument {
    public static final Factory FACTORY = new Factory();
    private final List<Object> value;

    public ListTemplateArgument(List<Object> value) {
        this.value = value;
    }

    @Override
    public List<Object> get() {
        return value;
    }

    @Override
    public Key type() {
        return TemplateArguments.LIST;
    }

    public static class Factory implements TemplateArgumentFactory {

        @Override
        public TemplateArgument create(Map<String, Object> arguments) {
            return new ListTemplateArgument(MiscUtils.castToList(arguments.getOrDefault("list", List.of()), false));
        }
    }
}
