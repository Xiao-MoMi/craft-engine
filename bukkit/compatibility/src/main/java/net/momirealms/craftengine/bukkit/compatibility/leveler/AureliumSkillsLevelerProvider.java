package net.momirealms.craftengine.bukkit.compatibility.leveler;

import com.archyx.aureliumskills.api.AureliumAPI;
import com.archyx.aureliumskills.leveler.Leveler;
import com.archyx.aureliumskills.skills.SkillRegistry;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.compatibility.LevelerProvider;

public class AureliumSkillsLevelerProvider implements LevelerProvider {
    private final Leveler leveler;
    private final SkillRegistry skillRegistry;

    public AureliumSkillsLevelerProvider() {
        this.leveler = AureliumAPI.getPlugin().getLeveler();
        this.skillRegistry = AureliumAPI.getPlugin().getSkillRegistry();
    }

    @Override
    public void addExp(Player player, String target, double amount) {
        this.leveler.addXp(((org.bukkit.entity.Player) player.platformPlayer()), this.skillRegistry.getSkill(target), amount);
    }

    @Override
    public int getLevel(Player player, String target) {
        return AureliumAPI.getSkillLevel(((org.bukkit.entity.Player) player.platformPlayer()), this.skillRegistry.getSkill(target));
    }
}
