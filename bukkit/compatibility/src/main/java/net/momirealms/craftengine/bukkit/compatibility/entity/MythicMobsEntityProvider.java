package net.momirealms.craftengine.bukkit.compatibility.entity;

import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.utils.serialize.Position;
import io.lumine.mythic.core.mobs.ActiveMob;
import net.momirealms.craftengine.bukkit.entity.BukkitEntity;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.entity.Entity;
import net.momirealms.craftengine.core.plugin.compatibility.EntityProvider;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class MythicMobsEntityProvider implements EntityProvider {

    @NotNull
    @Override
    public String plugin() {
        return "mythicmobs";
    }

    @Nullable
    @Override
    public Entity spawnEntity(WorldPosition position, String id, Context context) {
        Optional<MythicMob> mythicMob = getAPI().getMobManager().getMythicMob(id);
        if (mythicMob.isPresent()) {
            MythicMob theMob = mythicMob.get();
            Position mmPos = Position.of(LocationUtils.toLocation(position));
            AbstractLocation abstractLocation = new AbstractLocation(mmPos);
            double level = context.getOptionalParameter(DirectContextParameters.MOB_LEVEL).orElse(0d);
            ActiveMob activeMob = theMob.spawn(abstractLocation, level);
            return new BukkitEntity(activeMob.getEntity().getBukkitEntity());
        }
        return null;
    }

    @Nullable
    @Override
    public String getEntityId(Entity entity) {
        org.bukkit.entity.Entity bukkitEntity = ((BukkitEntity) entity).platformEntity();
        Optional<ActiveMob> activeMob = getAPI().getMobManager().getActiveMob(bukkitEntity.getUniqueId());
        return activeMob.map(ActiveMob::getMobType).orElse(null);
    }

    private MythicBukkit getAPI() {
        return MythicBukkit.inst();
    }
}
