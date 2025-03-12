package net.momirealms.craftengine.bukkit.plugin.network;

public interface PacketIds {

    int clientboundBlockUpdatePacket();

    int clientboundSectionBlocksUpdatePacket();

    int clientboundLevelParticlesPacket();

    int clientboundLevelEventPacket();

    int clientboundAddEntityPacket();
}
