package net.momirealms.craftengine.bedrock.geyser.bossbar;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.bedrock.GuiTitlePart;
import net.momirealms.craftengine.bedrock.geyser.CraftEngineGeyserExtension;
import net.momirealms.craftengine.bedrock.geyser.config.GeyserConfig;
import net.momirealms.craftengine.bedrock.geyser.network.GeyserJavaPackets;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import net.momirealms.craftengine.bedrock.geyser.proxy.network.BossEventPacketProxy;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.AdventureComponentProxy;
import net.momirealms.craftengine.bedrock.geyser.util.GeyserTextParts;
import net.momirealms.craftengine.bedrock.text.BossBarStyle;
import net.momirealms.craftengine.bedrock.text.BossBarTitleCodec;
import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;
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
    @Nullable private final ContainerTitleLayout layout;
    private final boolean leftAnchored;
    private final boolean fontFaces;
    private final boolean compactPositions;
    private final Map<String, Integer> fontBaselines;
    private final int baselineCount;

    public GeyserBossBars(JsonObject styles, @Nullable ContainerTitleLayout layout) {
        JsonElement titleLayout = styles.get(BedrockPackLayout.BOSS_BAR_TITLE_LAYOUT);
        int version = titleLayout != null && titleLayout.getAsJsonPrimitive().isNumber() ? titleLayout.getAsInt() : 0;
        this.compactPositions = version == BossBarTitleCodec.COMPACT_POSITION_VERSION;
        this.fontFaces = this.compactPositions || version == BossBarTitleCodec.FONT_FACE_VERSION;
        this.leftAnchored = this.fontFaces || version == BossBarTitleCodec.LEFT_ANCHORED_VERSION || version == BossBarTitleCodec.VERTICAL_OFFSET_VERSION;
        Map<String, Integer> baselines = new HashMap<>();
        int highest = 0;
        if ((this.fontFaces || version == BossBarTitleCodec.VERTICAL_OFFSET_VERSION) && styles.has(BedrockPackLayout.BOSS_BAR_FONT_BASELINES)) {
            JsonObject mapping = styles.getAsJsonObject(BedrockPackLayout.BOSS_BAR_FONT_BASELINES);
            for (Map.Entry<String, JsonElement> entry : mapping.entrySet()) {
                int id = entry.getValue().getAsInt();
                baselines.put(entry.getKey(), id);
                highest = Math.max(highest, id);
            }
        }
        this.fontBaselines = Map.copyOf(baselines);
        this.baselineCount = highest + 1;
        this.layout = this.leftAnchored || (titleLayout != null && titleLayout.getAsBoolean()) ? layout : null;
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
                Title title = this.title(player, packet, this.marker(packet));
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
                    Title title = this.title(player, packet, previous.marker());
                    this.titles.put(bar, title);
                    original.translate(session, title.apply(packet));
                } else {
                    original.translate(session, packet);
                    String marker = this.marker(packet);
                    if (!Objects.equals(marker, previous.marker())) {
                        // 样式更新包没有标题。用缓存的原始标题补发一次标题更新，让 HUD 切换样式
                        // 新样式没有自定义贴图时，补发的标题会去掉旧标记，HUD 就会恢复原版进度条
                        Title title = new Title(previous.component(), previous.encoded(), marker);
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

    private Title title(GeyserPlayer player, ClientboundBossEventPacket packet, @Nullable String marker) {
        Object component = BossEventPacketProxy.INSTANCE.title(packet);
        String encoded = null;
        if (this.layout != null && player.clientPackLoaded()) {
            encoded = this.encode(component, player.locale());
        }
        return new Title(component, encoded, marker);
    }

    @Nullable
    private String encode(Object component, String locale) {
        try {
            List<GuiTitlePart> parts = new ArrayList<>();
            if (!GeyserTextParts.collectParts(component, "minecraft:default", 0xffffff, 0, parts, locale)) {
                return null;
            }
            ContainerTitleLayout.MeasuredLayout measured = this.layout.layoutBossBar(parts, this.leftAnchored, this.fontBaselines);
            if (measured.layers().isEmpty()) {
                return null;
            }
            if (this.compactPositions) {
                return BossBarTitleCodec.encodePositioned(measured, this.baselineCount, layer -> GeyserTextParts.serializeLayer(layer, parts, locale));
            }
            return BossBarTitleCodec.encode(measured, this.leftAnchored, this.baselineCount, this.fontFaces, layer -> GeyserTextParts.serializeLayer(layer, parts, locale));
        } catch (UnsupportedOperationException ignored) {
            return null;
        } catch (RuntimeException exception) {
            if (GeyserConfig.debug()) {
                CraftEngineGeyserExtension.instance().logger().error("Failed to lay out boss bar title", exception);
            }
            return null;
        }
    }

    private record Title(Object component, @Nullable String encoded, @Nullable String marker) {

        ClientboundBossEventPacket apply(ClientboundBossEventPacket packet) {
            boolean layered = this.encoded != null && BossBarTitleCodec.fits(this.encoded, this.marker);
            if (!layered && this.marker == null) {
                return packet;
            }
            Object title = layered ? GeyserTextParts.resolvedText(this.encoded) : this.component;
            if (this.marker != null) {
                AdventureComponentProxy components = AdventureComponentProxy.INSTANCE;
                title = components.children(components.text(this.marker + "§r"), List.of(title));
            }
            return BossEventPacketProxy.INSTANCE.withTitle(packet, title);
        }
    }
}
