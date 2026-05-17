package net.momirealms.craftengine.bukkit.plugin.network.listener.common;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.util.TagUtils;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;

import java.util.ArrayList;
import java.util.HashMap;
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
        Map<Integer, List<Key>> reversedTags = new HashMap<>();
        for (Map.Entry<Key, IntList> tagEntry : payload.entrySet()) {
            for (int id : tagEntry.getValue()) {
                reversedTags.computeIfAbsent(id, k -> new ArrayList<>()).add(tagEntry.getKey());
            }
        }
        for (TagUtils.TagEntry tagEntry : cachedUpdateTags) {
            reversedTags.remove(tagEntry.id());
            for (Key tag : tagEntry.tags()) {
                reversedTags.computeIfAbsent(tagEntry.id(), k -> new ArrayList<>()).add(tag);
            }
        }
        Map<Key, IntList> processedTags = new HashMap<>();
        for (Map.Entry<Integer, List<Key>> tagEntry : reversedTags.entrySet()) {
            for (Key tag : tagEntry.getValue()) {
                processedTags.computeIfAbsent(tag, k -> new IntArrayList()).addLast(tagEntry.getKey());
            }
        }
        tags.put(BLOCK, processedTags);
        event.setChanged(true);
        buf.clear();
        buf.writeVarInt(event.packetID());
        buf.writeMap(tags, FriendlyByteBuf::writeKey, (b, m) -> b.writeMap(m, FriendlyByteBuf::writeKey, FriendlyByteBuf::writeIntIdList));
    }
}
