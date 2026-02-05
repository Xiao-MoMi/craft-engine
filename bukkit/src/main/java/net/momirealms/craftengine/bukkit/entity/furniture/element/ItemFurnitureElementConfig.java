package net.momirealms.craftengine.bukkit.entity.furniture.element;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.momirealms.craftengine.bukkit.entity.data.ItemEntityData;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.item.DataComponentTypes;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.FurnitureColorSource;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElementConfig;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElementConfigFactory;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.item.data.FireworkExplosion;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.PlayerContext;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public final class ItemFurnitureElementConfig implements FurnitureElementConfig<ItemFurnitureElement> {
    public static final FurnitureElementConfigFactory<ItemFurnitureElement> FACTORY = new Factory();
    public final BiFunction<Player, FurnitureColorSource, List<Object>> metadata;
    public final Key itemId;
    public final boolean applyDyedColor;
    public final Vector3f position;
    public final Predicate<PlayerContext> predicate;
    public final boolean hasCondition;

    private ItemFurnitureElementConfig(Key itemId,
                                      Vector3f position,
                                      boolean applyDyedColor,
                                      Predicate<PlayerContext> predicate,
                                      boolean hasCondition) {
        this.position = position;
        this.applyDyedColor = applyDyedColor;
        this.itemId = itemId;
        this.hasCondition = hasCondition;
        this.predicate = predicate;
        BiFunction<Player, FurnitureColorSource, Item<?>> itemFunction = (player, colorSource) -> {
            Item<ItemStack> wrappedItem = BukkitItemManager.instance().createWrappedItem(itemId, player);
            if (applyDyedColor && colorSource != null && wrappedItem != null) {
                Optional.ofNullable(colorSource.dyedColor()).ifPresent(wrappedItem::dyedColor);
                Optional.ofNullable(colorSource.fireworkColors()).ifPresent(colors -> wrappedItem.fireworkExplosion(new FireworkExplosion(
                        FireworkExplosion.Shape.SMALL_BALL,
                        new IntArrayList(colors),
                        new IntArrayList(),
                        false,
                        false
                )));
                if (DataComponentTypes.POTION_CONTENTS != null) {
                    Optional.ofNullable(colorSource.potionContents()).ifPresent(contents -> wrappedItem.setComponent(DataComponentTypes.POTION_CONTENTS, contents));
                }
            }
            return Optional.ofNullable(wrappedItem).orElseGet(() -> BukkitItemManager.instance().createWrappedItem(ItemKeys.BARRIER, null));
        };
        this.metadata = (player, source) -> {
            List<Object> dataValues = new ArrayList<>();
            ItemEntityData.Item.addEntityData(itemFunction.apply(player, source).getLiteralObject(), dataValues);
            ItemEntityData.NoGravity.addEntityData(true, dataValues);
            return dataValues;
        };
    }

    @Override
    public ItemFurnitureElement create(@NotNull Furniture furniture) {
        return new ItemFurnitureElement(furniture, this);
    }

    private static class Factory implements FurnitureElementConfigFactory<ItemFurnitureElement> {

        @Override
        public ItemFurnitureElementConfig create(Map<String, Object> arguments) {
            List<Condition<PlayerContext>> conditions = ResourceConfigUtils.parseConfigAsList(arguments.get("conditions"), CommonConditions::fromMap);
            return new ItemFurnitureElementConfig(
                    Key.of(ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("item"), "warning.config.furniture.element.item.missing_item")),
                    ResourceConfigUtils.getAsVector3f(arguments.getOrDefault("position", 0f), "position"),
                    ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("apply-dyed-color", true), "apply-dyed-color"),
                    MiscUtils.allOf(conditions),
                    !conditions.isEmpty()
            );
        }
    }
}
