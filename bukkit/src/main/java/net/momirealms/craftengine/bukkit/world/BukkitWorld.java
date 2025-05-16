package net.momirealms.craftengine.bukkit.world;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.bukkit.util.ItemUtils;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockInWorld;
import net.momirealms.craftengine.core.world.Position;
import net.momirealms.craftengine.core.world.World;
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
    public org.bukkit.World platformWorld() {
        return world.get();
    }

    @Override
    public Object serverWorld() {
        return FastNMS.INSTANCE.field$CraftWorld$ServerLevel(platformWorld());
    }

    @Override
    public WorldHeight worldHeight() {
        if (this.worldHeight == null) {
            this.worldHeight = WorldHeight.create(platformWorld().getMinHeight(), platformWorld().getMaxHeight() - platformWorld().getMinHeight());
        }
        return this.worldHeight;
    }

    @Override
    public BlockInWorld getBlockAt(int x, int y, int z) {
        return new BukkitBlockInWorld(platformWorld().getBlockAt(x, y, z));
    }

    @Override
    public String name() {
        return platformWorld().getName();
    }

    @Override
    public Path directory() {
        return platformWorld().getWorldFolder().toPath();
    }

    @Override
    public UUID uuid() {
        return platformWorld().getUID();
    }

    @Override
    public void dropItemNaturally(Position location, Item<?> item) {
        ItemStack itemStack = (ItemStack) item.load();
        if (ItemUtils.isEmpty(itemStack)) return;
        if (VersionHelper.isOrAbove1_21_2()) {
            platformWorld().dropItemNaturally(new Location(null, location.x(), location.y(), location.z()), (ItemStack) item.getItem());
        } else {
            platformWorld().dropItemNaturally(new Location(null, location.x() - 0.5, location.y() - 0.5, location.z() - 0.5), (ItemStack) item.getItem());
        }
    }

    @Override
    public void dropExp(Position location, int amount) {
        if (amount <= 0) return;
        EntityUtils.spawnEntity(platformWorld(), new Location(platformWorld(), location.x(), location.y(), location.z()), EntityType.EXPERIENCE_ORB, (e) -> {
            ExperienceOrb orb = (ExperienceOrb) e;
            orb.setExperience(amount);
        });
    }

    @Override
    public void playBlockSound(Position location, Key sound, float volume, float pitch) {
        platformWorld().playSound(new Location(null, location.x(), location.y(), location.z()), sound.toString(), SoundCategory.BLOCKS, volume, pitch);
    }

    @Override
    public long time() {
        return platformWorld().getTime();
    }
}
