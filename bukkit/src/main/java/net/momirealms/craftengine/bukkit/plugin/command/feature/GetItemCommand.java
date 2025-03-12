package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.util.PlayerUtils;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.FlagKeys;
import net.momirealms.craftengine.core.plugin.locale.MessageConstants;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bukkit.parser.NamespacedKeyParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.concurrent.CompletableFuture;

public class GetItemCommand extends BukkitCommandFeature<CommandSender> {

    public GetItemCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .senderType(Player.class)
                .flag(FlagKeys.SILENT_FLAG)
                .flag(FlagKeys.TO_INVENTORY_FLAG)
                .required("id", NamespacedKeyParser.namespacedKeyComponent().suggestionProvider(new SuggestionProvider<>() {
                    @Override
                    public @NonNull CompletableFuture<? extends @NonNull Iterable<? extends @NonNull Suggestion>> suggestionsFuture(@NonNull CommandContext<Object> context, @NonNull CommandInput input) {
                        return CompletableFuture.completedFuture(plugin().itemManager().cachedSuggestions());
                    }
                }))
                .optional("amount", IntegerParser.integerParser(1, 6400))
                .handler(context -> {
                    Player player = context.sender();
                    int amount = context.getOrDefault("amount", 1);
                    boolean toInv = context.flags().hasFlag(FlagKeys.TO_INVENTORY);
                    NamespacedKey namespacedKey = context.get("id");
                    Key key = Key.of(namespacedKey.namespace(), namespacedKey.value());
                    ItemStack builtItem = plugin().itemManager().buildCustomItemStack(key, plugin().adapt(context.sender()));
                    if (builtItem == null) {
                        handleFeedback(context, MessageConstants.COMMAND_ITEM_GET_FAILURE_NOT_EXIST, Component.text(key.toString()));
                        return;
                    }
                    int amountToGive = amount;
                    int maxStack = builtItem.getMaxStackSize();
                    while (amountToGive > 0) {
                        int perStackSize = Math.min(maxStack, amountToGive);
                        amountToGive -= perStackSize;
                        ItemStack more = builtItem.clone();
                        more.setAmount(perStackSize);
                        if (toInv) {
                            PlayerUtils.putItemsToInventory(player.getInventory(), more, more.getAmount());
                        } else {
                            PlayerUtils.dropItem(player, more, false, true, false);
                        }
                    }
                    handleFeedback(context, MessageConstants.COMMAND_ITEM_GET_SUCCESS, Component.text(amount), Component.text(key.toString()));
                });
    }

    @Override
    public String getFeatureID() {
        return "get_item";
    }
}
