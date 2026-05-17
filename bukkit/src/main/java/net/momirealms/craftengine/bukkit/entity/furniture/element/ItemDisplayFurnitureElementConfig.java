package net.momirealms.craftengine.bukkit.entity.furniture.element;

import net.momirealms.craftengine.bukkit.entity.data.DisplayData;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.core.entity.display.Billboard;
import net.momirealms.craftengine.core.entity.display.ItemDisplayContext;
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
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.PlayerContext;
import net.momirealms.craftengine.core.util.Color;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public final class ItemDisplayFurnitureElementConfig implements FurnitureElementConfig<ItemDisplayFurnitureElement> {
    public static final FurnitureElementConfigFactory<ItemDisplayFurnitureElement> FACTORY = new Factory();
    public final BiFunction<Player, FurnitureTintSource, List<Object>> metadata;
    public final Key itemId;
    public final Vector3f scale;
    public final Vector3f position;
    public final Vector3f translation;
    public final float xRot;
    public final float yRot;
    public final Quaternionf rotation;
    public final ItemDisplayContext displayContext;
    public final Billboard billboard;
    public final float shadowRadius;
    public final float shadowStrength;
    public final FurnitureTintSourceConfig<? extends FurnitureTintSource> tint;
    public final Color glowColor;
    public final int blockLight;
    public final int skyLight;
    public final float viewRange;
    public final Predicate<PlayerContext> predicate;
    public final boolean hasCondition;

    private ItemDisplayFurnitureElementConfig(Key itemId,
                                             Vector3f scale,
                                             Vector3f position,
                                             Vector3f translation,
                                             float xRot,
                                             float yRot,
                                             Quaternionf rotation,
                                             ItemDisplayContext displayContext,
                                             Billboard billboard,
                                             float shadowRadius,
                                             float shadowStrength,
                                             FurnitureTintSourceConfig<? extends FurnitureTintSource> tint,
                                             @Nullable Color glowColor,
                                             int blockLight,
                                             int skyLight,
                                             float viewRange,
                                             Predicate<PlayerContext> predicate,
                                             boolean hasCondition) {
        this.scale = scale;
        this.position = position;
        this.translation = translation;
        this.xRot = xRot;
        this.yRot = yRot;
        this.rotation = rotation;
        this.displayContext = displayContext;
        this.billboard = billboard;
        this.shadowRadius = shadowRadius;
        this.shadowStrength = shadowStrength;
        this.tint = tint;
        this.itemId = itemId;
        this.glowColor = glowColor;
        this.blockLight = blockLight;
        this.skyLight = skyLight;
        this.viewRange = viewRange;
        this.predicate = predicate;
        this.hasCondition = hasCondition;
        BiFunction<Player, FurnitureTintSource, Item> itemFunction = (player, tintSource) -> {
            Item wrappedItem = BukkitItemManager.instance().createWrappedItem(itemId, player);
            if (tintSource != null && wrappedItem != null) {
                tintSource.applyTint(wrappedItem);
            }
            return Optional.ofNullable(wrappedItem).orElseGet(() -> BukkitItemManager.instance().createWrappedItem(ItemKeys.BARRIER, null));
        };
        this.metadata = (player, source) -> {
            List<Object> dataValues = new ArrayList<>();
            if (glowColor != null) {
                DisplayData.ItemDisplayData.SharedFlags.addEntityData((byte) 0x40, dataValues);
                DisplayData.ItemDisplayData.GlowColorOverride.addEntityData(glowColor.color(), dataValues);
            }
            DisplayData.ItemDisplayData.ItemStack.addEntityData(itemFunction.apply(player, source).minecraftItem(), dataValues);
            DisplayData.ItemDisplayData.Scale.addEntityDataIfNotDefaultValue(this.scale, dataValues);
            DisplayData.ItemDisplayData.LeftRotation.addEntityDataIfNotDefaultValue(this.rotation, dataValues);
            DisplayData.ItemDisplayData.BillboardConstraints.addEntityDataIfNotDefaultValue(this.billboard.id(), dataValues);
            DisplayData.ItemDisplayData.Translation.addEntityDataIfNotDefaultValue(this.translation, dataValues);
            DisplayData.ItemDisplayData.ItemTransform.addEntityDataIfNotDefaultValue(this.displayContext.id(), dataValues);
            DisplayData.ItemDisplayData.ShadowRadius.addEntityDataIfNotDefaultValue(this.shadowRadius, dataValues);
            DisplayData.ItemDisplayData.ShadowStrength.addEntityDataIfNotDefaultValue(this.shadowStrength, dataValues);
            if (this.blockLight != -1 && this.skyLight != -1) {
                DisplayData.ItemDisplayData.BrightnessOverride.addEntityData(this.blockLight << 4 | this.skyLight << 20, dataValues);
            }
            DisplayData.ItemDisplayData.ViewRange.addEntityDataIfNotDefaultValue((float) (this.viewRange * player.displayEntityViewDistance()), dataValues);
            return dataValues;
        };
    }

    @Override
    public ItemDisplayFurnitureElement create(@NotNull Furniture furniture) {
        return new ItemDisplayFurnitureElement(furniture, this);
    }

    public FurnitureTintSource createTintSource(@NotNull Furniture furniture) {
        return this.tint == null ? null : this.tint.create(furniture);
    }

    private static class Factory implements FurnitureElementConfigFactory<ItemDisplayFurnitureElement> {
        private static final String[] DISPLAY_CONTEXT = new String[] {"display_context", "display_transform", "display-context", "display-transform"};
        private static final String[] SHADOW_RADIUS = new String[] {"shadow_radius", "shadow-radius"};
        private static final String[] SHADOW_STRENGTH = new String[] {"shadow_strength", "shadow-strength"};
        private static final String[] APPLY_DYED_COLOR = new String[] {"apply_dyed_color", "apply-dyed-color"};
        private static final String[] GLOW_COLOR = new String[] {"glow_color", "glow-color"};
        private static final String[] BLOCK_LIGHT = new String[] {"block_light", "block-light"};
        private static final String[] SKY_LIGHT = new String[] {"sky_light", "sky-light"};
        private static final String[] VIEW_RANGE = new String[] {"view_range", "view-range"};
        private static final String[] TINT_SOURCE = new String[] {"tint_source", "tint-source"};

        @Override
        public ItemDisplayFurnitureElementConfig create(ConfigSection section) {
            ConfigSection brightness = section.getSection("brightness");
            List<Condition<PlayerContext>> conditions = section.getSectionList("conditions", CommonConditions::fromConfig);
            boolean legacyTintSource = section.getBoolean(APPLY_DYED_COLOR, false);
            return new ItemDisplayFurnitureElementConfig(
                    section.getNonNullIdentifier("item"),
                    section.getVector3f("scale", ConfigConstants.NORMAL_SCALE),
                    section.getVector3f("position", ConfigConstants.ZERO_VECTOR3),
                    section.getVector3f("translation", ConfigConstants.ZERO_VECTOR3),
                    section.getFloat("pitch", 0f),
                    section.getFloat("yaw", 0f),
                    section.getQuaternion("rotation", ConfigConstants.ZERO_QUATERNION),
                    section.getEnum(DISPLAY_CONTEXT, ItemDisplayContext.class, ItemDisplayContext.NONE),
                    section.getEnum("billboard", Billboard.class, Billboard.FIXED),
                    section.getFloat(SHADOW_RADIUS, 0f),
                    section.getFloat(SHADOW_STRENGTH, 1f),
                    legacyTintSource ?
                            DefaultFurnitureTintSourceConfig.create(List.of(DataComponentKeys.DYED_COLOR, DataComponentKeys.FIREWORK_EXPLOSION)) :
                            section.getValue(TINT_SOURCE, FurnitureTintSources::fromConfig),
                    section.getValue(GLOW_COLOR, ConfigValue::getAsColor),
                    brightness != null ? brightness.getInt(BLOCK_LIGHT, -1) : -1,
                    brightness != null ? brightness.getInt(SKY_LIGHT, -1) : -1,
                    section.getFloat(VIEW_RANGE, 1f),
                    MiscUtils.allOf(conditions),
                    !conditions.isEmpty()
            );
        }
    }
}
