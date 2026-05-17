package net.momirealms.craftengine.bukkit.block.entity.renderer.constant;

import net.momirealms.craftengine.bukkit.entity.data.item.ItemEntityData;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElementConfig;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElementConfigFactory;
import net.momirealms.craftengine.core.block.entity.render.tint.BlockEntityTintSource;
import net.momirealms.craftengine.core.block.entity.render.tint.BlockEntityTintSourceConfig;
import net.momirealms.craftengine.core.block.entity.render.tint.BlockEntityTintSources;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.PlayerContext;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public final class ItemBlockEntityElementConfig implements BlockEntityElementConfig<ItemBlockEntityElement> {
    public static final BlockEntityElementConfigFactory<ItemBlockEntityElement> FACTORY = new Factory();
    public final BiFunction<Player, BlockEntityTintSource, List<Object>> lazyMetadataPacket;
    public final Key itemId;
    public final Vector3f position;
    public final BlockEntityTintSourceConfig<? extends BlockEntityTintSource> tintSource;
    public final Predicate<PlayerContext> predicate;
    public final boolean hasCondition;

    public ItemBlockEntityElementConfig(Key itemId,
                                        Vector3f position,
                                        BlockEntityTintSourceConfig<? extends BlockEntityTintSource> tintSource,
                                        Predicate<PlayerContext> predicate,
                                        boolean hasCondition) {
        this.itemId = itemId;
        this.position = position;
        this.tintSource = tintSource;
        this.hasCondition = hasCondition;
        this.predicate = predicate;
        this.lazyMetadataPacket = (player, ts) -> {
            List<Object> dataValues = new ArrayList<>();
            Item wrappedItem = BukkitItemManager.instance().createWrappedItem(itemId, player);
            if (wrappedItem == null) {
                wrappedItem = Objects.requireNonNull(BukkitItemManager.instance().createWrappedItem(ItemKeys.BARRIER, player));
            }
            if (ts != null) {
                ts.applyTint(wrappedItem);
            }
            ItemEntityData.Item.addEntityData(wrappedItem.minecraftItem(), dataValues);
            ItemEntityData.NoGravity.addEntityData(true, dataValues);
            return dataValues;
        };
    }

    @Override
    public ItemBlockEntityElement create(CEChunk chunk, BlockPos pos) {
        return new ItemBlockEntityElement(this, pos, createTintSource(chunk, pos));
    }

    @Override
    public ItemBlockEntityElement create(CEChunk chunk, BlockPos pos, ItemBlockEntityElement previous) {
        return new ItemBlockEntityElement(this, pos, createTintSource(chunk, pos), previous.entityId1, previous.entityId2, !previous.config.position.equals(this.position));
    }

    @Override
    public ItemBlockEntityElement createExact(CEChunk chunk, BlockPos pos, ItemBlockEntityElement previous) {
        if (!previous.config.isSamePosition(this)) {
            return null;
        }
        return new ItemBlockEntityElement(this, pos, createTintSource(chunk, pos), previous.entityId1, previous.entityId2, false);
    }

    @Override
    public Class<ItemBlockEntityElement> elementClass() {
        return ItemBlockEntityElement.class;
    }

    public Vector3f position() {
        return this.position;
    }

    public Key itemId() {
        return this.itemId;
    }

    public BlockEntityTintSource createTintSource(CEChunk chunk, BlockPos pos) {
        if (this.tintSource != null) {
            return this.tintSource.create(chunk, pos);
        }
        return null;
    }

    public List<Object> metadataValues(Player player, BlockEntityTintSource tintSource) {
        return this.lazyMetadataPacket.apply(player, tintSource);
    }

    public boolean isSamePosition(ItemBlockEntityElementConfig that) {
        return this.position.equals(that.position);
    }

    private static class Factory implements BlockEntityElementConfigFactory<ItemBlockEntityElement> {
        private static final String[] TINT_SOURCE = new String[] {"tint_source", "tint-source"};

        @Override
        public ItemBlockEntityElementConfig create(ConfigSection section) {
            List<Condition<PlayerContext>> conditions = section.getSectionList("conditions", CommonConditions::fromConfig);
            return new ItemBlockEntityElementConfig(
                    section.getNonNullIdentifier("item"),
                    section.getVector3f("position", ConfigConstants.CENTER_VECTOR3),
                    section.getValue(TINT_SOURCE, BlockEntityTintSources::fromConfig),
                    MiscUtils.allOf(conditions),
                    !conditions.isEmpty()
            );
        }
    }
}
