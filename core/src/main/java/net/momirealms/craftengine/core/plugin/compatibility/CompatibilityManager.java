package net.momirealms.craftengine.core.plugin.compatibility;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.entity.furniture.ExternalModel;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.sparrow.message.tag.resolver.TagResolver;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiConsumer;

public interface CompatibilityManager {

    void onEnable();

    void onDelayedEnable();

    default void onInitialResourcesLoaded() {}

    void runDelayedSyncTasks();

    void registerTagResolverProvider(TagResolverProvider provider);

    ExternalModel createModel(String id);

    boolean hasPlaceholderAPI();

    boolean isPluginEnabled(String plugin);

    boolean hasPlugin(String plugin);

    String parse(Player player, String text);

    String parse(Player player1, Player player2, String text);

    int getViaVersionProtocolVersion(NetWorkUser user);

    TagResolver[] createExternalTagResolvers();

    boolean isBedrockPlayer(Player player);

    ModelProvider getModelProvider(String id);

    void registerModelProvider(ModelProvider provider);

    ItemSource getItemSource(String id);

    List<ItemSource> itemSources();

    void registerItemSource(ItemSource itemSource);

    LevelerProvider getLevelerProvider(String id);

    void registerLevelerProvider(LevelerProvider provider);

    EntityProvider getEntityProvider(String id);

    void registerEntityProvider(EntityProvider provider);

    void registerProtectionLogger(ProtectionLogger logger);

    void logSingleSlotContainerTransaction(Player player,
                                           WorldPosition position,
                                           @Nullable Item oldItem,
                                           @Nullable Item newItem);

    void logItemFrameTransaction(Player player,
                                 WorldPosition position,
                                 Direction direction,
                                 @Nullable Item oldItem,
                                 @Nullable Item newItem);

    boolean hasPermission(NetWorkUser user, String permission);

    int remapEntityId(int entityId);

    void blueMapBlockColors(ImmutableBlockState state, BiConsumer<String, JsonElement> callback);
}
