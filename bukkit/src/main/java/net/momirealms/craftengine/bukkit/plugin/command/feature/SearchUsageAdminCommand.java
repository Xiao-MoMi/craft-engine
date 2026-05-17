package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.item.recipe.Recipe;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.util.Key;
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
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class SearchUsageAdminCommand extends BukkitCommandFeature<CommandSender> {

    public SearchUsageAdminCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .required("player", MultiplePlayerSelectorParser.multiplePlayerSelectorParser(false))
                .required("id", NamespacedKeyParser.namespacedKeyComponent().suggestionProvider(new SuggestionProvider<>() {
                    @Override
                    public @NonNull CompletableFuture<? extends @NonNull Iterable<? extends @NonNull Suggestion>> suggestionsFuture(@NonNull CommandContext<Object> context, @NonNull CommandInput input) {
                        return CompletableFuture.completedFuture(plugin().itemManager().cachedCustomItemSuggestions());
                    }
                }))
                .handler(context -> {
                    MultiplePlayerSelector selector = context.get("player");
                    Collection<Player> players = selector.values();
                    NamespacedKey namespacedKey = context.get("id");
                    for (Player player : players) {
                        BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
                        if (serverPlayer == null) continue;
                        Key itemId = Key.of(namespacedKey.namespace(), namespacedKey.value());
                        List<Recipe> inRecipes = plugin().recipeManager().recipeByIngredient(itemId);
                        if (!inRecipes.isEmpty()) {
                            plugin().itemBrowserManager().openRecipePage(serverPlayer, null, inRecipes, 0, 0, false);
                        }
                    }
                });
    }

    @Override
    public String getFeatureID() {
        return "search_usage_admin";
    }
}
