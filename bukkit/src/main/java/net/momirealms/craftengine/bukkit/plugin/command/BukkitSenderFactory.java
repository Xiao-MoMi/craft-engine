package net.momirealms.craftengine.bukkit.plugin.command;

import io.netty.channel.SimpleChannelInboundHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.core.plugin.command.sender.Sender;
import net.momirealms.craftengine.core.plugin.command.sender.SenderFactory;
import net.momirealms.craftengine.core.util.Tristate;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.entity.CraftEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSystemChatPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerPlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.server.network.ServerCommonPacketListenerImplProxy;
import net.momirealms.craftengine.proxy.minecraft.server.network.ServerGamePacketListenerImplProxy;
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
        if (sender instanceof Player player) {
            return player.getUniqueId();
        }
        return Sender.CONSOLE_UUID;
    }

    @Override
    protected void sendMessage(CommandSender sender, Component message) {
        // we can safely send async for players and the console - otherwise, send it sync
        switch (sender) {
            case Player player -> {
                SimpleChannelInboundHandler<Object> connection;
                if (VersionHelper.isOrAbove1_20_2()) {
                    connection = ServerCommonPacketListenerImplProxy.INSTANCE.getConnection$0(ServerPlayerProxy.INSTANCE.getConnection(CraftEntityProxy.INSTANCE.getEntity(player)));
                } else {
                    connection = ServerGamePacketListenerImplProxy.INSTANCE.getConnection$1(ServerPlayerProxy.INSTANCE.getConnection(CraftEntityProxy.INSTANCE.getEntity(player)));
                }
                FastNMS.INSTANCE.method$Connection$send(
                        connection,
                        ClientboundSystemChatPacketProxy.INSTANCE.newInstance(ComponentUtils.adventureToMinecraft(message), false),
                        null
                );
            }
            case ConsoleCommandSender commandSender ->
                    commandSender.sendMessage(LegacyComponentSerializer.legacySection().serialize(message));
            case RemoteConsoleCommandSender commandSender ->
                    commandSender.sendMessage(LegacyComponentSerializer.legacySection().serialize(message));
            default -> {
                String legacy = LegacyComponentSerializer.legacySection().serialize(message);
                plugin().scheduler().sync().run(() -> sender.sendMessage(legacy));
            }
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
