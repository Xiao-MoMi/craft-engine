/*
 * Copyright (C) <2025> <XiaoMoMi>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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

    ClientLangManager clientLangManager();

    default String miniMessageTranslation(String key) {
        return miniMessageTranslation(key, null);
    }

    void forcedLocale(Locale locale);

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
