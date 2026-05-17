package net.momirealms.craftengine.bukkit.entity.furniture.element;

import net.momirealms.craftengine.bukkit.entity.data.item.ItemEntityData;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElementConfig;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElementConfigFactory;
import net.momirealms.craftengine.core.entity.furniture.element.tint.DefaultFurnitureTintSourceConfig;
import net.momirealms.craftengine.core.entity.furniture.element.tint.FurnitureTintSource;
import net.momirealms.craftengine.core.entity.furniture.element.tint.FurnitureTintSourceConfig;
import net.momirealms.craftengine.core.entity.furniture.element.tint.FurnitureTintSources;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.PlayerContext;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public final class ItemFurnitureElementConfig implements FurnitureElementConfig<ItemFurnitureElement> {
    public static final FurnitureElementConfigFactory<ItemFurnitureElement> FACTORY = new Factory();
    public final BiFunction<Player, FurnitureTintSource, List<Object>> metadata;
    public final Key itemId;
    public final FurnitureTintSourceConfig<? extends FurnitureTintSource> tint;
    public final Vector3f position;
    public final Predicate<PlayerContext> predicate;
    public final boolean hasCondition;

    private ItemFurnitureElementConfig(Key itemId,
                                      Vector3f position,
                                       FurnitureTintSourceConfig<? extends FurnitureTintSource> tint,
                                      Predicate<PlayerContext> predicate,
                                      boolean hasCondition) {
        this.position = position;
        this.tint = tint;
        this.itemId = itemId;
        this.hasCondition = hasCondition;
        this.predicate = predicate;
        BiFunction<Player, FurnitureTintSource, Item> itemFunction = (player, tintSource) -> {
            Item wrappedItem = BukkitItemManager.instance().createWrappedItem(itemId, player);
            if (tintSource != null && wrappedItem != null) {
                tintSource.applyTint(wrappedItem);
            }
            return Optional.ofNullable(wrappedItem).orElseGet(() -> BukkitItemManager.instance().createWrappedItem(ItemKeys.BARRIER, null));
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
        return new ItemFurnitureElement(furniture, this);
    }

    public FurnitureTintSource createTintSource(@NotNull Furniture furniture) {
        return this.tint == null ? null : this.tint.create(furniture);
    }

    private static class Factory implements FurnitureElementConfigFactory<ItemFurnitureElement> {
        private static final String[] APPLY_DYED_COLOR = new String[] {"apply_dyed_color", "apply-dyed-color"};
        private static final String[] TINT_SOURCE = new String[] {"tint_source", "tint-source"};

        @Override
        public ItemFurnitureElementConfig create(ConfigSection section) {
            List<Condition<PlayerContext>> conditions = section.getSectionList("conditions", CommonConditions::fromConfig);
            boolean legacyTintSource = section.getBoolean(APPLY_DYED_COLOR, false);
            return new ItemFurnitureElementConfig(
                    section.getNonNullIdentifier("item"),
                    section.getVector3f("position", ConfigConstants.ZERO_VECTOR3),
                    legacyTintSource ?
                            DefaultFurnitureTintSourceConfig.create(List.of(DataComponentKeys.DYED_COLOR, DataComponentKeys.FIREWORK_EXPLOSION)) :
                            section.getValue(TINT_SOURCE, FurnitureTintSources::fromConfig),
                    MiscUtils.allOf(conditions),
                    !conditions.isEmpty()
            );
        }
    }
}
