package net.momirealms.craftengine.core.block.property;

import com.google.common.collect.ImmutableMap;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.sparrow.nbt.StringTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.List;
import java.util.Optional;

public final class StringProperty extends Property<String> {
    public static final PropertyFactory<String> FACTORY = new Factory();
    private final List<String> values;
    private final ImmutableMap<String, String> names;

    private StringProperty(String name, List<String> values, String defaultValue) {
        super(name, String.class, defaultValue);

        this.values = List.copyOf(values);

        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        for (String value : values) {
            builder.put(value, value);
        }
        this.names = builder.build();

        this.setById(values.toArray(new String[0]));
    }

    @Override
    public List<String> possibleValues() {
        return this.values;
    }

    @Override
    public Optional<String> optional(String valueName) {
        return Optional.ofNullable(this.names.get(valueName));
    }

    @Override
    public Optional<Tag> createOptionalTag(String valueName) {
        return optional(valueName).map(StringTag::new);
    }

    @Override
    public Tag pack(String value) {
        return new StringTag(valueName(value));
    }

    @Override
    public String unpack(Tag tag) {
        if (tag instanceof StringTag stringTag) {
            return this.names.get(stringTag.getAsString());
        }
        throw new IllegalArgumentException("Invalid string tag: " + tag);
    }

    @Override
    public int idFor(String value) {
        int index = indexOf(value);
        if (index == -1) {
            throw new IllegalArgumentException("Invalid value: " + value);
        }
        return index;
    }

    @Override
    public String valueName(String value) {
        return value;
    }

    @Override
    public String valueByName(String name) {
        return this.names.get(name);
    }

    @Override
    public int indexOf(String value) {
        return this.values.indexOf(value);
    }

    @Override
    public int generateHashCode() {
        int i = super.generateHashCode();
        return 31 * i + this.values.hashCode();
    }

    public static StringProperty create(String name, List<String> values, String defaultValue) {
        return new StringProperty(name, values, defaultValue);
    }

    private static class Factory implements PropertyFactory<String> {

        @Override
        public Property<String> create(String name, ConfigSection section) {
            List<String> values = section.getNonEmptyList("values", ConfigValue::getAsString);
            String defaultValueName = section.getString("default");
            String defaultValue = values.stream()
                    .filter(e -> e.equals(defaultValueName))
                    .findFirst()
                    .orElseGet(values::getFirst);
            return StringProperty.create(
                    name,
                    values,
                    defaultValue
            );
        }
    }
}