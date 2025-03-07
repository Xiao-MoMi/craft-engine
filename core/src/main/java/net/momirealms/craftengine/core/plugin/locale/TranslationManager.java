package net.momirealms.craftengine.core.plugin.locale;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.Translator;
import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.plugin.Reloadable;
import net.momirealms.craftengine.core.plugin.config.ConfigSectionParser;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public interface TranslationManager extends Reloadable, ConfigSectionParser {
    String CONFIG_SECTION_NAME = "i18n";

    static TranslationManager instance() {
        return TranslationManagerImpl.instance;
    }

    default String miniMessageTranslation(String key) {
        return miniMessageTranslation(key, null);
    }

    void forceLocale(Locale locale);

    String miniMessageTranslation(String key, @Nullable Locale locale);

    String translateI18NTag(String i18nId);

    default Component render(Component component) {
        return render(component, null);
    }

    Component render(Component component, @Nullable Locale locale);

    static @Nullable Locale parseLocale(@Nullable String locale) {
        return locale == null || locale.isEmpty() ? null : Translator.parseLocale(locale);
    }

    @Override
    default int loadingSequence() {
        return LoadingSequence.TRANSLATION;
    }

    @Override
    default String sectionId() {
        return CONFIG_SECTION_NAME;
    }
}
