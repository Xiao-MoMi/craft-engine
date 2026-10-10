package net.momirealms.craftengine.bukkit.block.entity.renderer.constant;

import com.google.common.base.Objects;
import net.momirealms.craftengine.bukkit.entity.data.DisplayData;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.entity.render.BlockEntityRenderOffset;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElementConfig;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElementConfigFactory;
import net.momirealms.craftengine.core.entity.display.Billboard;
import net.momirealms.craftengine.core.entity.player.Player;
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
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class BlockDisplayBlockEntityElementConfig implements BlockEntityElementConfig<BlockDisplayBlockEntityElement> {
    public static final BlockEntityElementConfigFactory<BlockDisplayBlockEntityElement> FACTORY = new Factory();
    public final BlockEntityMetadataProvider lazyMetadataPacket;
    public final LazyReference<BlockStateWrapper> blockState;
    public final Vector3f scale;
    public final Vector3f position;
    public final BlockEntityRenderOffset offset;
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
    public final boolean hasCondition;

    public BlockDisplayBlockEntityElementConfig(LazyReference<BlockStateWrapper> blockState,
                                                Vector3f scale,
                                                Vector3f position,
                                                BlockEntityRenderOffset offset,
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
                                                Predicate<PlayerContext> predicate,
                                                boolean hasCondition) {
        this.blockState = blockState;
        this.scale = scale;
        this.position = position;
        this.offset = offset;
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
        this.hasCondition = hasCondition;
        this.predicate = predicate;
        this.lazyMetadataPacket = (player, ts, force) -> {
            List<Object> dataValues = new ArrayList<>();
            if (glowColor != null) {
                DisplayData.BlockDisplayData.SharedFlags.addEntityData((byte) 0x40, dataValues);
                DisplayData.BlockDisplayData.GlowColorOverride.addEntityData(glowColor.color(), dataValues);
            } else {
                DisplayData.BlockDisplayData.SharedFlags.addEntityData((byte) 0x0, dataValues, force);
                DisplayData.BlockDisplayData.GlowColorOverride.addEntityData(-1, dataValues, force);
            }
            DisplayData.BlockDisplayData.BlockState.addEntityData(this.blockState.get().minecraftState(), dataValues, force);
            DisplayData.BlockDisplayData.Scale.addEntityData(this.scale, dataValues, force);
            DisplayData.BlockDisplayData.LeftRotation.addEntityData(this.rotation, dataValues, force);
            DisplayData.BlockDisplayData.BillboardConstraints.addEntityData(this.billboard.id(), dataValues, force);
            DisplayData.BlockDisplayData.Translation.addEntityData(this.translation, dataValues, force);
            DisplayData.BlockDisplayData.ShadowRadius.addEntityData(this.shadowRadius, dataValues, force);
            DisplayData.BlockDisplayData.ShadowStrength.addEntityData(this.shadowStrength, dataValues, force);
            if (this.blockLight != -1 && this.skyLight != -1) {
                DisplayData.BlockDisplayData.BrightnessOverride.addEntityData(this.blockLight << 4 | this.skyLight << 20, dataValues);
            } else {
                DisplayData.BlockDisplayData.BrightnessOverride.addEntityData(-1, dataValues, force);
            }
            DisplayData.BlockDisplayData.ViewRange.addEntityData((float) (this.viewRange * player.displayEntityViewDistance()), dataValues, force);
            return dataValues;
        };
    }

    @Override
    public BlockDisplayBlockEntityElement create(CEChunk chunk, BlockPos pos) {
        return new BlockDisplayBlockEntityElement(this, pos);
    }

    @Override
    public BlockDisplayBlockEntityElement create(CEChunk chunk, BlockPos pos, BlockDisplayBlockEntityElement previous) {
        return new BlockDisplayBlockEntityElement(this, pos, previous.entityId,
                previous.config.yRot != this.yRot ||
                        previous.config.xRot != this.xRot ||
                        !previous.config.offset.equals(this.offset) ||
                        !previous.config.position.equals(this.position)
        );
    }

    @Override
    public BlockDisplayBlockEntityElement createExact(CEChunk chunk, BlockPos pos, BlockDisplayBlockEntityElement previous) {
        if (!previous.config.isSamePosition(this)) {
            return null;
        }
        return new BlockDisplayBlockEntityElement(this, pos, previous.entityId, false);
    }

    @Override
    public Class<BlockDisplayBlockEntityElement> elementClass() {
        return BlockDisplayBlockEntityElement.class;
    }

    public Vector3f scale() {
        return this.scale;
    }

    public Vector3f translation() {
        return this.translation;
    }

    public Vector3f position() {
        return this.position;
    }

    public float yRot() {
        return this.yRot;
    }

    public float xRot() {
        return this.xRot;
    }

    public Billboard billboard() {
        return this.billboard;
    }

    public Quaternionf rotation() {
        return this.rotation;
    }

    public LazyReference<BlockStateWrapper> blockState() {
        return this.blockState;
    }

    public List<Object> metadataValues(Player player, boolean force) {
        return this.lazyMetadataPacket.apply(player, null, force);
    }

    public boolean isSamePosition(BlockDisplayBlockEntityElementConfig that) {
        return this.offset.equals(that.offset) &&
                Float.compare(this.xRot, that.xRot) == 0 &&
                Float.compare(this.yRot, that.yRot) == 0 &&
                Objects.equal(this.position, that.position) &&
                Objects.equal(this.translation, that.translation) &&
                Objects.equal(this.rotation, that.rotation);
    }

    private static class Factory implements BlockEntityElementConfigFactory<BlockDisplayBlockEntityElement> {
        private static final String[] SHADOW_RADIUS = ConfigKeys.of("shadow_radius");
        private static final String[] SHADOW_STRENGTH = ConfigKeys.of("shadow_strength");
        private static final String[] GLOW_COLOR = ConfigKeys.of("glow_color");
        private static final String[] BLOCK_LIGHT = ConfigKeys.of("block_light");
        private static final String[] SKY_LIGHT = ConfigKeys.of("sky_light");
        private static final String[] VIEW_RANGE = ConfigKeys.of("view_range");

        @Override
        public BlockDisplayBlockEntityElementConfig create(ConfigSection section) {
            ConfigSection brightness = section.getSection("brightness");
            List<Condition<PlayerContext>> conditions = section.getSectionList(ConfigKeys.of("condition(s)"), CommonConditions::fromConfig);
            return new BlockDisplayBlockEntityElementConfig(
                    section.getNonNullValue("block", ConfigConstants.ARGUMENT_BLOCK_STATE, v -> {
                        String blockState = v.getAsString();
                        return LazyReference.untilNotNull(() -> CraftEngine.instance().blockManager().createBlockState(blockState));
                    }),
                    section.getVector3f("scale", ConfigConstants.NORMAL_SCALE),
                    section.getVector3f("position", ConfigConstants.ZERO_VECTOR3),
                    BlockEntityRenderOffset.fromConfig(section.getSection("offset")),
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
                    MiscUtils.allOf(conditions),
                    !conditions.isEmpty()
            );
        }
    }
}
