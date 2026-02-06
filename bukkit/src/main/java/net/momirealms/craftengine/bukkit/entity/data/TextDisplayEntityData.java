package net.momirealms.craftengine.bukkit.entity.data;

import net.momirealms.craftengine.core.entity.display.TextDisplayAlignment;
import net.momirealms.craftengine.proxy.minecraft.network.chat.ComponentProxy;

public class TextDisplayEntityData<T> extends DisplayEntityData<T> {
    public static final TextDisplayEntityData<Object> Text = new TextDisplayEntityData<>(TextDisplayEntityData.class, EntityDataValue.Serializers$COMPONENT, ComponentProxy.INSTANCE.empty());
    public static final TextDisplayEntityData<Integer> LineWidth = new TextDisplayEntityData<>(TextDisplayEntityData.class, EntityDataValue.Serializers$INT, 200);
    public static final TextDisplayEntityData<Integer> BackgroundColor = new TextDisplayEntityData<>(TextDisplayEntityData.class, EntityDataValue.Serializers$INT, 0x40000000);
    public static final TextDisplayEntityData<Byte> TextOpacity = new TextDisplayEntityData<>(TextDisplayEntityData.class, EntityDataValue.Serializers$BYTE, (byte) -1);
    public static final TextDisplayEntityData<Byte> TextDisplayMasks = new TextDisplayEntityData<>(TextDisplayEntityData.class, EntityDataValue.Serializers$BYTE, (byte) 0);

    public TextDisplayEntityData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }

    public static final int HAS_SHADOW = 0x01;
    public static final int IS_SEE_THROUGH = 0x02;
    public static final int USE_DEFAULT_BG_COLOR = 0x04;
    private static final int LEFT_ALIGNMENT = 0x08; // 8
    private static final int RIGHT_ALIGNMENT = 0x10; // 16

    public static byte encodeMask(boolean hasShadow, boolean isSeeThrough, boolean useDefaultBackground, TextDisplayAlignment alignment) {
        int bitMask = 0;

        if (hasShadow) {
            bitMask |= HAS_SHADOW;
        }
        if (isSeeThrough) {
            bitMask |= IS_SEE_THROUGH;
        }
        if (useDefaultBackground) {
            bitMask |= USE_DEFAULT_BG_COLOR;
        }

        switch (alignment) {
            case CENTER: // CENTER
                break;
            case LEFT: // LEFT
                bitMask |= LEFT_ALIGNMENT;
                break;
            case RIGHT: // RIGHT
                bitMask |= RIGHT_ALIGNMENT;
                break;
            default:
                throw new IllegalArgumentException("Invalid alignment value");
        }

        return (byte) bitMask;
    }
}
