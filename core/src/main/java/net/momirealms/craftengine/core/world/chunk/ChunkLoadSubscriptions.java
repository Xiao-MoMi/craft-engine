package net.momirealms.craftengine.core.world.chunk;

import ca.spottedleaf.concurrentutil.map.concurrent.longs.ConcurrentChainedLong2ReferenceHashTable;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.LongPredicate;

public final class ChunkLoadSubscriptions {
    private final ConcurrentChainedLong2ReferenceHashTable<Watchers> watchers = ConcurrentChainedLong2ReferenceHashTable.createWithCapacity(128);
    private final LongPredicate isChunkReady;

    public ChunkLoadSubscriptions(LongPredicate isChunkReady) {
        this.isChunkReady = isChunkReady;
    }

    private static void collectReady(Watchers watchers, List<Entry> notifications) {
        if (watchers.ready) return;
        watchers.ready = true;
        notifications.addAll(watchers.entries);
    }

    public Owner createOwner(Executor dispatcher, Executor initialDispatcher) {
        return new Owner(dispatcher, initialDispatcher);
    }

    public void onChunkLoad(long chunk) {
        List<Entry> notifications = new ObjectArrayList<>();
        this.watchers.computeIfPresent(chunk, (key, watchers) -> {
            collectReady(watchers, notifications);
            return watchers;
        });
        // Direct callbacks may cancel or add subscriptions to this same chunk.
        for (Entry entry : notifications) {
            entry.owner.dispatcher.execute(entry);
        }
    }

    public void onChunkUnload(long chunk) {
        this.watchers.computeIfPresent(chunk, (key, watchers) -> {
            watchers.ready = false;
            return watchers;
        });
    }

    private static final class Watchers {
        private final List<Entry> entries = new ObjectArrayList<>();
        private boolean ready;
    }

    public final class Owner {
        private final Executor dispatcher;
        private final Executor initialDispatcher;
        private final List<Entry> entries = new ObjectArrayList<>();

        private Owner(Executor dispatcher, Executor initialDispatcher) {
            this.dispatcher = dispatcher;
            this.initialDispatcher = initialDispatcher;
        }

        public ChunkSubscription subscribe(long chunk, @NotNull Runnable callback) {
            Entry entry = new Entry(this, chunk, callback);
            this.entries.add(entry);
            List<Entry> notifications = new ObjectArrayList<>();
            ChunkLoadSubscriptions.this.watchers.compute(chunk, (key, watchers) -> {
                if (watchers == null) watchers = new Watchers();
                watchers.entries.add(entry);
                if (watchers.ready) {
                    notifications.add(entry);
                } else if (ChunkLoadSubscriptions.this.isChunkReady.test(key)) {
                    // Activation can finish before its notification reaches this bucket.
                    // Notify existing subscribers too, and suppress the later duplicate.
                    collectReady(watchers, notifications);
                }
                return watchers;
            });
            for (Entry notification : notifications) {
                notification.owner.initialDispatcher.execute(notification);
            }
            return entry;
        }

        public void cancelAll() {
            while (!this.entries.isEmpty()) {
                this.entries.getLast().cancel();
            }
        }
    }

    private final class Entry implements ChunkSubscription, Runnable {
        private final Owner owner;
        private final long chunk;
        private final Runnable callback;
        private boolean cancelled;

        private Entry(Owner owner, long chunk, Runnable callback) {
            this.owner = owner;
            this.chunk = chunk;
            this.callback = callback;
        }

        @Override
        public void run() {
            if (this.cancelled) return;
            try {
                this.callback.run();
            } catch (Throwable t) {
                CraftEngine.instance().logger().warn("Failed to notify chunk load subscriber for chunk " + this.chunk, t);
            }
        }

        @Override
        public void cancel() {
            if (this.cancelled) return;
            this.cancelled = true;
            this.owner.entries.remove(this);
            ChunkLoadSubscriptions.this.watchers.computeIfPresent(this.chunk, (key, watchers) -> {
                watchers.entries.remove(this);
                return watchers.entries.isEmpty() ? null : watchers;
            });
        }

        @Override
        public boolean cancelled() {
            return this.cancelled;
        }
    }
}
