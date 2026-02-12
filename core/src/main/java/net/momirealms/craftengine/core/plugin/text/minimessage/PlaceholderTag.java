package net.momirealms.craftengine.core.plugin.text.minimessage;

import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.PlayerContext;
import net.momirealms.craftengine.core.util.AdventureHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlaceholderTag implements TagResolver {
    private final net.momirealms.craftengine.core.plugin.context.Context context;

    public PlaceholderTag(@NotNull net.momirealms.craftengine.core.plugin.context.Context context) {
        this.context = context;
    }

    @Override
    public @Nullable Tag resolve(@NotNull String name, @NotNull ArgumentQueue arguments, @NotNull Context ctx) throws ParsingException {
        if (!this.has(name) || !CraftEngine.instance().compatibilityManager().hasPlaceholderAPI()) {
            return null;
        }
        String rawArgument = arguments.popOr("No argument relational placeholder provided").toString();
        if (rawArgument.contains("<")) rawArgument = AdventureHelper.resolvePlainStringTags(rawArgument, this.context.tagResolvers());
        String placeholder = "%" + rawArgument + "%";
        String parsed = (this.context instanceof PlayerContext playerContext)
                ? CraftEngine.instance().compatibilityManager().parse(playerContext.player(), placeholder)
                : CraftEngine.instance().compatibilityManager().parse(null, placeholder);
        if (parsed.equals(placeholder)) {
            parsed = arguments.popOr("No default papi value provided").toString();
        }
        return Tag.selfClosingInserting(ctx.deserialize(parsed));
    }

    @Override
    public boolean has(@NotNull String name) {
        return "papi".equals(name);
    }
}
