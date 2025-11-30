package net.momirealms.craftengine.core.util.snbt.parse;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.AdventureHelper;

import java.util.Optional;
import java.util.function.Supplier;

public class LocalizedCommandSyntaxException extends CommandSyntaxException {
    public static final int CONTEXT_AMOUNT = 10;
    public static final String PARSE_ERROR_NODE = "warning.config.type.snbt.invalid_syntax.parse_error";
    public static final String HERE_NODE = "warning.config.type.snbt.invalid_syntax.here";
    private final Message message;
    private final String input;
    private final int cursor;

    public LocalizedCommandSyntaxException(CommandExceptionType type, Message message) {
        super(type, message);
        this.message = message;
        this.input = null;
        this.cursor = -1;
    }

    public LocalizedCommandSyntaxException(CommandExceptionType type, Message message, String input, int cursor) {
        super(type, message, input, cursor);
        this.message = message;
        this.input = input;
        this.cursor = cursor;
    }

    @Override
    public String getMessage() {
        String message = this.message.getString();
        final String context = getContext();
        if (context == null) {
            return message;
        }
        return generateLocalizedMessage(
                PARSE_ERROR_NODE,
                () -> message + " at position " + this.cursor + ": " + context,
                message, String.valueOf(this.cursor), context
        );
    }

    @Override
    public String getContext() {
        if (this.input == null || this.cursor < 0) {
            return null;
        }
        final StringBuilder builder = new StringBuilder();
        final int cursor = Math.min(this.input.length(), this.cursor);

        if (cursor > CONTEXT_AMOUNT) {
            builder.append("...");
        }

        builder.append(this.input, Math.max(0, cursor - CONTEXT_AMOUNT), cursor);
        builder.append(generateLocalizedMessage(HERE_NODE, () -> "<--[HERE]"));

        return builder.toString();
    }


    private String generateLocalizedMessage(String node, Supplier<String> fallback, String... arguments) {
        try {
            String rawMessage = Optional.ofNullable(TranslationManager.instance()
                    .miniMessageTranslation(node)).orElse(fallback.get());
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
            return fallback.get();
        }
    }
}
