package net.momirealms.craftengine.core.plugin.ui.internal;

import net.momirealms.craftengine.core.plugin.ui.Observable;
import net.momirealms.craftengine.core.plugin.ui.Observer;
import net.momirealms.craftengine.core.plugin.ui.Subscription;
import net.momirealms.craftengine.core.util.ThrowableUtils;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public final class ObservableDispatcher<T> implements Observable<T> {
    private final CopyOnWriteArrayList<Entry<T>> entries = new CopyOnWriteArrayList<>();

    @Override
    public Subscription subscribe(Observer<? super T> observer) {
        Objects.requireNonNull(observer, "observer");
        Entry<T> entry = new Entry<>(this, observer);
        entries.add(entry);
        return entry;
    }

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

        private Observer<? super T> observer() {
            return observer.get();
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
