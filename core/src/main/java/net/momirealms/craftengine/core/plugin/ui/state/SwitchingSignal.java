package net.momirealms.craftengine.core.plugin.ui.state;

import net.momirealms.craftengine.core.plugin.ui.Subscription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;

final class SwitchingSignal<K, T> extends AbstractSignal<T> {
    private final Function<? super K, ? extends Signal<T>> sourceOf;
    private final AbstractSignal<K> key;
    private final Object switchLock = new Object();

    @Nullable private volatile Selected<K, T> selected;
    private volatile long version;
    private long notifiedVersion;
    @Nullable private Subscription keyUpstream;
    @Nullable private Subscription sourceUpstream;

    SwitchingSignal(@NotNull Function<? super K, ? extends Signal<T>> sourceOf, @NotNull AbstractSignal<K> key) {
        this.sourceOf = sourceOf;
        this.key = key;
    }

    @Override
    public T get() {
        Selected<K, T> current = this.selected;
        if (current == null || !Objects.equals(current.key(), this.key.get())) {
            current = this.refresh();
        }
        return current.source().get();
    }

    @Override
    long version() {
        Selected<K, T> current = this.selected;
        if (current == null
                || !Objects.equals(current.key(), this.key.get())
                || current.sourceVersion() != current.source().version()) {
            this.refresh();
        }
        return this.version;
    }

    // 将选中来源与当前 key 对齐, 上一条转发在锁外关闭.
    @NotNull
    private Selected<K, T> refresh() {
        Subscription previous;
        Selected<K, T> current;
        synchronized (this.switchLock) {
            previous = this.refreshLocked();
            current = this.selected;
        }
        if (previous != null) {
            previous.close();
        }
        assert current != null;
        return current;
    }

    // 重新选择来源, 来源或来源版本变化时推进本节点版本.
    @Nullable
    private Subscription refreshLocked() {
        K currentKey = this.key.get();
        Selected<K, T> current = this.selected;
        if (current != null && Objects.equals(current.key(), currentKey)) {
            long sourceVersion = current.source().version();
            if (current.sourceVersion() != sourceVersion) {
                this.version++;
                this.selected = new Selected<>(currentKey, current.source(), sourceVersion);
            }
            return null;
        }

        AbstractSignal<T> source = require(this.sourceOf.apply(currentKey));
        Subscription previous = this.sourceUpstream;
        Subscription attached = null;
        if (previous != null) {
            attached = this.linkTo(source, this::onUpstreamDirty);
        }
        long sourceVersion;
        try {
            sourceVersion = source.version();
        } catch (RuntimeException | Error exception) {
            if (attached != null) {
                attached.close();
            }
            throw exception;
        }
        if (attached != null) {
            this.sourceUpstream = attached;
        }
        this.version++;
        this.selected = new Selected<>(currentKey, source, sourceVersion);
        return previous;
    }

    private void onUpstreamDirty() {
        Subscription previous;
        boolean shouldNotify = false;
        synchronized (this.switchLock) {
            previous = this.refreshLocked();
            if (this.version > this.notifiedVersion) {
                this.notifiedVersion = this.version;
                shouldNotify = true;
            }
        }
        if (previous != null) {
            previous.close();
        }
        if (shouldNotify) {
            this.notifyDirty();
        }
    }

    @Override
    protected void onActive() {
        Subscription discarded = null;
        synchronized (this.switchLock) {
            this.keyUpstream = this.linkTo(this.key, this::onUpstreamDirty);
            try {
                this.refreshLocked();
                Selected<K, T> current = this.selected;
                assert current != null;
                this.sourceUpstream = this.linkTo(current.source(), this::onUpstreamDirty);
                // 再次对齐, 收进建立转发期间发生的来源变化.
                discarded = this.refreshLocked();
            } catch (RuntimeException | Error exception) {
                if (this.sourceUpstream != null) {
                    this.sourceUpstream.close();
                    this.sourceUpstream = null;
                }
                this.keyUpstream.close();
                this.keyUpstream = null;
                throw exception;
            }
            this.notifiedVersion = this.version;
        }
        if (discarded != null) {
            discarded.close();
        }
    }

    @Override
    protected void onInactive() {
        Subscription previousKey;
        Subscription previousSource;
        synchronized (this.switchLock) {
            previousKey = this.keyUpstream;
            previousSource = this.sourceUpstream;
            this.keyUpstream = null;
            this.sourceUpstream = null;
        }
        previousKey.close();
        previousSource.close();
    }

    private record Selected<K, T>(K key, AbstractSignal<T> source, long sourceVersion) {
    }
}
