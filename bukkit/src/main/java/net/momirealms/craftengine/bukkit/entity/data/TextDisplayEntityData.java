package net.momirealms.craftengine.bukkit.entity.data;

import net.momirealms.craftengine.bukkit.util.Reflections;

public class TextDisplayEntityData<T> extends DisplayEntityData<T> {
    public static final DisplayEntityData<Object> Text = of(23, EntityDataValue.Serializers$COMPONENT, Reflections.instance$Component$empty);
    public static final DisplayEntityData<Integer> LineWidth = of(24, EntityDataValue.Serializers$INT, 200);
    public static final DisplayEntityData<Integer> BackgroundColor = of(25, EntityDataValue.Serializers$INT, 0x40000000);
    public static final DisplayEntityData<Byte> TextOpacity = of(26, EntityDataValue.Serializers$BYTE, (byte) -1);
    public static final DisplayEntityData<Byte> TextDisplayMasks = of(27, EntityDataValue.Serializers$BYTE, (byte) 0);

    public TextDisplayEntityData(int id, Object serializer, T defaultValue) {
        super(id, serializer, defaultValue);
    }
}
