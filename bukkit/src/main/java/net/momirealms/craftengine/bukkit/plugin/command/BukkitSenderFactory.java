package net.momirealms.craftengine.bukkit.plugin.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.core.plugin.command.sender.Sender;
import net.momirealms.craftengine.core.plugin.command.sender.SenderFactory;
import net.momirealms.craftengine.core.util.Tristate;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BukkitSenderFactory extends SenderFactory<BukkitCraftEngine, CommandSender> {

    public BukkitSenderFactory(BukkitCraftEngine plugin) {
        super(plugin);
    }

    @Override
    protected String name(CommandSender sender) {
        if (sender instanceof Player) {
            return sender.getName();
        }
        return Sender.CONSOLE_NAME;
    }

    @Override
    protected UUID uniqueId(CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).getUniqueId();
        }
        return Sender.CONSOLE_UUID;
    }

    @Override
    protected void sendMessage(CommandSender sender, Component message) {
        // we can safely send async for players and the console - otherwise, send it sync
        if (sender instanceof Player player) {
            FastNMS.INSTANCE.method$Connection$send(
                    FastNMS.INSTANCE.field$ServerGamePacketListenerImpl$connection(FastNMS.INSTANCE.field$Player$connection(FastNMS.INSTANCE.method$CraftPlayer$getHandle(player))),
                    FastNMS.INSTANCE.constructor$ClientboundSystemChatPacket(ComponentUtils.adventureToMinecraft(message), false));
        } else if (sender instanceof ConsoleCommandSender commandSender) {
            commandSender.sendMessage(LegacyComponentSerializer.legacySection().serialize(message));
        } else if (sender instanceof RemoteConsoleCommandSender commandSender) {
            commandSender.sendMessage(LegacyComponentSerializer.legacySection().serialize(message));
        } else {
            String legacy = LegacyComponentSerializer.legacySection().serialize(message);
            plugin().scheduler().sync().run(() -> sender.sendMessage(legacy));
        }
    }

    @Override
    protected Tristate permissionState(CommandSender sender, String node) {
        if (sender.hasPermission(node)) {
            return Tristate.TRUE;
        } else if (sender.isPermissionSet(node)) {
            return Tristate.FALSE;
        } else {
            return Tristate.UNDEFINED;
        }
    }

    @Override
    protected boolean hasPermission(CommandSender sender, String node) {
        return sender.hasPermission(node);
    }

    @Override
    protected void performCommand(CommandSender sender, String command) {
        plugin().javaPlugin().getServer().dispatchCommand(sender, command);
    }

    @Override
    protected boolean isConsole(CommandSender sender) {
        return sender instanceof ConsoleCommandSender || sender instanceof RemoteConsoleCommandSender;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <C extends CommandSender> C consoleCommandSender() {
        return (C) Bukkit.getConsoleSender();
    }

    @Override
    public void close() {
        super.close();
    }
}
