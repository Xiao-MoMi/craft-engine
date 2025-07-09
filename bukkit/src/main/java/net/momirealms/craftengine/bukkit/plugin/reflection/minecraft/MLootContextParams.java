package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import net.momirealms.craftengine.bukkit.plugin.reflection.ReflectionInitException;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static java.util.Objects.requireNonNull;

public final class MLootContextParams {
    public static final Object THIS_ENTITY;
    public static final Object ORIGIN;
    public static final Object TOOL;
    public static final Object EXPLOSION_RADIUS;

    static {
        try {
            Object params$THIS_ENTITY = null;
            Object params$ORIGIN = null;
            Object params$TOOL = null;
            Object params$EXPLOSION_RADIUS = null;
            Field[] fields = CoreReflections.clazz$LootContextParams.getDeclaredFields();
            for (Field field : fields) {
                Type fieldType = field.getGenericType();
                if (fieldType instanceof ParameterizedType paramType) {
                    Type type = paramType.getActualTypeArguments()[0];
                    if (type == CoreReflections.clazz$Entity && params$THIS_ENTITY == null) {
                        params$THIS_ENTITY = field.get(null);
                    } else if (type == CoreReflections.clazz$ItemStack) {
                        params$TOOL = field.get(null);
                    } else if (type == CoreReflections.clazz$Vec3) {
                        params$ORIGIN = field.get(null);
                    } else if (type == Float.class) {
                        params$EXPLOSION_RADIUS = field.get(null);
                    }
                }
            }
            THIS_ENTITY = requireNonNull(params$THIS_ENTITY);
            TOOL = requireNonNull(params$TOOL);
            ORIGIN = requireNonNull(params$ORIGIN);
            EXPLOSION_RADIUS = requireNonNull(params$EXPLOSION_RADIUS);
        } catch (ReflectiveOperationException e) {
            throw new ReflectionInitException("Failed to init LootContextParams", e);
        }
    }
}
