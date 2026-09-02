package net.momirealms.craftengine.bukkit.entity.furniture.element;

import net.momirealms.craftengine.bukkit.entity.data.BaseEntityData;
import net.momirealms.craftengine.bukkit.entity.data.decoration.ArmorStandData;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.data.*;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElementConfig;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElementConfigFactory;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.PlayerContext;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.LegacyChatFormatter;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public final class ArmorStandFurnitureElementConfig implements FurnitureElementConfig<ArmorStandFurnitureElement> {
    public static final FurnitureElementConfigFactory<ArmorStandFurnitureElement> FACTORY = new Factory();
    public final Function<Player, List<Object>> metadata;
    public final Key itemId;
    public final float scale;
    public final FurnitureDataSourceConfig<ItemPatch> itemPatchSource;
    public final Vector3f position;
    public final float xRot;
    public final float yRot;
    public final boolean small;
    public final LegacyChatFormatter glowColor;
    public final Predicate<PlayerContext> predicate;
    public final boolean hasCondition;

    private ArmorStandFurnitureElementConfig(Key itemId,
                                             float scale,
                                             Vector3f position,
                                             float xRot,
                                             float yRot,
                                             FurnitureDataSourceConfig<ItemPatch> itemPatchSource,
                                             boolean small,
                                             LegacyChatFormatter glowColor,
                                             Predicate<PlayerContext> predicate,
                                             boolean hasCondition) {
        this.position = position;
        this.xRot = xRot;
        this.yRot = yRot;
        this.itemPatchSource = itemPatchSource;
        this.small = small;
        this.scale = scale;
        this.itemId = itemId;
        this.glowColor = glowColor;
        this.predicate = predicate;
        this.hasCondition = hasCondition;
        this.metadata = (player) -> {
            List<Object> dataValues = new ArrayList<>(2);
            if (glowColor != null) {
                BaseEntityData.SharedFlags.addEntityData((byte) 0x60, dataValues);
            } else {
                BaseEntityData.SharedFlags.addEntityData((byte) 0x20, dataValues);
            }
            if (small) {
                ArmorStandData.ClientFlags.addEntityData((byte) 0x01, dataValues);
            }
            return dataValues;
        };
    }

    public Item item(Player player, FurnitureDataResolver<ItemPatch> itemPatch) {
        Item wrappedItem = Item.byId(this.itemId, player);
        if (itemPatch != null && wrappedItem != null) {
            ItemPatch patch = itemPatch.resolve();
            if (patch != null) {
                patch.applyTo(wrappedItem);
            }
        }
        return Optional.ofNullable(wrappedItem).orElseGet(() -> Item.byId(ItemKeys.BARRIER));
    }

    public FurnitureDataResolver<ItemPatch> createItemPatch(@NotNull Furniture furniture) {
        return this.itemPatchSource == null ? null : this.itemPatchSource.bind(furniture);
    }

    @Override
    public ArmorStandFurnitureElement create(@NotNull Furniture furniture) {
        return new ArmorStandFurnitureElement(furniture, this, getPos(furniture));
    }

    @Override
    public ArmorStandFurnitureElement create(@NotNull Furniture furniture, @NotNull ArmorStandFurnitureElement previous) {
        WorldPosition pos = getPos(furniture);
        return new ArmorStandFurnitureElement(furniture, this, pos, previous.entityId, !pos.equals(previous.position));
    }

    @Override
    public ArmorStandFurnitureElement createExact(@NotNull Furniture furniture, @NotNull ArmorStandFurnitureElement previous) {
        WorldPosition pos = getPos(furniture);
        if (!pos.equals(previous.position)) {
            return null;
        }
        return new ArmorStandFurnitureElement(furniture, this, pos, previous.entityId, false);
    }

    @Override
    public Class<ArmorStandFurnitureElement> elementClass() {
        return ArmorStandFurnitureElement.class;
    }

    public WorldPosition getPos(Furniture furniture) {
        WorldPosition furniturePos = furniture.position();
        Vec3d position = Furniture.getRelativePosition(furniturePos, this.position);
        return new WorldPosition(furniturePos.world, position.x, position.y, position.z, furniturePos.xRot + xRot, furniturePos.yRot + yRot);
    }

    private static class Factory implements FurnitureElementConfigFactory<ArmorStandFurnitureElement> {
        private static final String[] APPLY_DYED_COLOR = ConfigKeys.of("apply_dyed_color");
        private static final String[] GLOW_COLOR = ConfigKeys.of("glow_color");
        private static final String[] TINT_SOURCE = ConfigKeys.of("tint_source(s)|copy_data");

        @Override
        public ArmorStandFurnitureElementConfig create(ConfigSection section) {
            List<Condition<PlayerContext>> conditions = section.getSectionList(ConfigKeys.of("condition(s)"), CommonConditions::fromConfig);
            boolean legacyTintSource = section.getBoolean(APPLY_DYED_COLOR, false);
            return new ArmorStandFurnitureElementConfig(
                    section.getNonNullIdentifier("item"),
                    section.getFloat("scale", 1f),
                    section.getVector3f("position", ConfigConstants.ZERO_VECTOR3),
                    section.getFloat("pitch", 0f),
                    section.getFloat("yaw", 0f),
                    legacyTintSource ?
                            SourceItemComponentsDataSourceConfig.create(List.of(DataComponentKeys.DYED_COLOR, DataComponentKeys.FIREWORK_EXPLOSION)) :
                            section.getValue(TINT_SOURCE, SourceItemComponentsDataSourceConfig::fromConfig, SourceItemComponentsDataSourceConfig.DEFAULT),
                    section.getBoolean("small"),
                    section.getEnum(GLOW_COLOR, LegacyChatFormatter.class),
                    MiscUtils.allOf(conditions),
                    !conditions.isEmpty()
            );
        }
    }
}
