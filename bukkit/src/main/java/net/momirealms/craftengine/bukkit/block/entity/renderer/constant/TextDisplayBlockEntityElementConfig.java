package net.momirealms.craftengine.bukkit.block.entity.renderer.constant;

import com.google.common.base.Objects;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.entity.data.DisplayData;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElementConfig;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElementConfigFactory;
import net.momirealms.craftengine.core.entity.display.Billboard;
import net.momirealms.craftengine.core.entity.display.TextDisplayAlignment;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.NetworkTextReplaceContext;
import net.momirealms.craftengine.core.plugin.context.PlayerContext;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Color;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public final class TextDisplayBlockEntityElementConfig implements BlockEntityElementConfig<TextDisplayBlockEntityElement> {
    public static final BlockEntityElementConfigFactory<TextDisplayBlockEntityElement> FACTORY = new Factory();
    public final Function<Player, List<Object>> lazyMetadataPacket;
    public final String text;
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
    public final int lineWidth;
    public final int backgroundColor;
    public final byte opacity;
    public final boolean hasShadow;
    public final boolean isSeeThrough;
    public final boolean useDefaultBackgroundColor;
    public final TextDisplayAlignment alignment;
    public final Predicate<PlayerContext> predicate;
    public final boolean hasCondition;

    public TextDisplayBlockEntityElementConfig(String text,
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
                                               int lineWidth,
                                               int backgroundColor,
                                               byte opacity,
                                               boolean hasShadow,
                                               boolean isSeeThrough,
                                               boolean useDefaultBackgroundColor,
                                               TextDisplayAlignment alignment,
                                               Predicate<PlayerContext> predicate,
                                               boolean hasCondition) {
        this.text = text;
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
        this.lineWidth = lineWidth;
        this.backgroundColor = backgroundColor;
        this.opacity = opacity;
        this.hasShadow = hasShadow;
        this.useDefaultBackgroundColor = useDefaultBackgroundColor;
        this.alignment = alignment;
        this.isSeeThrough = isSeeThrough;
        this.hasCondition = hasCondition;
        this.predicate = predicate;
        this.lazyMetadataPacket = player -> {
            List<Object> dataValues = new ArrayList<>();
            if (glowColor != null) {
                DisplayData.TextDisplayData.SharedFlags.addEntityData((byte) 0x40, dataValues);
                DisplayData.TextDisplayData.GlowColorOverride.addEntityData(glowColor.color(), dataValues);
            } else {
                DisplayData.TextDisplayData.SharedFlags.addEntityData((byte) 0x0, dataValues);
                DisplayData.TextDisplayData.GlowColorOverride.addEntityData(-1, dataValues);
            }
            DisplayData.TextDisplayData.Text.addEntityData(ComponentUtils.adventureToMinecraft(text(player)), dataValues);
            DisplayData.TextDisplayData.Scale.addEntityData(this.scale, dataValues);
            DisplayData.TextDisplayData.LeftRotation.addEntityData(this.rotation, dataValues);
            DisplayData.TextDisplayData.BillboardConstraints.addEntityData(this.billboard.id(), dataValues);
            DisplayData.TextDisplayData.Translation.addEntityData(this.translation, dataValues);
            DisplayData.TextDisplayData.LineWidth.addEntityData(this.lineWidth, dataValues);
            DisplayData.TextDisplayData.BackgroundColor.addEntityData(this.backgroundColor, dataValues);
            DisplayData.TextDisplayData.TextOpacity.addEntityData(this.opacity, dataValues);
            DisplayData.TextDisplayData.ShadowRadius.addEntityDataIfNotDefaultValue(this.shadowRadius, dataValues);
            DisplayData.TextDisplayData.ShadowStrength.addEntityDataIfNotDefaultValue(this.shadowStrength, dataValues);
            DisplayData.TextDisplayData.Flags.addEntityData(DisplayData.TextDisplayData.encodeFlags(this.hasShadow, this.isSeeThrough, this.useDefaultBackgroundColor, this.alignment), dataValues);
            if (this.blockLight != -1 && this.skyLight != -1) {
                DisplayData.TextDisplayData.BrightnessOverride.addEntityData(this.blockLight << 4 | this.skyLight << 20, dataValues);
            } else {
                DisplayData.TextDisplayData.BrightnessOverride.addEntityData(-1, dataValues);
            }
            DisplayData.TextDisplayData.ViewRange.addEntityData((float) (this.viewRange * player.displayEntityViewDistance()), dataValues);
            return dataValues;
        };
    }

    @Override
    public TextDisplayBlockEntityElement create(CEChunk chunk, BlockPos pos) {
        return new TextDisplayBlockEntityElement(this, pos);
    }

    @Override
    public TextDisplayBlockEntityElement create(CEChunk chunk, BlockPos pos, TextDisplayBlockEntityElement previous) {
        return new TextDisplayBlockEntityElement(this, pos, previous.entityId,
                previous.config.yRot != this.yRot ||
                        previous.config.xRot != this.xRot ||
                        !previous.config.position.equals(this.position)
        );
    }

    @Override
    public TextDisplayBlockEntityElement createExact(CEChunk chunk, BlockPos pos, TextDisplayBlockEntityElement previous) {
        if (!previous.config.isSamePosition(this)) {
            return null;
        }
        return new TextDisplayBlockEntityElement(this, pos, previous.entityId, false);
    }

    @Override
    public Class<TextDisplayBlockEntityElement> elementClass() {
        return TextDisplayBlockEntityElement.class;
    }

    public String text() {
        return text;
    }

    public Component text(Player player) {
        return AdventureHelper.miniMessage().deserialize(this.text, NetworkTextReplaceContext.of(player).tagResolvers());
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

    public List<Object> metadataValues(Player player) {
        return this.lazyMetadataPacket.apply(player);
    }

    public boolean isSamePosition(TextDisplayBlockEntityElementConfig that) {
        return Float.compare(xRot, that.xRot) == 0 &&
                Float.compare(yRot, that.yRot) == 0 &&
                Objects.equal(position, that.position) &&
                Objects.equal(translation, that.translation) &&
                Objects.equal(rotation, that.rotation);
    }

    private static class Factory implements BlockEntityElementConfigFactory<TextDisplayBlockEntityElement> {
        private static final String[] SHADOW_RADIUS = new String[] {"shadow_radius", "shadow-radius"};
        private static final String[] SHADOW_STRENGTH = new String[] {"shadow_strength", "shadow-strength"};
        private static final String[] GLOW_COLOR = new String[] {"glow_color", "glow-color"};
        private static final String[] BLOCK_LIGHT = new String[] {"block_light", "block-light"};
        private static final String[] SKY_LIGHT = new String[] {"sky_light", "sky-light"};
        private static final String[] VIEW_RANGE = new String[] {"view_range", "view-range"};
        private static final String[] LINE_WIDTH = new String[] {"line_width", "line-width"};
        private static final String[] BACKGROUND_COLOR = new String[] {"background_color", "background-color"};
        private static final String[] TEXT_OPACITY = new String[] {"text_opacity", "text-opacity"};
        private static final String[] HAS_SHADOW = new String[] {"has_shadow", "has-shadow"};
        private static final String[] IS_SEE_THROUGH = new String[] {"is_see_through", "is-see-through"};
        private static final String[] USE_DEFAULT_BACKGROUND_COLOR = new String[] {"use_default_background_color", "use-default-background-color"};

        @Override
        public TextDisplayBlockEntityElementConfig create(ConfigSection section) {
            ConfigSection brightness = section.getSection("brightness");
            List<Condition<PlayerContext>> conditions = section.getSectionList("conditions", CommonConditions::fromConfig);
            return new TextDisplayBlockEntityElementConfig(
                    section.getNonNullString("text"),
                    section.getVector3f("scale", ConfigConstants.NORMAL_SCALE),
                    section.getVector3f("position", ConfigConstants.CENTER_VECTOR3),
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
                    section.getInt(LINE_WIDTH, 200),
                    section.getValue(BACKGROUND_COLOR, o -> o.getAsColor().color(), 0x40000000),
                    (byte) section.getInt(TEXT_OPACITY, -1),
                    section.getBoolean(HAS_SHADOW),
                    section.getBoolean(IS_SEE_THROUGH),
                    section.getBoolean(USE_DEFAULT_BACKGROUND_COLOR),
                    section.getEnum("alignment", TextDisplayAlignment.class, TextDisplayAlignment.CENTER),
                    MiscUtils.allOf(conditions),
                    !conditions.isEmpty()
            );
        }
    }
}
