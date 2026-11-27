package net.momirealms.craftengine.core.item.network;

import io.netty.buffer.ByteBuf;

@FunctionalInterface
public interface ClientboundItemRewriter {

    void rewrite(ByteBuf source, ByteBuf target);
}