package net.momirealms.craftengine.core.plugin.locale;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.translation.Translator;
import net.momirealms.craftengine.core.plugin.Manageable;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.plugin.text.minimessage.IndexedArgumentTag;
import net.momirealms.craftengine.core.util.AdventureHelper;
import org.incendo.cloud.suggestion.Suggestion;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public interface TranslationManager extends Manageable {

    static TranslationManager instance() {
        return TranslationManagerImpl.instance;
    }

    ConfigParser[] parsers();

    default String miniMessageTranslation(String key) {
        return miniMessageTranslation(key, null);
    }

    String miniMessageTranslation(String key, @Nullable Locale locale);

    default Component render(TranslatableComponent component) {
        return render(component, null);
    }

    Component render(TranslatableComponent component, @Nullable Locale locale);

    default String plainTranslation(String key, @Nullable Locale locale, String... arguments) {
        String translation = miniMessageTranslation(key, locale);
        if (translation == null) {
            return key;
        }
        Component deserialize = AdventureHelper.customMiniMessage().deserialize(translation, new IndexedArgumentTag(Arrays.stream(arguments).map(Component::text).toList()));
        return AdventureHelper.plainTextContent(deserialize);
    }

    default String plainTranslation(String key, String... arguments) {
        String translation = miniMessageTranslation(key);
        if (translation == null) {
            return key;
        }
        Component deserialize = AdventureHelper.customMiniMessage().deserialize(translation, new IndexedArgumentTag(Arrays.stream(arguments).map(Component::text).toList()));
        return AdventureHelper.plainTextContent(deserialize);
    }

    static @Nullable Locale parseLocale(@Nullable String locale) {
        return locale == null || locale.isEmpty() ? null : Translator.parseLocale(locale);
    }

    static String formatLocale(Locale locale) {
        String language = locale.getLanguage().toLowerCase(Locale.ROOT);
        String country = locale.getCountry().toLowerCase(Locale.ROOT);
        if (country.isEmpty()) {
            return language;
        } else {
            return language + "_" + country;
        }
    }

    Set<String> translationKeys();

    Map<String, ClientLangData> clientLangData();

    void addClientTranslation(String langId, Map<String, String> translations);

    Set<String> allLang();

    List<Suggestion> allLangSuggestions();

    Map<String, List<String>> locale2Countries();
}
