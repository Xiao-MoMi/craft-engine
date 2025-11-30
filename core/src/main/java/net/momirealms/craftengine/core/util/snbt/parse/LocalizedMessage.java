package net.momirealms.craftengine.core.util.snbt.parse;

import com.mojang.brigadier.Message;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.AdventureHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;

public class LocalizedMessage implements Message {
    private final String node;
    private final String[] arguments;

    public LocalizedMessage(
            @NotNull String node,
            @Nullable String... arguments
    ) {
        this.node = node;
        this.arguments = arguments != null
                ? Arrays.copyOf(arguments, arguments.length)
                : new String[0];
    }

    @Override
    public String getString() {
        return generateLocalizedMessage();
    }

    private String generateLocalizedMessage() {
        try {
            String rawMessage = Optional.ofNullable(TranslationManager.instance()
                    .miniMessageTranslation(this.node)).orElse(this.node);
            String cleanMessage = AdventureHelper.miniMessage()
                    .stripTags(rawMessage);
            for (int i = 0; i < arguments.length; i++) {
                cleanMessage = cleanMessage.replace(
                        "<arg:" + i + ">",
                        arguments[i] != null ? arguments[i] : "null"
                );
            }
            return cleanMessage;
        } catch (Exception e) {
            return String.format(
                    "Failed to translate. Node: %s, Arguments: %s. Cause: %s",
                    node,
                    Arrays.toString(arguments),
                    e.getMessage()
            );
        }
    }
}
