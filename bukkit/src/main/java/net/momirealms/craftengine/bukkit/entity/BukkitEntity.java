package net.momirealms.craftengine.bukkit.entity;

import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.core.entity.AbstractEntity;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.World;

import java.lang.ref.WeakReference;
import java.util.UUID;

public class BukkitEntity extends AbstractEntity {
    private final WeakReference<org.bukkit.entity.Entity> entity;

    public BukkitEntity(org.bukkit.entity.Entity entity) {
        this.entity = new WeakReference<>(entity);
    }

    @Override
    public double x() {
        return literalObject().getX();
    }

    @Override
    public double y() {
        return literalObject().getY();
    }

    @Override
    public double z() {
        return literalObject().getZ();
    }

    @Override
    public void tick() {
    }

    @Override
    public int entityID() {
        return literalObject().getEntityId();
    }

    @Override
    public float xRot() {
        return literalObject().getYaw();
    }

    @Override
    public float yRot() {
        return literalObject().getPitch();
    }

    @Override
    public World world() {
        return new BukkitWorld(literalObject().getWorld());
    }

    @Override
    public Direction getDirection() {
        return Direction.NORTH;
    }

    @Override
    public org.bukkit.entity.Entity literalObject() {
        return this.entity.get();
    }

    @Override
    public Key type() {
        return EntityUtils.getEntityType(literalObject());
    }

    @Override
    public String name() {
        return literalObject().getName();
    }

    @Override
    public UUID uuid() {
        return literalObject().getUniqueId();
    }
}
