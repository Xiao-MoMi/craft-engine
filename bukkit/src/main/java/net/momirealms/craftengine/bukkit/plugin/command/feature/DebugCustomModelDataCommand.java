package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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

public class DebugCustomModelDataCommand extends BukkitCommandFeature<CommandSender> {

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
        @Nullable BukkitServerPlayer player = context.sender() instanceof Player p ? BukkitAdaptors.adapt(p) : null;

        if (namespacedKey != null) {
            Key itemId = KeyUtils.namespacedKeyToKey(namespacedKey);
            CustomItem<ItemStack> customItem = CraftEngineItems.byId(itemId);
            if (customItem == null) return;
            Item<ItemStack> item = customItem.buildItem(player);
            sendMessage(context, getCustomModelData(item, player));
            return;
        }

        if (player != null) {
            Item<ItemStack> item = player.getItemInHand(InteractionHand.MAIN_HAND).copyWithCount(1);
            sendMessage(context, getCustomModelData(item, player));
        }
    }

    private int getCustomModelData(Item<ItemStack> itemStack, BukkitServerPlayer player) {
        return plugin().itemManager().s2c(itemStack, player)
                .map(Item::customModelData)
                .orElse(itemStack.customModelData())
                .orElse(0);
    }

    private void sendMessage(CommandContext<CommandSender> context, int customModelData) {
        Component message = Component.text(customModelData)
                .hoverEvent(Component.text("Copy", NamedTextColor.YELLOW))
                .clickEvent(ClickEvent.suggestCommand(String.valueOf(customModelData)));
        plugin().senderFactory().wrap(context.sender()).sendMessage(message);
    }
}
