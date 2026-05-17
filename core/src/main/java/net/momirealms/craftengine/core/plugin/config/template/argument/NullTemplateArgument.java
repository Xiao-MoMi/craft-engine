package net.momirealms.craftengine.core.plugin.config.template.argument;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;

import java.util.Map;

public final class NullTemplateArgument implements TemplateArgument {
    public static final NullTemplateArgument INSTANCE = new NullTemplateArgument();
    public static final TemplateArgumentFactory<NullTemplateArgument> FACTORY = new Factory();

    private NullTemplateArgument() {
    }

    @Override
    public Object get(String node, Map<String, TemplateArgument> arguments) {
        return null;
    }

    private static class Factory implements TemplateArgumentFactory<NullTemplateArgument> {

        @Override
        public NullTemplateArgument create(ConfigSection section) {
            return NullTemplateArgument.INSTANCE;
        }
    }
}
