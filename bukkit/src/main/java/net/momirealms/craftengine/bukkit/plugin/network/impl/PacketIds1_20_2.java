package net.momirealms.craftengine.bukkit.plugin.network.impl;

import net.momirealms.craftengine.bukkit.plugin.network.PacketIds;

public class PacketIds1_20_2 implements PacketIds {

    @Override
    public int clientboundBlockUpdatePacket() {
        return 9;
    }

    @Override
    public int clientboundSectionBlocksUpdatePacket() {
        return 69;
    }

    @Override
    public int clientboundLevelParticlesPacket() {
        return 39;
    }

    @Override
    public int clientboundAddEntityPacket() {
        return 1;
    }
}
