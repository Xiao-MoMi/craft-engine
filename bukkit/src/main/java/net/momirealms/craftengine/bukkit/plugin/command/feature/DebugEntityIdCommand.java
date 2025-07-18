package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.parser.WorldParser;
import org.incendo.cloud.parser.standard.IntegerParser;

public class DebugEntityIdCommand extends BukkitCommandFeature<CommandSender> {

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
                    Object entityLookup = FastNMS.INSTANCE.method$ServerLevel$getEntityLookup(FastNMS.INSTANCE.field$CraftWorld$ServerLevel(world));
                    Object entity = FastNMS.INSTANCE.method$EntityLookup$get(entityLookup, entityId);
                    if (entity == null) {
                        context.sender().sendMessage("entity not found");
                        return;
                    }
                    context.sender().sendMessage(entity.toString());
                });
    }

    @Override
    public String getFeatureID() {
        return "debug_entity_id";
    }
}