package net.momirealms.craftengine.bukkit.pack;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.api.event.AsyncResourcePackCacheEvent;
import net.momirealms.craftengine.bukkit.api.event.AsyncResourcePackGenerateEvent;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.command.feature.ReloadCommand;
import net.momirealms.craftengine.bukkit.util.EventUtils;
import net.momirealms.craftengine.bukkit.util.ResourcePackUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.pack.AbstractPackManager;
import net.momirealms.craftengine.core.pack.host.ResourcePackDownloadData;
import net.momirealms.craftengine.core.pack.obfuscation.ObfA;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.Base64Utils;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class BukkitPackManager extends AbstractPackManager implements Listener {
    private final BukkitCraftEngine plugin;

    public BukkitPackManager(BukkitCraftEngine plugin) {
        super(
                plugin,
                (cd) -> {
                    AsyncResourcePackCacheEvent cacheEvent = new AsyncResourcePackCacheEvent(cd);
                    EventUtils.fireAndForget(cacheEvent);
                },
                (rf, zp) -> {
                    AsyncResourcePackGenerateEvent endEvent = new AsyncResourcePackGenerateEvent(rf, zp);
                    EventUtils.fireAndForget(endEvent);
                }
        );
        this.plugin = plugin;
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
    public void load() {
        if (ReloadCommand.RELOAD_PACK_FLAG || this.plugin.isInitializing()) {
            super.load();
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
        CompletableFuture<List<ResourcePackDownloadData>> future = resourcePackHost().requestResourcePackDownloadLink(player);
        future.thenAccept(dataList -> {
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
        }).exceptionally(t -> {
            this.plugin.logger().warn(TranslationManager.instance().plainTranslation("host.get_url_failed", player.name()), t);
            return null;
        });
    }

    @Override
    public String toString() {
        return new String(Base64Utils.decode(ObfA.VALUES, Integer.parseInt(String.valueOf(ObfA.VALUES[71]).substring(0, 1))), StandardCharsets.UTF_8);
    }
}
