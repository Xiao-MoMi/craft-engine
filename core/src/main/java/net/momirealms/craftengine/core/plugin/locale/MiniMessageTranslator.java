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
import net.kyori.adventure.text.renderer.TranslatableComponentRenderer;
import net.kyori.adventure.translation.Translator;
import net.kyori.examination.Examinable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public interface MiniMessageTranslator extends Translator, Examinable {
    static @NotNull MiniMessageTranslator translator() {
        return MiniMessageTranslatorImpl.INSTANCE;
    }

    static @NotNull TranslatableComponentRenderer<Locale> renderer() {
        return MiniMessageTranslatorImpl.INSTANCE.renderer;
    }

    static @NotNull Component render(final @NotNull Component component, final @NotNull Locale locale) {
        return renderer().render(component, locale);
    }

    @NotNull Iterable<? extends Translator> sources();

    boolean addSource(final @NotNull Translator source);

    boolean removeSource(final @NotNull Translator source);
}
