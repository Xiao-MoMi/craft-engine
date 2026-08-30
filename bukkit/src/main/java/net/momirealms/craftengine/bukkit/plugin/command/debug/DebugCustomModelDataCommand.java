package net.momirealms.craftengine.bukkit.plugin.command.debug;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.parser.NamespacedKeyParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public final class DebugCustomModelDataCommand extends BukkitCommandFeature<CommandSender> {

    public DebugCustomModelDataCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .optional("id", NamespacedKeyParser.namespacedKeyComponent().suggestionProvider(new SuggestionProvider<>() {
                    @Override
                    public @NonNull CompletableFuture<? extends @NonNull Iterable<? extends @NonNull Suggestion>> suggestionsFuture(@NonNull CommandContext<Object> context, @NonNull CommandInput input) {
                        return CompletableFuture.completedFuture(plugin().itemManager().cachedCustomItemSuggestions());
                    }
                }))
                .handler(this::handleCommand);
    }

    @Override
    public String getFeatureID() {
        return "debug_custom_model_data";
    }

    private void handleCommand(CommandContext<CommandSender> context) {
        NamespacedKey namespacedKey = context.getOrDefault("id", null);
        @Nullable BukkitServerPlayer player = context.sender() instanceof Player p ? BukkitAdaptor.adapt(p) : null;
        var sender = plugin().senderFactory().wrap(context.sender());

        if (namespacedKey != null) {
            Key itemId = KeyUtils.namespacedKeyToKey(namespacedKey);
            ItemDefinition itemDefinition = CraftEngineItems.byId(itemId);
            if (itemDefinition == null) {
                sender.sendMessage(DebugCommandOutput.error("Unknown custom item '" + itemId + "'"));
                return;
            }
            Item item = itemDefinition.buildItem(player);
            sendMessage(context, item, player);
            return;
        }

        if (player != null) {
            Item item = player.getItemInHand(InteractionHand.MAIN_HAND).copyWithCount(1);
            if (item.isEmpty()) {
                sender.sendMessage(DebugCommandOutput.error("The main hand is empty"));
                return;
            }
            sendMessage(context, item, player);
        } else {
            sender.sendMessage(DebugCommandOutput.error("An item ID is required from the console"));
        }
    }

    private void sendMessage(CommandContext<CommandSender> context, Item itemStack, BukkitServerPlayer player) {
        Item clientBoundItem = plugin().itemManager().s2c(itemStack, player).orElse(itemStack);
        int customModelData = clientBoundItem.customModelData().orElse(0);
        var sender = plugin().senderFactory().wrap(context.sender());
        sender.sendMessage(DebugCommandOutput.title("Custom Model Data"));
        sender.sendMessage(DebugCommandOutput.value("Value", customModelData));
    }

}
