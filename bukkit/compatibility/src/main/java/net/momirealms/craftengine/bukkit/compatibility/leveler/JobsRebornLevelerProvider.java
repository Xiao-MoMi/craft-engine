package net.momirealms.craftengine.bukkit.compatibility.leveler;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.Job;
import com.gamingmesh.jobs.container.JobProgression;
import com.gamingmesh.jobs.container.JobsPlayer;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.compatibility.LevelerProvider;

public class JobsRebornLevelerProvider implements LevelerProvider {

    @Override
    public void addExp(Player player, String target, double amount) {
        JobsPlayer jobsPlayer = Jobs.getPlayerManager().getJobsPlayer(player.uuid());
        Job job = Jobs.getJob(target);
        if (jobsPlayer != null && jobsPlayer.isInJob(job)) {
            Jobs.getPlayerManager().addExperience(jobsPlayer, job, amount);
        }
    }

    @Override
    public int getLevel(Player player, String target) {
        JobsPlayer jobsPlayer = Jobs.getPlayerManager().getJobsPlayer(player.uuid());
        if (jobsPlayer != null) {
            JobProgression jobProgression = jobsPlayer.getJobProgression(Jobs.getJob(target));
            return jobProgression.getLevel();
        }
        return 0;
    }
}
