package net.momirealms.craftengine.bedrock.geyser.util;

import net.momirealms.craftengine.bedrock.GuiTitlePart;
import net.momirealms.craftengine.bedrock.GuiTitlePart.Shadow;
import net.momirealms.craftengine.bedrock.geyser.glyph.GeyserGlyphSerializer;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.*;
import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;

import java.util.ArrayList;
import java.util.List;

public final class GeyserTextParts {
    private static final Object RESOLVED_FONT = AdventureKeyProxy.INSTANCE.key(GeyserGlyphSerializer.RESOLVED_FONT);

    private GeyserTextParts() {
    }

    public static Object resolvedText(String text) {
        AdventureComponentProxy proxy = AdventureComponentProxy.INSTANCE;
        return proxy.font(proxy.text(text), RESOLVED_FONT);
    }

    public static Object nativeContainerTitle(List<GuiTitlePart> parts, ContainerTitleLayout layout) {
        List<Object> children = new ArrayList<>(parts.size());
        for (GuiTitlePart part : parts) {
            String text = layout.nativeTitleText(part);
            if (!text.isEmpty()) {
                children.add(component(part, text, RESOLVED_FONT));
            }
        }
        return AdventureComponentProxy.INSTANCE.children(resolvedText(""), children);
    }

    public static String serializeLayer(List<ContainerTitleLayout.Run> layer, List<GuiTitlePart> parts, String locale) {
        if (layer.size() == 1) {
            ContainerTitleLayout.Run run = layer.getFirst();
            return run.part() < 0 ? "§r" + run.text() : serializePart(parts.get(run.part()), run.text(), locale);
        }
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
        Object component = component(part, text, RESOLVED_FONT);
        return MessageTranslatorProxy.INSTANCE.convertMessage(component, locale);
    }

    public static String serializeParts(List<GuiTitlePart> parts, String locale) {
        if (parts.isEmpty()) {
            return "";
        }
        if (parts.size() == 1) {
            GuiTitlePart part = parts.getFirst();
            return MessageTranslatorProxy.INSTANCE.convertMessage(component(part, part.text(), AdventureKeyProxy.INSTANCE.key(part.font())), locale);
        }
        List<Object> children = new ArrayList<>(parts.size());
        for (GuiTitlePart part : parts) {
            children.add(component(part, part.text(), AdventureKeyProxy.INSTANCE.key(part.font())));
        }
        AdventureComponentProxy proxy = AdventureComponentProxy.INSTANCE;
        return MessageTranslatorProxy.INSTANCE.convertMessage(proxy.children(proxy.text(""), children), locale);
    }

    private static Object component(GuiTitlePart part, String text, Object font) {
        AdventureComponentProxy proxy = AdventureComponentProxy.INSTANCE;
        Object component = proxy.color(proxy.text(text), AdventureTextColorProxy.INSTANCE.color(part.color()));
        component = proxy.font(component, font);
        if (part.decorations() == 0) {
            return component;
        }
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
        return component;
    }

    public static boolean collectParts(Object component,
                                       String inheritedFont,
                                       int inheritedColor,
                                       int inheritedDecorations,
                                       List<GuiTitlePart> parts,
                                       String locale) {
        GuiTitlePart.Builder builder = new GuiTitlePart.Builder(parts);
        boolean complete = collectParts(component, inheritedFont, inheritedColor, inheritedDecorations, Shadow.DEFAULT, builder::appendFormatted, locale, true);
        builder.finish();
        return complete;
    }

    public static boolean collectDisplayParts(Object component, List<GuiTitlePart> parts, String locale) {
        // Java 在每个组件起点恢复其样式。先保留边界，避免 § 格式延续到相邻组件。
        return collectParts(
                component,
                "minecraft:default",
                0xffffff,
                0,
                Shadow.DEFAULT,
                (font, text, color, decorations, shadow) -> parts.add(new GuiTitlePart(font, text, color, decorations, shadow)),
                locale,
                false
        );
    }

    @FunctionalInterface
    private interface PartSink {
        void append(String font, String text, int color, int decorations, Shadow shadow);
    }

    private static boolean collectParts(Object component,
                                        String inheritedFont,
                                        int inheritedColor,
                                        int inheritedDecorations,
                                        Shadow inheritedShadow,
                                        PartSink parts,
                                        String locale, boolean hud) {
        AdventureComponentProxy proxy = AdventureComponentProxy.INSTANCE;
        // 当前组件显式设置的样式覆盖父级；未设置的样式继续继承
        Object ownFont = proxy.font(component);
        String font = ownFont == null ? inheritedFont : ownFont.toString();
        Object style = proxy.style(component);
        Object ownColor = AdventureStyleProxy.INSTANCE.color(style);
        int color = ownColor == null ? inheritedColor : AdventureTextColorProxy.INSTANCE.value(ownColor);
        int decorations = GuiTitlePart.decorations(inheritedDecorations, AdventureStyleProxy.INSTANCE.decorations(style));
        Shadow shadow = inheritedShadow;
        if (hud) {
            Object ownShadow = AdventureStyleProxy.INSTANCE.shadowColor(style);
            if (ownShadow != null) {
                shadow = AdventureShadowColorProxy.INSTANCE.value(ownShadow) == 0 ? Shadow.OFF : Shadow.ON;
            }
        }
        if (AdventureTextComponentProxy.CLASS.isInstance(component)) {
            String content = AdventureTextComponentProxy.INSTANCE.content(component);
            if (!content.isEmpty()) {
                parts.append(font, content, color, decorations, shadow);
            }
        } else if (AdventureTranslatableComponentProxy.CLASS.isInstance(component)) {
            if (!hud && !AdventureTranslatableComponentProxy.INSTANCE.arguments(component).isEmpty()) {
                return false;
            }
            // 只翻译当前节点，子组件留到下方逐个检查，避免绕过带参数翻译的限制
            Object translated = AdventureTranslationRendererProxy.INSTANCE.render(MessageTranslatorProxy.INSTANCE.renderer(), proxy.children(component, List.of()), locale);
            if (!AdventureTextComponentProxy.CLASS.isInstance(translated) || !collectParts(translated, font, color, decorations, shadow, parts, locale, hud)) {
                return false;
            }
        } else if (hud && AdventureScoreComponentProxy.CLASS.isInstance(component)) {
            // 空 score 可以包住整条 HUD。它的子组件继承这里的字体、颜色和阴影
            String value = AdventureScoreComponentProxy.INSTANCE.value(component);
            if (value != null && !value.isEmpty()) {
                return false;
            }
        } else {
            return false;
        }
        // 保持父文本在前、子文本在后的顺序；任一节点不支持就放弃整个标题的转换
        List<?> children = proxy.children(component);
        for (int i = 0, size = children.size(); i < size; i++) {
            Object child = children.get(i);
            if (!collectParts(child, font, color, decorations, shadow, parts, locale, hud)) {
                return false;
            }
        }
        return true;
    }
}
