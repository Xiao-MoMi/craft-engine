package net.momirealms.craftengine.core.plugin.minimessage;

import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class IndexedArgumentTag implements TagResolver {
    private static final String NAME_0 = "argument";
    private static final String NAME_1 = "arg";

    private final List<? extends ComponentLike> argumentComponents;

    public IndexedArgumentTag(@NotNull List<? extends ComponentLike> argumentComponents) {
        this.argumentComponents = Objects.requireNonNull(argumentComponents, "argumentComponents");
    }

    @Override
    public @Nullable Tag resolve(@NotNull String name, @NotNull ArgumentQueue arguments, @NotNull Context ctx) throws ParsingException {
        if (!has(name)) {
            return null;
        }

        final int index = arguments.popOr("No argument number provided").asInt().orElseThrow(() -> ctx.newException("Invalid argument number", arguments));

        if (index < 0 || index >= argumentComponents.size()) {
            throw ctx.newException("Invalid argument number", arguments);
        }

        return Tag.inserting(argumentComponents.get(index));
    }

    @Override
    public boolean has(@NotNull String name) {
        return name.equals(NAME_0) || name.equals(NAME_1);
    }
}