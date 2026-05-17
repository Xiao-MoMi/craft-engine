package net.momirealms.craftengine.bukkit.entity.data.monster;

import net.momirealms.craftengine.bukkit.entity.data.MobData;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.network.syncher.EntityDataSerializersProxy;

public class ShulkerData<T> extends MobData<T> {
    public static final ShulkerData<Object> AttachFace = new ShulkerData<>(ShulkerData.class, EntityDataSerializersProxy.DIRECTION, DirectionProxy.DOWN);
    public static final ShulkerData<Byte> RawPeekAmount = new ShulkerData<>(ShulkerData.class, EntityDataSerializersProxy.BYTE, (byte) 0);
    /**
     * DyeColor:
     * 0  = WHITE
     * 1  = ORANGE
     * 2  = MAGENTA
     * 3  = LIGHT_BLUE
     * 4  = YELLOW
     * 5  = LIME
     * 6  = PINK
     * 7  = GRAY
     * 8  = LIGHT_GRAY
     * 9  = CYAN
     * 10 = PURPLE
     * 11 = BLUE
     * 12 = BROWN
     * 13 = GREEN
     * 14 = RED
     * 15 = BLACK
     * 16 = UNDEFINED 这个不在枚举内
     */
    public static final ShulkerData<Byte> Color = new ShulkerData<>(ShulkerData.class, EntityDataSerializersProxy.BYTE, (byte) 16);

    protected ShulkerData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}