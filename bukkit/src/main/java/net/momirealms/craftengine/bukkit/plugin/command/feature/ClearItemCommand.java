package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.FlagKeys;
import net.momirealms.craftengine.core.plugin.locale.MessageConstants;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.UniqueKey;
import net.momirealms.craftengine.proxy.minecraft.world.entity.player.InventoryProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.player.PlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.inventory.AbstractContainerMenuProxy;
import net.momirealms.craftengine.proxy.minecraft.world.inventory.InventoryMenuProxy;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
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
import java.util.function.Predicate;

public class ClearItemCommand extends BukkitCommandFeature<CommandSender> {

    public ClearItemCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .flag(FlagKeys.SILENT_FLAG)
                .flag(FlagKeys.MATCH_TAG_FLAG)
                .required("player", MultiplePlayerSelectorParser.multiplePlayerSelectorParser(true))
                .required("id", NamespacedKeyParser.namespacedKeyComponent().suggestionProvider(new SuggestionProvider<>() {
                    @Override
                    public @NonNull CompletableFuture<? extends @NonNull Iterable<? extends @NonNull Suggestion>> suggestionsFuture(@NonNull CommandContext<Object> context, @NonNull CommandInput input) {
                        return CompletableFuture.completedFuture(plugin().itemManager().cachedCustomItemSuggestions());
                    }
                }))
                .optional("amount", IntegerParser.integerParser(0))
                .handler(context -> {
                    MultiplePlayerSelector selector = context.get("player");
                    int amount = context.getOrDefault("amount", -1);
                    NamespacedKey namespacedKey = context.get("id");
                    Key idOrTag = Key.of(namespacedKey.namespace(), namespacedKey.value());
                    Predicate<Object> predicate = !context.flags().hasFlag(FlagKeys.MATCH_TAG) ?
                            nmsStack -> {
                                Key id = BukkitItemManager.instance().wrap(ItemStackUtils.asCraftMirror(nmsStack)).id();
                                return id.equals(idOrTag);
                            } :
                            nmsStack -> {
                                Key id = BukkitItemManager.instance().wrap(ItemStackUtils.asCraftMirror(nmsStack)).id();
                                for (UniqueKey key : BukkitItemManager.instance().itemIdsByTag(idOrTag)) {
                                    if (key.key().equals(id)) {
                                        return true;
                                    }
                                }
                                return false;
                            };
                    int totalCount = 0;
                    Collection<Player> players = selector.values();
                    for (Player player : players) {
                        Object serverPlayer = FastNMS.INSTANCE.method$CraftPlayer$getHandle(player);
                        Object inventory = FastNMS.INSTANCE.method$Player$getInventory(serverPlayer);
                        Object inventoryMenu = PlayerProxy.INSTANCE.getInventoryMenu(serverPlayer);
                        totalCount += InventoryProxy.INSTANCE.clearOrCountMatchingItems(inventory, predicate, amount, InventoryMenuProxy.INSTANCE.getCraftSlots(inventoryMenu));
                        AbstractContainerMenuProxy.INSTANCE.broadcastChanges(FastNMS.INSTANCE.field$Player$containerMenu(serverPlayer));
                        InventoryMenuProxy.INSTANCE.slotsChanged(inventoryMenu, inventory);
                    }
                    if (totalCount == 0) {
                        if (players.size() == 1) {
                            handleFeedback(context, MessageConstants.COMMAND_ITEM_CLEAR_FAILED_SINGLE, Component.text(players.iterator().next().getName()));
                        } else {
                            handleFeedback(context, MessageConstants.COMMAND_ITEM_CLEAR_FAILED_MULTIPLE, Component.text(players.size()));
                        }
                    } else {
                        if (amount == 0) {
                            if (players.size() == 1) {
                                handleFeedback(context, MessageConstants.COMMAND_ITEM_CLEAR_TEST_SINGLE, Component.text(totalCount), Component.text(players.iterator().next().getName()));
                            } else {
                                handleFeedback(context, MessageConstants.COMMAND_ITEM_CLEAR_TEST_MULTIPLE, Component.text(totalCount), Component.text(players.size()));
                            }
                        } else {
                            if (players.size() == 1) {
                                handleFeedback(context, MessageConstants.COMMAND_ITEM_CLEAR_SUCCESS_SINGLE, Component.text(totalCount), Component.text(players.iterator().next().getName()));
                            } else {
                                handleFeedback(context, MessageConstants.COMMAND_ITEM_CLEAR_SUCCESS_MULTIPLE, Component.text(totalCount), Component.text(players.size()));
                            }
                        }
                    }
                });
    }

    @Override
    public String getFeatureID() {
        return "clear_item";
    }
}
