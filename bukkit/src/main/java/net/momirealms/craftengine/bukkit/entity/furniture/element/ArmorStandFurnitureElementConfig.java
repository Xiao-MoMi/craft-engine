package net.momirealms.craftengine.bukkit.entity.furniture.element;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.momirealms.craftengine.bukkit.entity.data.ArmorStandData;
import net.momirealms.craftengine.bukkit.entity.data.BaseEntityData;
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
import net.momirealms.craftengine.core.util.LegacyChatFormatter;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public final class ArmorStandFurnitureElementConfig implements FurnitureElementConfig<ArmorStandFurnitureElement> {
    public static final FurnitureElementConfigFactory<ArmorStandFurnitureElement> FACTORY = new Factory();
    public final Function<Player, List<Object>> metadata;
    public final Key itemId;
    public final float scale;
    public final boolean applyDyedColor;
    public final Vector3f position;
    public final boolean small;
    public final LegacyChatFormatter glowColor;
    public final Predicate<PlayerContext> predicate;
    public final boolean hasCondition;

    private ArmorStandFurnitureElementConfig(Key itemId,
                                            float scale,
                                            Vector3f position,
                                            boolean applyDyedColor,
                                            boolean small,
                                            LegacyChatFormatter glowColor,
                                            Predicate<PlayerContext> predicate,
                                            boolean hasCondition) {
        this.position = position;
        this.applyDyedColor = applyDyedColor;
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
                ArmorStandData.ArmorStandFlags.addEntityData((byte) 0x01, dataValues);
            }
            return dataValues;
        };
    }

    public Item<?> item(Player player, FurnitureColorSource colorSource) {
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
    }

    @Override
    public ArmorStandFurnitureElement create(@NotNull Furniture furniture) {
        return new ArmorStandFurnitureElement(furniture, this);
    }

    private static class Factory implements FurnitureElementConfigFactory<ArmorStandFurnitureElement> {

        @Override
        public ArmorStandFurnitureElementConfig create(Map<String, Object> arguments) {
            List<Condition<PlayerContext>> conditions = ResourceConfigUtils.parseConfigAsList(arguments.get("conditions"), CommonConditions::fromMap);
            return new ArmorStandFurnitureElementConfig(
                    Key.of(ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("item"), "warning.config.furniture.element.armor_stand.missing_item")),
                    ResourceConfigUtils.getAsFloat(arguments.getOrDefault("scale", 1f), "scale"),
                    ResourceConfigUtils.getAsVector3f(arguments.getOrDefault("position", 0f), "position"),
                    ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("apply-dyed-color", true), "apply-dyed-color"),
                    ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("small", false), "small"),
                    ResourceConfigUtils.getAsEnum(arguments.get("glow-color"), LegacyChatFormatter.class, null),
                    MiscUtils.allOf(conditions),
                    !conditions.isEmpty()
            );
        }
    }
}
