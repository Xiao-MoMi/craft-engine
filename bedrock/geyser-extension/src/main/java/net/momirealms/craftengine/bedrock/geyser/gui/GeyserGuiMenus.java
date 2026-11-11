package net.momirealms.craftengine.bedrock.geyser.gui;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.bedrock.GuiTitlePart;
import net.momirealms.craftengine.bedrock.geyser.CraftEngineGeyserExtension;
import net.momirealms.craftengine.bedrock.geyser.config.GeyserConfig;
import net.momirealms.craftengine.bedrock.geyser.glyph.GeyserGlyphSerializer;
import net.momirealms.craftengine.bedrock.geyser.locale.GeyserTranslationManager;
import net.momirealms.craftengine.bedrock.geyser.proxy.inventory.OpenScreenPacketProxy;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.*;
import net.momirealms.craftengine.bedrock.text.ContainerTitleCodec;
import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;
import org.geysermc.floodgate.util.UiProfile;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket;

import java.util.ArrayList;
import java.util.List;

public final class GeyserGuiMenus extends PacketTranslator<ClientboundOpenScreenPacket> {
    private static final String DEFAULT_FONT = "minecraft:default";
    private static final int DEFAULT_TITLE_COLOR = 0x404040;
    // 标记字形已完成映射，避免后续序列化再次转换
    private static final Object RESOLVED_FONT = AdventureKeyProxy.INSTANCE.key(GeyserGlyphSerializer.RESOLVED_FONT);
    private final PacketTranslator<ClientboundOpenScreenPacket> delegate;
    private final ContainerTitleLayout layout;

    public GeyserGuiMenus(PacketTranslator<ClientboundOpenScreenPacket> delegate, ContainerTitleLayout layout) {
        this.delegate = delegate;
        this.layout = layout;
    }

    @SuppressWarnings("unchecked")
    public static void install(ContainerTitleLayout layout) {
        PacketTranslator<ClientboundOpenScreenPacket> original = (PacketTranslator<ClientboundOpenScreenPacket>) Registries.JAVA_PACKET_TRANSLATORS.get(ClientboundOpenScreenPacket.class);
        if (original instanceof GeyserGuiMenus menus) {
            original = menus.delegate;
        }
        Registries.JAVA_PACKET_TRANSLATORS.register(ClientboundOpenScreenPacket.class, new GeyserGuiMenus(original, layout));
        GeyserTranslationManager.info("gui.layout_enabled");
    }

    @Override
    public void translate(GeyserSession session, ClientboundOpenScreenPacket packet) {
        // 资源包的容器布局仅适配经典 UI，Pocket 布局沿用原生处理。
        if (session.getClientData().getUiProfile() == UiProfile.CLASSIC && this.layout != null) {
            packet = this.rewrite(packet, session.locale());
        }
        this.delegate.translate(session, packet);
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
        if (!this.collectParts(originalTitle, DEFAULT_FONT, DEFAULT_TITLE_COLOR, 0, parts, locale)) {
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
            layerTexts.add(this.serializeLayer(layer, parts, locale));
        }
        // 将各图层及其切片信息写入标题，由资源包 UI 拆分并叠加显示
        String title = ContainerTitleCodec.encode(rows, layerTexts);
        return AdventureComponentProxy.INSTANCE.font(AdventureComponentProxy.INSTANCE.text(title), RESOLVED_FONT);
    }

    private String serializeLayer(List<ContainerTitleLayout.Run> layer, List<GuiTitlePart> parts, String locale) {
        StringBuilder text = new StringBuilder();
        for (ContainerTitleLayout.Run run : layer) {
            if (run.part() < 0) {
                // 间距字符不继承文本装饰，尤其不能受粗体的额外宽度影响
                text.append("§r").append(run.text());
            } else {
                text.append(this.serializePart(parts.get(run.part()), run.text(), locale));
            }
        }
        return text.toString();
    }

    private String serializePart(GuiTitlePart part, String text, String locale) {
        AdventureComponentProxy proxy = AdventureComponentProxy.INSTANCE;
        Object component = proxy.color(proxy.text(text), AdventureTextColorProxy.INSTANCE.color(part.color()));
        component = proxy.font(component, RESOLVED_FONT);
        // 将片段的装饰位标记还原为 Adventure 样式，再交给 Geyser 序列化
        for (Object decoration : AdventureTextDecorationProxy.VALUES) {
            int flag = switch (((Enum<?>) decoration).name()) {
                case "OBFUSCATED" -> 1;
                case "BOLD" -> 2;
                case "STRIKETHROUGH" -> 4;
                case "UNDERLINED" -> 8;
                case "ITALIC" -> 16;
                default -> 0;
            };
            if ((part.decorations() & flag) != 0) {
                component = proxy.decorate(component, decoration);
            }
        }
        return MessageTranslatorProxy.INSTANCE.convertMessage(component, locale);
    }

    private boolean collectParts(Object component,
                                 String inheritedFont,
                                 int inheritedColor,
                                 int inheritedDecorations,
                                 List<GuiTitlePart> parts,
                                 String locale) {
        AdventureComponentProxy proxy = AdventureComponentProxy.INSTANCE;
        // 当前组件显式设置的样式覆盖父级；未设置的样式继续继承
        Object ownFont = proxy.font(component);
        String font = ownFont == null ? inheritedFont : ownFont.toString();
        Object style = proxy.style(component);
        Object ownColor = AdventureStyleProxy.INSTANCE.color(style);
        int color = ownColor == null ? inheritedColor : AdventureTextColorProxy.INSTANCE.value(ownColor);
        int decorations = GuiTitlePart.decorations(inheritedDecorations, AdventureStyleProxy.INSTANCE.decorations(style));
        if (AdventureTextComponentProxy.CLASS.isInstance(component)) {
            GuiTitlePart.append(parts, font, AdventureTextComponentProxy.INSTANCE.content(component), color, decorations);
        } else if (AdventureTranslatableComponentProxy.CLASS.isInstance(component)) {
            // 暂不展开带参数的翻译组件，避免丢失参数内部的样式和布局信息
            if (!AdventureTranslatableComponentProxy.INSTANCE.arguments(component).isEmpty()) {
                return false;
            }
            // 只翻译当前节点，子组件留到下方逐个检查，避免绕过带参数翻译的限制
            Object translated = AdventureTranslationRendererProxy.INSTANCE.render(MessageTranslatorProxy.INSTANCE.renderer(), proxy.children(component, List.of()), locale);
            if (!AdventureTextComponentProxy.CLASS.isInstance(translated) || !this.collectParts(translated, font, color, decorations, parts, locale)) {
                return false;
            }
        } else {
            return false;
        }
        // 保持父文本在前、子文本在后的顺序；任一节点不支持就放弃整个标题的转换
        List<?> children = proxy.children(component);
        for (int i = 0, size = children.size(); i < size; i++) {
            Object child = children.get(i);
            if (!this.collectParts(child, font, color, decorations, parts, locale)) {
                return false;
            }
        }
        return true;
    }
}
