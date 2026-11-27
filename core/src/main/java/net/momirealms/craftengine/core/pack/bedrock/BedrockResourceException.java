package net.momirealms.craftengine.core.pack.bedrock;

import net.momirealms.craftengine.core.plugin.locale.TranslationManager;

public final class BedrockResourceException extends UnsupportedOperationException {
    private final String translationKey;
    private final String[] arguments;

    public BedrockResourceException(String translationKey, String... arguments) {
        super(translationKey);
        this.translationKey = translationKey;
        this.arguments = arguments;
    }

    @Override
    public String getLocalizedMessage() {
        return TranslationManager.message(this.translationKey, this.arguments);
    }
}
