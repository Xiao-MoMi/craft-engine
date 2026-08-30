package net.momirealms.craftengine.bukkit.plugin.command.debug;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.sender.Sender;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

public final class DebugDimensionCommand extends BukkitCommandFeature<CommandSender> {

    public DebugDimensionCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .senderType(Player.class)
                .handler(context -> {
                    Player player = context.sender();
                    BukkitWorld world = BukkitAdaptor.adapt(player.getWorld());
                    Key dimension = world.dimension();
                    Object dimensionTypeHolder = LevelProxy.INSTANCE.getDimensionTypeRegistration(world.minecraftWorld());
                    String dimensionType = HolderProxy.ReferenceProxy.CLASS.isInstance(dimensionTypeHolder)
                            ? KeyUtils.unwrapHolder(dimensionTypeHolder).asString()
                            : "<unregistered>";

                    Sender sender = plugin().senderFactory().wrap(player);
                    sender.sendMessage(DebugCommandOutput.title("Dimension"));
                    sender.sendMessage(DebugCommandOutput.value("Bukkit world", world.name()));
                    sender.sendMessage(DebugCommandOutput.value("Dimension", dimension.asString()));
                    sender.sendMessage(DebugCommandOutput.value("Dimension type", dimensionType));
                });
    }

    @Override
    public String getFeatureID() {
        return "debug_dimension";
    }
}
