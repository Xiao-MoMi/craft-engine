package net.momirealms.craftengine.core.block.properties;

import com.google.common.collect.ImmutableMap;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.sparrow.nbt.StringTag;
import net.momirealms.sparrow.nbt.Tag;

import java.lang.reflect.Array;
import java.util.*;

public class EnumProperty<T extends Enum<T>> extends Property<T> {
    private final List<T> values;
    private final Map<String, T> names;
    private final int[] ordinalToIndex;
    private final int[] idLookupTable;

    public EnumProperty(String name, Class<T> type, List<T> values, T defaultValue) {
        super(name, type, defaultValue);
        this.values = List.copyOf(values);
        T[] enums = type.getEnumConstants();
        this.ordinalToIndex = new int[enums.length];

        for (T enum_ : enums) {
            this.ordinalToIndex[enum_.ordinal()] = values.indexOf(enum_);
        }

        ImmutableMap.Builder<String, T> builder = ImmutableMap.builder();
        for (T enum2 : values) {
            String string = enum2.name().toLowerCase(Locale.ENGLISH);
            builder.put(string, enum2);
        }

        this.names = builder.buildOrThrow();
        Class<T> clazz = this.valueClass();

        int id = 0;
        this.idLookupTable = new int[clazz.getEnumConstants().length];
        Arrays.fill(this.idLookupTable, -1);
        @SuppressWarnings("unchecked")
        final T[] byId = (T[]) Array.newInstance(clazz, values.size());

        for (final T value : values) {
            int valueId = id++;
            this.idLookupTable[value.ordinal()] = valueId;
            byId[valueId] = value;
        }

        this.setById(byId);
    }

    @Override
    public List<T> possibleValues() {
        return this.values;
    }

    @Override
    public Optional<T> optional(String valueName) {
        return Optional.ofNullable(this.names.get(valueName));
    }

    @Override
    public Optional<Tag> createOptionalTag(String valueName) {
        return optional(valueName).map(it -> new StringTag(names.get(valueName).name().toLowerCase(Locale.ENGLISH)));
    }

    @Override
    public Tag pack(T value) {
        return new StringTag(valueName(value));
    }

    @Override
    public T unpack(Tag tag) {
        if (tag instanceof StringTag stringTag) {
            return names.get(stringTag.getAsString());
        }
        throw new IllegalArgumentException("Invalid string tag: " + tag);
    }

    @Override
    public final int idFor(T value) {
        final Class<T> target = this.valueClass();
        return ((value.getClass() != target && value.getDeclaringClass() != target)) ? -1 : this.idLookupTable[value.ordinal()];
    }

    @Override
    public String valueName(T value) {
        return value.name().toLowerCase(Locale.ENGLISH);
    }

    @Override
    public int indexOf(T value) {
        return this.ordinalToIndex[value.ordinal()];
    }

    @Override
    public int generateHashCode() {
        int i = super.generateHashCode();
        return 31 * i + this.values.hashCode();
    }

    public static <T extends Enum<T>> EnumProperty<T> create(String name, Class<T> type, List<T> values, T defaultValue) {
        return new EnumProperty<>(name, type, values, defaultValue);
    }

    public static class Factory<A extends Enum<A>> implements PropertyFactory {
        private final Class<A> enumClass;

        public Factory(Class<A> enumClass) {
            this.enumClass = enumClass;
        }

        @Override
        public Property<?> create(String name, Map<String, Object> arguments) {
            List<A> enums = Arrays.asList(enumClass.getEnumConstants());
            String defaultValueName = arguments.getOrDefault("default", "").toString().toLowerCase(Locale.ENGLISH);
            A defaultValue = enums.stream()
                    .filter(e -> e.name().toLowerCase(Locale.ENGLISH).equals(defaultValueName))
                    .findFirst()
                    .orElseGet(() -> {
                        CraftEngine.instance().logger().warn("Invalid default value '" + defaultValueName + "' for property '" + name + "'");
                        return enums.get(0);
                    });
            return EnumProperty.create(name, enumClass, enums, defaultValue);
        }
    }
}