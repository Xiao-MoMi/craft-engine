package net.momirealms.craftengine.bukkit.compatibility.model;

import net.momirealms.craftengine.bedrock.network.NativeModelPayload;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.pack.bedrock.BedrockEntitySettings;
import net.momirealms.craftengine.core.pack.bedrock.BedrockModelIntegration;
import net.momirealms.craftengine.core.plugin.config.Config;
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

public abstract class NativeModelBridge<T, V> implements Listener {
    protected final BukkitCraftEngine plugin;
    private final Plugin owner;
    private final String namespace;
    // BetterModel 的 tracker 按模型名判断相等，同名的两个实体仍须独立同步。
    private final Map<T, NativeModelInstance<T, V>> models = new IdentityHashMap<>();
    private List<NativeModelInstance<T, V>> registered;
    private final Map<UUID, Viewer> viewers = new HashMap<>();
    private final List<Viewer> online = new ArrayList<>();
    private final NativeModelParts parts = new NativeModelParts();
    private BedrockEntitySettings lastSettings;
    private final List<Viewer> visibleViewers = new ArrayList<>();
    private boolean configured;
    private SchedulerTask task;
    private boolean failed;
    private boolean closed;
    private long tickNumber;
    private int nextVisibilityPhase;

    protected NativeModelBridge(BukkitCraftEngine plugin, Plugin owner, String namespace) {
        this.plugin = plugin;
        this.owner = owner;
        this.namespace = namespace;
    }

    public final void start() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin.javaPlugin());
        this.task = this.plugin.scheduler().asyncRepeating(this::tick, 100, 50, TimeUnit.MILLISECONDS);
    }

    protected abstract NativeModelInstance<T, V> create(T source);

    protected abstract V adaptViewer(Player player);

    protected final void register(T source) {
        synchronized (this.models) {
            if (!this.closed && !this.models.containsKey(source)) {
                NativeModelInstance<T, V> model = this.create(source);
                model.visibilityPhase = this.nextVisibilityPhase++;
                this.models.put(source, model);
                this.registered = null;
            }
        }
    }

    protected final void unregister(T source) {
        synchronized (this.models) {
            if (this.models.remove(source) != null) {
                this.registered = null;
            }
        }
    }

    private List<NativeModelInstance<T, V>> registeredModels() {
        synchronized (this.models) {
            if (this.registered == null) {
                this.registered = List.copyOf(this.models.values());
            }
            return this.registered;
        }
    }

    private synchronized void tick() {
        if (this.closed) {
            return;
        }
        try {
            this.synchronize(this.plugin.networkManager().onlineUsers(), Config.bedrockEntities(), this.owner.isEnabled());
            this.failed = false;
        } catch (RuntimeException exception) {
            if (!this.failed) {
                this.plugin.logger().warn("Failed to synchronize native models for " + this.owner.getName(), exception);
                this.failed = true;
            }
        } finally {
            this.online.clear();
            this.visibleViewers.clear();
        }
    }

    void synchronize(Player[] players, BedrockEntitySettings settings, boolean enabled) {
        long tick = this.tickNumber++;
        boolean refresh = tick % 20 == 0;
        if (this.lastSettings != settings) {
            this.lastSettings = settings;
            this.configured = settings.uses(this.namespace, BedrockModelIntegration.CUSTOM_ENTITY) || settings.uses(this.namespace, BedrockModelIntegration.DISPLAY_ENTITY);
        }
        this.online.clear();
        if (enabled && this.configured) {
            this.collectViewers(players, tick);
        } else {
            this.removeAll(players);
        }
        if (this.online.isEmpty() && !refresh) {
            return;
        }
        for (NativeModelInstance<T, V> model : this.registeredModels()) {
            if (model.removed()) {
                synchronized (this.models) {
                    if (this.models.remove(model.source, model)) {
                        this.registered = null;
                    }
                }
            } else if (!this.online.isEmpty()) {
                BedrockModelIntegration mode = model.mode(this.namespace, settings);
                if (mode != BedrockModelIntegration.DISABLED && model.prepare()) {
                    this.syncModel(model, mode, tick, refresh);
                }
            }
        }
        for (Viewer viewer : this.online) {
            viewer.removeUnseen(tick);
        }
    }

    private void collectViewers(Player[] players, long tick) {
        for (Player player : players) {
            if (!player.hasBedrockExtension()) {
                continue;
            }
            UUID id = player.uuid();
            Viewer viewer = this.viewers.get(id);
            if (viewer == null || viewer.player != player) {
                viewer = new Viewer(player);
                this.viewers.put(id, viewer);
            }
            viewer.visibility = this.adaptViewer(player);
            viewer.seen = tick;
            this.online.add(viewer);
        }
        this.viewers.values().removeIf(viewer -> viewer.seen != tick);
    }

    private void removeAll(Player[] players) {
        if (this.viewers.isEmpty()) {
            return;
        }
        for (Player player : players) {
            Viewer viewer = this.viewers.get(player.uuid());
            if (viewer != null && viewer.player == player && player.hasBedrockExtension()) {
                viewer.removeUnseen(this.tickNumber);
            }
        }
        this.viewers.clear();
    }

    private void syncModel(NativeModelInstance<T, V> model, BedrockModelIntegration mode, long tick, boolean refresh) {
        NativeModelPayload.Snapshot previous = model.snapshot;
        this.visibleViewers.clear();
        boolean newViewer = false;
        NativeModelPayload.Delta delta = null;
        for (int i = 0; i < this.online.size(); i++) {
            Viewer viewer = this.online.get(i);
            if (!model.visible(viewer.visibility)) {
                continue;
            }
            viewer.prior = viewer.models.get(model.id);
            newViewer |= viewer.prior == null;
            this.visibleViewers.add(viewer);
        }
        if (this.visibleViewers.isEmpty()) {
            return;
        }
        NativeModelPayload.Snapshot current;
        try {
            int interval = this.lastSettings.boneVisibilityIntervalTicks();
            boolean checkVisibility = mode == BedrockModelIntegration.CUSTOM_ENTITY && (newViewer
                    || !(previous instanceof NativeModelPayload.NativeState)
                    || model.visibilityInterval != interval || (tick + model.visibilityPhase) % interval == 0);
            current = model.sample(mode, this.parts, checkVisibility);
            model.visibilityInterval = interval;
            model.snapshot = current;
        } finally {
            this.parts.clear();
        }
        for (int i = 0; i < this.visibleViewers.size(); i++) {
            Viewer viewer = this.visibleViewers.get(i);
            Sent sent = viewer.prior;
            viewer.prior = null;
            if (sent == null) {
                viewer.send(current);
                viewer.models.put(model.id, new Sent(current, tick));
                continue;
            }
            NativeModelPayload.Snapshot old = sent.snapshot;
            if (!refresh && old == current) {
                sent.seen = tick;
                continue;
            }
            NativeModelPayload update = current;
            if (old.getClass() != current.getClass() || !old.model().equals(current.model())) {
                viewer.send(old.remove());
            } else if (!refresh && current instanceof NativeModelPayload.NativeState state && old instanceof NativeModelPayload.NativeState last) {
                // 观察到同一上次快照的玩家共用增量，其余玩家从自己最后成功接收的状态追上。
                if (old == previous) {
                    if (delta == null) {
                        delta = state.changesSince(last);
                    }
                    update = delta;
                } else {
                    update = state.changesSince(last);
                }
            }
            viewer.send(update);
            sent.snapshot = current;
            sent.seen = tick;
        }
    }

    protected void close() {
    }

    @EventHandler
    public synchronized void onDisable(PluginDisableEvent event) {
        if (event.getPlugin() != this.owner && event.getPlugin() != this.plugin.javaPlugin()) {
            return;
        }
        this.task.cancel();
        synchronized (this.models) {
            this.closed = true;
            this.models.clear();
            this.registered = null;
        }
        this.removeAll(this.plugin.networkManager().onlineUsers());
        this.online.clear();
        this.close();
        HandlerList.unregisterAll(this);
    }

    private final class Viewer {
        private final Player player;
        private V visibility;
        private Sent prior;
        private final Map<UUID, Sent> models = new HashMap<>();
        private long seen;

        private Viewer(Player player) {
            this.player = player;
        }

        private void send(NativeModelPayload payload) {
            this.player.sendCustomPacket(new ClientboundNativeModelPacket(payload));
        }

        private void removeUnseen(long tick) {
            Iterator<Sent> iterator = this.models.values().iterator();
            while (iterator.hasNext()) {
                Sent sent = iterator.next();
                if (sent.seen != tick) {
                    this.send(sent.snapshot.remove());
                    iterator.remove();
                }
            }
        }
    }

    private static final class Sent {
        private NativeModelPayload.Snapshot snapshot;
        private long seen;

        private Sent(NativeModelPayload.Snapshot snapshot, long seen) {
            this.snapshot = snapshot;
            this.seen = seen;
        }
    }
}
