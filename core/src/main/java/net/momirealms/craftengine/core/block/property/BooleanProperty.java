package net.momirealms.craftengine.core.block.property;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.sparrow.nbt.ByteTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.List;
import java.util.Optional;

public final class BooleanProperty extends Property<Boolean> {
    public static final PropertyFactory<Boolean> FACTORY = new Factory();
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
    public int idFor(final Boolean value) {
        return value ? 1 : 0;
    }

    @Override
    public String valueName(Boolean bool) {
        return bool.toString();
    }

    @Override
    public Boolean valueByName(String name) {
        if (name.equals("true")) {
            return true;
        } else if (name.equals("false")) {
            return false;
        }
        return null;
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

    private static class Factory implements PropertyFactory<Boolean> {

        @Override
        public Property<Boolean> create(String name, ConfigSection section) {
            return BooleanProperty.create(name, section.getBoolean("default"));
        }
    }
}