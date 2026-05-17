package net.momirealms.craftengine.bukkit.entity.data.animal;

import net.momirealms.craftengine.bukkit.entity.data.AgeableMobData;

public class AnimalData<T> extends AgeableMobData<T> {

    protected AnimalData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}
