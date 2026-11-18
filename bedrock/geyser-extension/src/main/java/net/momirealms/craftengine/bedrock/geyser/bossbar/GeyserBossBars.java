package net.momirealms.craftengine.bedrock.geyser.bossbar;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.geyser.proxy.network.BossEventPacketProxy;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.AdventureComponentProxy;
import net.momirealms.craftengine.bedrock.text.BossBarStyle;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.BossBar;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.BossBarAction;
import org.geysermc.mcprotocollib.protocol.data.game.BossBarColor;
import org.geysermc.mcprotocollib.protocol.data.game.BossBarDivision;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundBossEventPacket;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

public final class GeyserBossBars extends PacketTranslator<ClientboundBossEventPacket> {
    private final PacketTranslator<ClientboundBossEventPacket> delegate;
    private final String[][] markers = new String[BossBarColor.values().length][BossBarDivision.values().length];
    private final Map<BossBar, Title> titles = Collections.synchronizedMap(new WeakHashMap<>());

    GeyserBossBars(JsonObject styles, PacketTranslator<ClientboundBossEventPacket> delegate) {
        this.delegate = delegate;
        for (BossBarColor color : BossBarColor.values()) {
            String name = switch (color) {
                case CYAN -> "blue";
                case LIME -> "green";
                default -> color.name().toLowerCase(Locale.ROOT);
            };
            for (BossBarDivision division : BossBarDivision.values()) {
                String overlay = division.name().toLowerCase(Locale.ROOT).replace("notches_", "notched_");
                JsonElement id = styles.get(name + "/" + overlay);
                if (id != null) {
                    this.markers[color.ordinal()][division.ordinal()] = BossBarStyle.marker(id.getAsInt());
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static GeyserBossBars install(JsonObject styles) {
        PacketTranslator<ClientboundBossEventPacket> delegate = (PacketTranslator<ClientboundBossEventPacket>) Registries.JAVA_PACKET_TRANSLATORS.get(ClientboundBossEventPacket.class);
        GeyserBossBars translator = new GeyserBossBars(styles, delegate);
        Registries.JAVA_PACKET_TRANSLATORS.register(ClientboundBossEventPacket.class, translator);
        return translator;
    }

    public void uninstall() {
        Registries.JAVA_PACKET_TRANSLATORS.get().replace(ClientboundBossEventPacket.class, this, this.delegate);
        this.titles.clear();
    }

    @Override
    public void translate(GeyserSession session, ClientboundBossEventPacket packet) {
        switch (packet.getAction()) {
            case ADD -> {
                Title title = new Title(BossEventPacketProxy.INSTANCE.title(packet), this.marker(packet));
                this.delegate.translate(session, title.apply(packet));
                BossBar bar = session.getEntityCache().getBossBar(packet.getUuid());
                if (bar != null) {
                    this.titles.put(bar, title);
                }
            }
            case UPDATE_STYLE, UPDATE_TITLE -> {
                BossBar bar = session.getEntityCache().getBossBar(packet.getUuid());
                Title previous = this.titles.get(bar);
                if (previous == null) {
                    this.delegate.translate(session, packet);
                    return;
                }
                if (packet.getAction() == BossBarAction.UPDATE_TITLE) {
                    Title title = new Title(BossEventPacketProxy.INSTANCE.title(packet), previous.marker());
                    this.titles.put(bar, title);
                    this.delegate.translate(session, title.apply(packet));
                } else {
                    this.delegate.translate(session, packet);
                    String marker = this.marker(packet);
                    if (!Objects.equals(marker, previous.marker())) {
                        // Style updates do not carry a name. Resend the original component with its new marker.
                        Title title = new Title(previous.component(), marker);
                        this.titles.put(bar, title);
                        ClientboundBossEventPacket name = BossEventPacketProxy.INSTANCE.withTitle(packet.withAction(BossBarAction.UPDATE_TITLE), title.component());
                        this.delegate.translate(session, title.apply(name));
                    }
                }
            }
            case REMOVE -> {
                this.titles.remove(session.getEntityCache().getBossBar(packet.getUuid()));
                this.delegate.translate(session, packet);
            }
            default -> this.delegate.translate(session, packet);
        }
    }

    private String marker(ClientboundBossEventPacket packet) {
        return this.markers[packet.getColor().ordinal()][packet.getDivision().ordinal()];
    }

    private record Title(Object component, @Nullable String marker) {

        ClientboundBossEventPacket apply(ClientboundBossEventPacket packet) {
            if (this.marker == null) {
                return packet;
            }
            AdventureComponentProxy components = AdventureComponentProxy.INSTANCE;
            Object title = components.children(components.text(this.marker + "§r"), List.of(this.component));
            return BossEventPacketProxy.INSTANCE.withTitle(packet, title);
        }
    }
}
