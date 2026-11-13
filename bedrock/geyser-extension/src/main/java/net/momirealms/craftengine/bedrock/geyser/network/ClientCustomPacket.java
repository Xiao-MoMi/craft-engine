package net.momirealms.craftengine.bedrock.geyser.network;

import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;

public interface ClientCustomPacket {

    String id();

    void handle(GeyserPlayer player);
}
