package net.momirealms.craftengine.core.plugin.text.minimessage;

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

public final class IndexedArgumentTag implements TagResolver {
    private final List<? extends ComponentLike> args;

    public IndexedArgumentTag(@NotNull List<? extends ComponentLike> args) {
        this.args = Objects.requireNonNull(args, "argumentComponents");
    }

    @Override
    public @Nullable Tag resolve(@NotNull String name, @NotNull ArgumentQueue arguments, @NotNull Context ctx) throws ParsingException {
        if (!has(name)) {
            return null;
        }
        final int index = arguments.popOr("No argument number provided").asInt().orElseThrow(() -> ctx.newException("Invalid argument number", arguments));
        if (index < 0 || index >= this.args.size()) {
            throw ctx.newException("Invalid argument number", arguments);
        }
        return Tag.selfClosingInserting(this.args.get(index));
    }

    @Override
    public boolean has(@NotNull String name) {
        return name.equals("arg");
    }
}