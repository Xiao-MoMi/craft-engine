package net.momirealms.craftengine.core.pack.bedrock.block;

import net.momirealms.craftengine.core.world.collision.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record WorldBlockProperties(float hardness,
                                   int lightEmission,
                                   int lightDampening,
                                   float friction,
                                   List<AABB> collisionBoxes,
                                   @Nullable AABB selectionBox) {
}
