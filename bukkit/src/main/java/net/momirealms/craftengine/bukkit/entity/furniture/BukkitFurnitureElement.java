package net.momirealms.craftengine.bukkit.entity.furniture;

import net.momirealms.craftengine.bukkit.entity.data.ItemDisplayEntityData;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.core.entity.Billboard;
import net.momirealms.craftengine.core.entity.ItemDisplayContext;
import net.momirealms.craftengine.core.entity.furniture.AbstractFurnitureElement;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class BukkitFurnitureElement extends AbstractFurnitureElement {
    private final List<Object> commonValues;

    public BukkitFurnitureElement(Key item,
                                  Billboard billboard,
                                  ItemDisplayContext transform,
                                  Vector3f scale,
                                  Vector3f translation,
                                  Vector3f offset,
                                  Quaternionf rotation,
                                  boolean applyDyedColor) {
        super(item, billboard, transform, scale, translation, offset, rotation, applyDyedColor);
        this.commonValues = new ArrayList<>();
        ItemDisplayEntityData.Scale.addEntityDataIfNotDefaultValue(scale(), this.commonValues);
        ItemDisplayEntityData.RotationLeft.addEntityDataIfNotDefaultValue(rotation(), this.commonValues);
        ItemDisplayEntityData.BillboardConstraints.addEntityDataIfNotDefaultValue(billboard().id(), this.commonValues);
        ItemDisplayEntityData.Translation.addEntityDataIfNotDefaultValue(translation(), this.commonValues);
        ItemDisplayEntityData.DisplayType.addEntityDataIfNotDefaultValue(transform().id(), this.commonValues);
    }

    @Override
    public void initPackets(int entityId, @NotNull WorldPosition position, @NotNull Quaternionf conjugated, Integer dyedColor, Consumer<Object> packets) {
        Vector3f offset = conjugated.transform(new Vector3f(position()));
        packets.accept(FastNMS.INSTANCE.constructor$ClientboundAddEntityPacket(
                entityId, UUID.randomUUID(), position.x() + offset.x, position.y() + offset.y, position.z() - offset.z, 0, position.xRot(),
                Reflections.instance$EntityType$ITEM_DISPLAY, 0, Reflections.instance$Vec3$Zero, 0
        ));
        packets.accept(FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(entityId, getCachedValues(dyedColor)));
    }

    private synchronized List<Object> getCachedValues(Integer color) {
        List<Object> cachedValues = new ArrayList<>(this.commonValues);
        Item<ItemStack> item = BukkitItemManager.instance().createWrappedItem(item(), null);
        if (item == null) {
            CraftEngine.instance().debug(() -> "Failed to create furniture element because item " + item() + " not found");
            item = BukkitItemManager.instance().wrap(new ItemStack(Material.BARRIER));
        } else {
            if (color != null) {
                item.dyedColor(color);
                item.load();
            }
        }
        ItemDisplayEntityData.DisplayedItem.addEntityDataIfNotDefaultValue(item.getLiteralObject(), cachedValues);
        return cachedValues;
    }
}
