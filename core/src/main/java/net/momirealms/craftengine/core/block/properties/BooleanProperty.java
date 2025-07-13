package net.momirealms.craftengine.core.block.properties;

import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.sparrow.nbt.ByteTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BooleanProperty extends Property<Boolean> {
    public final static Factory FACTORY = new Factory();
    private static final List<Boolean> VALUES = List.of(true, false);
    private static final Boolean[] BY_ID = new Boolean[]{ Boolean.FALSE, Boolean.TRUE };
    private static final ByteTag TRUE = new ByteTag((byte) 1);
    private static final ByteTag FALSE = new ByteTag((byte) 0);

    private BooleanProperty(String name, boolean defaultValue) {
        super(name, Boolean.class, defaultValue);
        this.setById(BY_ID);
    }

    @Override
    public List<Boolean> possibleValues() {
        return VALUES;
    }

    @Override
    public Optional<Boolean> optional(String valueName) {
        return switch (valueName) {
            case "true" -> Optional.of(true);
            case "false" -> Optional.of(false);
            default -> Optional.empty();
        };
    }

    @Override
    public Optional<Tag> createOptionalTag(String valueName) {
        return optional(valueName).map(ByteTag::new);
    }

    @Override
    public Tag pack(Boolean value) {
        return value ? TRUE : FALSE;
    }

    @Override
    public final int idFor(final Boolean value) {
        return value ? 1 : 0;
    }

    @Override
    public String valueName(Boolean bool) {
        return bool.toString();
    }

    @Override
    public int indexOf(Boolean bool) {
        return bool ? 0 : 1;
    }

    @Override
    public Boolean unpack(Tag tag) {
        if (tag instanceof ByteTag byteTag) {
            return byteTag.booleanValue();
        }
        throw new IllegalArgumentException("Invalid boolean tag: " + tag);
    }

    public static BooleanProperty create(String name, boolean defaultValue) {
        return new BooleanProperty(name, defaultValue);
    }

    public static class Factory implements PropertyFactory {
        @Override
        public Property<?> create(String name, Map<String, Object> arguments) {
            boolean bool = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("default", false), "default");
            return BooleanProperty.create(name, bool);
        }
    }
}