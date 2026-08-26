package net.momirealms.craftengine.core.plugin.text.minimessage;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.text.StringTag;
import net.momirealms.craftengine.core.plugin.context.text.StringTemplates;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.sparrow.message.ParsingException;
import net.momirealms.sparrow.message.tag.Tag;
import net.momirealms.sparrow.message.tag.resolver.ArgumentQueue;
import net.momirealms.sparrow.message.tag.resolver.StaticTagResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class VariableTag extends StaticTagResolver implements StringTag {
    public static final VariableTag INSTANCE = new VariableTag();

    private VariableTag() {
        super("var");
    }

    @Override
    public @Nullable Tag resolve(@NotNull String name,
                                 @NotNull ArgumentQueue arguments,
                                 @NotNull net.momirealms.sparrow.message.Context ctx) throws ParsingException {
        if (!(ctx.target() instanceof Context context)) {
            return null;
        }
        String key = arguments.popOr("No variable name provided").toString();
        Object value = context.getVariable(key);
        if (value == null) {
            throw ctx.newException("Unknown variable: " + key, arguments);
        }
        if (value instanceof Component component) {
            return Tag.selfClosingInserting(component);
        }
        return Tag.selfClosingInserting(ctx.deserialize(String.valueOf(value)));
    }

    @Override
    public Object resolve(String[] args, Context context) {
        return resolveValue(StringTag.requireArg(args, 0, "No variable name provided"), context);
    }

    @Override
    public StringTag precompile(String[] args) {
        String key = StringTag.requireArg(args, 0, "No variable name provided");
        return (boundArgs, context) -> resolveValue(key, context);
    }

    private static Object resolveValue(String key, Context context) {
        Object value = context.getVariable(key);
        if (value == null) {
            throw new IllegalArgumentException("Unknown variable: " + key);
        }
        if (value instanceof Component component) {
            return AdventureHelper.plainTextContent(component);
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }
        return StringTemplates.render(String.valueOf(value), context);
    }
}
