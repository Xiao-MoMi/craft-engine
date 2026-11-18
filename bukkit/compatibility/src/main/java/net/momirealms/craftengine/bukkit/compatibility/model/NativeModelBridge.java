package net.momirealms.craftengine.bukkit.compatibility.model;

import net.momirealms.craftengine.bedrock.network.NativeModelPayload;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.network.mod.protocol.ClientboundNativeModelPacket;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerTask;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public abstract class NativeModelBridge implements Listener {
    protected final BukkitCraftEngine plugin;
    private final Plugin owner;
    private final Map<UUID, Map<UUID, NativeModelPayload>> sent = new HashMap<>();
    private SchedulerTask task;
    private boolean failed;
    private int ticks;

    protected NativeModelBridge(BukkitCraftEngine plugin, Plugin owner) {
        this.plugin = plugin;
        this.owner = owner;
    }

    public final void start() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin.javaPlugin());
        this.task = this.plugin.scheduler().asyncRepeating(this::tick, 100, 50, TimeUnit.MILLISECONDS);
    }

    protected abstract boolean enabled();

    protected abstract List<ModelState> collect();

    protected void close() {
    }

    private synchronized void tick() {
        try {
            if (!this.enabled() && this.sent.isEmpty()) {
                return;
            }
            boolean refresh = this.ticks++ % 20 == 0;
            List<ModelState> states = this.enabled() && this.owner.isEnabled() ? this.collect() : List.of();
            Set<UUID> online = new HashSet<>();
            for (Player player : this.plugin.networkManager().onlineUsers()) {
                if (!player.hasBedrockExtension()) {
                    continue;
                }
                UUID viewer = player.uuid();
                online.add(viewer);
                Map<UUID, NativeModelPayload> previous = this.sent.computeIfAbsent(viewer, ignored -> new HashMap<>());
                Set<UUID> visible = new HashSet<>();
                for (ModelState state : states) {
                    if (!state.visible().test(player)) {
                        continue;
                    }
                    NativeModelPayload payload = state.payload();
                    visible.add(payload.instance());
                    if (!payload.equals(previous.put(payload.instance(), payload)) || refresh) {
                        player.sendCustomPacket(new ClientboundNativeModelPacket(payload));
                    }
                }
                previous.entrySet().removeIf(entry -> {
                    if (visible.contains(entry.getKey())) {
                        return false;
                    }
                    player.sendCustomPacket(new ClientboundNativeModelPacket(entry.getValue().remove()));
                    return true;
                });
            }
            this.sent.keySet().retainAll(online);
            this.failed = false;
        } catch (RuntimeException exception) {
            if (!this.failed) {
                this.plugin.logger().warn("Failed to synchronize native models for " + this.owner.getName(), exception);
                this.failed = true;
            }
        }
    }

    @EventHandler
    public synchronized void onDisable(PluginDisableEvent event) {
        if (event.getPlugin() != this.owner && event.getPlugin() != this.plugin.javaPlugin()) {
            return;
        }
        this.task.cancel();
        for (Player player : this.plugin.networkManager().onlineUsers()) {
            Map<UUID, NativeModelPayload> states = this.sent.get(player.uuid());
            if (states != null) {
                for (NativeModelPayload state : states.values()) {
                    player.sendCustomPacket(new ClientboundNativeModelPacket(state.remove()));
                }
            }
        }
        this.sent.clear();
        this.close();
        HandlerList.unregisterAll(this);
    }

    protected record ModelState(NativeModelPayload payload, Predicate<Player> visible) {
        public ModelState {
        }
    }
}
