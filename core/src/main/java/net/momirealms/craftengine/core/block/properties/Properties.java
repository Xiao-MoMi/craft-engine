package net.momirealms.craftengine.core.block.properties;

import net.momirealms.craftengine.core.block.state.properties.*;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.*;

import java.util.Map;

public class Properties {
    public static final Key BOOLEAN = Key.of("craftengine:boolean");
    public static final Key INT = Key.of("craftengine:int");
    public static final Key STRING = Key.of("craftengine:string");
    public static final Key AXIS = Key.of("craftengine:axis");
    public static final Key HORIZONTAL_DIRECTION = Key.of("craftengine:horizontal_direction");
    public static final Key DIRECTION = Key.of("craftengine:direction");
    public static final Key SINGLE_BLOCK_HALF = Key.of("craftengine:single_block_half");
    public static final Key DOUBLE_BLOCK_HALF = Key.of("craftengine:double_block_half");
    public static final Key HINGE = Key.of("craftengine:hinge");
    public static final Key STAIRS_SHAPE = Key.of("craftengine:stairs_shape");
    public static final Key SLAB_TYPE = Key.of("craftengine:slab_type");

    static {
        register(BOOLEAN, BooleanProperty.FACTORY);
        register(INT, IntegerProperty.FACTORY);
        register(STRING, StringProperty.FACTORY);
        register(AXIS, new EnumProperty.Factory<>(Direction.Axis.class));
        register(DIRECTION, new EnumProperty.Factory<>(Direction.class));
        register(Key.of("craftengine:6-direction"), new EnumProperty.Factory<>(Direction.class));
        register(HORIZONTAL_DIRECTION, new EnumProperty.Factory<>(HorizontalDirection.class));
        register(Key.of("craftengine:4-direction"), new EnumProperty.Factory<>(HorizontalDirection.class));
        register(SINGLE_BLOCK_HALF, new EnumProperty.Factory<>(SingleBlockHalf.class));
        register(DOUBLE_BLOCK_HALF, new EnumProperty.Factory<>(DoubleBlockHalf.class));
        register(HINGE, new EnumProperty.Factory<>(DoorHinge.class));
        register(STAIRS_SHAPE, new EnumProperty.Factory<>(StairsShape.class));
        register(SLAB_TYPE, new EnumProperty.Factory<>(SlabType.class));
    }

    public static void register(Key key, PropertyFactory factory) {
        ((WritableRegistry<PropertyFactory>) BuiltInRegistries.PROPERTY_FACTORY).register(ResourceKey.create(Registries.PROPERTY_FACTORY.location(), key), factory);
    }

    public static Property<?> fromMap(String name, Map<String, Object> map) {
        String type = ResourceConfigUtils.requireNonEmptyStringOrThrow(map.get("type"), "warning.config.block.state.property.missing_type");
        Key key = Key.withDefaultNamespace(type, Key.DEFAULT_NAMESPACE);
        PropertyFactory factory = BuiltInRegistries.PROPERTY_FACTORY.getValue(key);
        if (factory == null) {
            throw new LocalizedResourceConfigException("warning.config.block.state.property.invalid_type", key.toString(), name);
        }
        return factory.create(name, map);
    }
}
