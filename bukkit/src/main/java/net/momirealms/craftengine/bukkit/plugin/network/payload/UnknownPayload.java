package net.momirealms.craftengine.bukkit.plugin.network.payload;

import io.netty.buffer.ByteBuf;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.NetworkReflections;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;

public record UnknownPayload(Key channel, ByteBuf rawPayload) implements Payload{

    public static UnknownPayload from(Object payload) {
        try {
            Object id = NetworkReflections.field$UnknownPayload$id.get(payload);
            ByteBuf data = (ByteBuf) NetworkReflections.field$UnknownPayload$data.get(payload);
            Key channel = KeyUtils.resourceLocationToKey(id);
            return new UnknownPayload(channel, data);
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to create UnknownPayload", e);
            return null;
        }
    }

    @Override
    public FriendlyByteBuf toBuffer() {
        return new FriendlyByteBuf(rawPayload);
    }
}
