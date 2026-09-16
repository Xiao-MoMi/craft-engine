package net.momirealms.craftengine.core.pack;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.pack.host.ResourcePackDownloadData;
import net.momirealms.craftengine.core.pack.host.ResourcePackHost;
import net.momirealms.craftengine.core.plugin.Manageable;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.util.Tristate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public interface PackManager extends Manageable {

    ConfigParser[] parsers();

    int loadResources(Predicate<ConfigParser> predicate);

    void initCachedAssets();

    @NotNull
    Collection<Pack> loadedPacks();

    boolean registerConfigSectionParser(ConfigParser parser);

    default void registerConfigSectionParsers(ConfigParser[] parsers) {
        for (ConfigParser parser : parsers) {
            registerConfigSectionParser(parser);
        }
    }

    boolean unregisterConfigSectionParser(String id);

    default void unregisterConfigSectionParser(ConfigParser parser) {
        for (String id : parser.sectionId()) {
            unregisterConfigSectionParser(id);
        }
    }

    void loadPacks();

    void updateCachedConfigFiles();

    void clearResourceConfigs();

    Collection<String> workflowNames();

    void runWorkflow(String name) throws Exception;

    void triggerWorkflows(String event) throws Exception;

    ResourcePackHost resourcePackHost();

    Map<String, ResourcePackHost> resourcePackHosts();

    @Nullable Map<String, Boolean> packPreferences(NetWorkUser user);

    CompletableFuture<List<ResourcePackDownloadData>> prepareResourcePacks(NetWorkUser user);

    CompletableFuture<Boolean> setPackPreference(UUID player, String pack, @NotNull Tristate enabled);

    CompletableFuture<Boolean> setPackPreferences(UUID player, Map<String, @NotNull Tristate> updates);

    Map<String, PackPreset> packPresets();

    CompletableFuture<Boolean> applyPackPreset(UUID player, String preset);

    CompletableFuture<Void> sendPackToUsers(String pack);

    CompletableFuture<Void> sendResourcePackAsync(Player player);

    void sendResourcePack(Player player);
}
