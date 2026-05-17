package net.momirealms.craftengine.core.plugin.config.template.argument;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;

import java.util.Map;

public final class ConditionTemplateArgument implements TemplateArgument {
    public static final TemplateArgumentFactory<ConditionTemplateArgument> FACTORY = new Factory();
    private final TemplateArgument result;

    private ConditionTemplateArgument(TemplateArgument result) {
        this.result = result;
    }

    public TemplateArgument result() {
        return this.result;
    }

    public static ConditionTemplateArgument condition(final TemplateArgument result) {
        return new ConditionTemplateArgument(result);
    }

    @Override
    public Object get(String node, Map<String, TemplateArgument> arguments) {
        return this.result.get(node, arguments);
    }

    private static class Factory extends NestedTemplateArgumentFactory<ConditionTemplateArgument> {
        private static final String[] ON_TRUE = new String[]{"on_true", "on-true"};
        private static final String[] ON_FALSE = new String[]{"on_false", "on-false"};

        @Override
        public ConditionTemplateArgument create(ConfigSection section) {
            TemplateArgument onTrue = TemplateArguments.fromConfig(section.getValue(ON_TRUE));
            TemplateArgument onFalse = TemplateArguments.fromConfig(section.getValue(ON_FALSE));
            return new ConditionTemplateArgument(section.getBoolean("condition") ? onTrue : onFalse);
        }
    }
}
