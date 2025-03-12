package net.momirealms.craftengine.bukkit.world;

import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.bukkit.util.ItemUtils;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldBlock;
import net.momirealms.craftengine.core.world.WorldHeight;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.inventory.ItemStack;

import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.UUID;

public class BukkitWorld implements World {
    private final WeakReference<org.bukkit.World> world;
    private WorldHeight worldHeight;

    public BukkitWorld(org.bukkit.World world) {
        this.world = new WeakReference<>(world);
    }

    @Override
    public org.bukkit.World getHandle() {
        return world.get();
    }

    @Override
    public WorldHeight worldHeight() {
        if (this.worldHeight == null) {
            this.worldHeight = WorldHeight.create(getHandle().getMinHeight(), getHandle().getMaxHeight() - getHandle().getMinHeight());
        }
        return this.worldHeight;
    }

    @Override
    public WorldBlock getBlockAt(int x, int y, int z) {
        return new BukkitWorldBlock(getHandle().getBlockAt(x, y, z));
    }

    @Override
    public String name() {
        return getHandle().getName();
    }

    @Override
    public Path directory() {
        return getHandle().getWorldFolder().toPath();
    }

    @Override
    public UUID uuid() {
        return getHandle().getUID();
    }

    @Override
    public void dropItemNaturally(Vec3d location, Item<?> item) {
        ItemStack itemStack = (ItemStack) item.load();
        if (ItemUtils.isEmpty(itemStack)) return;
        if (VersionHelper.isVersionNewerThan1_21_2()) {
            getHandle().dropItemNaturally(new Location(null, location.x(), location.y(), location.z()), (ItemStack) item.getItem());
        } else {
            getHandle().dropItemNaturally(new Location(null, location.x() - 0.5, location.y() - 0.5, location.z() - 0.5), (ItemStack) item.getItem());
        }
    }

    @Override
    public void dropExp(Vec3d location, int amount) {
        if (amount <= 0) return;
        EntityUtils.spawnEntity(getHandle(), new Location(getHandle(), location.x(), location.y(), location.z()), EntityType.EXPERIENCE_ORB, (e) -> {
            ExperienceOrb orb = (ExperienceOrb) e;
            orb.setExperience(amount);
        });
    }

    @Override
    public void playBlockSound(Vec3d location, Key sound, float volume, float pitch) {
        getHandle().playSound(new Location(null, location.x(), location.y(), location.z()), sound.toString(), SoundCategory.BLOCKS, volume, pitch);
    }
}
