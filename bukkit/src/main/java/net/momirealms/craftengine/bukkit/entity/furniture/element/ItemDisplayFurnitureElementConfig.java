package net.momirealms.craftengine.bukkit.entity.furniture.element;

import net.momirealms.craftengine.bukkit.entity.data.DisplayData;
import net.momirealms.craftengine.core.entity.display.Billboard;
import net.momirealms.craftengine.core.entity.display.ItemDisplayContext;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.data.FurnitureDataResolver;
import net.momirealms.craftengine.core.entity.furniture.data.FurnitureDataSourceConfig;
import net.momirealms.craftengine.core.entity.furniture.data.ItemPatch;
import net.momirealms.craftengine.core.entity.furniture.data.SourceItemComponentsDataSourceConfig;
import net.momirealms.craftengine.core.entity.furniture.element.ConditionalFurnitureElement;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElementConfigFactory;
import net.momirealms.craftengine.core.entity.furniture.element.TransformableFurnitureElementConfig;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.PlayerContext;
import net.momirealms.craftengine.core.util.Color;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public final class ItemDisplayFurnitureElementConfig implements TransformableFurnitureElementConfig<ItemDisplayFurnitureElement> {
    public static final FurnitureElementConfigFactory<ItemDisplayFurnitureElement> FACTORY = new Factory();
    public final FurnitureMetadataProvider metadata;
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
    public final FurnitureDataSourceConfig<ItemPatch> itemPatchSource;
    public final Color glowColor;
    public final int blockLight;
    public final int skyLight;
    public final float viewRange;
    public final Predicate<PlayerContext> predicate;

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
                                             FurnitureDataSourceConfig<ItemPatch> itemPatchSource,
                                             @Nullable Color glowColor,
                                             int blockLight,
                                             int skyLight,
                                             float viewRange,
                                             Predicate<PlayerContext> predicate) {
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
        this.itemPatchSource = itemPatchSource;
        this.itemId = itemId;
        this.glowColor = glowColor;
        this.blockLight = blockLight;
        this.skyLight = skyLight;
        this.viewRange = viewRange;
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
        Object[] spawnMetadata = createStaticMetadata(false);
        Object[] updateMetadata = createStaticMetadata(true);
        this.metadata = (player, source, force) -> {
            Object[] staticMetadata = force ? updateMetadata : spawnMetadata;
            List<Object> dataValues = new ArrayList<>(staticMetadata.length + 2);
            for (Object value : staticMetadata) {
                dataValues.add(value);
            }
            DisplayData.ItemDisplayData.ItemStack.addEntityData(itemFunction.apply(player, source).minecraftItem(), dataValues);
            DisplayData.ItemDisplayData.ViewRange.addEntityData((float) (this.viewRange * player.displayEntityViewDistance()), dataValues, force);
            return dataValues;
        };
    }

    private Object[] createStaticMetadata(boolean force) {
        List<Object> dataValues = new ArrayList<>();
        if (glowColor != null) {
            DisplayData.ItemDisplayData.SharedFlags.addEntityData((byte) 0x40, dataValues);
            DisplayData.ItemDisplayData.GlowColorOverride.addEntityData(glowColor.color(), dataValues);
        } else {
            DisplayData.ItemDisplayData.SharedFlags.addEntityData((byte) 0x0, dataValues, force);
            DisplayData.ItemDisplayData.GlowColorOverride.addEntityData(-1, dataValues, force);
        }
        DisplayData.ItemDisplayData.Scale.addEntityData(this.scale, dataValues, force);
        DisplayData.ItemDisplayData.LeftRotation.addEntityData(this.rotation, dataValues, force);
        DisplayData.ItemDisplayData.BillboardConstraints.addEntityData(this.billboard.id(), dataValues, force);
        DisplayData.ItemDisplayData.Translation.addEntityData(this.translation, dataValues, force);
        DisplayData.ItemDisplayData.ItemTransform.addEntityData(this.displayContext.id(), dataValues, force);
        DisplayData.ItemDisplayData.ShadowRadius.addEntityData(this.shadowRadius, dataValues, force);
        DisplayData.ItemDisplayData.ShadowStrength.addEntityData(this.shadowStrength, dataValues, force);
        if (this.blockLight != -1 && this.skyLight != -1) {
            DisplayData.ItemDisplayData.BrightnessOverride.addEntityData(this.blockLight << 4 | this.skyLight << 20, dataValues);
        } else {
            DisplayData.ItemDisplayData.BrightnessOverride.addEntityData(-1, dataValues, force);
        }
        return dataValues.toArray();
    }

    @Override
    public @NotNull ItemDisplayFurnitureElement create(@NotNull Furniture furniture, @NotNull WorldPosition pos) {
        return new ItemDisplayFurnitureElement(furniture, this, pos);
    }

    @Override
    public @NotNull ItemDisplayFurnitureElement transform(@NotNull Furniture furniture, @NotNull ItemDisplayFurnitureElement previous, @NotNull WorldPosition pos) {
        return new ItemDisplayFurnitureElement(furniture, this, pos, previous.entityId);
    }

    @Override
    public Class<ItemDisplayFurnitureElement> elementClass() {
        return ItemDisplayFurnitureElement.class;
    }

    @Override
    public @NotNull WorldPosition getPos(@NotNull Furniture furniture) {
        return furniture.placement().elementPosition(this.position, this.xRot, this.yRot);
    }

    public FurnitureDataResolver<ItemPatch> createItemPatch(@NotNull Furniture furniture) {
        return this.itemPatchSource == null ? null : this.itemPatchSource.bind(furniture);
    }

    private static class Factory implements FurnitureElementConfigFactory<ItemDisplayFurnitureElement> {
        private static final String[] DISPLAY_CONTEXT = ConfigKeys.of("display_(context|transform)");
        private static final String[] SHADOW_RADIUS = ConfigKeys.of("shadow_radius");
        private static final String[] SHADOW_STRENGTH = ConfigKeys.of("shadow_strength");
        private static final String[] APPLY_DYED_COLOR = ConfigKeys.of("apply_dyed_color");
        private static final String[] GLOW_COLOR = ConfigKeys.of("glow_color");
        private static final String[] BLOCK_LIGHT = ConfigKeys.of("block_light");
        private static final String[] SKY_LIGHT = ConfigKeys.of("sky_light");
        private static final String[] VIEW_RANGE = ConfigKeys.of("view_range");
        private static final String[] TINT_SOURCE = ConfigKeys.of("tint_source(s)|copy_data");

        @Override
        public ItemDisplayFurnitureElementConfig create(ConfigSection section) {
            ConfigSection brightness = section.getSection("brightness");
            List<Condition<PlayerContext>> conditions = section.getSectionList(ConfigKeys.of("condition(s)"), CommonConditions::fromConfig);
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
                            SourceItemComponentsDataSourceConfig.create(List.of(DataComponentKeys.DYED_COLOR, DataComponentKeys.FIREWORK_EXPLOSION)) :
                            section.getValue(TINT_SOURCE, SourceItemComponentsDataSourceConfig::fromConfig, SourceItemComponentsDataSourceConfig.DEFAULT),
                    section.getValue(GLOW_COLOR, ConfigValue::getAsColor),
                    brightness != null ? brightness.getInt(BLOCK_LIGHT, -1) : -1,
                    brightness != null ? brightness.getInt(SKY_LIGHT, -1) : -1,
                    section.getFloat(VIEW_RANGE, 1f),
                    conditions.isEmpty() ? ConditionalFurnitureElement.ALWAYS_VISIBLE : MiscUtils.allOf(conditions)
            );
        }
    }
}
