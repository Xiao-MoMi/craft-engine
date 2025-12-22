package net.momirealms.craftengine.bukkit.compatibility.leveler;

import cn.gtemc.levelerbridge.api.LevelerProvider;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class CustomLevelerProvider implements LevelerProvider<Player> {
    private final net.momirealms.craftengine.core.plugin.compatibility.LevelerProvider provider;
    private final String pluginName;

    public CustomLevelerProvider(net.momirealms.craftengine.core.plugin.compatibility.LevelerProvider provider) {
        this.provider = provider;
        this.pluginName = provider.plugin().toLowerCase(Locale.ROOT);
    }

    @NotNull
    @Override
    public String plugin() {
        return this.pluginName;
    }

    @Override
    public int getLevel(@NotNull Player player, String target) {
        return this.provider.getLevel(BukkitAdaptors.adapt(player), target);
    }

    @Override
    public double getExperience(@NotNull Player player, String target) {
        return 0;
    }

    @Override
    public void addLevel(@NotNull Player player, String target, int level) {
    }

    @Override
    public void addExperience(@NotNull Player player, String target, double experience) {
        this.provider.getLevel(BukkitAdaptors.adapt(player), target);
    }

    @Override
    public void setLevel(@NotNull Player player, String target, int level) {
    }

    @Override
    public void setExperience(@NotNull Player player, String target, double experience) {
    }
}
