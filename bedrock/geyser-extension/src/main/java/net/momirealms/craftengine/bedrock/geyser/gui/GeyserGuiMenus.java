package net.momirealms.craftengine.bedrock.geyser.gui;

import net.momirealms.craftengine.bedrock.GuiTitlePart;
import net.momirealms.craftengine.bedrock.geyser.proxy.inventory.OpenScreenPacketProxy;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.*;
import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;
import org.geysermc.floodgate.util.UiProfile;
import org.geysermc.geyser.api.extension.ExtensionLogger;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class GeyserGuiMenus extends PacketTranslator<ClientboundOpenScreenPacket> {
    private final PacketTranslator<ClientboundOpenScreenPacket> delegate;
    private final GeyserContainerTitle titles;
    private final ExtensionLogger logger;
    private final Set<String> reported = ConcurrentHashMap.newKeySet();

    public GeyserGuiMenus(PacketTranslator<ClientboundOpenScreenPacket> delegate, ContainerTitleLayout layout, ExtensionLogger logger) {
        this.delegate = delegate;
        this.titles = new GeyserContainerTitle(layout);
        this.logger = logger;
    }

    @SuppressWarnings("unchecked")
    public static void install(ExtensionLogger logger, ContainerTitleLayout layout) {
        if (layout == null) {
            return;
        }
        PacketTranslator<ClientboundOpenScreenPacket> original = (PacketTranslator<ClientboundOpenScreenPacket>)
                Registries.JAVA_PACKET_TRANSLATORS.get(ClientboundOpenScreenPacket.class);
        Registries.JAVA_PACKET_TRANSLATORS.register(ClientboundOpenScreenPacket.class, new GeyserGuiMenus(original, layout, logger));
        logger.info("Enabled unified classic container title layout.");
    }

    @Override
    public void translate(GeyserSession session, ClientboundOpenScreenPacket packet) {
        packet = this.rewrite(packet, session.getClientData().getUiProfile() == UiProfile.CLASSIC, session.locale());
        this.delegate.translate(session, packet);
    }

    public ClientboundOpenScreenPacket rewrite(ClientboundOpenScreenPacket packet, boolean classic) {
        return this.rewrite(packet, classic, "en_us");
    }

    public ClientboundOpenScreenPacket rewrite(ClientboundOpenScreenPacket packet, boolean classic, String locale) {
        int rows = switch (packet.getType()) {
            case GENERIC_9X1 -> 1;
            case GENERIC_9X2 -> 2;
            case GENERIC_9X3 -> 3;
            case GENERIC_9X4 -> 4;
            case GENERIC_9X5 -> 5;
            case GENERIC_9X6 -> 6;
            default -> 0;
        };
        if (!classic || rows == 0) {
            return packet;
        }
        List<GuiTitlePart> parts = new ArrayList<>();
        Object original = OpenScreenPacketProxy.INSTANCE.title(packet);
        if (!this.collect(original, "minecraft:default", 0x404040, 0, parts, locale)) {
            return packet;
        }
        try {
            Object title = this.titles.create(parts, rows, locale);
            return title == null ? packet : OpenScreenPacketProxy.INSTANCE.withTitle(packet, title);
        } catch (UnsupportedOperationException exception) {
            if (this.logger != null && this.reported.add(exception.getMessage())) {
                this.logger.warning("Container title kept its native layout: " + exception.getMessage());
            }
            return packet;
        }
    }

    private boolean collect(Object component, String inheritedFont, int inheritedColor, int inheritedDecorations, List<GuiTitlePart> parts, String locale) {
        AdventureComponentProxy proxy = AdventureComponentProxy.INSTANCE;
        Object ownFont = proxy.font(component);
        String font = ownFont == null ? inheritedFont : ownFont.toString();
        Object style = proxy.style(component);
        Object ownColor = AdventureStyleProxy.INSTANCE.color(style);
        int color = ownColor == null ? inheritedColor : AdventureTextColorProxy.INSTANCE.value(ownColor);
        int decorations = GuiTitlePart.decorations(inheritedDecorations, AdventureStyleProxy.INSTANCE.decorations(style));
        if (AdventureTextComponentProxy.CLASS.isInstance(component)) {
            GuiTitlePart.append(parts, font, AdventureTextComponentProxy.INSTANCE.content(component), color, decorations);
        } else if (AdventureTranslatableComponentProxy.CLASS.isInstance(component)) {
            if (!AdventureTranslatableComponentProxy.INSTANCE.arguments(component).isEmpty()) {
                return false;
            }
            // Translate this node only; children must be checked separately for parameterized translations.
            Object translated = AdventureTranslationRendererProxy.INSTANCE.render(MessageTranslatorProxy.INSTANCE.renderer(),
                    proxy.children(component, List.of()), locale);
            if (!AdventureTextComponentProxy.CLASS.isInstance(translated)
                    || !this.collect(translated, font, color, decorations, parts, locale)) {
                return false;
            }
        } else {
            return false;
        }
        for (Object child : proxy.children(component)) {
            if (!this.collect(child, font, color, decorations, parts, locale)) {
                return false;
            }
        }
        return true;
    }
}
