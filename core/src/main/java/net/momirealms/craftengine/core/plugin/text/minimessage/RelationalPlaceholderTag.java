package net.momirealms.craftengine.core.plugin.text.minimessage;

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

public class RelationalPlaceholderTag implements TagResolver {
    private final Player player1;
    private final Player player2;

    public RelationalPlaceholderTag(@NotNull Player player1, @NotNull Player player2) {
        this.player1 = player1;
        this.player2 = player2;
    }

    @Override
    public @Nullable Tag resolve(@NotNull String name, @NotNull ArgumentQueue arguments, @NotNull Context ctx) throws ParsingException {
        if (!this.has(name) || !CraftEngine.instance().compatibilityManager().hasPlaceholderAPI()) {
            return null;
        }
        String placeholder = "%" + arguments.popOr("No argument placeholder provided") + "%";
        String parsed = CraftEngine.instance().compatibilityManager().parse(player1, player2, placeholder);
        if (parsed.equals(placeholder)) {
            parsed = arguments.popOr("No default papi value provided").toString();
        }
        return Tag.selfClosingInserting(AdventureHelper.miniMessage().deserialize(parsed));
    }

    @Override
    public boolean has(@NotNull String name) {
        return "rel_papi".equals(name);
    }
}
