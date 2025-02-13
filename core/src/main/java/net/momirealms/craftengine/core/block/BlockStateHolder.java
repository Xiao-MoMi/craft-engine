package net.momirealms.craftengine.core.block;

import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.registry.Holder;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class BlockStateHolder {
    protected final Holder<CustomBlock> owner;
    private final Reference2ObjectArrayMap<Property<?>, Comparable<?>> propertyMap;
    private Map<Property<?>, ImmutableBlockState[]> withMap;

    public BlockStateHolder(Holder<CustomBlock> owner, Reference2ObjectArrayMap<Property<?>, Comparable<?>> propertyMap) {
        this.owner = owner;
        this.propertyMap = new Reference2ObjectArrayMap<>(propertyMap);
    }

    public Holder<CustomBlock> owner() {
        return owner;
    }

    public <T extends Comparable<T>> ImmutableBlockState cycle(Property<T> property) {
        T currentValue = get(property);
        List<T> values = property.possibleValues();
        return with(property, getNextValue(values, currentValue));
    }

    protected static <T> T getNextValue(List<T> values, T currentValue) {
        int index = values.indexOf(currentValue);
        if (index == -1) {
            throw new IllegalArgumentException("Current value not found in possible values");
        }
        return values.get((index + 1) % values.size());
    }

    @Override
    public String toString() {
        if (propertyMap.isEmpty()) {
            return owner.value().id().toString();
        }
        return owner.value().id() + "[" + getPropertiesAsString() + "]";
    }

    public String getPropertiesAsString() {
        return propertyMap.entrySet().stream()
                .map(entry -> {
                    Property<?> property = entry.getKey();
                    return property.name() + "=" + formatValue(property, entry.getValue());
                })
                .collect(Collectors.joining(","));
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> String formatValue(Property<T> property, Comparable<?> value) {
        return property.valueName((T) value);
    }

    public Collection<Property<?>> getProperties() {
        return Collections.unmodifiableSet(propertyMap.keySet());
    }

    public <T extends Comparable<T>> boolean contains(Property<T> property) {
        return propertyMap.containsKey(property);
    }

    public <T extends Comparable<T>> T get(Property<T> property) {
        T value = getNullable(property);
        if (value == null) {
            throw new IllegalArgumentException("Property " + property + " not found in " + owner.value().id());
        }
        return value;
    }

    public <T extends Comparable<T>> T get(Property<T> property, T fallback) {
        return Objects.requireNonNullElse(getNullable(property), fallback);
    }

    @Nullable
    public <T extends Comparable<T>> T getNullable(Property<T> property) {
        Comparable<?> value = propertyMap.get(property);
        return value != null ? property.valueClass().cast(value) : null;
    }

    public <T extends Comparable<T>, V extends T> ImmutableBlockState with(Property<T> property, V value) {
        if (!propertyMap.containsKey(property)) {
            throw new IllegalArgumentException("Property " + property + " not found in " + owner.value().id());
        }
        return withInternal(property, value);
    }

    private <T extends Comparable<T>, V extends T> ImmutableBlockState withInternal(Property<T> property, V newValue) {
        if (newValue.equals(propertyMap.get(property))) {
            return (ImmutableBlockState) this;
        }

        int index = property.indexOf(newValue);
        if (index == -1) {
            throw new IllegalArgumentException("Invalid value " + newValue + " for property " + property);
        }

        return withMap.get(property)[index];
    }

    public void createWithMap(Map<Map<Property<?>, Comparable<?>>, ImmutableBlockState> states) {
        if (withMap != null) {
            throw new IllegalStateException("WithMap already initialized");
        }

        Reference2ObjectArrayMap<Property<?>, ImmutableBlockState[]> map = new Reference2ObjectArrayMap<>(propertyMap.size());

        for (Property<?> property : propertyMap.keySet()) {
            ImmutableBlockState[] statesArray = property.possibleValues().stream()
                    .map(value -> {
                        Map<Property<?>, Comparable<?>> testMap = new Reference2ObjectArrayMap<>(propertyMap);
                        testMap.put(property, value);
                        ImmutableBlockState state = states.get(testMap);
                        if (state == null) {
                            throw new IllegalStateException("Missing state for " + testMap);
                        }
                        return state;
                    })
                    .toArray(ImmutableBlockState[]::new);

            map.put(property, statesArray);
        }

        this.withMap = Map.copyOf(map);
    }

    public Map<Property<?>, Comparable<?>> propertyEntries() {
        return Collections.unmodifiableMap(propertyMap);
    }
}