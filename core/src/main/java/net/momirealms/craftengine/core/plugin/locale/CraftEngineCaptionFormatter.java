package net.momirealms.craftengine.core.plugin.locale;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.caption.Caption;
import org.incendo.cloud.caption.CaptionVariable;
import org.incendo.cloud.minecraft.extras.caption.ComponentCaptionFormatter;

import java.util.List;

public class CraftEngineCaptionFormatter<C> implements ComponentCaptionFormatter<C> {
    private final TranslationManager translationManager;

    public CraftEngineCaptionFormatter(final TranslationManager translationManager) {
        this.translationManager = translationManager;
    }

    @Override
    public @NonNull Component formatCaption(@NonNull Caption captionKey, @NonNull C recipient, @NonNull String caption, @NonNull List<@NonNull CaptionVariable> variables) {
        Component component = ComponentCaptionFormatter.translatable().formatCaption(captionKey, recipient, caption, variables);
        if (component instanceof TranslatableComponent translatableComponent) {
            return this.translationManager.render(translatableComponent);
        } else {
            return component;
        }
    }
}
