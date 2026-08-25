package net.momirealms.craftengine.bukkit.plugin.command.debug;

import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.sender.Sender;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.parser.standard.StringParser;

public final class DebugGetBlockStateRegistryIdCommand extends BukkitCommandFeature<CommandSender> {

    public DebugGetBlockStateRegistryIdCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .required("state", StringParser.greedyStringParser())
                .handler(context -> {
                    String state = context.get("state");
                    BlockData blockData = Bukkit.createBlockData(state);
                    int id = BlockStateUtils.blockDataToId(blockData);
                    Sender sender = plugin().senderFactory().wrap(context.sender());
                    sender.sendMessage(DebugCommandOutput.title("Block State Registry ID"));
                    sender.sendMessage(DebugCommandOutput.value("State", blockData.getAsString()));
                    sender.sendMessage(DebugCommandOutput.value("Registry ID", id));
                });
    }

    @Override
    public String getFeatureID() {
        return "debug_get_block_state_registry_id";
    }
}
