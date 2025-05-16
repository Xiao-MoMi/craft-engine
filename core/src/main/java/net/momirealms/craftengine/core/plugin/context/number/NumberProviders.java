package net.momirealms.craftengine.core.plugin.context.number;

import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NumberProviders {
    public static final Key FIXED = Key.of("craftengine:fixed");
    public static final Key CONSTANT = Key.of("craftengine:constant");
    public static final Key UNIFORM = Key.of("craftengine:uniform");
    public static final Key EXPRESSION = Key.of("craftengine:expression");

    static {
        register(FIXED, FixedNumberProvider.FACTORY);
        register(CONSTANT, FixedNumberProvider.FACTORY);
        register(UNIFORM, UniformNumberProvider.FACTORY);
        register(EXPRESSION, ExpressionNumberProvider.FACTORY);
    }

    public static void register(Key key, NumberProviderFactory factory) {
        Holder.Reference<NumberProviderFactory> holder = ((WritableRegistry<NumberProviderFactory>) BuiltInRegistries.NUMBER_PROVIDER_FACTORY)
                .registerForHolder(new ResourceKey<>(Registries.NUMBER_PROVIDER_FACTORY.location(), key));
        holder.bindValue(factory);
    }

    public static List<NumberProvider> fromMapList(List<Map<String, Object>> mapList) {
        if (mapList == null || mapList.isEmpty()) return List.of();
        List<NumberProvider> functions = new ArrayList<>();
        for (Map<String, Object> map : mapList) {
            functions.add(fromMap(map));
        }
        return functions;
    }

    public static NumberProvider fromMap(Map<String, Object> map) {
        String type = ResourceConfigUtils.requireNonEmptyStringOrThrow(map.get("type"), "warning.config.number.missing_type");
        Key key = Key.withDefaultNamespace(type, Key.DEFAULT_NAMESPACE);
        NumberProviderFactory factory = BuiltInRegistries.NUMBER_PROVIDER_FACTORY.getValue(key);
        if (factory == null) {
            throw new LocalizedResourceConfigException("warning.config.number.invalid_type", type);
        }
        return factory.create(map);
    }

    @SuppressWarnings("unchecked")
    public static NumberProvider fromObject(Object object) {
        if (object == null) {
            throw new LocalizedResourceConfigException("warning.config.number.missing_argument");
        }
        if (object instanceof Number number) {
            return new FixedNumberProvider(number.floatValue());
        } else if (object instanceof Map<?,?> map) {
            return fromMap((Map<String, Object>) map);
        } else {
            String string = object.toString();
            if (string.contains("~")) {
                int first = string.indexOf('~');
                int second = string.indexOf('~', first + 1);
                if (second == -1) {
                    NumberProvider min = fromObject(string.substring(0, first));
                    NumberProvider max = fromObject(string.substring(first + 1));
                    return new UniformNumberProvider(min, max);
                } else {
                    throw new LocalizedResourceConfigException("warning.config.number.invalid_format", string);
                }
            } else if (string.contains("<") && string.contains(">") && string.contains(":")) {
                return new ExpressionNumberProvider(string);
            } else {
                try {
                    return new FixedNumberProvider(Float.parseFloat(string));
                } catch (NumberFormatException e) {
                    throw new LocalizedResourceConfigException("warning.config.number.invalid_format", e, string);
                }
            }
        }
    }
}
