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
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PackPresetAdminCommand extends BukkitCommandFeature<CommandSender> {
    public PackPresetAdminCommand(CraftEngineCommandManager<CommandSender> manager, CraftEngine plugin) {
        super(manager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .flag(FlagKeys.SILENT_FLAG)
                .required("player", MultiplePlayerSelectorParser.multiplePlayerSelectorParser(false))
                .required("preset", StringParser.stringParser(), (context, input) -> CompletableFuture.completedFuture(
                        plugin().packManager().packPresets().keySet().stream().map(Suggestion::suggestion).toList()))
                .handler(context -> {
                    String preset = context.get("preset");
                    if (!plugin().packManager().packPresets().containsKey(preset)) {
                        handleFeedback(context, MessageConstants.COMMAND_PACK_PRESET_UNKNOWN, Component.text(preset));
                        return;
                    }
                    MultiplePlayerSelector selector = context.get("player");
                    List<Player> players = List.copyOf(selector.values());
                    String singleName = players.size() == 1 ? players.getFirst().getName() : null;
                    List<CompletableFuture<Boolean>> operations = new ArrayList<>(players.size());
                    for (Player player : players) {
                        UUID playerId = player.getUniqueId();
                        operations.add(plugin().packManager().applyPackPreset(playerId, preset).whenComplete((changed, error) -> {
                            if (error != null) {
                                plugin().logger().warn("Failed to apply resource pack preset " + preset + " for " + playerId, error);
                            }
                        }));
                    }
                    PackCommandResult.collect(operations).thenAccept(result -> {
                        boolean single = singleName != null;
                        if (result.failed() > 0) {
                            handleFeedback(context, single ? MessageConstants.COMMAND_ADMIN_PACK_PRESET_FAILURE_SINGLE : MessageConstants.COMMAND_ADMIN_PACK_PRESET_FAILURE_MULTIPLE,
                                    single ? Component.text(singleName) : Component.text(result.updated()), Component.text(preset), Component.text(result.failed()));
                        } else if (result.updated() == 0) {
                            handleFeedback(context, single ? MessageConstants.COMMAND_ADMIN_PACK_PRESET_UNCHANGED_SINGLE : MessageConstants.COMMAND_ADMIN_PACK_PRESET_UNCHANGED_MULTIPLE,
                                    single ? Component.text(singleName) : Component.text(result.unchanged()), Component.text(preset));
                        } else {
                            handleFeedback(context, single ? MessageConstants.COMMAND_ADMIN_PACK_PRESET_SUCCESS_SINGLE : MessageConstants.COMMAND_ADMIN_PACK_PRESET_SUCCESS_MULTIPLE,
                                    single ? Component.text(singleName) : Component.text(result.updated()), Component.text(preset));
                        }
                    });
                });
    }

    @Override
    public String getFeatureID() {
        return "pack_preset_admin";
    }
}
