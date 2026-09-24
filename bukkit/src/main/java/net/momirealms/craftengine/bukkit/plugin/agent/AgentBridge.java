package net.momirealms.craftengine.bukkit.plugin.agent;

import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class AgentBridge {
    /** 注册表注入回调，由织入 Bootstrap#validate/DispenserRegistry 的 advice 触发（旧版本服务端） */
    public static Runnable REGISTRY_INJECTION;
    /** 区块数据预热回调（{world, chunkPos, protoChunk}），由织入 SerializableChunkData#read 的 advice 触发 */
    public static volatile Consumer<Object[]> CHUNK_DATA_WARMUP;
    public static volatile Consumer<Object[]> CHUNK_LIFECYCLE_START;
    public static volatile Function<Object, Object> CHUNK_LIFECYCLE_CONTEXT;
    public static volatile BiConsumer<Object, Object> CHUNK_LIFECYCLE_READ;
    public static volatile BiConsumer<Object, Object> CHUNK_LIFECYCLE_EMPTY;
    public static volatile Consumer<Object> CHUNK_LIFECYCLE_COMPLETE;
    public static volatile Consumer<Object[]> CHUNK_LIFECYCLE_RELEASE;
    /** 实体装备变化回调（entity, changed equipment map），由原版装备变化收集方法触发 */
    public static volatile BiConsumer<Object, Object> EQUIPMENT_CHANGE;
    /** 世界实体追踪开始/结束回调（server level, entity），仅在 Spigot 织入。 */
    public static volatile BiConsumer<Object, Object> ENTITY_ADDED_TO_WORLD;
    public static volatile BiConsumer<Object, Object> ENTITY_REMOVED_FROM_WORLD;
    /** 商人交易物品匹配回调（requirement, offered stack），返回 false 时拒绝原版匹配 */
    public static volatile BiPredicate<Object, Object> MERCHANT_ITEM_MATCH;
    /** 旧版商人交易匹配回调（{offer, first stack, second stack}），返回 false 时拒绝原版匹配 */
    public static volatile Predicate<Object[]> MERCHANT_OFFER_MATCH;

    private AgentBridge() {}

    static Class<?> inject(ClassLoader serverClassLoader, MethodHandles.Lookup lookup) {
        // Keep this class JDK-only: server advice cannot access the plugin's dependencies.
        // Define the same name in the server loader without Byte Buddy's Unsafe injection.
        try (InputStream input = AgentBridge.class.getResourceAsStream("/" + AgentBridge.class.getName().replace('.', '/') + ".class")) {
            if (input == null) throw new IllegalStateException("Missing agent bridge bytecode");
            byte[] bytes = input.readAllBytes();
            return (Class<?>) lookup.findVirtual(ClassLoader.class, "defineClass",
                            MethodType.methodType(Class.class, String.class, byte[].class, int.class, int.class))
                    .invoke(serverClassLoader, AgentBridge.class.getName(), bytes, 0, bytes.length);
        } catch (Throwable t) {
            throw new IllegalStateException("Failed to inject agent bridge", t);
        }
    }
}
