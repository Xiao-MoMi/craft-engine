package net.momirealms.craftengine.bedrock.geyser.bossbar;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.geyser.network.GeyserJavaPackets;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import net.momirealms.craftengine.bedrock.geyser.proxy.network.BossEventPacketProxy;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.AdventureComponentProxy;
import net.momirealms.craftengine.bedrock.text.BossBarStyle;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.BossBar;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.BossBarAction;
import org.geysermc.mcprotocollib.protocol.data.game.BossBarColor;
import org.geysermc.mcprotocollib.protocol.data.game.BossBarDivision;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundBossEventPacket;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class GeyserBossBars implements GeyserJavaPackets.Interceptor<ClientboundBossEventPacket> {
    private final String[][] markers = new String[BossBarColor.values().length][BossBarDivision.values().length];
    private final Map<BossBar, Title> titles = Collections.synchronizedMap(new WeakHashMap<>());

    public GeyserBossBars(JsonObject styles) {
        // 将封包里的颜色和分段枚举转换成资源包生成的键，例如 blue/notched_6
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

    @Override
    public void translate(GeyserPlayer player, ClientboundBossEventPacket packet, PacketTranslator<ClientboundBossEventPacket> original) {
        GeyserSession session = player.session();
        switch (packet.getAction()) {
            case ADD -> {
                Title title = new Title(BossEventPacketProxy.INSTANCE.title(packet), this.marker(packet));
                // 原翻译器处理 ADD 时会创建 BossBar，随后才能从会话缓存中取到它。
                original.translate(session, title.apply(packet));
                BossBar bar = session.getEntityCache().getBossBar(packet.getUuid());
                if (bar != null) {
                    this.titles.put(bar, title);
                }
            }
            case UPDATE_STYLE, UPDATE_TITLE -> {
                BossBar bar = session.getEntityCache().getBossBar(packet.getUuid());
                Title previous = this.titles.get(bar);
                if (previous == null) {
                    original.translate(session, packet);
                    return;
                }
                if (packet.getAction() == BossBarAction.UPDATE_TITLE) {
                    // 这次只改标题，沿用当前的样式标记
                    Title title = new Title(BossEventPacketProxy.INSTANCE.title(packet), previous.marker());
                    this.titles.put(bar, title);
                    original.translate(session, title.apply(packet));
                } else {
                    original.translate(session, packet);
                    String marker = this.marker(packet);
                    if (!Objects.equals(marker, previous.marker())) {
                        // 样式更新包没有标题。用缓存的原始标题补发一次标题更新，让 HUD 切换样式
                        // 新样式没有自定义贴图时，补发的标题会去掉旧标记，HUD 就会恢复原版进度条
                        Title title = new Title(previous.component(), marker);
                        this.titles.put(bar, title);
                        ClientboundBossEventPacket name = BossEventPacketProxy.INSTANCE.withTitle(packet.withAction(BossBarAction.UPDATE_TITLE), title.component());
                        original.translate(session, title.apply(name));
                    }
                }
            }
            case REMOVE -> {
                this.titles.remove(session.getEntityCache().getBossBar(packet.getUuid()));
                original.translate(session, packet);
            }
            default -> original.translate(session, packet);
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
            // 标记只由格式码组成。末尾重置格式，避免影响后面的原始标题
            AdventureComponentProxy components = AdventureComponentProxy.INSTANCE;
            Object title = components.children(components.text(this.marker + "§r"), List.of(this.component));
            return BossEventPacketProxy.INSTANCE.withTitle(packet, title);
        }
    }
}
