package net.momirealms.craftengine.bukkit.plugin.command.debug;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.sender.Sender;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.standard.IntegerParser;

public final class DebugInternalBlockStateCommand extends BukkitCommandFeature<CommandSender> {

    public DebugInternalBlockStateCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .required("id", IntegerParser.integerParser(0))
                .handler(context -> {
                    int id = context.get("id");
                    Sender sender = plugin().senderFactory().wrap(context.sender());
                    JsonElement json = plugin().blockManager().modBlockStates().get(id);
                    if (json == null) {
                        sender.sendMessage(DebugCommandOutput.error("No custom block state is mapped to craftengine:custom_" + id));
                        return;
                    }
                    int registryId = id + plugin().blockManager().vanillaBlockStateCount();
                    ImmutableBlockState state = plugin().blockManager().getImmutableBlockStateUnsafe(registryId);
                    sender.sendMessage(DebugCommandOutput.title("Internal Block State"));
                    sender.sendMessage(DebugCommandOutput.value("Internal ID", "craftengine:custom_" + id));
                    sender.sendMessage(DebugCommandOutput.value("Registry ID", registryId));
                    sender.sendMessage(DebugCommandOutput.value("Custom Block", state.owner().value().id().asString()));
                    sender.sendMessage(DebugCommandOutput.value("Properties", state.getPropertiesAsString()));
                    sender.sendMessage(DebugCommandOutput.value("Visual State", state.visualBlockState().getAsString()));
                });
    }

    @Override
    public String getFeatureID() {
        return "debug_internal_block_state";
    }
}
