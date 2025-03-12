package net.momirealms.craftengine.core.plugin.minimessage;

import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.AdventureHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlaceholderTag implements TagResolver {
    private final Player player;

    public PlaceholderTag(@Nullable Player player) {
        this.player = player;
    }

    @Override
    public @Nullable Tag resolve(@NotNull String name, @NotNull ArgumentQueue arguments, @NotNull Context ctx) throws ParsingException {
        if (!this.has(name) || !CraftEngine.instance().hasPlaceholderAPI()) {
            return null;
        }
        String placeholder = arguments.popOr("No argument placeholder provided").toString();
        String parsed = CraftEngine.instance().parse(player, "%" + placeholder + "%");
        return Tag.inserting(AdventureHelper.miniMessage(parsed));
    }

    @Override
    public boolean has(@NotNull String name) {
        return "papi".equals(name);
    }
}
