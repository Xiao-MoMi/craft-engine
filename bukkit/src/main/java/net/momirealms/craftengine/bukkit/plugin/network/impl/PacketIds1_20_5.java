package net.momirealms.craftengine.bukkit.plugin.network.impl;

import net.momirealms.craftengine.bukkit.plugin.network.PacketIds;

public class PacketIds1_20_5 implements PacketIds {

    @Override
    public int clientboundBlockUpdatePacket() {
        return 9;
    }

    @Override
    public int clientboundSectionBlocksUpdatePacket() {
        return 73;
    }

    @Override
    public int clientboundLevelParticlesPacket() {
        return 41;
    }

    @Override
    public int clientboundLevelEventPacket() {
        return 40;
    }

    @Override
    public int clientboundAddEntityPacket() {
        return 1;
    }
}
