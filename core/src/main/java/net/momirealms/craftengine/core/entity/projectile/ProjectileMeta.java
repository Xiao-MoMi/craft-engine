package net.momirealms.craftengine.core.entity.projectile;

import net.momirealms.craftengine.core.entity.display.Billboard;
import net.momirealms.craftengine.core.entity.display.ItemDisplayContext;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public record ProjectileMeta(Key item,
                             ItemDisplayContext displayType,
                             Billboard billboard,
                             Vector3f scale,
                             Vector3f translation,
                             Quaternionf rotation,
                             double range) {

    private static final String[] DISPLAY_TRANSFORM = new String[] {"display_transform", "display-transform"};

    public static ProjectileMeta fromConfig(ConfigSection section) {
        Key itemId = section.getNonNullIdentifier("item");
        ItemDisplayContext displayType = section.getEnum(DISPLAY_TRANSFORM, ItemDisplayContext.class, ItemDisplayContext.NONE);
        Billboard billboard = section.getEnum("billboard", Billboard.class, Billboard.FIXED);
        Vector3f translation = section.getVector3f("translation", ConfigConstants.ZERO_VECTOR3);
        Vector3f scale = section.getVector3f("scale", ConfigConstants.NORMAL_SCALE);
        Quaternionf rotation = section.getQuaternion("rotation", ConfigConstants.ZERO_QUATERNION);
        double range = section.getDouble("range", 1);
        return new ProjectileMeta(itemId, displayType, billboard, scale, translation, rotation, range);
    }
}
