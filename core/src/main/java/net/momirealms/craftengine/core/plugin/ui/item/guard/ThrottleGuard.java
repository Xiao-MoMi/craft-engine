package net.momirealms.craftengine.core.plugin.ui.item.guard;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.ui.item.Item;
import net.momirealms.craftengine.core.plugin.ui.item.click.ItemClick;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.LongSupplier;

final class ThrottleGuard implements ItemGuard<ItemClick> {
    private final long intervalMillis;
    private final LongSupplier timeSource;
    // 两级弱键避免共享 Guard 延长 Item 或 Player 的生命周期.
    private final Map<Item, Map<Player, Long>> timestamps = new WeakHashMap<>();

    ThrottleGuard(long intervalMillis, @NotNull LongSupplier timeSource) {
        if (intervalMillis <= 0) {
            throw new IllegalArgumentException("intervalMillis must be positive");
        }
        this.intervalMillis = intervalMillis;
        this.timeSource = timeSource;
    }

    @Override
    public boolean test(@NotNull Item item, @NotNull ItemClick click) {
        long now = this.timeSource.getAsLong();
        // WeakHashMap 的读取与更新时间必须共享同一把锁.
        synchronized (this.timestamps) {
            Map<Player, Long> itemTimestamps = this.timestamps.computeIfAbsent(item, ignoredItem -> new WeakHashMap<>());
            Long last = itemTimestamps.get(click.player());
            if (last != null && now - last < this.intervalMillis) {
                return false;
            }
            itemTimestamps.put(click.player(), now);
            return true;
        }
    }
}
