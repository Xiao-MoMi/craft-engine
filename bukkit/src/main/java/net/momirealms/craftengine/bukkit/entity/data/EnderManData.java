package net.momirealms.craftengine.bukkit.entity.data;

import java.util.Optional;

public class EnderManData<T> extends MonsterData<T> {
    public static final EnderManData<Optional<Object>> CarryState = new EnderManData<>(EnderManData.class, EntityDataValue.Serializers$OPTIONAL_BLOCK_STATE, Optional.empty());
    public static final EnderManData<Boolean> Creepy = new EnderManData<>(EnderManData.class, EntityDataValue.Serializers$BOOLEAN, false);
    public static final EnderManData<Boolean> StaredAt = new EnderManData<>(EnderManData.class, EntityDataValue.Serializers$BOOLEAN, false);

    public EnderManData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}
