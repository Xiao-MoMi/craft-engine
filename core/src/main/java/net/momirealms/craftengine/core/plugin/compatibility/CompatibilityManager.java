package net.momirealms.craftengine.core.plugin.compatibility;

import cn.gtemc.itembridge.api.ItemBridge;
import cn.gtemc.itembridge.api.Provider;
import cn.gtemc.levelerbridge.api.LevelerBridge;
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

    ExternalModel createModel(String plugin, String id);

    int interactionToBaseEntity(int id);

    boolean hasPlaceholderAPI();

    boolean isPluginEnabled(String plugin);

    boolean hasPlugin(String plugin);

    String parse(Player player, String text);

    String parse(Player player1, Player player2, String text);

    int getViaVersionProtocolVersion(NetWorkUser user);

    void executeMMSkill(String skill, float power, Player player);

    TagResolver[] createExternalTagResolvers(Context context);

    boolean isBedrockPlayer(Player player);

    /**
     * Please obtain the ItemBridge via {@link #itemBridge()} and utilise {@link ItemBridge#provider(String)}.
     */
    @Deprecated(forRemoval = true)
    ItemSource<?> getItemSource(String id);

    /**
     * Please obtain the ItemBridge via {@link #itemBridge()} and utilise {@link ItemBridge#register(Provider)}.
     */
    @Deprecated(forRemoval = true)
    void registerItemSource(ItemSource<?> itemSource);

    /**
     * Please obtain the LevelerBridge via {@link #levelerBridge()} and use {@link LevelerBridge#provider(String)}
     */
    @Deprecated(forRemoval = true)
    LevelerProvider getLevelerProvider(String id);

    /**
     * Please obtain the LevelerBridge via {@link #levelerBridge()} and use {@link LevelerBridge#register(cn.gtemc.levelerbridge.api.LevelerProvider)}
     */
    @Deprecated(forRemoval = true)
    void registerLevelerProvider(LevelerProvider provider);

    <I, P> ItemBridge<I, P> itemBridge();

    <P> LevelerBridge<P> levelerBridge();
}
