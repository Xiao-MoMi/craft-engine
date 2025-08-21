package net.momirealms.craftengine.core.block;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.pack.model.generation.ModelGenerator;
import net.momirealms.craftengine.core.plugin.Manageable;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.util.Key;
import org.incendo.cloud.suggestion.Suggestion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface BlockManager extends Manageable, ModelGenerator {

    ConfigParser parser();

    Collection<ModelGeneration> modelsToGenerate();

    Map<Key, Map<String, JsonElement>> blockOverrides();

    Map<Key, JsonElement> modBlockStates();

    Map<Key, CustomBlock> blocks();

    Optional<CustomBlock> blockById(Key key);

    Collection<Suggestion> cachedSuggestions();

    Map<Key, Key> soundMapper();

    int availableAppearances(Key blockType);

    Key getBlockOwnerId(BlockStateWrapper state);

    @NotNull
    ImmutableBlockState getImmutableBlockStateUnsafe(int stateId);

    @Nullable
    ImmutableBlockState getImmutableBlockState(int stateId);
    
    @Nullable
    BlockStateWrapper createPackedBlockState(String blockState);
}
