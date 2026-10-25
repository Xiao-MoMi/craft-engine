package net.momirealms.craftengine.bedrock.geyser.gui;

import net.momirealms.craftengine.bedrock.GuiTitlePart;
import net.momirealms.craftengine.bedrock.text.ContainerTitleCodec;
import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.AdventureComponentProxy;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.AdventureTextColorProxy;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.AdventureTextDecorationProxy;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.MessageTranslatorProxy;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.AdventureKeyProxy;
import net.momirealms.craftengine.bedrock.geyser.text.GeyserGlyphSerializer;

import java.util.ArrayList;
import java.util.List;

public final class GeyserContainerTitle {
    private static final Object RESOLVED_FONT = AdventureKeyProxy.INSTANCE.key(GeyserGlyphSerializer.RESOLVED_FONT);
    private final ContainerTitleLayout layout;

    public GeyserContainerTitle(ContainerTitleLayout layout) {
        this.layout = layout;
    }

    public Object create(List<GuiTitlePart> parts, int rows, String locale) {
        List<List<ContainerTitleLayout.Run>> layers = this.layout.layout(parts);
        if (layers.isEmpty()) {
            return null;
        }
        List<String> texts = new ArrayList<>(layers.size());
        for (List<ContainerTitleLayout.Run> layer : layers) {
            StringBuilder text = new StringBuilder();
            for (ContainerTitleLayout.Run run : layer) {
                if (run.part() < 0) {
                    text.append("§r").append(run.text());
                } else {
                    text.append(this.serialize(parts.get(run.part()), run.text(), locale));
                }
            }
            texts.add(text.toString());
        }
        String title = ContainerTitleCodec.encode(rows, texts);
        return AdventureComponentProxy.INSTANCE.font(AdventureComponentProxy.INSTANCE.text(title), RESOLVED_FONT);
    }

    private String serialize(GuiTitlePart part, String text, String locale) {
        AdventureComponentProxy proxy = AdventureComponentProxy.INSTANCE;
        Object component = proxy.color(proxy.text(text), AdventureTextColorProxy.INSTANCE.color(part.color()));
        component = proxy.font(component, RESOLVED_FONT);
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
}
