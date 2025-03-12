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
