package net.momirealms.craftengine.bedrock.geyser.player;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.momirealms.craftengine.bedrock.geyser.CraftEngineGeyserExtension;
import net.momirealms.craftengine.bedrock.geyser.gui.GeyserContainerCloseHandler;
import net.momirealms.craftengine.bedrock.geyser.item.GeyserHeldDye;
import net.momirealms.craftengine.bedrock.geyser.item.GeyserItemRuntime;
import net.momirealms.craftengine.bedrock.geyser.network.ServerCustomPacket;
import net.momirealms.craftengine.bedrock.geyser.scoreboard.GeyserSidebarConnection;
import net.momirealms.craftengine.bedrock.geyser.scoreboard.SidebarSession;
import net.momirealms.craftengine.bedrock.network.HardHitboxPayload;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.data.debugshape.DebugShape;
import org.cloudburstmc.protocol.bedrock.packet.DebugDrawerPacket;
import org.geysermc.floodgate.util.UiProfile;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.BlockBreakHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class GeyserPlayer {
    private final CraftEngineGeyserExtension extension;
    private final GeyserSession session;
    private final List<ChannelHandler> handlers = new ArrayList<>();
    private Int2ObjectMap<HardHitboxPayload> pendingHitboxes;
    private Long2ObjectLinkedOpenHashMap<DebugShape> pendingShapes;
    private GeyserSidebarConnection sidebar;
    private GeyserItemRuntime items;
    private GeyserHeldDye heldDye;
    private BlockBreakHandler originalBlockBreakHandler;
    private BlockBreakHandler blockBreakHandler;
    private boolean placed;
    private long lastPlacement;
    private volatile boolean closed;

    GeyserPlayer(CraftEngineGeyserExtension extension, GeyserSession session) {
        this.extension = extension;
        this.session = session;
    }

    synchronized void initialize() {
        if (this.isClosed()) {
            return;
        }
        this.extension.blockManager().initPlayer(this);
        this.heldDye = this.extension.itemManager().initPlayerDye(this);
        GeyserContainerCloseHandler.install(this);
        this.items = this.extension.itemManager().initPlayer(this);
    }

    public GeyserSession session() {
        return this.session;
    }

    public String locale() {
        return this.session.locale();
    }

    public UiProfile uiProfile() {
        return this.session.getClientData().getUiProfile();
    }

    public boolean isClosed() {
        return this.closed || this.session.isClosed();
    }

    @Nullable
    public GeyserItemRuntime items() {
        return this.items;
    }

    @Nullable
    public synchronized SidebarSession sidebar() {
        if (this.isClosed() || this.extension.sidebarManager() == null) {
            return null;
        }
        if (this.sidebar == null) {
            this.sidebar = this.extension.sidebarManager().createConnection(this.session);
        }
        return this.sidebar.sidebar();
    }

    public boolean placementWithin(long now, long interval) {
        return this.placed && now - this.lastPlacement < interval;
    }

    public void placed(long now) {
        this.placed = true;
        this.lastPlacement = now;
    }

    public void queueHitbox(HardHitboxPayload hitbox) {
        if (!this.isClosed()) {
            if (this.pendingHitboxes == null) {
                this.pendingHitboxes = new Int2ObjectOpenHashMap<>();
            }
            this.pendingHitboxes.put(hitbox.entityId(), hitbox);
        }
    }

    @Nullable
    public HardHitboxPayload takeHitbox(int entityId) {
        return this.isClosed() || this.pendingHitboxes == null ? null : this.pendingHitboxes.remove(entityId);
    }

    public void queueShape(DebugShape shape) {
        if (this.isClosed()) {
            return;
        }
        boolean schedule = this.pendingShapes == null;
        if (schedule) {
            this.pendingShapes = new Long2ObjectLinkedOpenHashMap<>();
        }
        this.pendingShapes.put(shape.getId(), shape);
        if (schedule) {
            this.session.executeInEventLoop(this::flushShapes);
        }
    }

    private void flushShapes() {
        Long2ObjectLinkedOpenHashMap<DebugShape> shapes = this.pendingShapes;
        this.pendingShapes = null;
        if (shapes == null || this.isClosed()) {
            return;
        }
        DebugDrawerPacket packet = new DebugDrawerPacket();
        packet.getShapes().addAll(shapes.values());
        this.session.sendUpstreamPacket(packet);
    }

    public void send(ServerCustomPacket packet) {
        if (!this.isClosed()) {
            this.extension.customPayloads().send(this.session, packet);
        }
    }

    public void installHandler(String name, ChannelHandler handler, boolean tickEventLoop) {
        Channel channel = this.session.getUpstream().getSession().getPeer().getChannel();
        if (tickEventLoop) {
            channel.pipeline().addBefore(this.session.getTickEventLoop(), BedrockPeer.NAME, name, handler);
        } else {
            channel.pipeline().addBefore(BedrockPeer.NAME, name, handler);
        }
        this.handlers.add(handler);
    }

    public void blockBreakHandler(BlockBreakHandler handler) {
        this.originalBlockBreakHandler = this.session.getBlockBreakHandler();
        this.blockBreakHandler = handler;
        this.session.setBlockBreakHandler(handler);
    }

    void close() {
        GeyserSidebarConnection connection;
        synchronized (this) {
            if (this.closed) {
                return;
            }
            this.closed = true;
            connection = this.sidebar;
            this.sidebar = null;
        }
        if (connection != null) {
            connection.close();
        }
        this.session.executeInEventLoop(() -> {
            if (this.heldDye != null) {
                this.heldDye.close();
            }
            if (this.items != null) {
                this.items.close();
            }
            if (this.blockBreakHandler != null && this.session.getBlockBreakHandler() == this.blockBreakHandler) {
                this.session.setBlockBreakHandler(this.originalBlockBreakHandler);
            }
            Channel channel = this.session.getUpstream().getSession().getPeer().getChannel();
            for (ChannelHandler handler : this.handlers) {
                if (channel.pipeline().context(handler) != null) {
                    channel.pipeline().remove(handler);
                }
            }
            this.handlers.clear();
            this.pendingHitboxes = null;
            this.pendingShapes = null;
        });
    }
}
