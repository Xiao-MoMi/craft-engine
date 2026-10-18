package net.momirealms.craftengine.bedrock.geyser;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

final class ExperimentalSpearItemAdapter {
    private static final String USE_MODIFIERS = "minecraft:use_modifiers";
    private static final String KINETIC_WEAPON = "minecraft:kinetic_weapon";
    private final Field componentData;
    private final Method toBuilder;
    private final Method build;
    private final Method builder;

    ExperimentalSpearItemAdapter() throws ReflectiveOperationException {
        Class<?> definition = Class.forName("org.cloudburstmc.protocol.bedrock.data.definitions.SimpleItemDefinition");
        this.componentData = definition.getDeclaredField("componentData");
        this.componentData.setAccessible(true);
        this.toBuilder = Class.forName("org.cloudburstmc.nbt.NbtMap").getMethod("toBuilder");
        this.build = Class.forName("org.cloudburstmc.nbt.NbtMapBuilder").getMethod("build");
        this.builder = Class.forName("org.cloudburstmc.nbt.NbtMap").getMethod("builder");
    }

    static int apply(Map<String, String> spears) throws ReflectiveOperationException {
        if (spears.isEmpty()) {
            return 0;
        }
        ExperimentalSpearItemAdapter adapter = new ExperimentalSpearItemAdapter();
        Class<?> registries = Class.forName("org.geysermc.geyser.registry.Registries");
        Method registryGet = Class.forName("org.geysermc.geyser.registry.Registry").getMethod("get");
        Map<?, ?> versions = (Map<?, ?>) registryGet.invoke(registries.getField("ITEMS").get(null));
        Map<?, ?> javaItems = (Map<?, ?>) registryGet.invoke(registries.getField("JAVA_ITEM_IDENTIFIERS").get(null));
        List<?> defaults = (List<?>) registryGet.invoke(registries.getField("DEFAULT_DATA_COMPONENTS").get(null));
        Method javaId = Class.forName("org.geysermc.geyser.item.type.Item").getMethod("javaId");
        Method component = Class.forName("org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents")
                .getMethod("get", Class.forName("org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType"));
        Object kineticType = Class.forName("org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes").getField("KINETIC_WEAPON").get(null);
        Method definition = Class.forName("org.geysermc.geyser.registry.type.ItemMappings").getMethod("getDefinition", String.class);
        int patched = 0;
        for (Map.Entry<String, String> spear : spears.entrySet()) {
            Object weapon = component.invoke(defaults.get((int) javaId.invoke(javaItems.get(spear.getValue()))), kineticType);
            Map<String, Object> conditions = adapter.conditions(weapon);
            for (Object mappings : versions.values()) {
                Object custom = definition.invoke(mappings, spear.getKey());
                Object vanilla = definition.invoke(mappings, spear.getValue());
                if (custom != null && vanilla != null && adapter.patch(custom, vanilla, conditions)) {
                    patched++;
                }
            }
        }
        return patched;
    }

    boolean patch(Object custom, Object vanilla, Map<String, Object> conditions) throws ReflectiveOperationException {
        Map<?, ?> data = (Map<?, ?>) this.componentData.get(custom);
        Map<?, ?> components = (Map<?, ?>) data.get("components");
        Map<?, ?> nativeData = (Map<?, ?>) this.componentData.get(vanilla);
        Map<?, ?> nativeComponents = (Map<?, ?>) nativeData.get("components");
        Map<String, Object> updated = this.copy(components);
        if (!components.containsKey(USE_MODIFIERS) && nativeComponents.containsKey(USE_MODIFIERS)) {
            updated.put(USE_MODIFIERS, nativeComponents.get(USE_MODIFIERS));
            Map<?, ?> properties = (Map<?, ?>) components.get("item_properties");
            Map<?, ?> nativeProperties = (Map<?, ?>) nativeComponents.get("item_properties");
            Number duration = (Number) properties.get("use_duration");
            if (duration == null || duration.intValue() == 0) {
                updated.put("item_properties", this.with(properties, "use_duration", nativeProperties.get("use_duration")));
            }
        }
        // Geyser 2.11.3 omits damage/knockback conditions and emits a flat kinetic component. Preserve existing Java values.
        if (components.get(KINETIC_WEAPON) instanceof Map<?, ?> kinetic
                && nativeComponents.get(KINETIC_WEAPON) instanceof Map<?, ?> nativeKinetic) {
            Map<?, ?> values = kinetic.get(KINETIC_WEAPON) instanceof Map<?, ?> nested ? nested : kinetic;
            Map<String, Object> fixed = this.copy(values);
            for (Map.Entry<String, Object> condition : conditions.entrySet()) {
                fixed.putIfAbsent(condition.getKey(), condition.getValue());
            }
            Object fixedValues = this.build.invoke(fixed);
            if (nativeKinetic.containsKey(KINETIC_WEAPON)) {
                Map<String, Object> envelope = this.newBuilder();
                envelope.put(KINETIC_WEAPON, fixedValues);
                updated.put(KINETIC_WEAPON, this.build.invoke(envelope));
            } else {
                updated.put(KINETIC_WEAPON, fixedValues);
            }
        }
        if (updated.equals(components)) {
            return false;
        }
        // Run before clients connect. Keep the definition object shared by mapping/creative registries and replace its immutable NBT once.
        this.componentData.set(custom, this.with(data, "components", this.build.invoke(updated)));
        return true;
    }

    Map<String, Object> conditions(Object weapon) throws ReflectiveOperationException {
        Map<String, Object> conditions = this.newBuilder();
        this.addCondition(conditions, "damage_conditions", weapon.getClass().getMethod("damageConditions").invoke(weapon));
        this.addCondition(conditions, "knockback_conditions", weapon.getClass().getMethod("knockbackConditions").invoke(weapon));
        return conditions;
    }

    private void addCondition(Map<String, Object> conditions, String name, Object condition) throws ReflectiveOperationException {
        if (condition == null) {
            return;
        }
        Class<?> type = condition.getClass();
        Map<String, Object> values = this.newBuilder();
        values.put("max_duration", ((Number) type.getMethod("maxDurationTicks").invoke(condition)).shortValue());
        values.put("min_speed", type.getMethod("minSpeed").invoke(condition));
        values.put("min_relative_speed", type.getMethod("minRelativeSpeed").invoke(condition));
        conditions.put(name, this.build.invoke(values));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> newBuilder() throws ReflectiveOperationException {
        return (Map<String, Object>) this.builder.invoke(null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> copy(Map<?, ?> compound) throws ReflectiveOperationException {
        return (Map<String, Object>) this.toBuilder.invoke(compound);
    }

    private Object with(Map<?, ?> compound, String key, Object value) throws ReflectiveOperationException {
        Map<String, Object> builder = this.copy(compound);
        builder.put(key, value);
        return this.build.invoke(builder);
    }
}
