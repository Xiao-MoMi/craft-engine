package net.momirealms.craftengine.bukkit.entity.furniture;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.momirealms.craftengine.bukkit.entity.data.ItemDisplayEntityData;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MEntityTypes;
import net.momirealms.craftengine.core.entity.Billboard;
import net.momirealms.craftengine.core.entity.ItemDisplayContext;
import net.momirealms.craftengine.core.entity.furniture.AbstractFurnitureElement;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.FurnitureElement;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.data.FireworkExplosion;
import net.momirealms.craftengine.core.util.Color;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
                                  Vector3f position,
                                  Quaternionf rotation,
                                  boolean applyDyedColor) {
        super(item, billboard, transform, scale, translation, position, rotation, applyDyedColor);
        this.commonValues = new ArrayList<>();
        ItemDisplayEntityData.Scale.addEntityDataIfNotDefaultValue(scale(), this.commonValues);
        ItemDisplayEntityData.RotationLeft.addEntityDataIfNotDefaultValue(rotation(), this.commonValues);
        ItemDisplayEntityData.BillboardConstraints.addEntityDataIfNotDefaultValue(billboard().id(), this.commonValues);
        ItemDisplayEntityData.Translation.addEntityDataIfNotDefaultValue(translation(), this.commonValues);
        ItemDisplayEntityData.DisplayType.addEntityDataIfNotDefaultValue(transform().id(), this.commonValues);
    }

    @Override
    public void initPackets(Furniture furniture, int entityId, @NotNull Quaternionf conjugated, Consumer<Object> packets) {
        WorldPosition position = furniture.position();
        Vector3f offset = conjugated.transform(new Vector3f(position()));
        packets.accept(FastNMS.INSTANCE.constructor$ClientboundAddEntityPacket(
                entityId, UUID.randomUUID(), position.x() + offset.x, position.y() + offset.y, position.z() - offset.z, 0, position.xRot(),
                MEntityTypes.ITEM_DISPLAY, 0, CoreReflections.instance$Vec3$Zero, 0
        ));
        if (applyDyedColor()) {
            packets.accept(FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(entityId, getCachedValues(
                    furniture.extraData().dyedColor().orElse(null),
                    furniture.extraData().fireworkExplosionColors().orElse(null)
            )));
        } else {
            packets.accept(FastNMS.INSTANCE.constructor$ClientboundSetEntityDataPacket(entityId, getCachedValues(null, null)));
        }
    }

    private synchronized List<Object> getCachedValues(@Nullable Color color, int @Nullable [] colors) {
        List<Object> cachedValues = new ArrayList<>(this.commonValues);
        Item<ItemStack> item = BukkitItemManager.instance().createWrappedItem(item(), null);
        if (item == null) {
            item = BukkitItemManager.instance().wrap(new ItemStack(Material.BARRIER));
        } else {
            if (color != null) {
                item.dyedColor(color);
            }
            if (colors != null) {
                item.fireworkExplosion(new FireworkExplosion(
                    FireworkExplosion.Shape.SMALL_BALL,
                        new IntArrayList(colors),
                        new IntArrayList(),
                        false,
                        false
                ));
            }
        }
        ItemDisplayEntityData.DisplayedItem.addEntityDataIfNotDefaultValue(item.getLiteralObject(), cachedValues);
        return cachedValues;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public static class BuilderImpl implements Builder {
        private boolean applyDyedColor;
        private Key item;
        private Billboard billboard;
        private ItemDisplayContext transform;
        private Vector3f scale;
        private Vector3f translation;
        private Vector3f position;
        private Quaternionf rotation;

        @Override
        public Builder applyDyedColor(boolean applyDyedColor) {
            this.applyDyedColor = applyDyedColor;
            return this;
        }

        @Override
        public Builder item(Key item) {
            this.item = item;
            return this;
        }

        @Override
        public Builder billboard(Billboard billboard) {
            this.billboard = billboard;
            return this;
        }

        @Override
        public Builder transform(ItemDisplayContext transform) {
            this.transform = transform;
            return this;
        }

        @Override
        public Builder scale(Vector3f scale) {
            this.scale = scale;
            return this;
        }

        @Override
        public Builder translation(Vector3f translation) {
            this.translation = translation;
            return this;
        }

        @Override
        public Builder position(Vector3f position) {
            this.position = position;
            return this;
        }

        @Override
        public Builder rotation(Quaternionf rotation) {
            this.rotation = rotation;
            return this;
        }

        @Override
        public FurnitureElement build() {
            return new BukkitFurnitureElement(item, billboard, transform, scale, translation, position, rotation, applyDyedColor);
        }
    }
}
