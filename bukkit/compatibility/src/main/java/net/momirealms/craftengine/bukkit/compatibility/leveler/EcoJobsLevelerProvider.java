package net.momirealms.craftengine.bukkit.compatibility.leveler;

import com.willfp.ecojobs.api.EcoJobsAPI;
import com.willfp.ecojobs.jobs.Job;
import com.willfp.ecojobs.jobs.Jobs;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.compatibility.LevelerProvider;

public class EcoJobsLevelerProvider implements LevelerProvider {

    @Override
    public void addExp(Player player, String target, double amount) {
        org.bukkit.entity.Player bukkitPlayer = (org.bukkit.entity.Player) player.platformPlayer();
        for (Job job : EcoJobsAPI.getActiveJobs(bukkitPlayer)) {
            if (job.getId().equals(target)) {
                EcoJobsAPI.giveJobExperience(bukkitPlayer, job, amount);
                return;
            }
        }
    }

    @Override
    public int getLevel(Player player, String target) {
        Job job = Jobs.getByID(target);
        if (job == null) return 0;
        return EcoJobsAPI.getJobLevel(((org.bukkit.entity.Player) player.platformPlayer()), job);
    }
}
