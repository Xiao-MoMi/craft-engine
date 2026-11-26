package net.momirealms.craftengine.bedrock.geyser.title;

import net.momirealms.craftengine.bedrock.GuiTitlePart;
import net.momirealms.craftengine.bedrock.geyser.CraftEngineGeyserExtension;
import net.momirealms.craftengine.bedrock.geyser.config.GeyserConfig;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.SetActionBarTextPacketProxy;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.SetSubtitleTextPacketProxy;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.SetTitleTextPacketProxy;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.SystemChatPacketProxy;
import net.momirealms.craftengine.bedrock.geyser.util.GeyserTextParts;
import net.momirealms.craftengine.bedrock.text.ContainerTitleCodec;
import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;
import org.cloudburstmc.protocol.bedrock.packet.SetTitlePacket;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.title.ClientboundSetActionBarTextPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.title.ClientboundSetSubtitleTextPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.title.ClientboundSetTitleTextPacket;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GeyserTitle {
    private final ContainerTitleLayout layout;
    private final boolean forceJavaLayout;
    @Nullable private final Map<String, Integer> offsets;

    public GeyserTitle(ContainerTitleLayout layout, boolean forceJavaLayout, @Nullable Map<String, Integer> offsets) {
        this.layout = layout;
        this.forceJavaLayout = forceJavaLayout;
        this.offsets = offsets;
    }

    public void title(GeyserPlayer player, ClientboundSetTitleTextPacket packet, PacketTranslator<ClientboundSetTitleTextPacket> original) {
        if (player.clientPackLoaded()) {
            String text = this.rewrite(SetTitleTextPacketProxy.INSTANCE.text(packet), player.locale(), false);
            if (text != null) {
                packet = SetTitleTextPacketProxy.INSTANCE.withText(packet, GeyserTextParts.resolvedText(text));
            }
        }
        original.translate(player.session(), packet);
    }

    public void subtitle(GeyserPlayer player, ClientboundSetSubtitleTextPacket packet, PacketTranslator<ClientboundSetSubtitleTextPacket> original) {
        if (player.clientPackLoaded()) {
            String text = this.rewrite(SetSubtitleTextPacketProxy.INSTANCE.text(packet), player.locale(), false);
            if (text != null) {
                packet = SetSubtitleTextPacketProxy.INSTANCE.withText(packet, GeyserTextParts.resolvedText(text));
            }
        }
        original.translate(player.session(), packet);
    }

    public void actionBar(GeyserPlayer player, ClientboundSetActionBarTextPacket packet, PacketTranslator<ClientboundSetActionBarTextPacket> original) {
        if (player.clientPackLoaded() && this.sendActionBar(player, SetActionBarTextPacketProxy.INSTANCE.text(packet))) {
            return;
        }
        original.translate(player.session(), packet);
    }

    public void systemChat(GeyserPlayer player, ClientboundSystemChatPacket packet, PacketTranslator<ClientboundSystemChatPacket> original) {
        if (packet.isOverlay() && player.clientPackLoaded() && this.sendActionBar(player, SystemChatPacketProxy.INSTANCE.text(packet))) {
            return;
        }
        original.translate(player.session(), packet);
    }

    private boolean sendActionBar(GeyserPlayer player, Object component) {
        String text = this.rewrite(component, player.locale(), true);
        if (text == null) {
            return false;
        }
        // Geyser normally sends system chat overlays as jukebox popups. Both Java paths need the actionbar HUD factory.
        player.session().sendUpstreamPacket(TitleSession.packet(SetTitlePacket.Type.ACTIONBAR, text));
        return true;
    }

    @Nullable
    private String rewrite(Object component, String locale, boolean actionBar) {
        try {
            List<GuiTitlePart> parts = new ArrayList<>();
            if (!GeyserTextParts.collectParts(component, "minecraft:default", 0xffffff, 0, parts, locale)) {
                return null;
            }
            if (this.offsets != null) {
                ContainerTitleLayout.MeasuredLayout measured = this.layout.positioned(parts, this.offsets, !actionBar && this.forceJavaLayout);
                if (measured.layers().isEmpty()) {
                    return null;
                }
                String marker = actionBar ? ContainerTitleCodec.ACTIONBAR_MARKER : ContainerTitleCodec.TITLE_MARKER;
                return ContainerTitleCodec.positioned(marker, measured, true,
                        runs -> GeyserTextParts.serializeLayer(runs, parts, locale));
            }
            List<List<ContainerTitleLayout.Run>> layers = this.layout.layoutTitle(parts, this.forceJavaLayout);
            if (layers.isEmpty()) {
                return null;
            }
            return ContainerTitleCodec.encode(ContainerTitleCodec.TITLE_MARKER, GeyserTextParts.serializeLayers(layers, parts, locale));
        } catch (RuntimeException exception) {
            if (GeyserConfig.debug()) {
                CraftEngineGeyserExtension.instance().logger().error("Failed to lay out title", exception);
            }
            return null;
        }
    }
}
