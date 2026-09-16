package net.momirealms.craftengine.bukkit.entity.furniture;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.momirealms.craftengine.core.entity.furniture.Collider;
import net.momirealms.craftengine.core.entity.furniture.FurnitureSnapshotState;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElement;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBox;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelWriterProxy;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class BukkitVariantSnapshot extends FurnitureSnapshotState {

    public BukkitVariantSnapshot(List<FurnitureElement> elements,
                                 List<FurnitureHitBox> hitboxes,
                                 Int2ObjectMap<FurnitureHitBox> hitboxMap,
                                 List<Collider> colliders) {
        super(elements, hitboxes, hitboxMap, colliders);
    }

    @Override
    public void addCollidersToWorld(World cWorld) {
        // 只把运行时派生 Collider 加入世界；元数据实体才是家具存档的来源。
        // 非持久化不等于不能被 WorldEdit 导出 NBT：复制后自定义子类会变成普通 Interaction/Boat，
        // 因此入世界前写 PDC 标记，供 manager 的单实体补载入口识别并清理副本。
        Object world = cWorld.minecraftWorld();
        for (int colliderIndex = 0, colliderCount = super.colliders.size(); colliderIndex < colliderCount; colliderIndex++) {
            Collider entity = super.colliders.get(colliderIndex);
            Object minecraftEntity = entity.handle();
            Entity bukkitEntity = EntityProxy.INSTANCE.getBukkitEntity(minecraftEntity);
            bukkitEntity.getPersistentDataContainer().set(BukkitFurnitureManager.FURNITURE_COLLISION, PersistentDataType.BYTE, (byte) 1);
            bukkitEntity.setPersistent(false);
            if (!bukkitEntity.isValid()) {
                LevelWriterProxy.INSTANCE.addFreshEntity(world, minecraftEntity, CreatureSpawnEvent.SpawnReason.CUSTOM);
            }
        }
    }
}
