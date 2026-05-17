package net.momirealms.craftengine.core.plugin.config.template.argument;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;

import java.util.Map;

public final class WhenTemplateArgument implements TemplateArgument {
    public static final TemplateArgumentFactory<WhenTemplateArgument> FACTORY = new Factory();
    private final TemplateArgument result;

    private WhenTemplateArgument(TemplateArgument result) {
        this.result = result;
    }

    public static WhenTemplateArgument when(TemplateArgument result) {
        return new WhenTemplateArgument(result);
    }

    public TemplateArgument result() {
        return this.result;
    }

    @Override
    public Object get(String node, Map<String, TemplateArgument> arguments) {
        return this.result.get(node, arguments);
    }

    private static class Factory extends NestedTemplateArgumentFactory<WhenTemplateArgument> {

        @Override
        public WhenTemplateArgument create(ConfigSection section) {
            String source = section.getString("source");
            if (source == null) {
                return new WhenTemplateArgument(TemplateArguments.fromConfig(section.getValue("fallback")));
            }
            ConfigSection whenSection = section.getNonNullSection("when");
            if (whenSection.containsKey(source)) {
                return new WhenTemplateArgument(TemplateArguments.fromConfig(whenSection.getValue(source)));
            }
            return new WhenTemplateArgument(TemplateArguments.fromConfig(section.getValue("fallback")));
        }
    }
}
