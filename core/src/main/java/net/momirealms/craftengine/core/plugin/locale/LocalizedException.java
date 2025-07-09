package net.momirealms.craftengine.core.plugin.locale;

import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class LocalizedException extends RuntimeException {
    private final String node;
    private String[] arguments;

    public LocalizedException(
            @NotNull String node,
            @Nullable Exception cause,
            @Nullable String... arguments
    ) {
        super(node, cause);
        this.node = node;
        this.arguments = arguments != null
                ? Arrays.copyOf(arguments, arguments.length)
                : new String[0];
    }

    public LocalizedException(@NotNull String node, @Nullable String... arguments) {
        this(node, (Exception) null, arguments);
    }

    public LocalizedException(
            @NotNull String node,
            @Nullable String[] prefixArgs,
            @Nullable String... suffixArgs
    ) {
        this(
                node,
                (Exception) null,
                ArrayUtils.merge(
                        prefixArgs != null ? prefixArgs : new String[0],
                        suffixArgs != null ? suffixArgs : new String[0]
                )
        );
    }

    public String[] arguments() {
        return Arrays.copyOf(arguments, arguments.length);
    }

    public void setArgument(int index, @NotNull String argument) {
        if (index < 0 || index >= arguments.length) {
            throw new IndexOutOfBoundsException("Invalid argument index: " + index);
        }
        this.arguments[index] = argument;
    }

    public void appendHeadArgument(@NotNull String argument) {
        this.arguments = ArrayUtils.appendElementToArrayHead(arguments, argument);
    }

    public void appendTailArgument(@NotNull String argument) {
        this.arguments = ArrayUtils.appendElementToArrayTail(arguments, argument);
    }

    public @NotNull String node() {
        return node;
    }

    @Override
    public String getMessage() {
        return generateLocalizedMessage();
    }

    private String generateLocalizedMessage() {
        try {
            String rawMessage = TranslationManager.instance()
                    .miniMessageTranslation(this.node);
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