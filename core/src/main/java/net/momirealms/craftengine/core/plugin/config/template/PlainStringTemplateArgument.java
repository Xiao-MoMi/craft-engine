package net.momirealms.craftengine.core.plugin.config.template;

import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

public class PlainStringTemplateArgument implements TemplateArgument {
    public static final Factory FACTORY = new Factory();
    private final String value;

    public PlainStringTemplateArgument(String value) {
        this.value = value;
    }

    @Override
    public String get() {
        return value;
    }

    @Override
    public Key type() {
        return TemplateArguments.PLAIN;
    }

    public static class Factory implements TemplateArgumentFactory {
        @Override
        public TemplateArgument create(Map<String, Object> arguments) {
            return new PlainStringTemplateArgument(arguments.getOrDefault("value", "").toString());
        }
    }
}
