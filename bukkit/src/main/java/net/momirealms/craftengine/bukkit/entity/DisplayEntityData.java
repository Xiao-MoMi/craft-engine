package net.momirealms.craftengine.bukkit.entity;

import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.util.List;

public class DisplayEntityData<T> {

    private final int id;
    private final Object serializer;
    private final T defaultValue;

    // Entity
    public static final DisplayEntityData<Byte> EntityMasks = of(0, EntityDataValue.Serializers$BYTE, (byte) 0);

    // Display only
    public static final DisplayEntityData<Integer> InterpolationDelay = of(8, EntityDataValue.Serializers$INT, 0);

    // 1.19.4-1.20.1
    public static final DisplayEntityData<Integer> InterpolationDuration = of(9, EntityDataValue.Serializers$INT, 0);

    // 1.20.2+
    public static final DisplayEntityData<Integer> TransformationInterpolationDuration = of(9, EntityDataValue.Serializers$INT, 0);
    public static final DisplayEntityData<Integer> PositionRotationInterpolationDuration = of(10, EntityDataValue.Serializers$INT, 0);

    public static final DisplayEntityData<Object> Translation = of(11, EntityDataValue.Serializers$VECTOR3, Reflections.instance$Vector3f$None);
    public static final DisplayEntityData<Object> Scale = of(12, EntityDataValue.Serializers$VECTOR3, Reflections.instance$Vector3f$Normal);
    public static final DisplayEntityData<Object> RotationLeft = of(13, EntityDataValue.Serializers$QUATERNION, Reflections.instance$Quaternionf$None);
    public static final DisplayEntityData<Object> RotationRight = of(14, EntityDataValue.Serializers$QUATERNION, Reflections.instance$Quaternionf$None);
    /**
     * 	Billboard Constraints (0 = FIXED, 1 = VERTICAL, 2 = HORIZONTAL, 3 = CENTER)
     */
    public static final DisplayEntityData<Byte> BillboardConstraints = of(15, EntityDataValue.Serializers$BYTE, (byte) 0);
    /**
     * Brightness override (blockLight << 4 | skyLight << 20)
     */
    public static final DisplayEntityData<Integer> BrightnessOverride = of(16, EntityDataValue.Serializers$INT, -1);
    public static final DisplayEntityData<Float> ViewRange = of(17, EntityDataValue.Serializers$FLOAT, 1f);
    public static final DisplayEntityData<Float> ShadowRadius = of(18, EntityDataValue.Serializers$FLOAT, 0f);
    public static final DisplayEntityData<Float> ShadowStrength = of(19, EntityDataValue.Serializers$FLOAT, 0f);
    public static final DisplayEntityData<Float> Width = of(20, EntityDataValue.Serializers$FLOAT, 0f);
    public static final DisplayEntityData<Float> Height = of(21, EntityDataValue.Serializers$FLOAT, 0f);
    public static final DisplayEntityData<Integer> GlowColorOverride = of(22, EntityDataValue.Serializers$INT, -1);

    // Text display only
    public static final DisplayEntityData<Object> Text = of(23, EntityDataValue.Serializers$COMPONENT, Reflections.instance$Component$empty);
    public static final DisplayEntityData<Integer> LineWidth = of(24, EntityDataValue.Serializers$INT, 200);
    public static final DisplayEntityData<Integer> BackgroundColor = of(25, EntityDataValue.Serializers$INT, 0x40000000);
    public static final DisplayEntityData<Byte> TextOpacity = of(26, EntityDataValue.Serializers$BYTE, (byte) -1);
    public static final DisplayEntityData<Byte> TextDisplayMasks = of(27, EntityDataValue.Serializers$BYTE, (byte) 0);

    // Item display only
    public static final DisplayEntityData<Object> DisplayedItem = of(23, EntityDataValue.Serializers$ITEM_STACK, Reflections.instance$ItemStack$Air);
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
    public static final DisplayEntityData<Byte> DisplayType = of(24, EntityDataValue.Serializers$BYTE, (byte) 0);

    // Block display only
    public static final DisplayEntityData<Object> DisplayedBlock = of(23, EntityDataValue.Serializers$BLOCK_STATE, Reflections.instance$Blocks$AIR$defaultState);

    public static <T> DisplayEntityData<T> of(final int id, final Object serializer, T defaultValue) {
        return new DisplayEntityData<>(id, serializer, defaultValue);
    }

    public DisplayEntityData(int id, Object serializer, T defaultValue) {
        if (!VersionHelper.isVersionNewerThan1_20_2()) {
            if (id >= 11) {
                id--;
            }
        }
        this.id = id;
        this.serializer = serializer;
        this.defaultValue = defaultValue;
    }

    public Object serializer() {
        return serializer;
    }

    public int id() {
        return id;
    }

    public T defaultValue() {
        return defaultValue;
    }

    public Object createEntityDataIfNotDefaultValue(T value) {
        if (defaultValue().equals(value)) return null;
        return EntityDataValue.create(id, serializer, value);
    }

    public void addEntityDataIfNotDefaultValue(T value, List<Object> list) {
        if (defaultValue().equals(value)) return;
        list.add(EntityDataValue.create(id, serializer, value));
    }

    public void addEntityData(T value, List<Object> list) {
        list.add(EntityDataValue.create(id, serializer, value));
    }
}
