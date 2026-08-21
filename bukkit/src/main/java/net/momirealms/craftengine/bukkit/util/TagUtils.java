package net.momirealms.craftengine.bukkit.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MarkedHashMap;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.common.ClientboundUpdateTagsPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.FileToIdConverterProxy;
import net.momirealms.craftengine.proxy.minecraft.server.MinecraftServerProxy;
import net.momirealms.craftengine.proxy.minecraft.server.packs.PackTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.server.packs.repository.PackProxy;
import net.momirealms.craftengine.proxy.minecraft.server.packs.repository.PackRepositoryProxy;
import net.momirealms.craftengine.proxy.minecraft.server.packs.resources.MultiPackResourceManagerProxy;
import net.momirealms.craftengine.proxy.minecraft.server.packs.resources.ResourceProxy;
import net.momirealms.craftengine.proxy.minecraft.tags.TagNetworkSerializationProxy;

import java.io.Reader;
import java.util.*;

public final class TagUtils {
    private TagUtils() {}

    public record TagEntry(int id, Collection<Key> tags) {
    }

    public static Map<Key, IntList> mergeTagOverrides(Map<Key, IntList> originalTags, Collection<TagEntry> overrides) {
        Map<Integer, List<Key>> reversedTags = new HashMap<>();
        for (Map.Entry<Key, IntList> tagEntry : originalTags.entrySet()) {
            for (int id : tagEntry.getValue()) {
                reversedTags.computeIfAbsent(id, k -> new ArrayList<>()).add(tagEntry.getKey());
            }
        }
        for (TagEntry tagEntry : overrides) {
            reversedTags.remove(tagEntry.id);
            for (Key tag : tagEntry.tags) {
                reversedTags.computeIfAbsent(tagEntry.id, k -> new ArrayList<>()).add(tag);
            }
        }
        Map<Key, IntList> processedTags = new HashMap<>();
        // Named holder sets must still resolve on the client when a tag has no members.
        for (Key tag : originalTags.keySet()) {
            processedTags.put(tag, new IntArrayList());
        }
        for (Map.Entry<Integer, List<Key>> tagEntry : reversedTags.entrySet()) {
            for (Key tag : tagEntry.getValue()) {
                processedTags.computeIfAbsent(tag, k -> new IntArrayList()).addLast(tagEntry.getKey());
            }
        }
        return processedTags;
    }

    // 方块标签的嵌套关系：子标签 -> 直接引用它的父标签集合
    private static volatile Map<Key, Set<Key>> blockTagParents;

    /**
     * 重置方块标签嵌套缓存（服务端重载数据包后调用）
     */
    public static void resetBlockTagNesting() {
        blockTagParents = null;
    }

    /**
     * 将标签列表展开为其自身及全部祖先标签。
     * 原版在绑定标签时会将嵌套引用（如 armadillo_spawnable_on 引用 #animals_spawnable_on）展平，
     * 使方块同时隶属于所有祖先标签；CraftEngine 绑定时需做同样的展开。
     */
    public static Collection<Key> expandBlockTags(Collection<Key> tags) {
        if (tags.isEmpty()) return List.of();
        Map<Key, Set<Key>> nesting = blockTagNesting();
        if (nesting.isEmpty()) return tags;
        LinkedHashSet<Key> expanded = new LinkedHashSet<>(tags);
        Deque<Key> queue = new ArrayDeque<>(tags);
        while (!queue.isEmpty()) {
            for (Key parent : nesting.getOrDefault(queue.poll(), Set.of())) {
                if (expanded.add(parent)) {
                    queue.add(parent);
                }
            }
        }
        return List.copyOf(expanded);
    }

    public static Map<Key, Set<Key>> blockTagNesting() {
        Map<Key, Set<Key>> nesting = blockTagParents;
        if (nesting == null) {
            nesting = scanBlockTagNesting();
            blockTagParents = nesting;
        }
        return nesting;
    }

    /**
     * 扫描全部已启用数据包中的方块标签文件，提取标签间的嵌套引用关系。
     * 与原版 TagLoader.load 读取同一数据源，但不做展平，仅保留引用关系。
     * 返回的映射方向为：被引用的子标签 -> 引用它的父标签集合。
     * 例如 armadillo_spawnable_on.json 引用了 #animals_spawnable_on，
     * 则映射中包含 animals_spawnable_on -> {armadillo_spawnable_on}。
     */
    private static Map<Key, Set<Key>> scanBlockTagNesting() {
        Map<Key, Set<Key>> nesting = new HashMap<>();
        Object fileToIdConverter = FileToIdConverterProxy.INSTANCE.json(VersionHelper.isOrAbove1_21 ? "tags/block" : "tags/blocks");
        Object minecraftServer = MinecraftServerProxy.INSTANCE.getServer();
        Object packRepository = MinecraftServerProxy.INSTANCE.getPackRepository(minecraftServer);
        List<Object> selected = PackRepositoryProxy.INSTANCE.getSelected(packRepository);
        List<Object> packResources = new ArrayList<>();
        for (Object pack : selected) {
            packResources.add(PackProxy.INSTANCE.open(pack));
        }
        try (AutoCloseable resourceManager = (AutoCloseable) MultiPackResourceManagerProxy.INSTANCE.newInstance(PackTypeProxy.SERVER_DATA, packResources)) {
            Map<Object, List<Object>> scannedResourceStacks = FileToIdConverterProxy.INSTANCE.listMatchingResourceStacks(fileToIdConverter, resourceManager);
            for (Map.Entry<Object, List<Object>> entry : scannedResourceStacks.entrySet()) {
                Key tagId = KeyUtils.identifierToKey(FileToIdConverterProxy.INSTANCE.fileToId(fileToIdConverter, entry.getKey()));
                Set<Key> references = new HashSet<>();
                for (Object resource : entry.getValue()) {
                    try (Reader reader = ResourceProxy.INSTANCE.openAsReader(resource)) {
                        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                        if (json.has("replace") && json.get("replace").getAsBoolean()) {
                            references.clear();
                        }
                        JsonElement values = json.get("values");
                        if (values == null || !values.isJsonArray()) continue;
                        for (JsonElement value : values.getAsJsonArray()) {
                            String id = null;
                            if (value.isJsonPrimitive()) {
                                id = value.getAsString();
                            } else if (value.isJsonObject()) {
                                JsonElement idElement = value.getAsJsonObject().get("id");
                                if (idElement != null && idElement.isJsonPrimitive()) {
                                    id = idElement.getAsString();
                                }
                            }
                            if (id != null && id.startsWith("#")) {
                                references.add(Key.of(id.substring(1)));
                            }
                        }
                    } catch (Throwable e) {
                        CraftEngine.instance().logger().warn("Couldn't read block tag " + tagId + " from " + entry.getKey(), e);
                    }
                }
                for (Key referenced : references) {
                    nesting.computeIfAbsent(referenced, k -> new HashSet<>()).add(tagId);
                }
            }
        } catch (Throwable e) {
            CraftEngine.instance().logger().warn("Failed to scan block tag nesting from data packs", e);
        }
        return nesting;
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
            TagNetworkSerializationProxy.NetworkPayloadProxy.INSTANCE.write(payload.getValue(), PacketUtils.ensureNMSFriendlyByteBuf(deserializeBuf));
            Map<Key, IntList> originalTags = deserializeBuf.readMap(
                    FriendlyByteBuf::readKey,
                    FriendlyByteBuf::readIntIdList
            );
            Map<Key, IntList> processedTags = mergeTagOverrides(originalTags, overrides);
            FriendlyByteBuf serializeBuf = new FriendlyByteBuf(Unpooled.buffer());
            serializeBuf.writeMap(processedTags,
                    FriendlyByteBuf::writeKey,
                    FriendlyByteBuf::writeIntIdList
            );
            Object mergedPayload = TagNetworkSerializationProxy.NetworkPayloadProxy.INSTANCE.read(PacketUtils.ensureNMSFriendlyByteBuf(serializeBuf));
            modified.put(payload.getKey(), mergedPayload);
        }
        return ClientboundUpdateTagsPacketProxy.INSTANCE.newInstance(modified);
    }
}
