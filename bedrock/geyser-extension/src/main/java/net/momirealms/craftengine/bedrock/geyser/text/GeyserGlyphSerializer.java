package net.momirealms.craftengine.bedrock.geyser.text;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.modifier.FieldManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.matcher.ElementMatchers;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.AdventureComponentProxy;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.AdventureTextComponentProxy;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.LegacyComponentSerializerProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.constructor.SConstructor2;
import net.momirealms.sparrow.reflection.constructor.matcher.ConstructorMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GeyserGlyphSerializer {
    public static final String RESOLVED_FONT = "craftengine:resolved_glyphs";
    private static final SConstructor2 WRAPPER_CONSTRUCTOR = createConstructor();
    private final Object delegate;
    private final Map<String, Int2IntMap> fonts;

    public GeyserGlyphSerializer(Object delegate, Map<String, Int2IntMap> fonts) {
        this.delegate = delegate;
        this.fonts = fonts;
    }

    public Object wrap() {
        return WRAPPER_CONSTRUCTOR.newInstance(this.delegate, this);
    }

    private static SConstructor2 createConstructor() {
        Class<?> wrapper = new ByteBuddy(ClassFileVersion.JAVA_V21)
                .subclass(Object.class)
                .implement(LegacyComponentSerializerProxy.CLASS)
                .name(GeyserGlyphSerializer.class.getName() + "$Generated")
                .defineField("delegate", LegacyComponentSerializerProxy.CLASS, Visibility.PRIVATE, FieldManifestation.FINAL)
                .defineField("glyphSerializer", GeyserGlyphSerializer.class, Visibility.PRIVATE, FieldManifestation.FINAL)
                .defineConstructor(Visibility.PUBLIC)
                .withParameters(LegacyComponentSerializerProxy.CLASS, GeyserGlyphSerializer.class)
                .intercept(MethodCall.invoke(SparrowClass.of(Object.class).getDeclaredConstructor(ConstructorMatcher.takeArguments(new Class<?>[0])))
                        .andThen(FieldAccessor.ofField("delegate").setsArgumentAt(0))
                        .andThen(FieldAccessor.ofField("glyphSerializer").setsArgumentAt(1)))
                .method(ElementMatchers.isAbstract())
                .intercept(MethodDelegation.toField("delegate"))
                .method(ElementMatchers.named("serialize").and(ElementMatchers.takesArguments(1)))
                .intercept(MethodDelegation.withDefaultConfiguration().filter(ElementMatchers.named("serialize")).toField("glyphSerializer"))
                .method(ElementMatchers.isToString())
                .intercept(FixedValue.value("CraftEngine glyph serializer"))
                .make()
                .load(GeyserGlyphSerializer.class.getClassLoader())
                .getLoaded();
        return SparrowClass.of(wrapper)
                .getSparrowConstructor(ConstructorMatcher.takeArguments(LegacyComponentSerializerProxy.CLASS, GeyserGlyphSerializer.class))
                .asm$2();
    }

    public String serialize(@Argument(0) Object source) {
        return LegacyComponentSerializerProxy.INSTANCE.serialize(this.delegate, this.rewrite(source, "minecraft:default"));
    }

    public Object rewrite(Object source, String inheritedFont) {
        AdventureComponentProxy component = AdventureComponentProxy.INSTANCE;
        Object ownFont = component.font(source);
        String font = ownFont == null ? inheritedFont : ownFont.toString();
        if (RESOLVED_FONT.equals(font)) {
            return source;
        }
        Object result = source;
        Int2IntMap glyphs = this.fonts.get(font);
        if (glyphs != null && AdventureTextComponentProxy.CLASS.isInstance(source)) {
            String content = AdventureTextComponentProxy.INSTANCE.content(source);
            String rewritten = rewriteText(content, glyphs);
            if (rewritten != null) {
                result = AdventureTextComponentProxy.INSTANCE.content(source, rewritten);
            }
        }
        // Geyser already expanded translations, so their text/arguments now retain ordinary font inheritance.
        List<Object> children = component.children(source);
        List<Object> rewrittenChildren = null;
        for (int i = 0; i < children.size(); i++) {
            Object child = children.get(i);
            Object rewritten = this.rewrite(child, font);
            if (child != rewritten) {
                if (rewrittenChildren == null) {
                    rewrittenChildren = new ArrayList<>(children);
                }
                rewrittenChildren.set(i, rewritten);
            }
        }
        if (rewrittenChildren != null) {
            result = component.children(result, rewrittenChildren);
        }
        return result;
    }

    private static String rewriteText(String content, Int2IntMap glyphs) {
        StringBuilder rewritten = null;
        for (int offset = 0; offset < content.length();) {
            int codepoint = content.codePointAt(offset);
            int target = glyphs.getOrDefault(codepoint, codepoint);
            if (target != codepoint && rewritten == null) {
                rewritten = new StringBuilder(content.length()).append(content, 0, offset);
            }
            if (rewritten != null && target >= 0) {
                rewritten.appendCodePoint(target);
            }
            offset += Character.charCount(codepoint);
        }
        return rewritten == null ? null : rewritten.toString();
    }
}
