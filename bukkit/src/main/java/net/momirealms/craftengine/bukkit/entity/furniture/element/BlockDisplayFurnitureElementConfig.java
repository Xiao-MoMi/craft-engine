package net.momirealms.craftengine.bukkit.entity.furniture.element;

import net.momirealms.craftengine.bukkit.entity.data.DisplayData;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.entity.display.Billboard;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.element.ConditionalFurnitureElement;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElementConfigFactory;
import net.momirealms.craftengine.core.entity.furniture.element.TransformableFurnitureElementConfig;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.PlayerContext;
import net.momirealms.craftengine.core.util.Color;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class BlockDisplayFurnitureElementConfig implements TransformableFurnitureElementConfig<BlockDisplayFurnitureElement> {
    public static final FurnitureElementConfigFactory<BlockDisplayFurnitureElement> FACTORY = new Factory();
    public final FurnitureMetadataProvider metadata;
    public final LazyReference<BlockStateWrapper> blockState;
    public final Vector3f scale;
    public final Vector3f position;
    public final Vector3f translation;
    public final float xRot;
    public final float yRot;
    public final Quaternionf rotation;
    public final Billboard billboard;
    public final float shadowRadius;
    public final float shadowStrength;
    public final Color glowColor;
    public final int blockLight;
    public final int skyLight;
    public final float viewRange;
    public final Predicate<PlayerContext> predicate;

    private BlockDisplayFurnitureElementConfig(LazyReference<BlockStateWrapper> blockState,
                                               Vector3f scale,
                                               Vector3f position,
                                               Vector3f translation,
                                               float xRot,
                                               float yRot,
                                               Quaternionf rotation,
                                               Billboard billboard,
                                               float shadowRadius,
                                               float shadowStrength,
                                               @Nullable Color glowColor,
                                               int blockLight,
                                               int skyLight,
                                               float viewRange,
                                               Predicate<PlayerContext> predicate) {
        this.blockState = blockState;
        this.scale = scale;
        this.position = position;
        this.translation = translation;
        this.xRot = xRot;
        this.yRot = yRot;
        this.rotation = rotation;
        this.billboard = billboard;
        this.glowColor = glowColor;
        this.shadowRadius = shadowRadius;
        this.shadowStrength = shadowStrength;
        this.blockLight = blockLight;
        this.skyLight = skyLight;
        this.viewRange = viewRange;
        this.predicate = predicate;
        this.metadata = (player, tintSource, force) -> {
            List<Object> dataValues = new ArrayList<>();
            if (glowColor != null) {
                DisplayData.BlockDisplayData.SharedFlags.addEntityData((byte) 0x40, dataValues);
                DisplayData.BlockDisplayData.GlowColorOverride.addEntityData(glowColor.color(), dataValues);
            } else {
                DisplayData.BlockDisplayData.SharedFlags.addEntityData((byte) 0x0, dataValues);
                DisplayData.BlockDisplayData.GlowColorOverride.addEntityData(-1, dataValues);
            }
            DisplayData.BlockDisplayData.BlockState.addEntityData(this.blockState.get().minecraftState(), dataValues, force);
            DisplayData.BlockDisplayData.Scale.addEntityData(this.scale, dataValues, force);
            DisplayData.BlockDisplayData.LeftRotation.addEntityData(this.rotation, dataValues, force);
            DisplayData.BlockDisplayData.BillboardConstraints.addEntityData(this.billboard.id(), dataValues, force);
            DisplayData.BlockDisplayData.Translation.addEntityData(this.translation, dataValues, force);
            DisplayData.BlockDisplayData.ShadowRadius.addEntityData(this.shadowRadius, dataValues, force);
            DisplayData.BlockDisplayData.ShadowStrength.addEntityData(this.shadowStrength, dataValues, force);
            if (this.blockLight != -1 && this.skyLight != -1) {
                DisplayData.BlockDisplayData.BrightnessOverride.addEntityData(this.blockLight << 4 | this.skyLight << 20, dataValues, force);
            } else {
                DisplayData.BlockDisplayData.BrightnessOverride.addEntityData(-1, dataValues, force);
            }
            DisplayData.BlockDisplayData.ViewRange.addEntityData((float) (this.viewRange * player.displayEntityViewDistance()), dataValues, force);
            return dataValues;
        };
    }

    @Override
    public @NotNull BlockDisplayFurnitureElement create(@NotNull Furniture furniture, @NotNull WorldPosition pos) {
        return new BlockDisplayFurnitureElement(furniture, this, pos);
    }

    @Override
    public @NotNull BlockDisplayFurnitureElement transform(@NotNull Furniture furniture, @NotNull BlockDisplayFurnitureElement previous, @NotNull WorldPosition pos) {
        return new BlockDisplayFurnitureElement(furniture, this, pos, previous.entityId);
    }

    @Override
    public Class<BlockDisplayFurnitureElement> elementClass() {
        return BlockDisplayFurnitureElement.class;
    }

    @Override
    public @NotNull WorldPosition getPos(@NotNull Furniture furniture) {
        return furniture.placement().elementPosition(this.position, this.xRot, this.yRot);
    }

    private static class Factory implements FurnitureElementConfigFactory<BlockDisplayFurnitureElement> {
        private static final String[] SHADOW_RADIUS = ConfigKeys.of("shadow_radius");
        private static final String[] SHADOW_STRENGTH = ConfigKeys.of("shadow_strength");
        private static final String[] GLOW_COLOR = ConfigKeys.of("glow_color");
        private static final String[] BLOCK_LIGHT = ConfigKeys.of("block_light");
        private static final String[] SKY_LIGHT = ConfigKeys.of("sky_light");
        private static final String[] VIEW_RANGE = ConfigKeys.of("view_range");

        @Override
        public BlockDisplayFurnitureElementConfig create(ConfigSection section) {
            ConfigSection brightness = section.getSection("brightness");
            List<Condition<PlayerContext>> conditions = section.getSectionList(ConfigKeys.of("condition(s)"), CommonConditions::fromConfig);
            return new BlockDisplayFurnitureElementConfig(
                    section.getNonNullValue("block", ConfigConstants.ARGUMENT_BLOCK_STATE, v -> {
                        String state = v.getAsString();
                        return LazyReference.untilNotNull(() -> CraftEngine.instance().blockManager().createBlockState(state));
                    }),
                    section.getVector3f("scale", ConfigConstants.NORMAL_SCALE),
                    section.getVector3f("position", ConfigConstants.ZERO_VECTOR3),
                    section.getVector3f("translation", ConfigConstants.ZERO_VECTOR3),
                    section.getFloat("pitch", 0f),
                    section.getFloat("yaw", 0f),
                    section.getQuaternion("rotation", ConfigConstants.ZERO_QUATERNION),
                    section.getEnum("billboard", Billboard.class, Billboard.FIXED),
                    section.getFloat(SHADOW_RADIUS, 0f),
                    section.getFloat(SHADOW_STRENGTH, 1f),
                    section.getValue(GLOW_COLOR, ConfigValue::getAsColor),
                    brightness != null ? brightness.getInt(BLOCK_LIGHT, -1) : -1,
                    brightness != null ? brightness.getInt(SKY_LIGHT, -1) : -1,
                    section.getFloat(VIEW_RANGE, 1f),
                    conditions.isEmpty() ? ConditionalFurnitureElement.ALWAYS_VISIBLE : MiscUtils.allOf(conditions)
            );
        }
    }
}
