package net.momirealms.craftengine.bukkit.compatibility.leveler;

import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.experience.EXPSource;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.compatibility.LevelerProvider;

public class MMOCoreLevelerProvider implements LevelerProvider {

    @Override
    public void addExp(Player player, String target, double amount) {
        MMOCore.plugin.professionManager.get(target).giveExperience(PlayerData.get(player.uuid()), amount, null , EXPSource.OTHER);
    }

    @Override
    public int getLevel(Player player, String target) {
        return PlayerData.get(player.uuid()).getCollectionSkills().getLevel(MMOCore.plugin.professionManager.get(target));
    }
}
