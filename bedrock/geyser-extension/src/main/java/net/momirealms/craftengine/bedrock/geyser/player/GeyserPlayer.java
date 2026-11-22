package net.momirealms.craftengine.bedrock.geyser.player;

import io.netty.channel.Channel;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.momirealms.craftengine.bedrock.geyser.CraftEngineGeyserExtension;
import net.momirealms.craftengine.bedrock.geyser.block.GeyserBlockPlacementHandler;
import net.momirealms.craftengine.bedrock.geyser.config.GeyserConfig;
import net.momirealms.craftengine.bedrock.geyser.entity.display.GeyserDisplayModelOwners;
import net.momirealms.craftengine.bedrock.geyser.entity.model.GeyserNativeModels;
import net.momirealms.craftengine.bedrock.geyser.item.*;
import net.momirealms.craftengine.bedrock.geyser.item.component.GeyserItemComponentPatcher;
import net.momirealms.craftengine.bedrock.geyser.network.GeyserJavaPacketHandler;
import net.momirealms.craftengine.bedrock.geyser.network.GeyserPacketHandler;
import net.momirealms.craftengine.bedrock.geyser.network.ServerCustomPacket;
import net.momirealms.craftengine.bedrock.geyser.scoreboard.SidebarSession;
import net.momirealms.craftengine.bedrock.geyser.title.TitleSession;
import net.momirealms.craftengine.bedrock.network.HardHitboxPayload;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.data.debugshape.DebugShape;
import org.cloudburstmc.protocol.bedrock.packet.DebugDrawerPacket;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePackClientResponsePacket;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePackStackPacket;
import org.geysermc.floodgate.util.UiProfile;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.BlockBreakHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class GeyserPlayer {
    private final CraftEngineGeyserExtension extension;
    private final GeyserSession session;
    private final GeyserActiveItemUse activeUse;
    private final GeyserNativeModels.Connection nativeModels;
    private final GeyserDisplayModelOwners displayOwners;
    private GeyserBlockPlacementHandler placement;
    private GeyserItemComponentPatcher componentPatcher;
    private final GeyserJavaPacketHandler javaConnection;
    private GeyserPacketHandler packetHandler;
    private Int2ObjectMap<HardHitboxPayload> pendingHitboxes;
    private Long2ObjectLinkedOpenHashMap<DebugShape> pendingShapes;
    private Vector3f cameraDirection;
    private Vector3f sampledCameraDirection;
    private Vector2f cameraAngles;
    private SidebarSession sidebar;
    private GeyserItemRuntime items;
    private GeyserHeldDye heldDye;
    private GeyserElytraEquipment elytra;
    private BlockBreakHandler originalBlockBreakHandler;
    private BlockBreakHandler blockBreakHandler;
    private boolean clientPackOffered;
    private volatile boolean clientPackLoaded;
    private final TitleSession titleSession;
    private volatile boolean closed;

    GeyserPlayer(CraftEngineGeyserExtension extension, GeyserSession session) {
        this.extension = extension;
        this.nativeModels = extension.entityManager().createNativeConnection(session);
        this.displayOwners = extension.entityManager().createDisplayOwners(session);
        this.session = session;
        this.titleSession = new TitleSession(session.getTickEventLoop());
        this.javaConnection = new GeyserJavaPacketHandler(this, extension.javaPackets());
        this.activeUse = new GeyserActiveItemUse(session, extension.itemManager()::isRegisteredItem);
    }

    synchronized void initialize() {
        this.heldDye = this.extension.itemManager().initPlayerDye(this);
        this.componentPatcher = this.extension.itemManager().createComponentPatcher(this);
        this.items = this.extension.itemManager().initPlayer(this);
        this.placement = this.extension.blockManager().initPlayer(this);
        this.elytra = this.extension.itemManager().initPlayerElytra(this);

        this.packetHandler = new GeyserPacketHandler(this, GeyserConfig.containerCloseHandlerEnabled());
        this.session.getUpstream().getSession().getPeer().getChannel().pipeline().addBefore(BedrockPeer.NAME, "craftengine-packets", this.packetHandler);
    }

    public GeyserJavaPacketHandler javaConnection() {
        return this.javaConnection;
    }

    public GeyserActiveItemUse activeUse() {
        return this.activeUse;
    }

    public GeyserSession session() {
        return this.session;
    }

    @Nullable
    public Vector2f displayCameraAngles() {
        Vector3f direction = this.cameraDirection;
        if (!Objects.equals(direction, this.sampledCameraDirection)) {
            this.sampledCameraDirection = direction;
            float lengthSquared = direction == null ? 0 : direction.lengthSquared();
            if (lengthSquared >= 0.8f && lengthSquared <= 1.2f) {
                double horizontal = Math.hypot(direction.getX(), direction.getZ());
                float yaw = horizontal < 1e-5 ? this.session.getPlayerEntity().getYaw() : (float) Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ()));
                float pitch = (float) Math.toDegrees(Math.atan2(-direction.getY(), horizontal));
                this.cameraAngles = Vector2f.from(yaw, pitch);
            } else {
                this.cameraAngles = null;
            }
        }
        return this.cameraAngles;
    }

    public String locale() {
        return this.session.locale();
    }

    public UiProfile uiProfile() {
        return this.session.getClientData().getUiProfile();
    }

    public void trackClientPack(ResourcePackStackPacket packet) {
        String packId = this.extension.clientPackId();
        this.clientPackOffered = packId != null && packet.getResourcePacks().stream().anyMatch(entry -> packId.equals(entry.getPackId()));
    }

    public void confirmClientPack(ResourcePackClientResponsePacket packet) {
        if (packet.getStatus() == ResourcePackClientResponsePacket.Status.COMPLETED) {
            this.clientPackLoaded = this.clientPackOffered;
        }
    }

    @Nullable
    public TitleSession titleSession() {
        return this.clientPackLoaded ? this.titleSession : null;
    }

    public boolean clientPackLoaded() {
        return this.clientPackLoaded;
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
        if (this.closed || this.extension.sidebarManager() == null) {
            return null;
        }
        if (this.sidebar == null) {
            this.sidebar = this.extension.sidebarManager().createSession(this.session);
        }
        return this.sidebar;
    }

    public void queueHitbox(HardHitboxPayload hitbox) {
        if (this.pendingHitboxes == null) {
            this.pendingHitboxes = new Int2ObjectOpenHashMap<>();
        }
        this.pendingHitboxes.put(hitbox.entityId(), hitbox);
    }

    @Nullable
    public HardHitboxPayload takeHitbox(int entityId) {
        return this.pendingHitboxes == null ? null : this.pendingHitboxes.remove(entityId);
    }

    public void queueShape(DebugShape shape) {
        if (this.pendingShapes == null) {
            this.pendingShapes = new Long2ObjectLinkedOpenHashMap<>();
        }
        boolean schedule = this.pendingShapes.isEmpty();
        this.pendingShapes.put(shape.getId(), shape);
        if (schedule) {
            this.session.executeInEventLoop(this::flushShapes);
        }
    }

    private void flushShapes() {
        Long2ObjectLinkedOpenHashMap<DebugShape> shapes = this.pendingShapes;
        if (shapes == null || shapes.isEmpty()) {
            return;
        }
        DebugDrawerPacket packet = new DebugDrawerPacket();
        packet.getShapes().addAll(shapes.values());
        shapes.clear();
        this.session.sendUpstreamPacket(packet);
    }

    public void send(ServerCustomPacket packet) {
        this.extension.customPayloads().send(this.session, packet);
    }

    @Nullable
    public GeyserNativeModels.Connection nativeModels() {
        return this.nativeModels;
    }

    @Nullable
    public GeyserDisplayModelOwners displayOwners() {
        return this.displayOwners;
    }

    @Nullable
    public GeyserSelectedItemHandler selectedItems() {
        return this.items == null ? null : this.items.selectedHandler();
    }

    @Nullable
    public GeyserHeldDye heldDye() {
        return this.heldDye;
    }

    @Nullable
    public GeyserElytraEquipment elytra() {
        return this.elytra;
    }

    @Nullable
    public GeyserBlockPlacementHandler placement() {
        return this.placement;
    }

    @Nullable
    public GeyserItemComponentPatcher componentPatcher() {
        return this.componentPatcher;
    }

    public void cameraDirection(Vector3f direction) {
        this.cameraDirection = direction;
    }

    public void blockBreakHandler(BlockBreakHandler handler) {
        this.originalBlockBreakHandler = this.session.getBlockBreakHandler();
        this.blockBreakHandler = handler;
        this.session.setBlockBreakHandler(handler);
    }

    void close() {
        SidebarSession connection;
        synchronized (this) {
            if (this.closed) {
                return;
            }
            this.closed = true;
            connection = this.sidebar;
            this.sidebar = null;
        }
        this.javaConnection.close();
        if (connection != null) {
            connection.close();
        }
        this.session.executeInEventLoop(() -> {
            this.titleSession.close();
            if (this.elytra != null) {
                this.elytra.close();
            }
            if (this.items != null) {
                this.items.close();
            }
            if (this.blockBreakHandler != null && this.session.getBlockBreakHandler() == this.blockBreakHandler) {
                this.session.setBlockBreakHandler(this.originalBlockBreakHandler);
            }
            Channel channel = this.session.getUpstream().getSession().getPeer().getChannel();
            if (this.packetHandler != null && channel.pipeline().context(this.packetHandler) != null) {
                channel.pipeline().remove(this.packetHandler);
            }
            this.pendingHitboxes = null;
            this.pendingShapes = null;
        });
    }
}
