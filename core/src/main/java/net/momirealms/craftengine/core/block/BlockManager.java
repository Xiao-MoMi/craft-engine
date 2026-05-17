package net.momirealms.craftengine.core.block;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.core.pack.model.generation.ModelGenerator;
import net.momirealms.craftengine.core.plugin.Manageable;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.plugin.network.mod.protocol.VisualBlockStatePacket;
import net.momirealms.craftengine.core.util.Key;
import org.incendo.cloud.suggestion.Suggestion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface BlockManager extends Manageable, ModelGenerator {

    ConfigParser[] parsers();

    Map<Key, Map<String, JsonElement>> blockOverrides();

    Map<Integer, JsonElement> modBlockStates();

    boolean isTransparentModelInUse();

    Map<Key, BlockDefinition> loadedBlocks();

    @Deprecated(forRemoval = true)
    default Map<Key, BlockDefinition> blocks() {
        return loadedBlocks();
    }

    Optional<BlockDefinition> blockById(Key key);

    Collection<Suggestion> cachedSuggestions();

    Map<Key, Key> soundReplacements();

    Key getBlockOwnerId(BlockStateWrapper state);

    @NotNull
    ImmutableBlockState getImmutableBlockStateUnsafe(int stateId);

    @Nullable
    ImmutableBlockState getImmutableBlockState(int stateId);
    
    @Nullable
    BlockStateWrapper createBlockState(String blockState);

    @Nullable
    BlockStateWrapper createVanillaBlockState(String blockState);

    int vanillaBlockStateCount();

    int currentBlockRegistrySize();

    VisualBlockStatePacket cachedVisualBlockStatePacket();

    static Key createCustomBlockKey(int id) {
        return Key.of("craftengine", "custom_" + id);
    }
}
