package net.momirealms.craftengine.bukkit.pack;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.api.event.AsyncResourcePackCacheEvent;
import net.momirealms.craftengine.bukkit.api.event.AsyncResourcePackGenerateEvent;
import net.momirealms.craftengine.bukkit.api.event.AsyncResourcePackPrepareEvent;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.EventUtils;
import net.momirealms.craftengine.bukkit.util.ResourcePackUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.pack.AbstractPackManager;
import net.momirealms.craftengine.core.pack.PackCacheData;
import net.momirealms.craftengine.core.pack.host.ResourcePackDownloadData;
import net.momirealms.craftengine.core.pack.obfuscation.ObfA;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.util.Base64Utils;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class BukkitPackManager extends AbstractPackManager implements Listener {
    private final BukkitCraftEngine plugin;

    public BukkitPackManager(BukkitCraftEngine plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    protected void dispatchCacheEvent(PackCacheData cacheData) {
        EventUtils.fireAndForget(new AsyncResourcePackCacheEvent(cacheData));
    }

    @Override
    protected void dispatchGenerationEvent(Path resourceFolder, Path zipPath) {
        EventUtils.fireAndForget(new AsyncResourcePackGenerateEvent(resourceFolder, zipPath));
    }

    @Override
    public void delayedInit() {
        super.delayedInit();
        Bukkit.getPluginManager().registerEvents(this, this.plugin.javaPlugin());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (Config.sendPackOnJoin() && !VersionHelper.isOrAbove1_20_2) {
            Player player = BukkitAdaptor.adapt(event.getPlayer());
            // 可能有假人
            if (player == null) return;
            this.sendResourcePack(player);
        }
    }

    @Override
    protected void loadHosts() {
        // 仅托管配置需要显式重载；工作流由父类 load 在每次普通重载时重新解析。
        if (this.plugin.isReloadingPack() || this.plugin.isReloadingHost() || this.plugin.isEnabling()) {
            super.loadHosts();
        }
    }

    @Override
    public void unload() {
        super.unload();
    }

    @Override
    public void disable() {
        super.disable();
        HandlerList.unregisterAll(this);
    }

    @Override
    public void sendResourcePack(Player player) {
        sendResourcePackAsync(player).exceptionally(t -> {
            this.plugin.logger().warn(TranslationManager.instance().plainTranslation("host.get_url_failed", player.name()), t);
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> sendResourcePackAsync(Player player) {
        return sendPreparedResourcePacks(player);
    }

    private CompletableFuture<Void> sendPreparedResourcePacks(Player player) {
        if (!player.isOnline()) return CompletableFuture.completedFuture(null);
        CompletableFuture<List<ResourcePackDownloadData>> future = prepareResourcePacks(player);
        return future.thenAccept(dataList -> {
            if (player.isOnline()) {
                player.unloadCurrentResourcePack();
                if (dataList.isEmpty()) {
                    return;
                }
                if (dataList.size() == 1 || !VersionHelper.isOrAbove1_20_3) { // 1.20~1.20.2 只支持一个服务器资源包
                    ResourcePackDownloadData data = dataList.getFirst();
                    player.sendPacket(ResourcePackUtils.createPacket(data.uuid(), data.url(), data.sha1()), true);
                    player.addResourcePackUUID(data.uuid());
                } else {
                    List<Object> packets = new ArrayList<>();
                    for (ResourcePackDownloadData data : dataList) {
                        packets.add(ResourcePackUtils.createPacket(data.uuid(), data.url(), data.sha1()));
                        player.addResourcePackUUID(data.uuid());
                    }
                    player.sendPackets(packets, true);
                }
            }
        });
    }

    @Override
    protected void prepareResourcePackList(NetWorkUser user, List<String> packs) {
        EventUtils.fireAndForget(new AsyncResourcePackPrepareEvent(user.uuid(), user.name(), packs));
    }

    @Override
    public String toString() {
        return new String(Base64Utils.decode(ObfA.VALUES, Integer.parseInt(String.valueOf(ObfA.VALUES[71]).substring(0, 1))), StandardCharsets.UTF_8);
    }
}
