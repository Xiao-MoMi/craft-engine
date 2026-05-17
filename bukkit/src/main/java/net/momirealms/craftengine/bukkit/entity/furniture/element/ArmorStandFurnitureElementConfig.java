package net.momirealms.craftengine.bukkit.entity.furniture.element;

import net.momirealms.craftengine.bukkit.entity.data.BaseEntityData;
import net.momirealms.craftengine.bukkit.entity.data.decoration.ArmorStandData;
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
import net.momirealms.craftengine.core.util.LegacyChatFormatter;
import net.momirealms.craftengine.core.util.MiscUtils;
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
    public final FurnitureTintSourceConfig<? extends FurnitureTintSource> tint;
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
                                             FurnitureTintSourceConfig<? extends FurnitureTintSource> tint,
                                             boolean small,
                                             LegacyChatFormatter glowColor,
                                             Predicate<PlayerContext> predicate,
                                             boolean hasCondition) {
        this.position = position;
        this.xRot = xRot;
        this.yRot = yRot;
        this.tint = tint;
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

    public Item item(Player player, FurnitureTintSource tintSource) {
        Item wrappedItem = BukkitItemManager.instance().createWrappedItem(this.itemId, player);
        if (tintSource != null && wrappedItem != null) {
            tintSource.applyTint(wrappedItem);
        }
        return Optional.ofNullable(wrappedItem).orElseGet(() -> BukkitItemManager.instance().createWrappedItem(ItemKeys.BARRIER, null));
    }

    public FurnitureTintSource createTintSource(@NotNull Furniture furniture) {
        return this.tint == null ? null : this.tint.create(furniture);
    }

    @Override
    public ArmorStandFurnitureElement create(@NotNull Furniture furniture) {
        return new ArmorStandFurnitureElement(furniture, this);
    }

    private static class Factory implements FurnitureElementConfigFactory<ArmorStandFurnitureElement> {
        private static final String[] APPLY_DYED_COLOR = new String[]{"apply_dyed_color", "apply-dyed-color"};
        private static final String[] GLOW_COLOR = new String[]{"glow_color", "glow-color"};
        private static final String[] TINT_SOURCE = new String[]{"tint_source", "tint-source"};

        @Override
        public ArmorStandFurnitureElementConfig create(ConfigSection section) {
            List<Condition<PlayerContext>> conditions = section.getSectionList("conditions", CommonConditions::fromConfig);
            boolean legacyTintSource = section.getBoolean(APPLY_DYED_COLOR, false);
            return new ArmorStandFurnitureElementConfig(
                    section.getNonNullIdentifier("item"),
                    section.getFloat("scale", 1f),
                    section.getVector3f("position", ConfigConstants.ZERO_VECTOR3),
                    section.getFloat("pitch", 0f),
                    section.getFloat("yaw", 0f),
                    legacyTintSource ?
                            DefaultFurnitureTintSourceConfig.create(List.of(DataComponentKeys.DYED_COLOR, DataComponentKeys.FIREWORK_EXPLOSION)) :
                            section.getValue(TINT_SOURCE, FurnitureTintSources::fromConfig),
                    section.getBoolean("small"),
                    section.getEnum(GLOW_COLOR, LegacyChatFormatter.class),
                    MiscUtils.allOf(conditions),
                    !conditions.isEmpty()
            );
        }
    }
}
