package net.momirealms.craftengine.core.entity.projectile;

import net.momirealms.craftengine.core.entity.ItemDisplayContext;
import net.momirealms.craftengine.core.util.Key;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public record ProjectileMeta(Key item, ItemDisplayContext displayType, Vector3f scale, Vector3f translation, Quaternionf rotation, String type) {
}
