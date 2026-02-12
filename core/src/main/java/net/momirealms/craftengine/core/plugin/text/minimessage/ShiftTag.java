package net.momirealms.craftengine.core.plugin.text.minimessage;

import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.AdventureHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ShiftTag implements TagResolver {
    public static final ShiftTag INSTANCE = new ShiftTag();

    private ShiftTag() {}

    @Override
    public @Nullable Tag resolve(@NotNull String name, @NotNull ArgumentQueue arguments, @NotNull Context ctx) throws ParsingException {
        if (!this.has(name)) {
            return null;
        }
        String shiftAmount = arguments.popOr("No argument shift provided").toString();
        try {
            int shift = Integer.parseInt(shiftAmount);
            return Tag.selfClosingInserting(AdventureHelper.miniMessage().deserialize(CraftEngine.instance().fontManager().createMiniMessageOffsets(shift)));
        } catch (NumberFormatException e) {
            throw ctx.newException("Invalid shift value", arguments);
        }
    }

    @Override
    public boolean has(@NotNull String name) {
        return "shift".equals(name);
    }
}
