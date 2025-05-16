package net.momirealms.craftengine.core.plugin.text.minimessage;

import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.util.AdventureHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GlobalVariableTag implements TagResolver {
    private final Context context;

    public GlobalVariableTag(Context context) {
        this.context = context;
    }

    @Override
    public @Nullable Tag resolve(@NotNull String name, @NotNull ArgumentQueue arguments, @NotNull net.kyori.adventure.text.minimessage.Context ctx) throws ParsingException {
        if (!this.has(name)) {
            return null;
        }
        String id = arguments.popOr("No argument variable id provided").toString();
        String value = CraftEngine.instance().globalVariableManager().get(id);
        if (value == null) {
            throw ctx.newException("Unknown variable: ", arguments);
        }
        return Tag.selfClosingInserting(AdventureHelper.miniMessage().deserialize(value, this.context.tagResolvers()));
    }

    @Override
    public boolean has(@NotNull String name) {
        return "global".equals(name);
    }
}
