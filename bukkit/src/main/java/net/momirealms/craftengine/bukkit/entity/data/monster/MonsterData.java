package net.momirealms.craftengine.bukkit.entity.data.monster;

import net.momirealms.craftengine.bukkit.entity.data.PathfinderMobData;

public class MonsterData<T> extends PathfinderMobData<T> {

    protected MonsterData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}
