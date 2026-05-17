package net.momirealms.craftengine.bukkit.entity.data.projectile;

public class ThrowableProjectileData<T> extends ProjectileData<T> {

    protected ThrowableProjectileData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}
