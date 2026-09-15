package net.momirealms.craftengine.core.plugin.ui.item.guard;

import net.momirealms.craftengine.core.plugin.ui.item.Item;
import net.momirealms.craftengine.core.plugin.ui.item.click.ItemClick;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.LongSupplier;

final class ThrottleGuard implements ItemGuard<ItemClick> {
    private final long intervalNanos;
    private final LongSupplier timeSource;
    @Nullable private WeakHashMap<Item, ItemTimestamps> timestamps; // 首个有效点击后创建

    ThrottleGuard(long intervalNanos, @NotNull LongSupplier timeSource) {
        if (intervalNanos <= 0) {
            throw new IllegalArgumentException("interval must be positive");
        }
        this.intervalNanos = intervalNanos;
        this.timeSource = timeSource;
    }

    @Override
    public boolean test(@NotNull Item item, @NotNull ItemClick click) {
        UUID playerId = click.player().uuid();
        synchronized (this) {
            long now = this.timeSource.getAsLong();
            WeakHashMap<Item, ItemTimestamps> timestamps = this.timestamps;
            if (timestamps == null) {
                timestamps = WeakHashMap.newWeakHashMap(1);
                this.timestamps = timestamps;
            }
            ItemTimestamps itemTimestamps = timestamps.get(item);
            if (itemTimestamps == null) {
                timestamps.put(item, new ItemTimestamps(playerId, now));
                return true;
            }
            return itemTimestamps.test(playerId, now, this.intervalNanos);
        }
    }

    private static final class ItemTimestamps {
        private UUID playerId;
        private long timestamp;
        @Nullable private HashMap<UUID, Long> shared; // 同一 Item 有多个活跃玩家时创建

        private ItemTimestamps(UUID playerId, long timestamp) {
            this.playerId = playerId;
            this.timestamp = timestamp;
        }

        private boolean test(UUID playerId, long now, long intervalNanos) {
            HashMap<UUID, Long> shared = this.shared;
            if (shared == null) {
                if (now - this.timestamp >= intervalNanos) {
                    this.playerId = playerId;
                    this.timestamp = now;
                    return true;
                }
                if (this.playerId.equals(playerId)) return false;

                shared = HashMap.newHashMap(2);
                shared.put(this.playerId, this.timestamp);
                shared.put(playerId, now);
                this.shared = shared;
                return true;
            }

            // 多人路径清理当前 Item 的过期玩家, 并在剩一人时退回内联状态
            Iterator<Long> iterator = shared.values().iterator();
            while (iterator.hasNext()) {
                if (now - iterator.next() >= intervalNanos) {
                    iterator.remove();
                }
            }
            Long last = shared.get(playerId);
            if (last != null) {
                if (shared.size() == 1) {
                    this.playerId = playerId;
                    this.timestamp = last;
                    this.shared = null;
                }
                return false;
            }
            if (shared.isEmpty()) {
                this.playerId = playerId;
                this.timestamp = now;
                this.shared = null;
                return true;
            }
            shared.put(playerId, now);
            return true;
        }
    }
}
