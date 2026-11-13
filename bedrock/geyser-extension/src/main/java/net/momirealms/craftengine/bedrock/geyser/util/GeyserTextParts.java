package net.momirealms.craftengine.bedrock.geyser.util;

import net.momirealms.craftengine.bedrock.GuiTitlePart;
import net.momirealms.craftengine.bedrock.geyser.glyph.GeyserGlyphSerializer;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.*;
import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;

import java.util.List;

public final class GeyserTextParts {
    private static final Object RESOLVED_FONT = AdventureKeyProxy.INSTANCE.key(GeyserGlyphSerializer.RESOLVED_FONT);

    private GeyserTextParts() {
    }

    public static String serializeLayer(List<ContainerTitleLayout.Run> layer, List<GuiTitlePart> parts, String locale) {
        StringBuilder text = new StringBuilder();
        for (ContainerTitleLayout.Run run : layer) {
            if (run.part() < 0) {
                // 间距字符不继承文本装饰，尤其不能受粗体的额外宽度影响
                text.append("§r").append(run.text());
            } else {
                text.append(serializePart(parts.get(run.part()), run.text(), locale));
            }
        }
        return text.toString();
    }

    public static String serializePart(GuiTitlePart part, String text, String locale) {
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

    public static boolean collectParts(Object component,
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
            if (!AdventureTextComponentProxy.CLASS.isInstance(translated) || !collectParts(translated, font, color, decorations, parts, locale)) {
                return false;
            }
        } else {
            return false;
        }
        // 保持父文本在前、子文本在后的顺序；任一节点不支持就放弃整个标题的转换
        List<?> children = proxy.children(component);
        for (int i = 0, size = children.size(); i < size; i++) {
            Object child = children.get(i);
            if (!collectParts(child, font, color, decorations, parts, locale)) {
                return false;
            }
        }
        return true;
    }
}
