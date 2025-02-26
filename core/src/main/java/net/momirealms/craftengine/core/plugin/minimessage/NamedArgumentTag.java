package net.momirealms.craftengine.core.plugin.minimessage;

import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.context.ContextHolder;
import net.momirealms.craftengine.core.util.context.ContextKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class NamedArgumentTag implements TagResolver {
    private static final String NAME_0 = "argument";
    private static final String NAME_1 = "arg";

    private final ContextHolder contextHolder;

    public NamedArgumentTag(@NotNull ContextHolder contextHolder) {
        this.contextHolder = Objects.requireNonNull(contextHolder, "context holder");
    }

    @Override
    public @Nullable Tag resolve(@NotNull String name, @NotNull ArgumentQueue arguments, @NotNull Context ctx) throws ParsingException {
        if (!has(name)) {
            return null;
        }

        String argumentKey = arguments.popOr("No argument key provided").toString();
        ContextKey<String> key = ContextKey.of(Key.of(argumentKey));
        if (!this.contextHolder.has(key)) {
            throw ctx.newException("Invalid argument key", arguments);
        }

        return Tag.inserting(AdventureHelper.miniMessage().deserialize(this.contextHolder.getOrThrow(key), ShiftTag.INSTANCE, ImageTag.INSTANCE));
    }

    @Override
    public boolean has(@NotNull String name) {
        return name.equals(NAME_0) || name.equals(NAME_1);
    }
}