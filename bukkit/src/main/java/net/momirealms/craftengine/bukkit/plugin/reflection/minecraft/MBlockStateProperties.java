package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import net.momirealms.craftengine.bukkit.plugin.reflection.ReflectionInitException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;

import static java.util.Objects.requireNonNull;

public final class MBlockStateProperties {
    public static final Object WATERLOGGED;
    public static final Object FACING;

    static {
        try {
            Object waterlogged = null;
            Object facing = null;
            for (Field field : CoreReflections.clazz$BlockStateProperties.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers())) {
                    Object instance = field.get(null);
                    if (CoreReflections.clazz$Property.isInstance(instance) && CoreReflections.field$Property$name.get(instance).equals("waterlogged")) {
                        waterlogged = instance;
                    } else if (CoreReflections.clazz$EnumProperty.isInstance(instance) && CoreReflections.field$Property$name.get(instance).equals("facing")) {
                        @SuppressWarnings("unchecked")
                        Collection<Object> values = (Collection<Object>) CoreReflections.field$EnumProperty$values.get(instance);
                        if (values.size() == CoreReflections.Instance.direction$values.length) {
                            facing = instance;
                        }
                    }
                }
            }
            WATERLOGGED = requireNonNull(waterlogged);
            FACING = requireNonNull(facing);
        } catch (ReflectiveOperationException e) {
            throw new ReflectionInitException("Failed to init MBlockStateProperties", e);
        }
    }
}
