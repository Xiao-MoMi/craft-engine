package net.momirealms.craftengine.bukkit.pack;

import net.momirealms.craftengine.bukkit.util.ResourcePackUtils;
import net.momirealms.craftengine.core.pack.host.ResourcePackDownloadData;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.util.ReflectionUtils;
import net.momirealms.craftengine.proxy.minecraft.network.ConfigurationTaskProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.common.ServerboundResourcePackPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.server.network.ServerConfigurationPacketListenerImplProxy;
import net.momirealms.craftengine.proxy.minecraft.server.network.config.ServerResourcePackConfigurationTaskProxy;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 将一批资源包作为一个配置任务：start 连续发送所有下载请求，全部收到允许的终态后才结束任务。
 * 服务端的配置队列串行执行；若每个包各占一个任务，下一个包只能等上一个加载结束后才发送。
 * 使用运行时代理适配各版本的 NMS ConfigurationTask 接口。
 */
public final class ResourcePackConfigurationTask implements InvocationHandler {
    private final List<ResourcePackDownloadData> packs;
    // 按 UUID 等待成功或配置允许的拒绝/失败，允许乱序响应，避免重复结束任务。
    // 网络线程会读取待加载状态，平台线程会更新状态，因此状态访问统一加锁。
    private final Set<UUID> pending = new HashSet<>();
    // 失败后即使断线尚未完成，也不允许迟到的成功响应放行。
    private boolean failed;

    private ResourcePackConfigurationTask(List<ResourcePackDownloadData> packs) {
        this.packs = List.copyOf(packs);
        for (ResourcePackDownloadData pack : packs) {
            this.pending.add(pack.uuid());
        }
    }

    public static Object create(NetWorkUser user, List<ResourcePackDownloadData> packs) {
        ResourcePackConfigurationTask task = new ResourcePackConfigurationTask(packs);
        Object proxy = Proxy.newProxyInstance(ConfigurationTaskProxy.CLASS.getClassLoader(), new Class<?>[]{ConfigurationTaskProxy.CLASS}, task);
        // 必须先登记整批 UUID，再允许任务发送请求，避免快速响应被当成其他插件的资源包。
        for (ResourcePackDownloadData pack : packs) {
            user.addResourcePackUUID(pack.uuid());
        }
        return proxy;
    }

    @Nullable
    public static ResourcePackConfigurationTask from(Object task) {
        if (task != null && Proxy.isProxyClass(task.getClass()) && Proxy.getInvocationHandler(task) instanceof ResourcePackConfigurationTask batch) {
            return batch;
        }
        return null;
    }

    public synchronized boolean isPending(UUID id) {
        return !this.failed && this.pending.contains(id);
    }

    // 仅首次收齐整批允许的终态时返回 true；中间态和要求断线的失败不能调用此方法。
    public synchronized boolean markCompleted(UUID id) {
        return !this.failed && this.pending.remove(id) && this.pending.isEmpty();
    }

    public synchronized void fail() {
        this.failed = true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return switch (method.getName()) {
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "CraftEngine resource pack batch";
                default -> throw new UnsupportedOperationException(method.toString());
            };
        }
        // Paper 26.2 的 tick() 是返回 false 的默认方法，返回 true 会由服务端自动结束当前任务。
        // 保留接口默认实现，让整批完成逻辑独占放行入口；不能把所有无参方法都当成 type()。
        if (method.isDefault()) {
            return InvocationHandler.invokeDefault(proxy, method, args);
        }
        // 按签名识别 type()，兼容方法名映射；复用原版资源包任务类型供 finishCurrentTask 校验。
        if (method.getParameterCount() == 0 && method.getReturnType() == ServerResourcePackConfigurationTaskProxy.TYPE.getClass()) {
            return ServerResourcePackConfigurationTaskProxy.TYPE;
        }
        if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == Consumer.class && method.getReturnType() == void.class) {
            Consumer<Object> sender = (Consumer<Object>) args[0];
            // startNextTask 会先设置 currentTask 再调用 start，因此首个响应也能找到本批任务。
            // 在同一次 start 中按原顺序发送全部 push，不等待单包响应，保留资源包的叠加顺序。
            for (ResourcePackDownloadData pack : this.packs) {
                sender.accept(ResourcePackUtils.createPacket(pack.uuid(), pack.url(), pack.sha1()));
            }
            return null;
        }
        throw new UnsupportedOperationException(method.toString());
    }

    public static void handleResponse(Object listener, Object packet) throws Throwable {
        ResponseHandler.HANDLE.invoke(listener, packet);
    }

    private static final class ResponseHandler {
        private static final MethodHandle HANDLE;

        static {
            try {
                Class<?> commonListener = ServerConfigurationPacketListenerImplProxy.CLASS.getSuperclass();
                Method method = ReflectionUtils.getMethod(commonListener, void.class, ServerboundResourcePackPacketProxy.CLASS);
                // 配置监听器的覆写会在单包终态时尝试结束任务，不能直接调用该覆写。
                // 用 invokespecial 只执行父类处理，保留必选包拒绝检查和 Paper 的逐包回调。
                // 普通反射 invoke 仍会动态分派到子类；这里必须使用 unreflectSpecial。
                HANDLE = ReflectionUtils.LOOKUP.unreflectSpecial(method, commonListener);
            } catch (IllegalAccessException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }
}
