package net.momirealms.craftengine.core.plugin.network.mod.protocol;

import io.netty.buffer.Unpooled;
import net.momirealms.craftengine.core.plugin.network.mod.protocol.ClientboundCreativeModeTabCategoriesPacket.Entry;
import net.momirealms.craftengine.core.plugin.network.mod.protocol.ClientboundCreativeModeTabCategoriesPacket.TabCategory;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// 物品的编解码依赖平台实现，这里只覆盖不含物品（无图标、仅 ref 条目）的情况
class ClientboundCreativeModeTabCategoriesPacketTest {

    @Test
    void roundTrip() {
        ClientboundCreativeModeTabCategoriesPacket packet = new ClientboundCreativeModeTabCategoriesPacket(List.of(
                new TabCategory(Key.of("default:default"), "Default", null, List.of(Entry.ref(0), Entry.ref(5), Entry.ref(127))),
                new TabCategory(Key.of("default:misc"), "杂项", null, List.of(Entry.ref(300)))
        ));
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ClientboundCreativeModeTabCategoriesPacket.CODEC.encode(buf, packet);
            ClientboundCreativeModeTabCategoriesPacket decoded = ClientboundCreativeModeTabCategoriesPacket.CODEC.decode(buf);
            assertEquals(packet, decoded);
            assertEquals(0, buf.readableBytes());
        } finally {
            buf.release();
        }
    }

    @Test
    void emptyRoundTrip() {
        ClientboundCreativeModeTabCategoriesPacket packet = new ClientboundCreativeModeTabCategoriesPacket(List.of());
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ClientboundCreativeModeTabCategoriesPacket.CODEC.encode(buf, packet);
            assertEquals(2, buf.readableBytes());
            assertEquals(packet, ClientboundCreativeModeTabCategoriesPacket.CODEC.decode(buf));
        } finally {
            buf.release();
        }
    }

    @Test
    void wireLayout() {
        ClientboundCreativeModeTabCategoriesPacket packet = new ClientboundCreativeModeTabCategoriesPacket(List.of(
                new TabCategory(Key.of("a:b"), "Title", null, List.of(Entry.ref(0), Entry.ref(1)))
        ));
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ClientboundCreativeModeTabCategoriesPacket.CODEC.encode(buf, packet);
            assertEquals(ClientboundCreativeModeTabCategoriesPacket.FORMAT_VERSION, buf.readVarInt());
            assertEquals(1, buf.readVarInt());
            assertEquals("a:b", buf.readUtf());
            assertEquals("Title", buf.readUtf());
            assertFalse(buf.readBoolean());
            assertEquals(2, buf.readVarInt());
            assertEquals(1, buf.readVarInt());
            assertEquals(2, buf.readVarInt());
            assertEquals(0, buf.readableBytes());
        } finally {
            buf.release();
        }
    }

    @Test
    void rejectsUnknownFormatVersion() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buf.writeVarInt(ClientboundCreativeModeTabCategoriesPacket.FORMAT_VERSION + 1);
            buf.writeVarInt(0);
            assertThrows(Exception.class, () -> ClientboundCreativeModeTabCategoriesPacket.CODEC.decode(buf));
        } finally {
            buf.release();
        }
    }
}
