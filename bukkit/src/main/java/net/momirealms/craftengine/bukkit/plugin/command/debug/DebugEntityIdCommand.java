package net.momirealms.craftengine.bukkit.plugin.command.debug;

import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.util.LevelUtils;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.CraftWorldProxy;
import net.momirealms.craftengine.proxy.paper.chunk.system.entity.EntityLookupProxy;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.parser.WorldParser;
import org.incendo.cloud.parser.standard.IntegerParser;

public final class DebugEntityIdCommand extends BukkitCommandFeature<CommandSender> {

    public DebugEntityIdCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .required("world", WorldParser.worldParser())
                .required("entityId", IntegerParser.integerParser())
                .handler(context -> {
                    World world = context.get("world");
                    int entityId = context.get("entityId");
                    Object level = CraftWorldProxy.INSTANCE.getWorld(world);
                    Object entityLookup = LevelUtils.getEntityLookup(level);
                    Object entity = EntityLookupProxy.INSTANCE.get(entityLookup, entityId);
                    var sender = plugin().senderFactory().wrap(context.sender());
                    if (entity == null) {
                        sender.sendMessage(DebugCommandOutput.error("Entity was not found"));
                        sender.sendMessage(DebugCommandOutput.value("World", world.getName()));
                        sender.sendMessage(DebugCommandOutput.value("Entity ID", entityId));
                        return;
                    }
                    sender.sendMessage(DebugCommandOutput.title("Entity Lookup"));
                    sender.sendMessage(DebugCommandOutput.value("World", world.getName()));
                    sender.sendMessage(DebugCommandOutput.value("Entity ID", entityId));
                    sender.sendMessage(DebugCommandOutput.value("Entity", entity.toString()));
                });
    }

    @Override
    public String getFeatureID() {
        return "debug_entity_id";
    }
}
