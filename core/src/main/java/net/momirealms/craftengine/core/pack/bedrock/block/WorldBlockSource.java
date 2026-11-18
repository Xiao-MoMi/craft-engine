package net.momirealms.craftengine.core.pack.bedrock.block;

import net.momirealms.craftengine.core.block.BlockSounds;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.collision.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record WorldBlockSource(Key id,
                               int stateIndex,
                               int serverStateId,
                               String serverState,
                               BlockStateWrapper visual,
                               float hardness,
                               int lightEmission,
                               int lightDampening,
                               float friction,
                               List<AABB> collisionBoxes,
                               @Nullable AABB selectionBox,
                               BlockSounds sounds) {
    private static final BlockSounds STONE = new BlockSounds(SoundData.of(Key.of("block.stone.break")), SoundData.of(Key.of("block.stone.step")),
            SoundData.of(Key.of("block.stone.place")), SoundData.of(Key.of("block.stone.hit")), SoundData.of(Key.of("block.stone.fall")));

    public WorldBlockSource(Key id, int stateIndex, int serverStateId, String serverState, BlockStateWrapper visual,
                            float hardness, int lightEmission, int lightDampening, float friction, List<AABB> collisionBoxes, @Nullable AABB selectionBox) {
        this(id, stateIndex, serverStateId, serverState, visual, hardness, lightEmission, lightDampening, friction, collisionBoxes, selectionBox, STONE);
    }
}
