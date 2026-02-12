package net.momirealms.craftengine.core.plugin.text.minimessage;

import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.ContextKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public class NamedArgumentTag implements TagResolver {
    private final Context context;

    public NamedArgumentTag(@NotNull Context context) {
        this.context = Objects.requireNonNull(context, "context holder");
    }

    @Override
    public @Nullable Tag resolve(@NotNull String name, @NotNull ArgumentQueue arguments, @NotNull net.kyori.adventure.text.minimessage.Context ctx) throws ParsingException {
        if (!has(name)) {
            return null;
        }
        ContextKey<?> key = ContextKey.chain(arguments.popOr("No argument key provided").toString());
        Optional<?> optional = this.context.getOptionalParameter(key);
        Object value = optional.orElse(null);
        if (value == null) {
            value = arguments.popOr("No default value provided").toString();
        }
        return Tag.selfClosingInserting(ctx.deserialize(String.valueOf(value)));
    }

    @Override
    public boolean has(@NotNull String name) {
        return name.equals("arg");
    }
}