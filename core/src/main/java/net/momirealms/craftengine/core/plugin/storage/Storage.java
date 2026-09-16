package net.momirealms.craftengine.core.plugin.storage;

import java.util.Map;
import java.util.UUID;

public interface Storage extends AutoCloseable {

    Map<String, Boolean> loadPackPreferences(UUID player) throws Exception;

    default void setPackPreference(UUID player, String pack, Boolean enabled) throws Exception {
        setPackPreferences(player, java.util.Collections.singletonMap(pack, enabled));
    }

    // 批量合并偏好，null 表示移除覆盖；每个后端应在一次原子写入中完成。
    void setPackPreferences(UUID player, Map<String, Boolean> updates) throws Exception;

    @Override
    void close();
}
