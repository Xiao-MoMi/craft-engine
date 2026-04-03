package net.momirealms.craftengine.bukkit.plugin.network.mod;

import net.momirealms.craftengine.bukkit.plugin.network.mod.protocol.CancelBlockUpdatePacket;
import net.momirealms.craftengine.bukkit.plugin.network.mod.protocol.ClientBlockStateSizePacket;
import net.momirealms.craftengine.bukkit.plugin.network.mod.protocol.ClientCustomBlockPacket;
import net.momirealms.craftengine.bukkit.plugin.network.mod.protocol.VisualBlockStatePacket;
import net.momirealms.craftengine.core.plugin.network.mod.ModPacketType;
import net.momirealms.craftengine.core.plugin.network.mod.ModPackets;

public final class BukkitModPackets extends ModPackets {
    public static final ModPacketType<ClientCustomBlockPacket> CLIENT_CUSTOM_BLOCK = register(ClientCustomBlockPacket.TYPE, ClientCustomBlockPacket.CODEC);
    public static final ModPacketType<CancelBlockUpdatePacket> CANCEL_BLOCK_UPDATE = register(CancelBlockUpdatePacket.TYPE, CancelBlockUpdatePacket.CODEC);
    public static final ModPacketType<ClientBlockStateSizePacket> CLIENT_BLOCK_STATE_SIZE = register(ClientBlockStateSizePacket.TYPE, ClientBlockStateSizePacket.CODEC);
    public static final ModPacketType<VisualBlockStatePacket> VISUAL_BLOCK_STATE = register(VisualBlockStatePacket.TYPE, VisualBlockStatePacket.CODEC);

    private BukkitModPackets() {
    }

    public static void init() {
    }
}
