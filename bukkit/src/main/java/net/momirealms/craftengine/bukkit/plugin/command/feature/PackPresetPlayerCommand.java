package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.locale.MessageConstants;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;

import java.util.concurrent.CompletableFuture;

public final class PackPresetPlayerCommand extends BukkitCommandFeature<CommandSender> {
    public static final String PRESET_PERMISSION_PREFIX = "ce.resourcepack.preset.";

    public PackPresetPlayerCommand(CraftEngineCommandManager<CommandSender> manager, CraftEngine plugin) {
        super(manager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .senderType(Player.class)
                .required("preset", StringParser.stringParser(), (context, input) -> CompletableFuture.completedFuture(
                        plugin().packManager().packPresets().keySet().stream()
                                .filter(name -> context.sender().hasPermission(PRESET_PERMISSION_PREFIX + name))
                                .map(Suggestion::suggestion).toList()))
                .handler(context -> {
                    String preset = context.get("preset");
                    Player sender = context.sender();
                    if (!plugin().packManager().packPresets().containsKey(preset)) {
                        handleFeedback(context, MessageConstants.COMMAND_PACK_PRESET_UNKNOWN, Component.text(preset));
                        return;
                    }
                    // 预设权限授权整个组合，避免一键切换受每个单包权限的组合影响。
                    if (!sender.hasPermission(PRESET_PERMISSION_PREFIX + preset)) {
                        handleFeedback(context, MessageConstants.COMMAND_PACK_PRESET_NO_PERMISSION, Component.text(preset));
                        return;
                    }
                    plugin().packManager().applyPackPreset(sender.getUniqueId(), preset).whenComplete((changed, error) -> {
                        if (error != null) {
                            plugin().logger().warn("Failed to apply resource pack preset " + preset + " for " + sender.getName(), error);
                        } else if (!changed) {
                            handleFeedback(context, MessageConstants.COMMAND_PACK_PRESET_UNCHANGED, Component.text(preset));
                        } else {
                            handleFeedback(context, MessageConstants.COMMAND_PACK_PRESET_SUCCESS, Component.text(preset));
                        }
                    });
                });
    }

    @Override
    public String getFeatureID() {
        return "pack_preset_player";
    }
}
