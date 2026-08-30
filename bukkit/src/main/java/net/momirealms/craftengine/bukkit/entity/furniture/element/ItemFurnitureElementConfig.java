package net.momirealms.craftengine.bukkit.entity.furniture.element;

import net.momirealms.craftengine.bukkit.entity.data.item.ItemEntityData;
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
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public final class ItemFurnitureElementConfig implements FurnitureElementConfig<ItemFurnitureElement> {
    public static final FurnitureElementConfigFactory<ItemFurnitureElement> FACTORY = new Factory();
    public final BiFunction<Player, FurnitureDataResolver<ItemPatch>, List<Object>> metadata;
    public final Key itemId;
    public final FurnitureDataSourceConfig<ItemPatch> itemPatchSource;
    public final Vector3f position;
    public final Predicate<PlayerContext> predicate;
    public final boolean hasCondition;

    private ItemFurnitureElementConfig(Key itemId,
                                      Vector3f position,
                                      FurnitureDataSourceConfig<ItemPatch> itemPatchSource,
                                      Predicate<PlayerContext> predicate,
                                      boolean hasCondition) {
        this.position = position;
        this.itemPatchSource = itemPatchSource;
        this.itemId = itemId;
        this.hasCondition = hasCondition;
        this.predicate = predicate;
        BiFunction<Player, FurnitureDataResolver<ItemPatch>, Item> itemFunction = (player, itemPatch) -> {
            Item wrappedItem = Item.byId(itemId, player);
            if (itemPatch != null && wrappedItem != null) {
                ItemPatch patch = itemPatch.resolve();
                if (patch != null) {
                    patch.applyTo(wrappedItem);
                }
            }
            return Optional.ofNullable(wrappedItem).orElseGet(() -> Item.byId(ItemKeys.BARRIER));
        };
        this.metadata = (player, source) -> {
            List<Object> dataValues = new ArrayList<>();
            ItemEntityData.Item.addEntityData(itemFunction.apply(player, source).minecraftItem(), dataValues);
            ItemEntityData.NoGravity.addEntityData(true, dataValues);
            return dataValues;
        };
    }

    @Override
    public ItemFurnitureElement create(@NotNull Furniture furniture) {
        return new ItemFurnitureElement(furniture, this, getPos(furniture));
    }

    @Override
    public ItemFurnitureElement create(@NotNull Furniture furniture, @NotNull ItemFurnitureElement previous) {
        Vec3d pos = getPos(furniture);
        return new ItemFurnitureElement(furniture, this, pos, previous.entityId1, previous.entityId2, !pos.equals(previous.position));
    }

    @Override
    public ItemFurnitureElement createExact(@NotNull Furniture furniture, @NotNull ItemFurnitureElement previous) {
        Vec3d pos = getPos(furniture);
        if (!pos.equals(previous.position)) {
            return null;
        }
        return new ItemFurnitureElement(furniture, this, pos, previous.entityId1, previous.entityId2, false);
    }

    @Override
    public Class<ItemFurnitureElement> elementClass() {
        return ItemFurnitureElement.class;
    }

    public Vec3d getPos(Furniture furniture) {
        WorldPosition furniturePos = furniture.position();
        return Furniture.getRelativePosition(furniturePos, this.position);
    }

    public FurnitureDataResolver<ItemPatch> createItemPatch(@NotNull Furniture furniture) {
        return this.itemPatchSource == null ? null : this.itemPatchSource.bind(furniture);
    }

    private static class Factory implements FurnitureElementConfigFactory<ItemFurnitureElement> {
        private static final String[] APPLY_DYED_COLOR = ConfigKeys.of("apply_dyed_color");
        private static final String[] TINT_SOURCE = ConfigKeys.of("tint_source(s)|copy_data");

        @Override
        public ItemFurnitureElementConfig create(ConfigSection section) {
            List<Condition<PlayerContext>> conditions = section.getSectionList(ConfigKeys.of("condition(s)"), CommonConditions::fromConfig);
            boolean legacyTintSource = section.getBoolean(APPLY_DYED_COLOR, false);
            return new ItemFurnitureElementConfig(
                    section.getNonNullIdentifier("item"),
                    section.getVector3f("position", ConfigConstants.ZERO_VECTOR3),
                    legacyTintSource ?
                            SourceItemComponentsDataSourceConfig.create(List.of(DataComponentKeys.DYED_COLOR, DataComponentKeys.FIREWORK_EXPLOSION)) :
                            section.getValue(TINT_SOURCE, SourceItemComponentsDataSourceConfig::fromConfig),
                    MiscUtils.allOf(conditions),
                    !conditions.isEmpty()
            );
        }
    }
}
