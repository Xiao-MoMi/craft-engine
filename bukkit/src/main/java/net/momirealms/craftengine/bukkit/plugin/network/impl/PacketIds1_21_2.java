package net.momirealms.craftengine.bukkit.plugin.network.impl;

import net.momirealms.craftengine.bukkit.plugin.network.PacketIds;

public class PacketIds1_21_2 implements PacketIds {

    @Override
    public int clientboundBlockUpdatePacket() {
        return 9;
    }

    @Override
    public int clientboundSectionBlocksUpdatePacket() {
        return 78;
    }

    @Override
    public int clientboundLevelParticlesPacket() {
        return 42;
    }

    @Override
    public int clientboundLevelEventPacket() {
        return 41;
    }

    @Override
    public int clientboundAddEntityPacket() {
        return 1;
    }
}
