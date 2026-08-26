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
        String keyString = arguments.popOr("No argument key provided").toString();
        if (isAttributeNamespacePrefix(keyString) && arguments.hasNext()) {
            keyString += ":" + arguments.pop();
        }
        ContextKey<?> key = ContextKey.chain(keyString);
        Object value = parameter(context, key).orElse(null);
        if (value == null) {
            value = arguments.popOr("No default value provided").toString();
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
        final ParameterArgument parameter = parameterArgument(args);
        final ContextKey<?> key = ContextKey.chain(parameter.key());
        final net.momirealms.craftengine.core.plugin.context.text.StringTemplate defaultTemplate =
                args.length > parameter.consumedArguments()
                        ? net.momirealms.craftengine.core.plugin.context.text.StringTemplate.of(args[parameter.consumedArguments()])
                        : null;
        return (boundArgs, context) -> {
            Object value = parameter(context, key).orElse(null);
            if (value == null) {
                if (defaultTemplate == null) {
                    throw new IllegalArgumentException("No default value provided");
                }
                return defaultTemplate.render(context);
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
        ParameterArgument parameter = parameterArgument(args);
        Object value = parameter(context, ContextKey.chain(parameter.key())).orElse(null);
        if (value == null) {
            value = StringTag.requireArg(args, parameter.consumedArguments(), "No default value provided");
        }
        return value;
    }

    private static ParameterArgument parameterArgument(String[] args) {
        String key = StringTag.requireArg(args, 0, "No argument key provided");
        if (isAttributeNamespacePrefix(key) && args.length > 1) {
            return new ParameterArgument(key + ":" + args[1], 2);
        }
        return new ParameterArgument(key, 1);
    }

    private static boolean isAttributeNamespacePrefix(String key) {
        int namespaceSeparator = key.lastIndexOf('.');
        if (namespaceSeparator < 0 || key.indexOf(':', namespaceSeparator + 1) >= 0) {
            return false;
        }
        int attributeSeparator = key.lastIndexOf('.', namespaceSeparator - 1);
        return attributeSeparator >= 0
                && key.regionMatches(attributeSeparator + 1, "attr.", 0, 5);
    }

    private record ParameterArgument(String key, int consumedArguments) {
    }

    protected Optional<?> parameter(net.momirealms.craftengine.core.plugin.context.Context context, ContextKey<?> key) {
        return context.getOptionalParameter(key);
    }
}
