package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import net.momirealms.craftengine.bukkit.plugin.reflection.ReflectionInitException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static java.util.Objects.requireNonNull;

public final class MBlockStateProperties {
    public static final Object WATERLOGGED;

    static {
        try {
            Object waterlogged = null;
            for (Field field : CoreReflections.clazz$BlockStateProperties.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers())) {
                    Object instance = field.get(null);
                    if (CoreReflections.clazz$Property.isInstance(instance) && CoreReflections.field$Property$name.get(instance).equals("waterlogged")) {
                        waterlogged = instance;
                    }
                }
            }
            WATERLOGGED = requireNonNull(waterlogged);
        } catch (ReflectiveOperationException e) {
            throw new ReflectionInitException("Failed to init MBlockStateProperties", e);
        }
    }
}
