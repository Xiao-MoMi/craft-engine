package net.momirealms.craftengine.core.block.property;

import com.google.common.collect.ImmutableMap;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.sparrow.nbt.StringTag;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.*;

public final class EnumProperty<T extends Enum<T>> extends Property<T> {
    private final List<T> values;
    private final Map<String, T> names;
    private final int[] ordinalToIndex;
    private final int[] idLookupTable;

    private EnumProperty(String name, Class<T> type, List<T> values, T defaultValue) {
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
    public int idFor(T value) {
        final Class<T> target = this.valueClass();
        return ((value.getClass() != target && value.getDeclaringClass() != target)) ? -1 : this.idLookupTable[value.ordinal()];
    }

    @Override
    public String valueName(T value) {
        return value.name().toLowerCase(Locale.ENGLISH);
    }

    @Override
    public T valueByName(String name) {
        return this.names.get(name.toLowerCase(Locale.ENGLISH));
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

    public static <T extends Enum<T>> PropertyFactory<T> factory(Class<T> enumClass) {
        return new Factory<>(enumClass, null);
    }

    public static <T extends Enum<T>> PropertyFactory<T> factory(Class<T> enumClass, List<T> values) {
        return new Factory<>(enumClass, values);
    }

    private static class Factory<A extends Enum<A>> implements PropertyFactory<A> {
        private final Class<A> enumClass;
        private final List<A> defaultValues;

        public Factory(Class<A> enumClass, @Nullable List<A> defaultValues) {
            this.enumClass = enumClass;
            this.defaultValues = defaultValues;
        }

        @Override
        public Property<A> create(String name, ConfigSection section) {
            ConfigValue values = section.getValue("values");
            List<A> enums;
            if (values == null) {
                if (this.defaultValues == null || this.defaultValues.isEmpty()) {
                    enums = Arrays.asList(this.enumClass.getEnumConstants());
                } else {
                    enums = this.defaultValues;
                }
            } else {
                enums = values.getAsList(v -> v.getAsEnum(enumClass)).stream().distinct().toList();
            }
            String defaultValueName = section.getString("default");
            A defaultValue = enums.stream()
                    .filter(e -> e.name().toLowerCase(Locale.ROOT).equals(defaultValueName))
                    .findFirst()
                    .orElseGet(enums::getFirst);
            return EnumProperty.create(name, this.enumClass, enums, defaultValue);
        }
    }
}