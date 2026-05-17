package net.momirealms.craftengine.bukkit.plugin.command.debug;

import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.util.FileUtils;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DebugMigrateTemplatesCommand extends BukkitCommandFeature<CommandSender> {
    private static final Pattern PATTERN = Pattern.compile("(?<!\\$)\\{([0-9a-zA-Z_]+)}");

    public DebugMigrateTemplatesCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .handler(context -> {
                    for (Pack pack : BukkitCraftEngine.instance().packManager().loadedPacks()) {
                        for (Path file : FileUtils.getYmlConfigsDeeply(pack.configurationFolder())) {
                            try {
                                Files.writeString(file, replacePlaceholders(Files.readString(file)));
                            } catch (IOException e) {
                                plugin().logger().warn("Failed to write config file: " + file, e);
                            }
                        }
                    }
                    context.sender().sendMessage("Done");
                });
    }

    @Override
    public String getFeatureID() {
        return "debug_migrate_templates";
    }

    private static String replacePlaceholders(String input) {
        if (input == null) {
            return null;
        }
        Matcher matcher = PATTERN.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            // 将 {xxx} 替换为 ${xxx}
            matcher.appendReplacement(sb, "\\${" + matcher.group(1) + "}");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}