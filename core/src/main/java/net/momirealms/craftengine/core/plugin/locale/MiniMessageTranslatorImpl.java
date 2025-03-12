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

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.renderer.TranslatableComponentRenderer;
import net.kyori.adventure.translation.Translator;
import net.kyori.adventure.util.TriState;
import net.kyori.examination.ExaminableProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class MiniMessageTranslatorImpl implements MiniMessageTranslator {
    private static final Key NAME = Key.key("craftengine", "main");
    static final MiniMessageTranslatorImpl INSTANCE = new MiniMessageTranslatorImpl();
    final TranslatableComponentRenderer<Locale> renderer = TranslatableComponentRenderer.usingTranslationSource(this);
    private final Set<Translator> sources = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Override
    public @NotNull Key name() {
        return NAME;
    }

    @Override
    public @NotNull TriState hasAnyTranslations() {
        if (!this.sources.isEmpty()) {
            return TriState.TRUE;
        }
        return TriState.FALSE;
    }

    @Override
    public @Nullable MessageFormat translate(@NotNull String key, @NotNull Locale locale) {
        // No need to implement this method
        return null;
    }

    @Override
    public @Nullable Component translate(@NotNull TranslatableComponent component, @NotNull Locale locale) {
        for (final Translator source : this.sources) {
            final Component translation = source.translate(component, locale);
            if (translation != null) {
                return translation;
            }
        }
        return null;
    }

    @Override
    public @NotNull Iterable<? extends Translator> sources() {
        return Collections.unmodifiableSet(this.sources);
    }

    @Override
    public boolean addSource(final @NotNull Translator source) {
        if (source == this) throw new IllegalArgumentException("MiniMessageTranslationSource");
        return this.sources.add(source);
    }

    @Override
    public boolean removeSource(final @NotNull Translator source) {
        return this.sources.remove(source);
    }

    @Override
    public @NotNull Stream<? extends ExaminableProperty> examinableProperties() {
        return Stream.of(ExaminableProperty.of("sources", this.sources));
    }
}
