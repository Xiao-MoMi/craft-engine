package net.momirealms.craftengine.bedrock.geyser.text;

import net.momirealms.craftengine.bedrock.geyser.proxy.text.AdventureComponentProxy;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.AdventureTextComponentProxy;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.LegacyComponentSerializerProxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class GeyserGlyphSerializer implements InvocationHandler {
    public static final String RESOLVED_FONT = "craftengine:resolved_glyphs";
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
            StringBuilder rewritten = null;
            for (int offset = 0; offset < content.length();) {
                int codepoint = content.codePointAt(offset);
                Integer replacement = glyphs.get(codepoint);
                if (replacement != null && rewritten == null) {
                    rewritten = new StringBuilder(content.length()).append(content, 0, offset);
                }
                if (rewritten != null) {
                    rewritten.appendCodePoint(replacement == null ? codepoint : replacement);
                }
                offset += Character.charCount(codepoint);
            }
            if (rewritten != null) {
                result = text.content(source, rewritten.toString());
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
}
