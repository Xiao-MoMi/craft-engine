package net.momirealms.craftengine.core.plugin.text.minimessage;

import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.AdventureHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class I18NTag implements TagResolver {
    private final Context context;

    public I18NTag(Context context) {
        this.context = context;
    }

    @Override
    public @Nullable Tag resolve(@NotNull String name, @NotNull ArgumentQueue arguments, @NotNull net.kyori.adventure.text.minimessage.Context ctx) throws ParsingException {
        if (!this.has(name)) {
            return null;
        }
        String i18nKey = arguments.popOr("No argument i18n key provided").toString();
        String translation = TranslationManager.instance().miniMessageTranslation(i18nKey);
        return Tag.selfClosingInserting(AdventureHelper.miniMessage().deserialize(translation, this.context.combinedTagResolver()));
    }

    @Override
    public boolean has(@NotNull String name) {
        return "i18n".equals(name);
    }
}
