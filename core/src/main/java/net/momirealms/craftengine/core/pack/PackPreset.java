package net.momirealms.craftengine.core.pack;

import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Tristate;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public record PackPreset(Set<String> packs) {
    public PackPreset {
        packs = Set.copyOf(packs);
        for (String pack : packs) {
            if (!pack.matches("[A-Za-z0-9_-]{1,64}")) {
                throw new IllegalArgumentException("Invalid resource pack id: " + pack);
            }
        }
    }

    public static PackPreset fromConfig(String name, ConfigValue value) {
        if (!name.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("Invalid resource pack preset name: " + name);
        }
        return new PackPreset(Set.copyOf(value.getAsStringList()));
    }

    public Map<String, Tristate> preferences(Collection<String> managedPacks) {
        Map<String, Tristate> preferences = new LinkedHashMap<>();
        // 显式关闭未选中的包，避免旧偏好或 default 使高配包在切换后继续启用。
        managedPacks.forEach(pack -> preferences.put(pack, Tristate.FALSE));
        this.packs.forEach(pack -> preferences.put(pack, Tristate.TRUE));
        return preferences;
    }
}
