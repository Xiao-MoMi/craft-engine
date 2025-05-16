package net.momirealms.craftengine.bukkit.util;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TagUtils {

    private TagUtils() {}

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
    @SuppressWarnings("unchecked")
    public static Object createUpdateTagsPacket(Map<Object, List<TagEntry>> tags) {
        Map<Object, Object> registriesNetworkPayload = (Map<Object, Object>) FastNMS.INSTANCE.method$TagNetworkSerialization$serializeTagsToNetwork();
        Map<Object, Object> modified = new HashMap<>();
        for (Map.Entry<Object, List<TagEntry>> entry : tags.entrySet()) {
            Object existingPayload = registriesNetworkPayload.get(entry.getKey());
            if (existingPayload == null) continue;
            FriendlyByteBuf deserializeBuf = new FriendlyByteBuf(Unpooled.buffer());
            FastNMS.INSTANCE.method$TagNetworkSerialization$NetworkPayload$write(existingPayload, deserializeBuf);
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
            for (TagEntry tagEntry : entry.getValue()) {
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
            Object mergedPayload = FastNMS.INSTANCE.method$TagNetworkSerialization$NetworkPayload$read(serializeBuf);
            modified.put(entry.getKey(), mergedPayload);
        }
        return FastNMS.INSTANCE.constructor$ClientboundUpdateTagsPacket(modified);
    }

    public record TagEntry(int id, List<String> tags) {
    }
}
