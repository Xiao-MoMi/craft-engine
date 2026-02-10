package net.momirealms.craftengine.bukkit.util;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.MarkedHashMap;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.common.ClientboundUpdateTagsPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.tags.TagNetworkSerializationProxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TagUtils {

    private TagUtils() {}

    public record TagEntry(int id, List<String> tags) {
    }

    /**
     * 构建模拟标签更新数据包（用于向客户端添加虚拟标签）
     *
     * @param tags 需要添加的标签数据，结构为嵌套映射：
     *               <pre>{@code
     *               Map结构示例:
     *               {
     *                 注册表键1 (如BuiltInRegistries.ITEM.key) -> {
     *                   "命名空间:值1" -> IntList.of(1, 2, 3),  // 该命名空间下生效的物品ID列表
     *                   "命名空间:值2" -> IntList.of(5, 7)
     *                 },
     *                 注册表键2 (如BuiltInRegistries.BLOCK.key) -> {
     *                   "minecraft:beacon_base_blocks" -> IntList.of(1024, 2048)
     *                 },
     *                 ....
     *               }
     *               }</pre>
     *               其中：</br>
     *               - 外层键：注册表ResourceKey</br>
     *               - 中间层键：标签的命名空间:值（字符串）</br>
     *               - 值：包含注册表内项目数字ID的IntList
     *
     * @return 可发送给客户端的 ClientboundUpdateTagsPacket 数据包对象
     */
    public static Object createUpdateTagsPacket(Map<Object, List<TagEntry>> tags, Map<Object, Object> existingTags) {
        Map<Object, Object> modified = new MarkedHashMap<>();
        for (Map.Entry<Object, Object> payload : existingTags.entrySet()) {
            List<TagEntry> overrides = tags.get(payload.getKey());
            if (overrides == null || overrides.isEmpty()) {
                modified.put(payload.getKey(), payload.getValue());
                continue;
            }
            FriendlyByteBuf deserializeBuf = new FriendlyByteBuf(Unpooled.buffer());
            TagNetworkSerializationProxy.NetworkPayloadProxy.INSTANCE.write(payload.getValue(), PacketUtils.wrapByteBuf(deserializeBuf));
            Map<String, IntList> originalTags = deserializeBuf.readMap(
                    FriendlyByteBuf::readUtf,
                    FriendlyByteBuf::readIntIdList
            );
            Map<Integer, List<String>> reversedTags = new HashMap<>();
            for (Map.Entry<String, IntList> tagEntry : originalTags.entrySet()) {
                for (int id : tagEntry.getValue()) {
                    reversedTags.computeIfAbsent(id, k -> new ArrayList<>()).add(tagEntry.getKey());
                }
            }
            for (TagEntry tagEntry : overrides) {
                reversedTags.remove(tagEntry.id);
                for (String tag : tagEntry.tags) {
                    reversedTags.computeIfAbsent(tagEntry.id, k -> new ArrayList<>()).add(tag);
                }
            }
            Map<String, IntList> processedTags = new HashMap<>();
            for (Map.Entry<Integer, List<String>> tagEntry : reversedTags.entrySet()) {
                for (String tag : tagEntry.getValue()) {
                    processedTags.computeIfAbsent(tag, k -> new IntArrayList()).addLast(tagEntry.getKey());
                }
            }
            FriendlyByteBuf serializeBuf = new FriendlyByteBuf(Unpooled.buffer());
            serializeBuf.writeMap(processedTags,
                    FriendlyByteBuf::writeUtf,
                    FriendlyByteBuf::writeIntIdList
            );
            Object mergedPayload = TagNetworkSerializationProxy.NetworkPayloadProxy.INSTANCE.read(PacketUtils.wrapByteBuf(serializeBuf));
            modified.put(payload.getKey(), mergedPayload);
        }
        return ClientboundUpdateTagsPacketProxy.INSTANCE.newInstance(modified);
    }
}
