package net.momirealms.craftengine.bukkit.compatibility.mythicmobs;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.utils.MythicUtil;
import net.momirealms.craftengine.core.entity.player.Player;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public final class MythicSkillHelper {

    public static void execute(String skill, float power, Player player) {
        org.bukkit.entity.Player casterPlayer = (org.bukkit.entity.Player) player.platformPlayer();
        Location location = casterPlayer.getLocation();
        LivingEntity target = MythicUtil.getTargetedEntity(casterPlayer);
        List<Entity> targets = new ArrayList<>();
        List<Location> locations = null;
        if (target != null) {
            targets.add(target);
            locations = List.of(target.getLocation());
        }
        MythicBukkit.inst().getAPIHelper().castSkill(casterPlayer, skill, casterPlayer, location, targets, locations, power);
    }
}
