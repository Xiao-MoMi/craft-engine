package net.momirealms.craftengine.core.plugin.ui.state;

import net.momirealms.craftengine.core.plugin.ui.Subscription;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

// 集合装饰器共用版本与 batch 通知逻辑
abstract sealed class CollectionSignal<T> extends AbstractSignal<T> permits ListSignalImpl, SetSignalImpl, MapSignalImpl {
    private final AtomicLong version = new AtomicLong();
    private final ReentrantLock batchLock = new ReentrantLock();
    private boolean batchPending;   // 当前线程的 batch 已发生变更, 只在持锁时读写

    // 批量移除会反复查询实参集合, 将非 Set 转成哈希查找
    @NotNull
    static Collection<?> lookupOf(@NotNull Collection<?> c) {
        return c instanceof Set<?> ? c : new HashSet<>(c);
    }

    public final void batch(@NotNull Runnable changes) {
        this.batchLock.lock();
        try {
            changes.run();
        } finally {
            // changes 抛出时也通知已经落地的变更
            boolean notify = false;
            if (this.batchLock.getHoldCount() == 1) {
                notify = this.batchPending;
                this.batchPending = false;
            }
            this.batchLock.unlock();
            if (notify) {
                this.notifyDirty();
            }
        }
    }

    // 变更落地后推进版本, 当前线程处于 batch 时延后通知
    final void changed() {
        this.version.incrementAndGet();
        if (this.batchLock.isHeldByCurrentThread()) {
            this.batchPending = true;
            return;
        }
        this.notifyDirty();
    }

    @Override
    final long version() {
        return this.version.get();
    }

    // 按身份判等, 防止依赖 signal 身份的弱表与 merging 混淆内容相同的两个包装器
    @Override
    public final boolean equals(Object o) {
        return this == o;
    }

    @Override
    public final int hashCode() {
        return System.identityHashCode(this);
    }

    // 元素钩子表与失效订阅使用同一套寿命. 凭证被回收或关闭后, 弱持有的钩子条目在下次清扫时消失.
    static final class ElementHooks<H> {
        private final ReferenceQueue<H> deadHooks = new ReferenceQueue<>();
        private final CopyOnWriteArrayList<HookReference<H>> hooks = new CopyOnWriteArrayList<>();
        private volatile boolean active; // 没挂过钩子的集合在写路径上只读这一个标志

        // 注册后按注册顺序参与串联, 返回的凭证是钩子的唯一强引用.
        @NotNull
        Subscription register(@NotNull H hook) {
            this.reap();
            HookEntry<H> entry = new HookEntry<>(this, hook);
            this.hooks.add(entry.reference);
            this.active = true;
            return entry;
        }

        boolean active() {
            return this.active;
        }

        // 按注册顺序遍历, 已经消亡的钩子在这里读出 null, 调用方跳过即可.
        @NotNull
        Iterable<HookReference<H>> live() {
            return this.hooks;
        }

        // 凭证被回收后条目仍留在表里, 注册与显式关闭时顺带清扫.
        void reap() {
            Reference<? extends H> dead = this.deadHooks.poll();
            if (dead == null) {
                return;
            }
            do {
                if (dead instanceof HookReference<?> reference) {
                    this.hooks.remove(reference);
                }
            } while ((dead = this.deadHooks.poll()) != null);
            this.active = !this.hooks.isEmpty();
        }

        private void unregister(HookReference<H> reference) {
            this.hooks.remove(reference);
            this.active = !this.hooks.isEmpty();
        }
    }

    // 弱持有钩子本身, 凭证消亡即视为退订.
    static final class HookReference<H> extends WeakReference<H> {
        private final ElementHooks<H> owner;

        private HookReference(H hook, ElementHooks<H> owner) {
            super(hook, owner.deadHooks);
            this.owner = owner;
        }
    }

    // 调用方持有的凭证, 同时是钩子的唯一强引用.
    private static final class HookEntry<H> implements Subscription {
        private final AtomicBoolean closed = new AtomicBoolean();
        private final HookReference<H> reference;
        @SuppressWarnings("unused") private final H strongHook; // 凭证活着钩子就活着

        private HookEntry(ElementHooks<H> owner, H hook) {
            this.strongHook = hook;
            this.reference = new HookReference<>(hook, owner);
        }

        @Override
        public boolean isClosed() {
            return this.closed.get();
        }

        @Override
        public void close() {
            if (this.closed.compareAndSet(false, true)) {
                this.reference.owner.unregister(this.reference);
            }
        }
    }
}
