package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;

import java.util.List;

public class TestCommand extends BukkitCommandFeature<CommandSender> {

    public TestCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .handler(context -> {
                    List<Holder<Key>> holders = plugin().itemManager().tagToItems(Key.of("minecraft:planks"));
                    for (Holder<Key> holder : holders) {
                        context.sender().sendMessage(holder.registeredName());
                    }
                });
    }

    @Override
    public String getFeatureID() {
        return "debug_test";
    }
}
