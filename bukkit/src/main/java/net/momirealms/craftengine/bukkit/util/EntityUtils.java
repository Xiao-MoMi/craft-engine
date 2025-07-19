package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class EntityUtils {

    private EntityUtils() {}

    public static BlockPos getOnPos(Player player) {
        try {
            Object serverPlayer = FastNMS.INSTANCE.method$CraftPlayer$getHandle(player);
            Object blockPos = CoreReflections.method$Entity$getOnPos.invoke(serverPlayer, 1.0E-5F);
            return LocationUtils.fromBlockPos(blockPos);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static Entity spawnEntity(World world, Location loc, EntityType type, Consumer<Entity> function) {
        if (VersionHelper.isOrAbove1_20_2()) {
            return world.spawnEntity(loc, type, CreatureSpawnEvent.SpawnReason.CUSTOM, function);
        } else {
            return LegacyEntityUtils.spawnEntity(world, loc, type, function);
        }
    }

    public static boolean isPiglinWithGoldIngot(Entity entity, Item<ItemStack> item) {
        return entity.getType() == EntityType.PIGLIN &&
                item != null &&
                item.vanillaId().equals(Key.of("minecraft:gold_ingot"));
    }

    public static boolean isHappyGhastRideable(Entity entity) {
        if (!VersionHelper.isOrAbove1_21_6() &&
                !entity.getType().name().equals("HAPPY_GHAST")) return false;
        return entity instanceof LivingEntity livingEntity
                && livingEntity.getEquipment() != null
                && hasHarness(livingEntity.getEquipment());
    }

    public static boolean hasHarness(EntityEquipment equipment) {
        ItemStack bodyItem = equipment.getItem(EquipmentSlot.BODY);
        return ItemTags.ITEMS_HARNESSES != null &&
                ItemTags.ITEMS_HARNESSES.isTagged(bodyItem.getType());
    }
}
