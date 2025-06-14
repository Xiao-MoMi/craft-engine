package net.momirealms.craftengine.bukkit.compatibility.leveler;

import com.willfp.ecoskills.api.EcoSkillsAPI;
import com.willfp.ecoskills.skills.Skills;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.compatibility.LevelerProvider;

import java.util.Objects;

public class EcoSkillsLevelerProvider implements LevelerProvider {
    
    @Override
    public void addExp(Player player, String target, double amount) {
        EcoSkillsAPI.gainSkillXP(((org.bukkit.entity.Player) player.platformPlayer()), Objects.requireNonNull(Skills.INSTANCE.getByID(target)), amount);
    }

    @Override
    public int getLevel(Player player, String target) {
        return EcoSkillsAPI.getSkillLevel(((org.bukkit.entity.Player) player.platformPlayer()), Objects.requireNonNull(Skills.INSTANCE.getByID(target)));
    }
}
