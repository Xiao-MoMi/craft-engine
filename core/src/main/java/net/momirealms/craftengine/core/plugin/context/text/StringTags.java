package net.momirealms.craftengine.core.plugin.context.text;

import net.momirealms.craftengine.core.plugin.text.minimessage.*;
import net.momirealms.sparrow.message.tag.resolver.StaticTagResolver;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class StringTags {
    private static final Map<String, StringTag> TAGS = new HashMap<>();

    static {
        register(NamedArgumentTag.INSTANCE);
        register(ViewerNamedArgumentTag.INSTANCE);
        register(PlaceholderTag.INSTANCE);
        register(ViewerPlaceholderTag.INSTANCE);
        register(RelationalPlaceholderTag.INSTANCE);
        register(GlobalVariableTag.INSTANCE);
        register(VariableTag.INSTANCE);
        register(RandomTag.INSTANCE);
        register(ExpressionTag.INSTANCE);
        register(AttributeValueTag.ATTACKER);
        register(AttributeValueTag.VICTIM);
    }

    private StringTags() {
    }

    public static <T extends StaticTagResolver & StringTag> void register(T tag) {
        tag.contributeKnownNames((name, resolver) -> TAGS.put(name, tag));
    }

    public static void register(String name, StringTag tag) {
        TAGS.put(name, tag);
    }

    public static boolean has(String name) {
        return TAGS.containsKey(name);
    }

    @Nullable
    public static StringTag get(String name) {
        return TAGS.get(name);
    }
}
