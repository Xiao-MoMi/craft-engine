package net.momirealms.craftengine.core.loot.number;

import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NumberProviders {
    public static final Key FIXED = Key.of("craftengine:constant");
    public static final Key UNIFORM = Key.of("craftengine:uniform");

    static {
        register(FIXED, FixedNumberProvider.FACTORY);
        register(UNIFORM, UniformNumberProvider.FACTORY);
    }

    public static void register(Key key, NumberProviderFactory factory) {
        Holder.Reference<NumberProviderFactory> holder = ((WritableRegistry<NumberProviderFactory>) BuiltInRegistries.NUMBER_PROVIDER_FACTORY)
                .registerForHolder(new ResourceKey<>(Registries.NUMBER_PROVIDER_FACTORY.location(), key));
        holder.bindValue(factory);
    }

    static List<NumberProvider> fromMapList(List<Map<String, Object>> mapList) {
        if (mapList == null || mapList.isEmpty()) return List.of();
        List<NumberProvider> functions = new ArrayList<>();
        for (Map<String, Object> map : mapList) {
            functions.add(fromMap(map));
        }
        return functions;
    }

    public static NumberProvider fromMap(Map<String, Object> map) {
        String type = (String) map.get("type");
        if (type == null) {
            throw new NullPointerException("number type cannot be null");
        }
        Key key = Key.withDefaultNamespace(type, "craftengine");
        NumberProviderFactory factory = BuiltInRegistries.NUMBER_PROVIDER_FACTORY.getValue(key);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown number type: " + type);
        }
        return factory.create(map);
    }

    @SuppressWarnings("unchecked")
    public static NumberProvider fromObject(Object object) {
        if (object == null) {
            throw new NullPointerException("number argument is null");
        }
        if (object instanceof Number number) {
            return new FixedNumberProvider(number.floatValue());
        } else if (object instanceof String string) {
            return new FixedNumberProvider(Float.parseFloat(string));
        } else if (object instanceof Map<?,?> map) {
            return fromMap((Map<String, Object>) map);
        }
        throw new IllegalArgumentException("Can't convert " + object + " to " + NumberProvider.class.getSimpleName());
    }
}
