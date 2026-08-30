package net.momirealms.craftengine.bukkit.plugin.command.debug;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.font.BitmapImage;
import net.momirealms.craftengine.core.font.Image;
import net.momirealms.craftengine.core.font.ReferenceImage;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.sender.Sender;
import net.momirealms.craftengine.core.util.FormatUtils;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.parser.NamespacedKeyParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class DebugImageCommand extends BukkitCommandFeature<CommandSender> {

    public DebugImageCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .required("id", NamespacedKeyParser.namespacedKeyComponent().suggestionProvider(new SuggestionProvider<>() {
                    @Override
                    public @NonNull CompletableFuture<? extends @NonNull Iterable<? extends @NonNull Suggestion>> suggestionsFuture(@NonNull CommandContext<Object> context, @NonNull CommandInput input) {
                        return CompletableFuture.completedFuture(plugin().fontManager().cachedImagesSuggestions());
                    }
                }))
                .optional("row", IntegerParser.integerParser(0))
                .optional("column", IntegerParser.integerParser(0))
                .handler(context -> {
                    Key imageId = KeyUtils.namespacedKeyToKey(context.get("id"));
                    Sender sender = plugin().senderFactory().wrap(context.sender());
                    Image image = plugin().fontManager().imageById(imageId).orElse(null);
                    if (image == null) {
                        sender.sendMessage(DebugCommandOutput.error("Unknown image '" + imageId + "'"));
                        return;
                    }
                    if (image instanceof ReferenceImage referenceImage) {
                        int row = referenceImage.row();
                        int column = referenceImage.col();
                        Image referenced = referenceImage.image();
                        if (referenced instanceof BitmapImage bitmapImage) {
                            String value = referenced.id().asString() + ((row != 0 || column != 0) ? ":" + row + ":" + column : "");
                            sendResult(sender, image, bitmapImage, value, row, column);
                        } else {
                            sender.sendMessage(DebugCommandOutput.error("Referenced image is not a bitmap"));
                        }
                    } else if (image instanceof BitmapImage bitmapImage) {
                        int row = context.getOrDefault("row", 0);
                        int column = context.getOrDefault("column", 0);
                        String value = bitmapImage.isValidCoordinate(row, column)
                                ? imageId.asString() + ((row != 0 || column != 0) ? ":" + row + ":" + column : "") // 自动最小化
                                : imageId.asString() + ":" + (row = 0) + ":" + (column = 0); // 因为是无效的所以说要强调告诉获取的是00
                        sendResult(sender, image, bitmapImage, value, row, column);
                    } else {
                        sender.sendMessage(DebugCommandOutput.error("Image type is not supported by this command"));
                    }
                });
    }

    @Override
    public String getFeatureID() {
        return "debug_image";
    }

    private static void sendResult(Sender sender, Image image, BitmapImage bitmapImage, String value, int row, int column) {
        Component imageValue = Component.text(value, DebugCommandOutput.accentColor())
                .hoverEvent(image.componentAt(row, column).color(DebugCommandOutput.textColor()))
                .clickEvent(ClickEvent.suggestCommand(value));
        sender.sendMessage(DebugCommandOutput.title("Bitmap Image"));
        sender.sendMessage(DebugCommandOutput.value("Image", imageValue));
        sender.sendMessage(DebugCommandOutput.value("Coordinates", row + ", " + column));
        sender.sendMessage(DebugCommandOutput.section("Copy as"));
        sender.sendMessage(getHelperInfo(bitmapImage, row, column));
    }

    private static TextComponent getHelperInfo(BitmapImage image, int row, int column) {
        String raw = new String(Character.toChars(image.codepointAt(row, column)));
        String font = image.font().toString();
        return Component.empty().children(List.of(
                Component.text("    "),
                DebugCommandOutput.copyable("[MiniMessage]", FormatUtils.miniMessageFont(raw, font)),
                Component.text("  "),
                DebugCommandOutput.copyable("[MineDown]", FormatUtils.mineDownFont(raw, font)),
                Component.text("  "),
                DebugCommandOutput.copyable("[RAW]", "{\"text\":\"" + raw + "\",\"font\":\"" + font + "\"}")
        ));
    }
}
