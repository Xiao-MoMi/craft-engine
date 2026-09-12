package net.momirealms.craftengine.core.plugin.ui.state;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

@ApiStatus.Internal
public final class PlayerSignalRegistry {
    private static final Object LOCK = new Object();
    private static final Set<KeyedSignal<UUID, ?>> SIGNALS = Collections.newSetFromMap(new WeakHashMap<>());

    private PlayerSignalRegistry() {
    }

    // 弱集合不会延长玩家数据源的生命周期.
    static void track(@NotNull KeyedSignal<UUID, ?> signal) {
        synchronized (LOCK) {
            SIGNALS.add(signal);
        }
    }

    /**
     * 驱逐指定玩家在全部玩家数据源中的分区, 由平台现有的退出生命周期调用.
     *
     * @param uuid 离线玩家 UUID
     */
    public static void evict(@NotNull UUID uuid) {
        List<KeyedSignal<UUID, ?>> snapshot;
        synchronized (LOCK) {
            snapshot = new ArrayList<>(SIGNALS);
        }
        for (int index = 0; index < snapshot.size(); index++) {
            try {
                snapshot.get(index).remove(uuid);
            } catch (RuntimeException exception) {
                CraftEngine.instance().logger().error("Failed to evict a player signal partition on quit", exception);
            }
        }
    }
}
