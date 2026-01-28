package net.momirealms.craftengine.bukkit.entity.data;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.core.util.ReflectionUtils;
import net.momirealms.craftengine.core.util.VersionHelper;

public class EntityDataValue {
    private static int internalID = 0;

    private static final String[] fieldsObf = {
            "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"
    };

    public static final Object Serializers$BYTE;
    public static final Object Serializers$INT;
    public static final Object Serializers$LONG;
    public static final Object Serializers$FLOAT;
    public static final Object Serializers$STRING;
    public static final Object Serializers$COMPONENT;
    public static final Object Serializers$OPTIONAL_COMPONENT;
    public static final Object Serializers$ITEM_STACK;
    public static final Object Serializers$BLOCK_STATE;
    public static final Object Serializers$OPTIONAL_BLOCK_STATE;
    public static final Object Serializers$BOOLEAN;
    public static final Object Serializers$PARTICLE;
    public static final Object Serializers$PARTICLES;
    public static final Object Serializers$ROTATIONS;
    public static final Object Serializers$BLOCK_POS;
    public static final Object Serializers$OPTIONAL_BLOCK_POS;
    public static final Object Serializers$DIRECTION;
    public static final Object Serializers$OPTIONAL_UUID;
    public static final Object Serializers$OPTIONAL_LIVING_ENTITY_REFERENCE;
    public static final Object Serializers$OPTIONAL_GLOBAL_POS;
    public static final Object Serializers$COMPOUND_TAG;
    public static final Object Serializers$VILLAGER_DATA;
    public static final Object Serializers$OPTIONAL_UNSIGNED_INT;
    public static final Object Serializers$POSE;
    public static final Object Serializers$CAT_VARIANT;
    public static final Object Serializers$WOLF_VARIANT;
    public static final Object Serializers$FROG_VARIANT;
    public static final Object Serializers$PAINTING_VARIANT;
    public static final Object Serializers$ARMADILLO_STATE;
    public static final Object Serializers$SNIFFER_STATE;
    public static final Object Serializers$VECTOR3;
    public static final Object Serializers$QUATERNION;
    public static final Object Serializers$RESOLVABLE_PROFILE;
    public static final Object Serializers$HUMANOID_ARM;

    static {
        try {
            Serializers$BYTE = initSerializersByName("BYTE");
            Serializers$INT = initSerializersByName("INT");
            Serializers$LONG = initSerializersByName("LONG");
            Serializers$FLOAT = initSerializersByName("FLOAT");
            Serializers$STRING = initSerializersByName("STRING");
            Serializers$COMPONENT = initSerializersByName("COMPONENT");
            Serializers$OPTIONAL_COMPONENT = initSerializersByName("OPTIONAL_COMPONENT");
            Serializers$ITEM_STACK = initSerializersByName("ITEM_STACK");
            Serializers$BLOCK_STATE = initSerializersByName("BLOCK_STATE");
            Serializers$OPTIONAL_BLOCK_STATE = initSerializersByName("OPTIONAL_BLOCK_STATE");
            Serializers$BOOLEAN = initSerializersByName("BOOLEAN");
            Serializers$PARTICLE = initSerializersByName("PARTICLE");
            if (VersionHelper.isOrAbove1_20_5()) Serializers$PARTICLES = initSerializersByName("PARTICLES");
            else Serializers$PARTICLES = null;
            Serializers$ROTATIONS = initSerializersByName("ROTATIONS");
            Serializers$BLOCK_POS = initSerializersByName("BLOCK_POS");
            Serializers$OPTIONAL_BLOCK_POS = initSerializersByName("OPTIONAL_BLOCK_POS");
            Serializers$DIRECTION = initSerializersByName("DIRECTION");
            if (!VersionHelper.isOrAbove1_21_5()) Serializers$OPTIONAL_UUID = initSerializersByName("OPTIONAL_UUID");
            else Serializers$OPTIONAL_UUID = null;
            if (VersionHelper.isOrAbove1_21_5()) Serializers$OPTIONAL_LIVING_ENTITY_REFERENCE = initSerializersByName("OPTIONAL_LIVING_ENTITY_REFERENCE");
            else Serializers$OPTIONAL_LIVING_ENTITY_REFERENCE = null;
            Serializers$OPTIONAL_GLOBAL_POS = initSerializersByName("OPTIONAL_GLOBAL_POS");
            if (!VersionHelper.isOrAbove1_21_9()) Serializers$COMPOUND_TAG = initSerializersByName("COMPOUND_TAG");
            else Serializers$COMPOUND_TAG = null;
            Serializers$VILLAGER_DATA = initSerializersByName("VILLAGER_DATA");
            Serializers$OPTIONAL_UNSIGNED_INT = initSerializersByName("OPTIONAL_UNSIGNED_INT");
            Serializers$POSE = initSerializersByName("POSE");
            Serializers$CAT_VARIANT = initSerializersByName("CAT_VARIANT");
            if (VersionHelper.isOrAbove1_20_5()) Serializers$WOLF_VARIANT = initSerializersByName("WOLF_VARIANT");
            else Serializers$WOLF_VARIANT = null;
            Serializers$FROG_VARIANT = initSerializersByName("FROG_VARIANT");
            Serializers$PAINTING_VARIANT = initSerializersByName("PAINTING_VARIANT");
            if (VersionHelper.isOrAbove1_20_5()) Serializers$ARMADILLO_STATE = initSerializersByName("ARMADILLO_STATE");
            else Serializers$ARMADILLO_STATE = null;
            Serializers$SNIFFER_STATE = initSerializersByName("SNIFFER_STATE");
            Serializers$VECTOR3 = initSerializersByName("VECTOR3");
            Serializers$QUATERNION = initSerializersByName("QUATERNION");
            if (VersionHelper.isOrAbove1_21_9()) Serializers$RESOLVABLE_PROFILE = initSerializersByName("RESOLVABLE_PROFILE");
            else Serializers$RESOLVABLE_PROFILE = null;
            if (VersionHelper.isOrAbove1_21_11()) Serializers$HUMANOID_ARM = initSerializersByName("HUMANOID_ARM");
            else Serializers$HUMANOID_ARM = null;
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static Object initSerializersByName(String name) throws ReflectiveOperationException {
        return ReflectionUtils.getDeclaredField(CoreReflections.clazz$EntityDataSerializers, new String[]{fieldsObf[internalID++], name}).get(null);
    }

    private EntityDataValue() {
        throw new IllegalAccessError("Utility class");
    }

    public static Object create(Object entityDataAccessor, Object value) {
        return FastNMS.INSTANCE.method$SynchedEntityData$DataValue$create(entityDataAccessor, value);
    }
}
