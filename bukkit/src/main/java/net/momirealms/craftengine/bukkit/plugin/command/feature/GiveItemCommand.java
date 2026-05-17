package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.PlayerUtils;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.FlagKeys;
import net.momirealms.craftengine.core.plugin.locale.MessageConstants;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bukkit.data.MultiplePlayerSelector;
import org.incendo.cloud.bukkit.parser.NamespacedKeyParser;
import org.incendo.cloud.bukkit.parser.selector.MultiplePlayerSelectorParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public final class GiveItemCommand extends BukkitCommandFeature<CommandSender> {

    public GiveItemCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .flag(FlagKeys.SILENT_FLAG)
                .required("player", MultiplePlayerSelectorParser.multiplePlayerSelectorParser(false))
                .required("id", NamespacedKeyParser.namespacedKeyComponent().suggestionProvider(new SuggestionProvider<>() {
                    @Override
                    public @NonNull CompletableFuture<? extends @NonNull Iterable<? extends @NonNull Suggestion>> suggestionsFuture(@NonNull CommandContext<Object> context, @NonNull CommandInput input) {
                        return CompletableFuture.completedFuture(plugin().itemManager().cachedCustomItemSuggestions());
                    }
                }))
                .optional("amount", IntegerParser.integerParser(1, 9999))
                .handler(context -> {
                    MultiplePlayerSelector selector = context.get("player");
                    Collection<Player> players = selector.values();
                    int amount = context.getOrDefault("amount", 1);
                    NamespacedKey namespacedKey = context.get("id");
                    Key itemId = Key.of(namespacedKey.namespace(), namespacedKey.value());
                    ItemDefinition itemDefinition = CraftEngineItems.byId(itemId);
                    if (itemDefinition == null) {
                        itemDefinition = BukkitItemManager.instance().getItemDefinitionByPath(itemId.value()).orElse(null);
                        if (itemDefinition == null) {
                            handleFeedback(context, MessageConstants.COMMAND_ITEM_GIVE_FAILURE_NOT_EXIST, Component.text(itemId.toString()));
                            return;
                        } else {
                            itemId = itemDefinition.id();
                        }
                    }
                    ItemDefinition finalItemDefinition = itemDefinition;
                    for (Player player : players) {
                        this.plugin().scheduler().platform().run(() -> {
                            BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
                            if (serverPlayer == null) return;
                            Item builtItem = finalItemDefinition.buildItem(serverPlayer);
                            if (builtItem != null) {
                                PlayerUtils.giveItem(serverPlayer, amount, builtItem, true);
                            }
                        }, null, player);
                    }
                    if (players.size() == 1) {
                        handleFeedback(context, MessageConstants.COMMAND_ITEM_GIVE_SUCCESS_SINGLE, Component.text(amount), Component.text(itemId.toString()), Component.text(players.iterator().next().getName()));
                    } else if (players.size() > 1) {
                        handleFeedback(context, MessageConstants.COMMAND_ITEM_GIVE_SUCCESS_MULTIPLE, Component.text(amount), Component.text(itemId.toString()), Component.text(players.size()));
                    }
                });
    }

    @Override
    public String getFeatureID() {
        return "give_item";
    }
}
