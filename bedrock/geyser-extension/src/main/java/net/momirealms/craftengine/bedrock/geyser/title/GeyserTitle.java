package net.momirealms.craftengine.bedrock.geyser.title;

import net.momirealms.craftengine.bedrock.GuiTitlePart;
import net.momirealms.craftengine.bedrock.geyser.CraftEngineGeyserExtension;
import net.momirealms.craftengine.bedrock.geyser.config.GeyserConfig;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.SetSubtitleTextPacketProxy;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.SetTitleTextPacketProxy;
import net.momirealms.craftengine.bedrock.geyser.util.GeyserTextParts;
import net.momirealms.craftengine.bedrock.text.ContainerTitleCodec;
import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.title.ClientboundSetSubtitleTextPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.title.ClientboundSetTitleTextPacket;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class GeyserTitle {
    private final ContainerTitleLayout layout;
    private final boolean forceJavaLayout;

    public GeyserTitle(ContainerTitleLayout layout, boolean forceJavaLayout) {
        this.layout = layout;
        this.forceJavaLayout = forceJavaLayout;
    }

    public void title(GeyserPlayer player, ClientboundSetTitleTextPacket packet, PacketTranslator<ClientboundSetTitleTextPacket> original) {
        if (player.clientPackLoaded()) {
            Object text = this.rewrite(SetTitleTextPacketProxy.INSTANCE.text(packet), player.locale());
            if (text != null) {
                packet = SetTitleTextPacketProxy.INSTANCE.withText(packet, text);
            }
        }
        original.translate(player.session(), packet);
    }

    public void subtitle(GeyserPlayer player, ClientboundSetSubtitleTextPacket packet, PacketTranslator<ClientboundSetSubtitleTextPacket> original) {
        if (player.clientPackLoaded()) {
            Object text = this.rewrite(SetSubtitleTextPacketProxy.INSTANCE.text(packet), player.locale());
            if (text != null) {
                packet = SetSubtitleTextPacketProxy.INSTANCE.withText(packet, text);
            }
        }
        original.translate(player.session(), packet);
    }

    @Nullable
    private Object rewrite(Object component, String locale) {
        try {
            List<GuiTitlePart> parts = new ArrayList<>();
            if (!GeyserTextParts.collectParts(component, "minecraft:default", 0xffffff, 0, parts, locale)) {
                return null;
            }
            List<List<ContainerTitleLayout.Run>> layers = this.layout.layoutTitle(parts, this.forceJavaLayout);
            if (layers.isEmpty()) {
                return null;
            }
            String payload = ContainerTitleCodec.encode(ContainerTitleCodec.TITLE_MARKER, GeyserTextParts.serializeLayers(layers, parts, locale));
            return GeyserTextParts.resolvedText(payload);
        } catch (RuntimeException exception) {
            if (GeyserConfig.debug()) {
                CraftEngineGeyserExtension.instance().logger().error("Failed to lay out title", exception);
            }
            return null;
        }
    }
}
