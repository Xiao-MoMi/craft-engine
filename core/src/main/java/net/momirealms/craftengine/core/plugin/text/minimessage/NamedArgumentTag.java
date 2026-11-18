package net.momirealms.craftengine.core.plugin.text.minimessage;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.plugin.context.ContextKey;
import net.momirealms.craftengine.core.plugin.context.text.StringTag;
import net.momirealms.craftengine.core.plugin.context.text.StringTemplates;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.sparrow.message.ParsingException;
import net.momirealms.sparrow.message.tag.Tag;
import net.momirealms.sparrow.message.tag.resolver.ArgumentQueue;
import net.momirealms.sparrow.message.tag.resolver.StaticTagResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class NamedArgumentTag extends StaticTagResolver implements StringTag {
    public static final NamedArgumentTag INSTANCE = new NamedArgumentTag("arg");

    protected NamedArgumentTag(String name) {
        super(name);
    }

    @Override
    public @Nullable Tag resolve(@NotNull String name, @NotNull ArgumentQueue arguments, @NotNull net.momirealms.sparrow.message.Context ctx) throws ParsingException {
        if (!(ctx.target() instanceof net.momirealms.craftengine.core.plugin.context.Context context)) {
            return null;
        }
        ContextKey<?> key = ContextKey.chain(arguments.popOr("No argument key provided").toString());
        Object value = parameter(context, key).orElse(null);
        if (value == null) {
            if (!arguments.hasNext()) {
                return Tag.selfClosingInserting(Component.empty());
            }
            value = arguments.pop().toString();
        }
        if (value instanceof Component component) {
            return Tag.selfClosingInserting(component);
        }
        return Tag.selfClosingInserting(ctx.deserialize(String.valueOf(value)));
    }

    @Override
    public @Nullable String resolve(String[] args, net.momirealms.craftengine.core.plugin.context.Context context) {
        Object value = resolveValue(args, context);
        if (value instanceof Component component) {
            return AdventureHelper.plainTextContent(component);
        }
        return StringTemplates.render(String.valueOf(value), context);
    }

    @Override
    public StringTag precompile(String[] args) {
        // bind the parameter key and precompile the default value once
        final ContextKey<?> key = ContextKey.chain(StringTag.requireArg(args, 0, "No argument key provided"));
        final net.momirealms.craftengine.core.plugin.context.text.StringTemplate defaultTemplate =
                args.length > 1
                        ? net.momirealms.craftengine.core.plugin.context.text.StringTemplate.of(args[1])
                        : null;
        return (boundArgs, context) -> {
            Object value = parameter(context, key).orElse(null);
            if (value == null) {
                return defaultTemplate == null ? "" : defaultTemplate.render(context);
            }
            if (value instanceof Component component) {
                return AdventureHelper.plainTextContent(component);
            }
            if (value instanceof Number || value instanceof Boolean) {
                return value;
            }
            return StringTemplates.render(String.valueOf(value), context);
        };
    }

    protected Object resolveValue(String[] args, net.momirealms.craftengine.core.plugin.context.Context context) {
        ContextKey<?> key = ContextKey.chain(StringTag.requireArg(args, 0, "No argument key provided"));
        Object value = parameter(context, key).orElse(null);
        if (value == null) {
            value = args.length > 1 ? args[1] : "";
        }
        return value;
    }

    protected Optional<?> parameter(net.momirealms.craftengine.core.plugin.context.Context context, ContextKey<?> key) {
        return context.getOptionalParameter(key);
    }
}
