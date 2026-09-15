package net.momirealms.craftengine.core.plugin.ui;

import net.momirealms.craftengine.core.util.ThrowableUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public final class ObservableDispatcher<T> {
    private final CopyOnWriteArrayList<Entry<T>> entries = new CopyOnWriteArrayList<>();

    /**
     * 注册一个观察者.
     *
     * @param observer 观察者
     * @return 控制本次注册生命周期的订阅凭证
     */
    @NotNull
    public Subscription subscribe(@NotNull Observer<? super T> observer) {
        Objects.requireNonNull(observer, "observer");
        Entry<T> entry = new Entry<>(this, observer);
        this.entries.add(entry);
        return entry;
    }

    /**
     * 向当前所有观察者发布一次更新.
     *
     * @param update 更新内容
     * @throws RuntimeException 任一观察者失败时抛出首个异常, 其余异常作为 suppressed exception 附加
     */
    public void publish(T update) {
        RuntimeException failure = null;
        for (Entry<T> entry : this.entries) {
            Observer<? super T> observer = entry.observer();
            if (observer == null) {
                continue;
            }
            try {
                observer.onUpdate(update);
            } catch (RuntimeException exception) {
                failure = ThrowableUtils.combine(failure, exception);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    public int subscriptionCount() {
        return this.entries.size();
    }

    private void remove(Entry<T> entry) {
        this.entries.remove(entry);
    }

    private static final class Entry<T> implements Subscription {
        private final ObservableDispatcher<T> owner;
        private final AtomicReference<Observer<? super T>> observer;

        private Entry(ObservableDispatcher<T> owner, Observer<? super T> observer) {
            this.owner = owner;
            this.observer = new AtomicReference<>(observer);
        }

        @Nullable
        private Observer<? super T> observer() {
            return this.observer.get();
        }

        @Override
        public boolean isClosed() {
            return this.observer.get() == null;
        }

        @Override
        public void close() {
            if (this.observer.getAndSet(null) != null) {
                this.owner.remove(this);
            }
        }
    }
}
