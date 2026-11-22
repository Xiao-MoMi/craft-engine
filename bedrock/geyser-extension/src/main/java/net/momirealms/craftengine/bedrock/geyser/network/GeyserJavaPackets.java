package net.momirealms.craftengine.bedrock.geyser.network;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.geyser.CraftEngineGeyserExtension;
import net.momirealms.craftengine.bedrock.geyser.block.GeyserBlockBreakEffects;
import net.momirealms.craftengine.bedrock.geyser.block.GeyserWorldBlocks;
import net.momirealms.craftengine.bedrock.geyser.bossbar.GeyserBossBars;
import net.momirealms.craftengine.bedrock.geyser.gui.GeyserGuiMenus;
import net.momirealms.craftengine.bedrock.geyser.item.GeyserHeldDye;
import net.momirealms.craftengine.bedrock.geyser.item.GeyserWorldItemViews;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayerManager;
import net.momirealms.craftengine.bedrock.geyser.scoreboard.SidebarSession;
import net.momirealms.craftengine.bedrock.geyser.title.GeyserTitle;
import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;
import org.geysermc.geyser.registry.PacketTranslatorRegistry;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.network.event.session.PacketSendingEvent;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundCustomPayloadPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundBossEventPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundAddEntityPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelEventPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.title.ClientboundSetSubtitleTextPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.title.ClientboundSetTitleTextPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSetCarriedItemPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundLoginFinishedPacket;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public final class GeyserJavaPackets {
    private final PacketTranslatorRegistry<Packet> registry;
    private final GeyserPlayerManager players;
    private final Observers received = new Observers();
    private final Observers sent = new Observers();
    private final Map<Class<?>, BiFunction<GeyserPlayer, Packet, Packet>> sending = new ConcurrentHashMap<>();
    private final List<InterceptingTranslator<?>> translators = new ArrayList<>();

    public GeyserJavaPackets(GeyserPlayerManager players, PacketTranslatorRegistry<Packet> registry) {
        this.registry = registry;
        this.players = players;
    }

    public void install(CraftEngineGeyserExtension extension) {
        this.intercept(ClientboundLoginFinishedPacket.class, (player, packet, original) -> {
            GeyserSession session = player.session();
            boolean attached = player.javaConnection().attach(session.getDownstream().getSession());
            original.translate(session, packet);
            // MCProtocolLib 已开始分发登录成功包，本次新挂载的监听器从下一包才生效。
            if (attached) {
                this.receive(player, packet);
            }
        });
        this.intercept(ClientboundAddEntityPacket.class, extension.entityManager().spawnTranslator());
        this.intercept(ClientboundCustomPayloadPacket.class, extension.customPayloads());
        ContainerTitleLayout layout = extension.glyphManager().glyphs().layout();
        if (layout != null) {
            this.intercept(ClientboundOpenScreenPacket.class, new GeyserGuiMenus(layout));
            GeyserTitle titles = new GeyserTitle(layout, extension.extensionData().forceJavaTitleLayout());
            this.intercept(ClientboundSetTitleTextPacket.class, titles::title);
            this.intercept(ClientboundSetSubtitleTextPacket.class, titles::subtitle);
        }
        GeyserWorldBlocks blocks = extension.blockManager().worldBlocks();
        if (!blocks.isEmpty()) {
            this.intercept(ClientboundLevelEventPacket.class, new GeyserBlockBreakEffects(blocks));
        }
        JsonObject styles = extension.extensionData().bossBars();
        if (styles != null && !styles.isEmpty()) {
            this.intercept(ClientboundBossEventPacket.class, new GeyserBossBars(styles));
        }
        if (extension.sidebarManager() != null) {
            this.interceptSidebar(ClientboundSetObjectivePacket.class, SidebarSession::translate);
            this.interceptSidebar(ClientboundSetScorePacket.class, SidebarSession::translate);
            this.interceptSidebar(ClientboundResetScorePacket.class, SidebarSession::translate);
            this.interceptSidebar(ClientboundSetPlayerTeamPacket.class, SidebarSession::translate);
            this.interceptSidebar(ClientboundSetDisplayObjectivePacket.class, SidebarSession::translate);
        }
        if (extension.itemManager().hasRegisteredItems()) {
            this.intercept(ClientboundSetEntityDataPacket.class, (player, packet, original) -> {
                player.activeUse().onEntityData(packet);
                GeyserWorldItemViews.translateMetadata(player.session(), packet, original);
            });
        }
        if (extension.itemManager().hasDyedItems()) {
            this.onSent(ServerboundSetCarriedItemPacket.class, (player, packet) -> refreshHands(player));
            this.onSent(ServerboundContainerClickPacket.class, (player, packet) -> refreshHands(player));
            this.onSent(ServerboundPlayerActionPacket.class, (player, packet) -> {
                if (packet.getAction() == PlayerAction.DROP_ITEM || packet.getAction() == PlayerAction.DROP_ITEM_STACK) {
                    refreshHands(player);
                }
            });
        }
    }

    private static void refreshHands(GeyserPlayer player) {
        GeyserHeldDye dye = player.heldDye();
        if (dye != null) {
            dye.queueHands();
        }
    }

    private <T extends Packet> void interceptSidebar(Class<T> type, SidebarTranslator<T> translator) {
        this.intercept(type, (player, packet, original) -> {
            SidebarSession sidebar = player.sidebar();
            if (sidebar == null) {
                original.translate(player.session(), packet);
                return;
            }
            // 在出站监听读取前，一起提交原始组件、Geyser 槽位和对应的标题快照。
            synchronized (sidebar) {
                translator.translate(sidebar, packet, original);
            }
        });
    }

    @FunctionalInterface
    private interface SidebarTranslator<T extends Packet> {
        void translate(SidebarSession sidebar, T packet, PacketTranslator<T> original);
    }

    public <T extends Packet> void onReceive(Class<T> type, BiConsumer<GeyserPlayer, T> listener) {
        this.received.register(type, listener);
    }

    public <T extends Packet> void onSent(Class<T> type, BiConsumer<GeyserPlayer, T> listener) {
        this.sent.register(type, listener);
    }

    public <T extends Packet> void onSending(Class<T> type, SendFilter<T> filter) {
        this.sending.merge(type, (player, packet) -> filter.apply(player, type.cast(packet)),
                (first, next) -> (player, packet) -> {
                    Packet current = first.apply(player, packet);
                    return current == null ? null : next.apply(player, current);
                });
    }

    void receive(GeyserPlayer player, Packet packet) {
        this.received.dispatch(player, packet);
    }

    void sending(GeyserPlayer player, PacketSendingEvent event) {
        if (event.isCancelled()) {
            return;
        }
        Packet packet = event.getPacket();
        BiFunction<GeyserPlayer, Packet, Packet> filter = this.sending.get(packet.getClass());
        if (filter != null) {
            Packet result = filter.apply(player, packet);
            if (result == null) {
                event.setCancelled(true);
            } else if (result != packet) {
                event.setPacket(result);
            }
        }
    }

    void sent(GeyserPlayer player, Packet packet) {
        this.sent.dispatch(player, packet);
    }

    @SuppressWarnings("unchecked")
    public <T extends Packet> void intercept(Class<T> type, Interceptor<T> interceptor) {
        PacketTranslator<T> original = (PacketTranslator<T>) this.registry.get(type);
        if (original == null) {
            throw new IllegalStateException("Missing Geyser translator for " + type.getName());
        }
        InterceptingTranslator<T> translator = new InterceptingTranslator<>(type, original, this.players, interceptor);
        this.registry.register(type, translator);
        this.translators.add(translator);
    }

    public void uninstall() {
        for (InterceptingTranslator<?> translator : this.translators.reversed()) {
            this.registry.get().replace(translator.type, translator, translator.original);
        }
        this.translators.clear();
        this.sending.clear();
        this.received.listeners.clear();
        this.sent.listeners.clear();
    }

    @FunctionalInterface
    public interface SendFilter<T extends Packet> {
        // 返回原包或同类型替换包，返回 null 取消发送并停止后续处理。
        @Nullable
        T apply(GeyserPlayer player, T packet);
    }

    private static final class Observers {
        private final Map<Class<?>, BiConsumer<GeyserPlayer, Packet>> listeners = new ConcurrentHashMap<>();

        private <T extends Packet> void register(Class<T> type, BiConsumer<GeyserPlayer, T> listener) {
            this.listeners.merge(type, (player, packet) -> listener.accept(player, type.cast(packet)), BiConsumer::andThen);
        }

        private void dispatch(GeyserPlayer player, Packet packet) {
            BiConsumer<GeyserPlayer, Packet> listener = this.listeners.get(packet.getClass());
            if (listener != null) {
                listener.accept(player, packet);
            }
        }
    }

    @FunctionalInterface
    public interface Interceptor<T extends Packet> {
        void translate(GeyserPlayer player, T packet, PacketTranslator<T> original);
    }

    private static final class InterceptingTranslator<T extends Packet> extends PacketTranslator<T> {
        private final Class<T> type;
        private final PacketTranslator<T> original;
        private final GeyserPlayerManager players;
        private final Interceptor<T> interceptor;

        private InterceptingTranslator(Class<T> type, PacketTranslator<T> original, GeyserPlayerManager players, Interceptor<T> interceptor) {
            this.type = type;
            this.original = original;
            this.players = players;
            this.interceptor = interceptor;
        }

        @Override
        public void translate(GeyserSession session, T packet) {
            GeyserPlayer player = this.players.get(session);
            if (player == null || player.isClosed()) {
                this.original.translate(session, packet);
            } else {
                this.interceptor.translate(player, packet, this.original);
            }
        }

        @Override
        public boolean shouldExecuteInEventLoop() {
            return this.original.shouldExecuteInEventLoop();
        }
    }
}
