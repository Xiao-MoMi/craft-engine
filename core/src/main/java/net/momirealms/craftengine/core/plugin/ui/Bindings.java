package net.momirealms.craftengine.core.plugin.ui;

import net.momirealms.craftengine.core.util.ThrowableUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

@ApiStatus.Internal
public final class Bindings {
    private final CopyOnWriteArrayList<Binding> bindings = new CopyOnWriteArrayList<>(); // 写入由 suspendLock 保护
    private final Object suspendLock = new Object();
    private boolean suspended;  // 由 suspendLock 保护

    public Bindings() {
        this(false);
    }

    private Bindings(boolean suspended) {
        this.suspended = suspended;
    }

    /**
     * 创建一个出生即挂起的集合, 登记的声明先只记下来, 第一次 {@link #resumeAll()} 才挂上.
     * <p>给有打开期的宿主用, 例如 Window.
     *
     * @return 新集合
     */
    @NotNull
    public static Bindings suspended() {
        return new Bindings(true);
    }

    /**
     * 收下一条声明并立即建立订阅, 之后跟随宿主挂起与恢复.
     * <p>处于挂起状态时只保存声明, 恢复时再建立订阅.
     *
     * @param subscribe 建立订阅的方式, 每次挂上都会调用一次
     * @return 控制句柄, 跨挂起与恢复始终有效, 只能用来解绑或查状态
     */
    @NotNull
    public Subscription bind(@NotNull Supplier<? extends Subscription> subscribe) {
        Binding binding = new Binding(subscribe);
        // 挂载与挂起标志必须一起看, 否则与并发的 suspendAll 抢跑会留下一条关闭之后才建立的订阅
        synchronized (this.suspendLock) {
            this.bindings.removeIf(Binding::isClosed);
            if (!this.suspended) {
                binding.attach();
            }
            this.bindings.add(binding);
        }
        return new WeakHandle(new WeakReference<>(this), new WeakReference<>(binding));
    }

    /**
     * 挂起全部, 取消当前的全部订阅, 声明本身留着等恢复.
     * <p>宿主进入不需要接收失效的阶段时调用, 例如 Window 关闭.
     */
    public void suspendAll() {
        synchronized (this.suspendLock) {
            if (this.suspended) {
                return;
            }
            this.suspended = true;
            RuntimeException failure = null;
            for (int index = 0; index < this.bindings.size(); index++) {
                Binding binding = this.bindings.get(index);
                try {
                    binding.detach();
                } catch (RuntimeException exception) {
                    failure = ThrowableUtils.combine(failure, exception);
                }
            }
            if (failure != null) {
                throw failure;
            }
        }
    }

    /**
     * 取消挂起, 把全部声明重新进行订阅.
     * <p>中途失败会退回状态并取消本轮进行的订阅.
     */
    public void resumeAll() {
        synchronized (this.suspendLock) {
            if (!this.suspended) {
                return;
            }
            this.suspended = false;
            try {
                for (int index = 0; index < this.bindings.size(); index++) {
                    this.bindings.get(index).attach();
                }
            } catch (RuntimeException | Error exception) {
                this.suspended = true;
                // 摘除失败附到挂载失败上一起抛出
                for (int index = 0; index < this.bindings.size(); index++) {
                    Binding binding = this.bindings.get(index);
                    try {
                        binding.detach();
                    } catch (RuntimeException | Error detachFailure) {
                        exception.addSuppressed(detachFailure);
                    }
                }
                throw exception;
            }
        }
    }

    // 删除一条已经关闭的声明.
    private void discard(@NotNull Binding binding) {
        this.bindings.remove(binding);
    }

    // 一条声明, 记着怎样建立订阅, 当次凭证随挂起与恢复来回建立.
    private final class Binding implements Subscription {
        private final Supplier<? extends Subscription> subscribe;
        @Nullable private volatile Subscription active;  // 当次凭证, 写入由 suspendLock 保护
        private volatile boolean closed;

        private Binding(Supplier<? extends Subscription> subscribe) {
            this.subscribe = subscribe;
        }

        // 建立当次订阅, 已经挂着或已关闭时无操作. 调用方持有 suspendLock.
        private void attach() {
            if (this.closed || this.active != null) {
                return;
            }
            this.active = this.subscribe.get();
        }

        // 摘掉当次订阅, 声明本身留着. 调用方持有 suspendLock.
        private void detach() {
            Subscription current = this.active;
            this.active = null;
            if (current != null) {
                current.close();
            }
        }

        // 声明自己关了, 或者当次凭证被来源关掉了(来源终止), 都算关闭.
        @Override
        public boolean isClosed() {
            if (this.closed) {
                return true;
            }
            Subscription current = this.active;
            return current != null && current.isClosed();
        }

        @Override
        public void close() {
            synchronized (Bindings.this.suspendLock) {
                if (this.closed) {
                    return;
                }
                this.closed = true;
                this.detach();
            }
        }
    }

    private record WeakHandle(WeakReference<Bindings> owner, WeakReference<Binding> target) implements Subscription {

        @Override
        public boolean isClosed() {
            @Nullable Binding binding = this.target.get();
            return binding == null || binding.isClosed();
        }

        @Override
        public void close() {
            @Nullable Binding binding = this.target.get();
            if (binding == null) {
                return;
            }
            binding.close();
            // 顺手从持有方摘掉.
            @Nullable Bindings bindings = this.owner.get();
            if (bindings != null) {
                bindings.discard(binding);
            }
        }
    }
}
