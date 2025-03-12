package net.momirealms.craftengine.core.plugin.locale;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.translation.Translator;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public interface MiniMessageTranslationRegistry extends Translator {
    static @NotNull MiniMessageTranslationRegistry create(final Key name, final MiniMessage miniMessage) {
        return new MiniMessageTranslationRegistryImpl(requireNonNull(name, "name"), requireNonNull(miniMessage, "MiniMessage"));
    }

    void register(@NotNull String key, @NotNull Locale locale, @NotNull String format);

    void unregister(@NotNull String key);

    boolean contains(@NotNull String key);

    String miniMessageTranslation(@NotNull String key, @NotNull Locale locale);

    void defaultLocale(@NotNull Locale defaultLocale);

    default void registerAll(final @NotNull Locale locale, final @NotNull Map<String, String> bundle) {
        IllegalArgumentException firstError = null;
        int errorCount = 0;
        for (final Map.Entry<String, String> entry : bundle.entrySet()) {
            try {
                this.register(entry.getKey(), locale, entry.getValue());
            } catch (final IllegalArgumentException e) {
                if (firstError == null) {
                    firstError = e;
                }
                errorCount++;
            }
        }
        if (firstError != null) {
            if (errorCount == 1) {
                throw firstError;
            } else if (errorCount > 1) {
                throw new IllegalArgumentException(String.format("Invalid key (and %d more)", errorCount - 1), firstError);
            }
        }
    }
}
