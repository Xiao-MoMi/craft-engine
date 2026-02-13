package net.momirealms.craftengine.bukkit.compatibility.leveler;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.compatibility.LevelerProvider;

public final class LevelerBridgeLeveler implements LevelerProvider {
    private final cn.gtemc.levelerbridge.api.LevelerProvider<org.bukkit.entity.Player> provider;

    public LevelerBridgeLeveler(cn.gtemc.levelerbridge.api.LevelerProvider<org.bukkit.entity.Player> provider) {
        this.provider = provider;
    }

    @Override
    public String plugin() {
        return this.provider.plugin();
    }

    @Override
    public void addExp(Player player, String target, double amount) {
        this.provider.addExperience((org.bukkit.entity.Player) player.platformPlayer(), target, amount);
    }

    @Override
    public int getLevel(Player player, String target) {
        return this.provider.getLevel((org.bukkit.entity.Player) player.platformPlayer(), target);
    }
}
