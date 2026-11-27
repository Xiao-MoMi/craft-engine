package net.momirealms.craftengine.bukkit.block.entity.renderer.constant;

import com.google.common.base.Objects;
import net.momirealms.craftengine.bukkit.entity.data.BaseEntityData;
import net.momirealms.craftengine.bukkit.entity.data.decoration.ArmorStandData;
import net.momirealms.craftengine.core.block.entity.render.BlockEntityRenderOffset;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElementConfig;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElementConfigFactory;
import net.momirealms.craftengine.core.block.entity.render.tint.BlockEntityTintSource;
import net.momirealms.craftengine.core.block.entity.render.tint.BlockEntityTintSourceConfig;
import net.momirealms.craftengine.core.block.entity.render.tint.BlockEntityTintSources;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.PlayerContext;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.LegacyChatFormatter;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class ArmorStandBlockEntityElementConfig implements BlockEntityElementConfig<ArmorStandBlockEntityElement> {
    public static final BlockEntityElementConfigFactory<ArmorStandBlockEntityElement> FACTORY = new Factory();
    public final BlockEntityMetadataProvider lazyMetadataPacket;
    public final Key itemId;
    public final float scale;
    public final Vector3f position;
    public final BlockEntityRenderOffset offset;
    public final float xRot;
    public final float yRot;
    public final boolean small;
    public final LegacyChatFormatter glowColor;
    public final BlockEntityTintSourceConfig<? extends BlockEntityTintSource> tintSource;
    public final Predicate<PlayerContext> predicate;
    public final boolean hasCondition;

    public ArmorStandBlockEntityElementConfig(Key itemId,
                                              float scale,
                                              Vector3f position,
                                              BlockEntityRenderOffset offset,
                                              float xRot,
                                              float yRot,
                                              boolean small,
                                              LegacyChatFormatter glowColor,
                                              BlockEntityTintSourceConfig<? extends BlockEntityTintSource> tintSource,
                                              Predicate<PlayerContext> predicate,
                                              boolean hasCondition) {
        this.itemId = itemId;
        this.glowColor = glowColor;
        this.scale = scale;
        this.position = position;
        this.offset = offset;
        this.xRot = xRot;
        this.yRot = yRot;
        this.small = small;
        this.tintSource = tintSource;
        this.predicate = predicate;
        this.hasCondition = hasCondition;
        this.lazyMetadataPacket = (player, ts, force) -> {
            List<Object> dataValues = new ArrayList<>(2);
            if (glowColor != null) {
                BaseEntityData.SharedFlags.addEntityData((byte) 0x60, dataValues);
            } else {
                BaseEntityData.SharedFlags.addEntityData((byte) 0x20, dataValues);
            }
            if (small) {
                ArmorStandData.ClientFlags.addEntityData((byte) 0x01, dataValues);
            } else {
                ArmorStandData.ClientFlags.addEntityData((byte) 0, dataValues, force);
            }
            return dataValues;
        };
    }

    public BlockEntityTintSource createTintSource(CEChunk chunk, BlockPos pos) {
        if (this.tintSource != null) {
            return this.tintSource.create(chunk, pos);
        }
        return null;
    }

    @Override
    public ArmorStandBlockEntityElement create(CEChunk chunk, BlockPos pos) {
        return new ArmorStandBlockEntityElement(this, pos, createTintSource(chunk, pos));
    }

    @Override
    public ArmorStandBlockEntityElement create(CEChunk chunk, BlockPos pos, ArmorStandBlockEntityElement previous) {
        if (previous.config.scale != scale || previous.config.glowColor != glowColor) {
            return null;
        }
        return new ArmorStandBlockEntityElement(this, pos, createTintSource(chunk, pos), previous.entityId,
                previous.config.yRot != this.yRot ||
                previous.config.xRot != this.xRot ||
                !previous.config.offset.equals(this.offset) ||
                !previous.config.position.equals(this.position)
        );
    }

    @Override
    public ArmorStandBlockEntityElement createExact(CEChunk chunk, BlockPos pos, ArmorStandBlockEntityElement previous) {
        if (!previous.config.isSamePosition(this)) {
            return null;
        }
        return new ArmorStandBlockEntityElement(this, pos, createTintSource(chunk, pos), previous.entityId, false);
    }

    @Override
    public Class<ArmorStandBlockEntityElement> elementClass() {
        return ArmorStandBlockEntityElement.class;
    }

    public Item item(Player player, BlockEntityTintSource ts) {
        Item wrappedItem = Item.byId(this.itemId, player);
        if (wrappedItem == null) {
            wrappedItem = Item.byId(ItemKeys.BARRIER, player);
        }
        if (ts != null) {
            ts.applyTint(wrappedItem);
        }
        return wrappedItem;
    }

    public Key itemId() {
        return this.itemId;
    }

    public float scale() {
        return this.scale;
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

    public boolean small() {
        return this.small;
    }

    public List<Object> metadataValues(Player player, boolean force) {
        return this.lazyMetadataPacket.apply(player, null, force);
    }

    public boolean isSamePosition(ArmorStandBlockEntityElementConfig that) {
        return this.offset.equals(that.offset) &&
                Float.compare(this.xRot, that.xRot) == 0 &&
                Float.compare(this.yRot, that.yRot) == 0 &&
                Objects.equal(this.position, that.position);
    }

    private static class Factory implements BlockEntityElementConfigFactory<ArmorStandBlockEntityElement> {
        private static final String[] GLOW_COLOR = ConfigKeys.of("glow_color");
        private static final String[] TINT_SOURCE = ConfigKeys.of("tint_source");

        @Override
        public ArmorStandBlockEntityElementConfig create(ConfigSection section) {
            List<Condition<PlayerContext>> conditions = section.getSectionList(ConfigKeys.of("condition(s)"), CommonConditions::fromConfig);
            return new ArmorStandBlockEntityElementConfig(
                    section.getNonNullIdentifier("item"),
                    section.getFloat("scale", 1f),
                    section.getVector3f("position", ConfigConstants.CENTER_VECTOR3),
                    BlockEntityRenderOffset.fromConfig(section.getSection("offset")),
                    section.getFloat("pitch", 0f),
                    section.getFloat("yaw", 0f),
                    section.getBoolean("small"),
                    section.getEnum(GLOW_COLOR, LegacyChatFormatter.class),
                    section.getValue(TINT_SOURCE, BlockEntityTintSources::fromConfig),
                    MiscUtils.allOf(conditions),
                    !conditions.isEmpty()
            );
        }
    }
}
