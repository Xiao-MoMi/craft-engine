package net.momirealms.craftengine.core.plugin.compatibility;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.momirealms.craftengine.core.entity.furniture.ExternalModel;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;

public interface CompatibilityManager {

    void onLoad();

    void onEnable();

    void onDelayedEnable();

    void registerTagResolverProvider(TagResolverProvider provider);

    ExternalModel createModel(String id);

    int interactionToBaseEntity(int id);

    boolean hasPlaceholderAPI();

    boolean isPluginEnabled(String plugin);

    boolean hasPlugin(String plugin);

    String parse(Player player, String text);

    String parse(Player player1, Player player2, String text);

    int getViaVersionProtocolVersion(NetWorkUser user);

    TagResolver[] createExternalTagResolvers(Context context);

    boolean isBedrockPlayer(Player player);

    ModelProvider getModelProvider(String id);

    void registerModelProvider(ModelProvider provider);

    ItemSource getItemSource(String id);

    void registerItemSource(ItemSource itemSource);

    LevelerProvider getLevelerProvider(String id);

    void registerLevelerProvider(LevelerProvider provider);

    EntityProvider getEntityProvider(String id);

    void registerEntityProvider(EntityProvider provider);

    boolean hasPermission(NetWorkUser user, String permission);
}
