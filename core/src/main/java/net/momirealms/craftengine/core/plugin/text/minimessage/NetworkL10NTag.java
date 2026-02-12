package net.momirealms.craftengine.core.plugin.text.minimessage;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class NetworkL10NTag implements TagResolver {
    private final Context context;

    public NetworkL10NTag(Context context) {
        this.context = context;
    }

    @Override
    public @Nullable Tag resolve(@NotNull String name, @NotNull ArgumentQueue aq, @NotNull net.kyori.adventure.text.minimessage.Context ctx) throws ParsingException {
        if (!this.has(name)) {
            return null;
        }
        Locale locale = null;
        if (this.context instanceof PlayerOptionalContext playerOptionalContext && playerOptionalContext.isPlayerPresent()) {
            locale = playerOptionalContext.player().selectedLocale();
        }
        String i18nKey = aq.popOr("No argument l10n key provided").toString();
        String translation = TranslationManager.instance().miniMessageTranslation(i18nKey, locale);
        if (aq.hasNext()) {
            List<Component> arguments = new ArrayList<>();
            while (aq.hasNext()) {
                Tag.Argument arg = aq.pop();
                arguments.add(ctx.deserialize(arg.value()));
            }
            return Tag.selfClosingInserting(ctx.deserialize(translation, new IndexedArgumentTag(arguments)));
        } else {
            return Tag.selfClosingInserting(ctx.deserialize(translation));
        }
    }

    @Override
    public boolean has(@NotNull String name) {
        return "l10n".equals(name);
    }
}
