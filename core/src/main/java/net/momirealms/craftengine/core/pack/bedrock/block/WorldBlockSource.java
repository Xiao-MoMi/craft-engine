package net.momirealms.craftengine.core.pack.bedrock.block;

import net.momirealms.craftengine.core.block.BlockSounds;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

public record WorldBlockSource(Key id,
                               int stateIndex,
                               int serverStateId,
                               String serverState,
                               @Nullable BlockStateWrapper visual,
                               WorldBlockProperties properties,
                               BlockSounds sounds) {
}
