package net.momirealms.craftengine.core.plugin.ui.state;

import net.momirealms.craftengine.core.plugin.ui.Subscription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Function;

sealed class MapDistinctSignal<S, T> extends AbstractSignal<T> permits LensSignal {
    private final AbstractSignal<S> source;
    private final Function<? super S, ? extends T> mapper;
    private final BiPredicate<? super T, ? super T> sameValue;
    private final AtomicReference<Cached<T>> cached = new AtomicReference<>();
    private final AtomicLong notifiedVersion = new AtomicLong();
    @Nullable private Subscription upstream;

    MapDistinctSignal(@NotNull AbstractSignal<S> source, @NotNull Function<? super S, ? extends T> mapper, @NotNull BiPredicate<? super T, ? super T> sameValue) {
        this.source = source;
        this.mapper = mapper;
        this.sameValue = sameValue;
    }

    @Override
    public T get() {
        return this.align().value();
    }

    @Override
    long version() {
        return this.align().version();
    }

    // 推进到上游当前版本. 求值不持锁, CAS 决定哪次纯函数计算成为缓存结果.
    @NotNull
    private Cached<T> align() {
        while (true) {
            long sourceVersion = this.source.version();
            @Nullable Cached<T> current = this.cached.get();
            if (current != null && current.sourceVersion() == sourceVersion) {
                return current;
            }
            T value = this.mapper.apply(this.source.get());
            boolean changed = current == null || !same(this.sameValue, current.value(), value);
            long version = current == null ? 1L : current.version() + (changed ? 1L : 0L);
            Cached<T> next = new Cached<>(value, sourceVersion, version);
            if (this.cached.compareAndSet(current, next)) {
                return next;
            }
        }
    }

    @Override
    protected void onActive() {
        this.upstream = this.linkTo(this.source, this::onSourceDirty);
        try {
            // 首次订阅建立通知基线, 首次求值不算变化.
            this.notifiedVersion.accumulateAndGet(this.align().version(), Math::max);
        } catch (RuntimeException | Error exception) {
            this.upstream.close();
            this.upstream = null;
            throw exception;
        }
    }

    private void onSourceDirty() {
        long version = this.align().version();
        while (true) {
            long notified = this.notifiedVersion.get();
            if (version <= notified) return;
            if (this.notifiedVersion.compareAndSet(notified, version)) {
                this.notifyDirty();
                return;
            }
        }
    }

    @Override
    protected void onInactive() {
        this.upstream.close();
        this.upstream = null;
    }

    private record Cached<V>(V value, long sourceVersion, long version) {
    }
}
