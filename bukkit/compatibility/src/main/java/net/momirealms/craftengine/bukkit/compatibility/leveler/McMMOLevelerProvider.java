package net.momirealms.craftengine.bukkit.compatibility.leveler;

import com.gmail.nossr50.api.ExperienceAPI;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.compatibility.LevelerProvider;

public class McMMOLevelerProvider implements LevelerProvider {

    @Override
    public void addExp(Player player, String target, double amount) {
        ExperienceAPI.addRawXP((org.bukkit.entity.Player) player.platformPlayer(), target, (float) amount, "UNKNOWN");
    }

    @Override
    public int getLevel(Player player, String target) {
        return ExperienceAPI.getLevel((org.bukkit.entity.Player) player.platformPlayer(), PrimarySkillType.valueOf(target));
    }
}
