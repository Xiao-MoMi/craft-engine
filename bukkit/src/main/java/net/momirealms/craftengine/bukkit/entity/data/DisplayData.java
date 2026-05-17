package net.momirealms.craftengine.bukkit.entity.data;

import net.momirealms.craftengine.core.entity.display.TextDisplayAlignment;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.network.chat.ComponentProxy;
import net.momirealms.craftengine.proxy.minecraft.network.syncher.EntityDataSerializersProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlocksProxy;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class DisplayData<T> extends BaseEntityData<T> {
    public static final DisplayData<Integer> TransformationInterpolationDelay = of(DisplayData.class, EntityDataSerializersProxy.INT, 0, true);

    // 1.19.4-1.20.1
    public static final DisplayData<Integer> InterpolationDuration = of(DisplayData.class, EntityDataSerializersProxy.INT, 0, !VersionHelper.isOrAbove1_20_2);

    // 1.20.2+
    public static final DisplayData<Integer> TransformationInterpolationDuration = of(DisplayData.class, EntityDataSerializersProxy.INT, 0, VersionHelper.isOrAbove1_20_2);
    public static final DisplayData<Integer> PosRotInterpolationDuration = of(DisplayData.class, EntityDataSerializersProxy.INT, 0, VersionHelper.isOrAbove1_20_2);

    public static final DisplayData<Vector3f> Translation = of(DisplayData.class, EntityDataSerializersProxy.VECTOR3, new Vector3f(0f), true);
    public static final DisplayData<Vector3f> Scale = of(DisplayData.class, EntityDataSerializersProxy.VECTOR3, new Vector3f(1f), true);
    public static final DisplayData<Quaternionf> LeftRotation = of(DisplayData.class, EntityDataSerializersProxy.QUATERNION, new Quaternionf(0f, 0f, 0f, 1f), true);
    public static final DisplayData<Quaternionf> RightRotation = of(DisplayData.class, EntityDataSerializersProxy.QUATERNION, new Quaternionf(0f, 0f, 0f, 1f), true);
    /**
     * 	Billboard Constraints (0 = FIXED, 1 = VERTICAL, 2 = HORIZONTAL, 3 = CENTER)
     */
    public static final DisplayData<Byte> BillboardConstraints = of(DisplayData.class, EntityDataSerializersProxy.BYTE, (byte) 0, true);
    /**
     * Brightness override (blockLight << 4 | skyLight << 20)
     */
    public static final DisplayData<Integer> BrightnessOverride = of(DisplayData.class, EntityDataSerializersProxy.INT, -1, true);
    public static final DisplayData<Float> ViewRange = of(DisplayData.class, EntityDataSerializersProxy.FLOAT, 1f, true);
    public static final DisplayData<Float> ShadowRadius = of(DisplayData.class, EntityDataSerializersProxy.FLOAT, 0f, true);
    public static final DisplayData<Float> ShadowStrength = of(DisplayData.class, EntityDataSerializersProxy.FLOAT, 0f, true);
    public static final DisplayData<Float> Width = of(DisplayData.class, EntityDataSerializersProxy.FLOAT, 0f, true);
    public static final DisplayData<Float> Height = of(DisplayData.class, EntityDataSerializersProxy.FLOAT, 0f, true);
    public static final DisplayData<Integer> GlowColorOverride = of(DisplayData.class, EntityDataSerializersProxy.INT, -1, true);

    private static <T> DisplayData<T> of(final Class<?> clazz, final Object serializer, T defaultValue, boolean condition) {
        if (!condition) return null;
        return new DisplayData<>(clazz, serializer, defaultValue);
    }

    protected DisplayData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }

    public static class BlockDisplayData<T> extends DisplayData<T> {
        public static final BlockDisplayData<Object> BlockState = new BlockDisplayData<>(BlockDisplayData.class, EntityDataSerializersProxy.BLOCK_STATE, BlocksProxy.AIR$defaultState);

        protected BlockDisplayData(Class<?> clazz, Object serializer, T defaultValue) {
            super(clazz, serializer, defaultValue);
        }
    }

    public static class ItemDisplayData<T> extends DisplayData<T> {
        public static final ItemDisplayData<Object> ItemStack = new ItemDisplayData<>(ItemDisplayData.class, EntityDataSerializersProxy.ITEM_STACK, ItemStackProxy.EMPTY);
        /**
         * Display type:
         * 0 = NONE
         * 1 = THIRD_PERSON_LEFT_HAND
         * 2 = THIRD_PERSON_RIGHT_HAND
         * 3 = FIRST_PERSON_LEFT_HAND
         * 4 = FIRST_PERSON_RIGHT_HAND
         * 5 = HEAD
         * 6 = GUI
         * 7 = GROUND
         * 8 = FIXED
         */
        public static final ItemDisplayData<Byte> ItemTransform = new ItemDisplayData<>(ItemDisplayData.class, EntityDataSerializersProxy.BYTE, (byte) 0);

        protected ItemDisplayData(Class<?> clazz, Object serializer, T defaultValue) {
            super(clazz, serializer, defaultValue);
        }
    }

    public static class TextDisplayData<T> extends DisplayData<T> {
        public static final TextDisplayData<Object> Text = new TextDisplayData<>(TextDisplayData.class, EntityDataSerializersProxy.COMPONENT, ComponentProxy.INSTANCE.empty());
        public static final TextDisplayData<Integer> LineWidth = new TextDisplayData<>(TextDisplayData.class, EntityDataSerializersProxy.INT, 200);
        public static final TextDisplayData<Integer> BackgroundColor = new TextDisplayData<>(TextDisplayData.class, EntityDataSerializersProxy.INT, 0x40000000);
        public static final TextDisplayData<Byte> TextOpacity = new TextDisplayData<>(TextDisplayData.class, EntityDataSerializersProxy.BYTE, (byte) -1);
        public static final TextDisplayData<Byte> Flags = new TextDisplayData<>(TextDisplayData.class, EntityDataSerializersProxy.BYTE, (byte) 0);

        protected TextDisplayData(Class<?> clazz, Object serializer, T defaultValue) {
            super(clazz, serializer, defaultValue);
        }

        public static final int HAS_SHADOW = 0x01;
        public static final int IS_SEE_THROUGH = 0x02;
        public static final int USE_DEFAULT_BG_COLOR = 0x04;
        private static final int LEFT_ALIGNMENT = 0x08; // 8
        private static final int RIGHT_ALIGNMENT = 0x10; // 16

        public static byte encodeFlags(boolean hasShadow, boolean isSeeThrough, boolean useDefaultBackground, TextDisplayAlignment alignment) {
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
}
