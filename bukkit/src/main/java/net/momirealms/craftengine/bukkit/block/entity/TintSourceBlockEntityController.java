package net.momirealms.craftengine.bukkit.block.entity;

import net.momirealms.craftengine.bukkit.block.behavior.TintSourceBlockBehavior;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.ItemUtils;
import net.momirealms.craftengine.core.world.TintSource;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.IntTag;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.NotNull;

public final class TintSourceBlockEntityController extends BlockEntityController implements TintSource {
    private final TintSourceBlockBehavior behavior;
    @NotNull
    private Item sourceItem;

    public TintSourceBlockEntityController(BlockEntity blockEntity, TintSourceBlockBehavior behavior) {
        super(blockEntity);
        this.sourceItem = Item.empty();
        this.behavior = behavior;
    }

    @Override
    public Item tintSource() {
        return this.sourceItem;
    }

    @Override
    public void saveCustomData(CompoundTag tag) {
        if (ItemUtils.isEmpty(sourceItem)) return;
        CompoundTag data = new CompoundTag();
        data.put("data_version", new IntTag(Config.itemDataFixerUpperFallbackVersion()));
        data.put("tint_source_item", ItemStackUtils.saveMinecraftItemStackAsTag(this.sourceItem.minecraftItem()));
        tag.put(behavior.customDataKey, data);
    }

    @Override
    public void loadCustomData(CompoundTag tag) {
        CompoundTag dataTag = tag.getCompound(behavior.customDataKey);
        if (dataTag == null) {
            this.sourceItem = Item.empty();
            return;
        }
        int dataVersion = dataTag.getInt("data_version", Config.itemDataFixerUpperFallbackVersion());
        Tag itemTag = dataTag.get("tint_source_item");
        if (itemTag == null) {
            this.sourceItem = Item.empty();
            return;
        }
        this.sourceItem = ItemStackUtils.wrap(ItemStackUtils.parseMinecraftItem(itemTag, dataVersion));
    }

    @Override
    public void loadCustomDataFromItem(Item item) {
        this.setSourceItem(item.copyWithCount(1));
    }

    public void setSourceItem(@NotNull Item sourceItem) {
        if (this.sourceItem.isSimilar(sourceItem)) {
            return;
        }
        this.sourceItem = sourceItem;
        super.blockEntity.updateConstantRenderers();
    }

    @Override
    public void onRemove() {
        if (this.behavior.dropItem && !this.sourceItem.isEmpty()) {
            this.blockEntity.world.world.dropItemNaturally(Vec3d.atCenterOf(this.blockEntity.pos), this.sourceItem.copy());
        }
    }
}
