package net.momirealms.craftengine.core.plugin.text.minimessage;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.momirealms.craftengine.core.util.AdventureHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.StringJoiner;

public final class PlainL10NTag implements TagResolver {
    public static final TagResolver INSTANCE = new PlainL10NTag();

    private PlainL10NTag() {}

    @Override
    public @Nullable Tag resolve(@NotNull String name, @NotNull ArgumentQueue aq, @NotNull net.kyori.adventure.text.minimessage.Context ctx) throws ParsingException {
        if (!this.has(name)) {
            return null;
        }
        String l10nKey = aq.popOr("No argument l10n key provided").toString();
        if (aq.hasNext()) {
            StringJoiner joiner = new StringJoiner(":");
            while (aq.hasNext()) {
                Tag.Argument arg = aq.pop();
                joiner.add("'" + AdventureHelper.strictMiniMessage().serialize(ctx.deserialize(arg.value())) + "'");
            }
            return Tag.selfClosingInserting(Component.text("<l10n:" + l10nKey + ":" + joiner + ">"));
        } else {
            return Tag.selfClosingInserting(Component.text("<l10n:" + l10nKey + ">"));
        }
    }

    @Override
    public boolean has(@NotNull String name) {
        return "l10n".equals(name);
    }
}
