package net.momirealms.craftengine.core.plugin.minimessage;

import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.AdventureHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class I18NTag implements TagResolver {
    private final MiniMessageTextContext context;

    public I18NTag(MiniMessageTextContext context) {
        this.context = context;
    }

    @Override
    public @Nullable Tag resolve(@NotNull String name, @NotNull ArgumentQueue arguments, @NotNull Context ctx) throws ParsingException {
        if (!this.has(name)) {
            return null;
        }
        String i18nKey = arguments.popOr("No argument i18n key provided").toString();
        String translation = TranslationManager.instance().translateI18NTag(i18nKey);
        return Tag.inserting(AdventureHelper.miniMessage().deserialize(translation, this.context.tagResolvers()));
    }

    @Override
    public boolean has(@NotNull String name) {
        return "i18n".equals(name) || "l10n".equals(name);
    }
}
