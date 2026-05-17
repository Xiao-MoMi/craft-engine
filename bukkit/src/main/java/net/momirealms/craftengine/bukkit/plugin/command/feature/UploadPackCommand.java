package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.core.pack.host.ResourcePackHost;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.locale.MessageConstants;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;

public final class UploadPackCommand extends BukkitCommandFeature<CommandSender> {

    public UploadPackCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .handler(context -> {
                    ResourcePackHost host = plugin().packManager().resourcePackHost();
                    if (host.canUpload()) {
                        handleFeedback(context, MessageConstants.COMMAND_UPLOAD_ON_PROGRESS);
                        plugin().packManager().uploadResourcePack();
                    } else {
                        handleFeedback(context, MessageConstants.COMMAND_UPLOAD_FAILURE_NOT_SUPPORTED, Component.text(host.type().id().value()));
                    }
                });
    }

    @Override
    public String getFeatureID() {
        return "upload";
    }
}
