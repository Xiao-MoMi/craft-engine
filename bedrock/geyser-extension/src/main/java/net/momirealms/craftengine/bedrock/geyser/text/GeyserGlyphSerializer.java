package net.momirealms.craftengine.bedrock.geyser.text;

import net.momirealms.craftengine.bedrock.geyser.proxy.text.AdventureComponentProxy;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.AdventureTextComponentProxy;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.LegacyComponentSerializerProxy;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.translator.text.MessageTranslator;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class GeyserGlyphSerializer implements InvocationHandler {
    public static final String RESOLVED_FONT = "craftengine:resolved_glyphs";
    private static final StackWalker CALLERS = StackWalker.getInstance();
    private final Object delegate;
    private final Supplier<Map<String, Map<Integer, Integer>>> fonts;

    public GeyserGlyphSerializer(Object delegate, Map<String, Map<Integer, Integer>> fonts) {
        this(delegate, () -> fonts);
    }

    public GeyserGlyphSerializer(Object delegate, Supplier<Map<String, Map<Integer, Integer>>> fonts) {
        this.delegate = delegate;
        this.fonts = fonts;
    }

    public Object wrap() {
        Class<?> type = LegacyComponentSerializerProxy.CLASS;
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] arguments) {
        LegacyComponentSerializerProxy serializer = LegacyComponentSerializerProxy.INSTANCE;
        return switch (method.getName()) {
            case "serialize" -> serializer.serialize(this.delegate, this.rewrite(arguments[0], "minecraft:default"));
            case "deserialize" -> serializer.deserialize(this.delegate, (String) arguments[0]);
            case "toBuilder" -> serializer.toBuilder(this.delegate);
            case "toString" -> "CraftEngine glyph serializer";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == arguments[0];
            default -> throw new UnsupportedOperationException("Unsupported legacy serializer method " + method.getName());
        };
    }

    public Object rewrite(Object source, String inheritedFont) {
        AdventureComponentProxy component = AdventureComponentProxy.INSTANCE;
        Object ownFont = component.font(source);
        String font = ownFont == null ? inheritedFont : ownFont.toString();
        if (RESOLVED_FONT.equals(font)) {
            return source;
        }
        Object result = source;
        Map<Integer, Integer> glyphs = this.fonts.get().get(font);
        if (glyphs != null && AdventureTextComponentProxy.CLASS.isInstance(source)) {
            AdventureTextComponentProxy text = AdventureTextComponentProxy.INSTANCE;
            String content = text.content(source);
            String rewritten = rewriteText(content, glyphs);
            if (rewritten != content) {
                result = text.content(source, rewritten);
            }
        }
        // Geyser already expanded translations, so their text/arguments now retain ordinary font inheritance.
        List<Object> children = component.children(source);
        List<Object> rewrittenChildren = null;
        for (int i = 0; i < children.size(); i++) {
            Object child = children.get(i);
            Object rewritten = this.rewrite(child, font);
            if (child != rewritten && rewrittenChildren == null) {
                rewrittenChildren = new ArrayList<>(children);
            }
            if (rewrittenChildren != null) {
                rewrittenChildren.set(i, rewritten);
            }
        }
        if (rewrittenChildren != null) {
            result = component.children(result, rewrittenChildren);
        }
        return result;
    }

    private static String rewriteText(String content, Map<Integer, Integer> glyphs) {
        StringBuilder rewritten = null;
        Boolean lore = null;
        for (int offset = 0; offset < content.length();) {
            int codepoint = content.codePointAt(offset);
            Integer replacement = glyphs.get(codepoint);
            int target = replacement == null ? codepoint : replacement;
            if (target < 0) {
                if (lore == null) {
                    lore = isItemLore();
                }
                if (!lore) {
                    target = codepoint;
                }
            }
            if (target != codepoint && rewritten == null) {
                rewritten = new StringBuilder(content.length()).append(content, 0, offset);
            }
            if (rewritten != null && target >= 0) {
                rewritten.appendCodePoint(target);
            }
            offset += Character.charCount(codepoint);
        }
        return rewritten == null ? content : rewritten.toString();
    }

    private static boolean isItemLore() {
        // Geyser 的共享序列化器不传文本用途。仅遇到负间距时，识别直接调用 convertMessage 的 Lore 转换入口。
        String translator = MessageTranslator.class.getName();
        return CALLERS.walk(frames -> frames.dropWhile(frame -> !frame.getClassName().equals(translator))
                .dropWhile(frame -> frame.getClassName().equals(translator))
                .findFirst()
                .map(frame -> frame.getClassName().equals(Item.class.getName()) && frame.getMethodName().equals("translateComponentsToBedrock"))
                .orElse(false));
    }
}
