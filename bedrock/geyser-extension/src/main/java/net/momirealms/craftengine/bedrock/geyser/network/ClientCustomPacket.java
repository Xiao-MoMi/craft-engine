package net.momirealms.craftengine.bedrock.geyser.network;

import org.geysermc.geyser.session.GeyserSession;

public interface ClientCustomPacket {

    String id();

    void handle(GeyserSession session);
}
