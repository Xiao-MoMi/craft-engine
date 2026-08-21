package net.momirealms.craftengine.bukkit.plugin.agent;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class AgentBridge {
    /** 注册表注入回调，由织入 Bootstrap#validate/DispenserRegistry 的 advice 触发（旧版本服务端） */
    public static Runnable REGISTRY_INJECTION;
    /** 区块数据预热回调（{world, chunkPos, protoChunk}），由织入 SerializableChunkData#read 的 advice 触发 */
    public static volatile Consumer<Object[]> CHUNK_DATA_WARMUP;
    /** 实体装备变化回调（entity, changed equipment map），由原版装备变化收集方法触发 */
    public static volatile BiConsumer<Object, Object> EQUIPMENT_CHANGE;
    /** 商人交易物品匹配回调（requirement, offered stack），返回 false 时拒绝原版匹配 */
    public static volatile BiPredicate<Object, Object> MERCHANT_ITEM_MATCH;
    /** 旧版商人交易匹配回调（{offer, first stack, second stack}），返回 false 时拒绝原版匹配 */
    public static volatile Predicate<Object[]> MERCHANT_OFFER_MATCH;

    private AgentBridge() {}
}
