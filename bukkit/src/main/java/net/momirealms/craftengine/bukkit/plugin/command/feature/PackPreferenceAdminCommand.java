package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.FlagKeys;
import net.momirealms.craftengine.core.plugin.locale.MessageConstants;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.bukkit.data.MultiplePlayerSelector;
import org.incendo.cloud.bukkit.parser.selector.MultiplePlayerSelectorParser;
import org.incendo.cloud.parser.standard.EnumParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PackPreferenceAdminCommand extends BukkitCommandFeature<CommandSender> {
    public PackPreferenceAdminCommand(CraftEngineCommandManager<CommandSender> manager, CraftEngine plugin) {
        super(manager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .flag(FlagKeys.SILENT_FLAG)
                .required("player", MultiplePlayerSelectorParser.multiplePlayerSelectorParser(false))
                .required("action", EnumParser.enumParser(PackPreferencePlayerCommand.Action.class))
                .required("pack", StringParser.stringParser(), (context, input) -> CompletableFuture.completedFuture(
                        plugin().packManager().resourcePackHosts().keySet().stream().map(Suggestion::suggestion).toList()))
                .handler(context -> {
                    MultiplePlayerSelector selector = context.get("player");
                    String pack = context.get("pack");
                    PackPreferencePlayerCommand.Action action = context.get("action");
                    List<Player> players = List.copyOf(selector.values());
                    String singleName = players.size() == 1 ? players.getFirst().getName() : null;
                    List<CompletableFuture<Boolean>> operations = new ArrayList<>(players.size());
                    for (Player player : players) {
                        UUID playerId = player.getUniqueId();
                        operations.add(plugin().packManager().setPackPreference(playerId, pack, action.state()).whenComplete((changed, error) -> {
                            if (error != null) {
                                plugin().logger().warn("Failed to save resource pack preference for " + playerId, error);
                            }
                        }));
                    }
                    PackCommandResult.collect(operations).thenAccept(result -> {
                        boolean single = singleName != null;
                        if (result.failed() > 0) {
                            handleFeedback(context, single ? MessageConstants.COMMAND_ADMIN_PACK_PREFERENCE_FAILURE_SINGLE : MessageConstants.COMMAND_ADMIN_PACK_PREFERENCE_FAILURE_MULTIPLE,
                                    single ? Component.text(singleName) : Component.text(result.updated()), Component.text(pack), Component.text(result.failed()));
                        } else if (result.updated() == 0) {
                            handleFeedback(context, single ? MessageConstants.COMMAND_ADMIN_PACK_PREFERENCE_UNCHANGED_SINGLE : MessageConstants.COMMAND_ADMIN_PACK_PREFERENCE_UNCHANGED_MULTIPLE,
                                    single ? Component.text(singleName) : Component.text(result.unchanged()), Component.text(pack));
                        } else {
                            handleFeedback(context, switch (action) {
                                case ENABLE ->
                                        single ? MessageConstants.COMMAND_ADMIN_PACK_PREFERENCE_ENABLED_SINGLE : MessageConstants.COMMAND_ADMIN_PACK_PREFERENCE_ENABLED_MULTIPLE;
                                case DISABLE ->
                                        single ? MessageConstants.COMMAND_ADMIN_PACK_PREFERENCE_DISABLED_SINGLE : MessageConstants.COMMAND_ADMIN_PACK_PREFERENCE_DISABLED_MULTIPLE;
                                case RESET ->
                                        single ? MessageConstants.COMMAND_ADMIN_PACK_PREFERENCE_RESET_SINGLE : MessageConstants.COMMAND_ADMIN_PACK_PREFERENCE_RESET_MULTIPLE;
                            }, single ? Component.text(singleName) : Component.text(result.updated()), Component.text(pack));
                        }
                    });
                });
    }

    @Override
    public String getFeatureID() {
        return "pack_preference_admin";
    }
}
