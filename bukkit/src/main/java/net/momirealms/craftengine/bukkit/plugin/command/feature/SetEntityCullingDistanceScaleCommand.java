package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.FlagKeys;
import net.momirealms.craftengine.core.plugin.locale.MessageConstants;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.bukkit.data.SinglePlayerSelector;
import org.incendo.cloud.bukkit.parser.selector.SinglePlayerSelectorParser;
import org.incendo.cloud.parser.standard.DoubleParser;

public final class SetEntityCullingDistanceScaleCommand extends BukkitCommandFeature<CommandSender> {

    public SetEntityCullingDistanceScaleCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .flag(FlagKeys.SILENT_FLAG)
                .required("player", SinglePlayerSelectorParser.singlePlayerSelectorParser())
                .required("scale", DoubleParser.doubleParser(0.125, 8))
                .handler(context -> {
                    double scale = context.get("scale");
                    SinglePlayerSelector playerSelector = context.get("player");
                    BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(playerSelector.single());
                    if (serverPlayer == null) return;
                    serverPlayer.setEntityCullingDistanceScale(scale);
                    handleFeedback(context, MessageConstants.COMMAND_ENTITY_CULLING_DISTANCE_SCALE_SET_SUCCESS, Component.text(scale), Component.text(serverPlayer.name()));
                });
    }

    @Override
    public String getFeatureID() {
        return "set_entity_culling_distance_scale";
    }
}
