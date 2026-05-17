package net.momirealms.craftengine.bukkit.compatibility.mythicmobs;

import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.utils.MythicUtil;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public final class MythicMobsHelper {
    private MythicMobsHelper() {}

    public static void executeSkill(String skill, float power, Player player) {
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

    public static void summonMob(String mobId, WorldPosition worldPosition, double level) {
        MythicBukkit.inst().getMobManager().spawnMob(mobId, BukkitAdapter.adapt(LocationUtils.toLocation(worldPosition)), level);
    }
}
