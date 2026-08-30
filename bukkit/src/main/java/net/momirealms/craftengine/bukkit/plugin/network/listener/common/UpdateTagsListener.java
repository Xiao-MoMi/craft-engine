package net.momirealms.craftengine.bukkit.plugin.network.listener.common;

import it.unimi.dsi.fastutil.ints.IntList;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.util.TagUtils;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;
import java.util.Map;

public final class UpdateTagsListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new UpdateTagsListener();
    private static final Key BLOCK = Key.minecraft("block");

    private UpdateTagsListener() {}

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        List<TagUtils.TagEntry> cachedUpdateTags = BukkitBlockManager.instance().cachedUpdateTags();
        if (cachedUpdateTags.isEmpty()) return;
        FriendlyByteBuf buf = event.getBuffer();
        Map<Key, Map<Key, IntList>> tags = buf.readMap(FriendlyByteBuf::readKey, it -> it.readMap(FriendlyByteBuf::readKey, FriendlyByteBuf::readIntIdList));
        Map<Key, IntList> payload = tags.get(BLOCK);
        if (payload == null) return; // 需要虚假的 block
        tags.put(BLOCK, TagUtils.mergeTagOverrides(payload, cachedUpdateTags));
        event.setChanged(true);
        buf.clear();
        buf.writeVarInt(event.packetID());
        buf.writeMap(tags, FriendlyByteBuf::writeKey, (b, m) -> b.writeMap(m, FriendlyByteBuf::writeKey, FriendlyByteBuf::writeIntIdList));
    }
}
