package net.momirealms.craftengine.bedrock.geyser.gui;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.bedrock.GuiTitlePart;
import net.momirealms.craftengine.bedrock.geyser.CraftEngineGeyserExtension;
import net.momirealms.craftengine.bedrock.geyser.config.GeyserConfig;
import net.momirealms.craftengine.bedrock.geyser.glyph.GeyserGlyphSerializer;
import net.momirealms.craftengine.bedrock.geyser.network.GeyserJavaPackets;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import net.momirealms.craftengine.bedrock.geyser.proxy.inventory.OpenScreenPacketProxy;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.AdventureComponentProxy;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.AdventureKeyProxy;
import net.momirealms.craftengine.bedrock.geyser.util.GeyserTextParts;
import net.momirealms.craftengine.bedrock.text.ContainerTitleCodec;
import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;
import org.geysermc.floodgate.util.UiProfile;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket;

import java.util.ArrayList;
import java.util.List;

public final class GeyserGuiMenus implements GeyserJavaPackets.Interceptor<ClientboundOpenScreenPacket> {
    private static final String DEFAULT_FONT = "minecraft:default";
    private static final int DEFAULT_TITLE_COLOR = 0x404040;
    // 标记字形已完成映射，避免后续序列化再次转换
    private static final Object RESOLVED_FONT = AdventureKeyProxy.INSTANCE.key(GeyserGlyphSerializer.RESOLVED_FONT);
    private final ContainerTitleLayout layout;

    public GeyserGuiMenus(ContainerTitleLayout layout) {
        this.layout = layout;
    }

    @Override
    public void translate(GeyserPlayer player, ClientboundOpenScreenPacket packet, PacketTranslator<ClientboundOpenScreenPacket> original) {
        // 资源包的容器布局仅适配经典 UI，Pocket 布局沿用原生处理。
        if (player.uiProfile() == UiProfile.CLASSIC) {
            packet = this.rewrite(packet, player.locale());
        }
        original.translate(player.session(), packet);
    }

    public ClientboundOpenScreenPacket rewrite(ClientboundOpenScreenPacket packet, String locale) {
        int rows = switch (packet.getType()) {
            case GENERIC_9X1 -> 1;
            case GENERIC_9X2 -> 2;
            case GENERIC_9X3 -> 3;
            case GENERIC_9X4 -> 4;
            case GENERIC_9X5 -> 5;
            case GENERIC_9X6 -> 6;
            default -> 0;
        };
        if (rows == 0) {
            return packet;
        }
        List<GuiTitlePart> parts = new ObjectArrayList<>();
        Object originalTitle = OpenScreenPacketProxy.INSTANCE.title(packet);
        // 按组件顺序展开文本，并将继承后的字体、颜色和装饰保存到片段中
        if (!GeyserTextParts.collectParts(originalTitle, DEFAULT_FONT, DEFAULT_TITLE_COLOR, 0, parts, locale)) {
            return packet;
        }
        try {
            Object title = this.createTitle(parts, rows, locale);
            return title == null ? packet : OpenScreenPacketProxy.INSTANCE.withTitle(packet, title);
        } catch (Throwable t) {
            if (GeyserConfig.debug()) {
                CraftEngineGeyserExtension.instance().logger().error("Failed to rewrite container title packet", t);
            }
            return packet;
        }
    }

    private Object createTitle(List<GuiTitlePart> parts, int rows, String locale) {
        List<List<ContainerTitleLayout.Run>> layers = this.layout.layout(parts);
        // 空列表表示无需使用自定义布局，并不表示应清空标题
        if (layers.isEmpty()) {
            return null;
        }
        List<String> layerTexts = new ArrayList<>(layers.size());
        for (List<ContainerTitleLayout.Run> layer : layers) {
            layerTexts.add(GeyserTextParts.serializeLayer(layer, parts, locale));
        }
        // 将各图层及其切片信息写入标题，由资源包 UI 拆分并叠加显示
        String title = ContainerTitleCodec.encode(rows, layerTexts);
        return AdventureComponentProxy.INSTANCE.font(AdventureComponentProxy.INSTANCE.text(title), RESOLVED_FONT);
    }
}
