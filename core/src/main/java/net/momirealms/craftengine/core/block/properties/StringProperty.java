package net.momirealms.craftengine.core.block.properties;

import com.google.common.collect.ImmutableMap;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.sparrow.nbt.StringTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class StringProperty extends Property<String> {
    public static final Factory FACTORY = new Factory();
    private final List<String> values;
    private final ImmutableMap<String, String> names;

    public StringProperty(String name, List<String> values, String defaultValue) {
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
        return Optional.ofNullable(this.names.get(valueName.toLowerCase(Locale.ENGLISH)));
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
            return names.get(stringTag.getAsString());
        }
        throw new IllegalArgumentException("Invalid string tag: " + tag);
    }

    @Override
    public final int idFor(String value) {
        int index = indexOf(value.toLowerCase(Locale.ENGLISH));
        if (index == -1) {
            throw new IllegalArgumentException("Invalid value: " + value);
        }
        return index;
    }

    @Override
    public String valueName(String value) {
        return value.toLowerCase(Locale.ENGLISH);
    }

    @Override
    public int indexOf(String value) {
        return values.indexOf(value.toLowerCase(Locale.ENGLISH));
    }

    @Override
    public int generateHashCode() {
        int i = super.generateHashCode();
        return 31 * i + this.values.hashCode();
    }

    public static StringProperty create(String name, List<String> values, String defaultValue) {
        return new StringProperty(name, values, defaultValue);
    }

    public static class Factory implements PropertyFactory {

        @Override
        public Property<?> create(String name, Map<String, Object> arguments) {
            List<String> values = MiscUtils.getAsStringList(arguments.get("values"))
                    .stream()
                    .map(it -> it.toLowerCase(Locale.ENGLISH))
                    .toList();
            String defaultValueName = arguments.getOrDefault("default", "").toString().toLowerCase(Locale.ENGLISH);
            String defaultValue = values.stream()
                    .filter(e -> e.toLowerCase(Locale.ENGLISH).equals(defaultValueName))
                    .findFirst()
                    .orElseGet(() -> {
                        CraftEngine.instance().logger().warn("Invalid default value '" + defaultValueName + "' for property '" + name + "'");
                        return values.getFirst();
                    });
            return StringProperty.create(name, values, defaultValue);
        }
    }
}