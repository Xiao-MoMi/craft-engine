package net.momirealms.craftengine.core.plugin.config.template.argument;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.List;
import java.util.Map;

public final class TemplateArguments {
    public static final TemplateArgumentType<PlainStringTemplateArgument> PLAIN = register(Key.ce("plain"), PlainStringTemplateArgument.FACTORY);
    public static final TemplateArgumentType<SelfIncreaseIntTemplateArgument> SELF_INCREASE_INT = register(Key.ce("self_increase_int"), SelfIncreaseIntTemplateArgument.FACTORY);
    public static final TemplateArgumentType<MapTemplateArgument> MAP = register(Key.ce("map"), MapTemplateArgument.FACTORY);
    public static final TemplateArgumentType<ListTemplateArgument> LIST = register(Key.ce("list"), ListTemplateArgument.FACTORY);
    public static final TemplateArgumentType<NullTemplateArgument> NULL = register(Key.ce("null"), NullTemplateArgument.FACTORY);
    public static final TemplateArgumentType<ExpressionTemplateArgument> EXPRESSION = register(Key.ce("expression"), ExpressionTemplateArgument.FACTORY);
    public static final TemplateArgumentType<ConditionTemplateArgument> CONDITION = register(Key.ce("condition"), ConditionTemplateArgument.FACTORY);
    public static final TemplateArgumentType<ToUpperCaseTemplateArgument> TO_UPPER_CASE = register(Key.ce("to_upper_case"), ToUpperCaseTemplateArgument.FACTORY);
    public static final TemplateArgumentType<ToLowerCaseTemplateArgument> TO_LOWER_CASE = register(Key.ce("to_lower_case"), ToLowerCaseTemplateArgument.FACTORY);
    public static final TemplateArgumentType<ObjectTemplateArgument> OBJECT = register(Key.ce("object"), ObjectTemplateArgument.FACTORY);
    public static final TemplateArgumentType<WhenTemplateArgument> WHEN = register(Key.ce("when"), WhenTemplateArgument.FACTORY);

    private TemplateArguments() {}

    public static <T extends TemplateArgument> TemplateArgumentType<T> register(Key key, TemplateArgumentFactory<T> factory) {
        TemplateArgumentType<T> type = new TemplateArgumentType<>(key, factory);
        ((WritableRegistry<TemplateArgumentType<? extends TemplateArgument>>) BuiltInRegistries.TEMPLATE_ARGUMENT_TYPE)
                .register(ResourceKey.create(Registries.TEMPLATE_ARGUMENT_TYPE.location(), key), type);
        return type;
    }

    public static TemplateArgument fromConfig(ConfigValue value) {
        if (value == null) {
            return NullTemplateArgument.INSTANCE;
        }
        if (value.is(List.class)) {
            return ListTemplateArgument.list(value.getAsList());
        } else if (value.is(Map.class)) {
            return TemplateArguments.fromConfig(value.getAsSection());
        } else {
            return ObjectTemplateArgument.object(value.value());
        }
    }

    public static TemplateArgument fromConfig(ConfigSection section) {
        Object type = section.get("type");
        if (!(type instanceof String type0) || section.containsKey("__skip_template_argument__")) {
            return MapTemplateArgument.map(section.values());
        } else {
            Key key = Key.ce(type0);
            TemplateArgumentType<? extends TemplateArgument> argumentType = BuiltInRegistries.TEMPLATE_ARGUMENT_TYPE.getValue(key);
            if (argumentType == null) {
                throw new KnownResourceException("resource.template.unknown_argument_type", section.assemblePath("type"), key.asString());
            }
            return argumentType.factory().create(section);
        }
    }
}
