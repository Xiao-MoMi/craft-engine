package net.momirealms.craftengine.bukkit.plugin.command.debug;

import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.core.pack.AbstractPackManager;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;

import java.io.IOException;

public final class DebugSaveDefaultResourcesCommand extends BukkitCommandFeature<CommandSender> {

    public DebugSaveDefaultResourcesCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .handler(context -> {
                    AbstractPackManager packManager = (AbstractPackManager) CraftEngine.instance().packManager();
                    try {
                        packManager.saveDefaultConfigs();
                    } catch (IOException e) {
                        context.sender().sendMessage("Failed to save default configs: " + e.getMessage());
                        return;
                    }
                    context.sender().sendMessage("Saved default configs");
                });
    }

    @Override
    public String getFeatureID() {
        return "debug_save_default_resources";
    }
}
