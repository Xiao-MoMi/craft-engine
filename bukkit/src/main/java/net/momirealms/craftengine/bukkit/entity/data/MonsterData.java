package net.momirealms.craftengine.bukkit.entity.data;

public class MonsterData<T> extends PathfinderMobData<T> {

    public MonsterData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}
