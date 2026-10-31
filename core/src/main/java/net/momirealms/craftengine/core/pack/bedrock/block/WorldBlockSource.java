package net.momirealms.craftengine.core.pack.bedrock.block;

import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.collision.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record WorldBlockSource(Key id, int stateIndex, int serverStateId, String serverState,
                               BlockStateWrapper visual, float hardness, int lightEmission, int lightDampening,
                               float friction, List<AABB> collisionBoxes, @Nullable AABB selectionBox,
                               boolean entityRenderer) {
}
