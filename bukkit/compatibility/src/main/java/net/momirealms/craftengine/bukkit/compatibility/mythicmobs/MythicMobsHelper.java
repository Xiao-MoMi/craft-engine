package net.momirealms.craftengine.bukkit.compatibility.mythicmobs;

import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.skills.variables.Variable;
import io.lumine.mythic.core.utils.MythicUtil;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class MythicMobsHelper {
    private MythicMobsHelper() {}

    public static void executeSkill(String skill, float power, Player player) {
        executeSkill(skill, power, player, null);
    }

    public static void executeSkill(String skill, float power, Map<String, String> parameters, Player player) {
        executeSkill(skill, power, parameters, Map.of(), player);
    }

    public static void executeSkill(String skill,
                                    float power,
                                    Map<String, String> parameters,
                                    Map<String, Variable> variables,
                                    Player player) {
        executeSkill(
                skill,
                power,
                player,
                metadata -> {
                    metadata.getParameters().putAll(parameters);
                    metadata.getVariables().putAll(variables);
                }
        );
    }

    private static void executeSkill(String skill,
                                     float power,
                                     Player player,
                                     @Nullable Consumer<SkillMetadata> metadataConsumer) {
        org.bukkit.entity.Player casterPlayer = (org.bukkit.entity.Player) player.platformPlayer();
        Location location = casterPlayer.getLocation();
        List<Entity> targets = null;
        List<Location> locations = null;
        LivingEntity target = MythicUtil.getTargetedEntity(casterPlayer);
        if (target != null) {
            targets = List.of(target);
            locations = List.of(target.getLocation());
        }
        if (metadataConsumer == null) {
            MythicBukkit.inst().getAPIHelper().castSkill(
                    casterPlayer,
                    skill,
                    casterPlayer,
                    location,
                    targets,
                    locations,
                    power
            );
        } else {
            MythicBukkit.inst().getAPIHelper().castSkill(
                    casterPlayer,
                    skill,
                    casterPlayer,
                    location,
                    targets,
                    locations,
                    power,
                    metadataConsumer
            );
        }
    }

    public static void summonMob(String mobId, WorldPosition worldPosition, double level) {
        MythicBukkit.inst().getMobManager().spawnMob(mobId, BukkitAdapter.adapt(LocationUtils.toLocation(worldPosition)), level);
    }
}
