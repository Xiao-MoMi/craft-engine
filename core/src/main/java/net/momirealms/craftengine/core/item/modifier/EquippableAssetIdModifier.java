package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.NetworkItemHandler;
import net.momirealms.craftengine.core.item.setting.EquipmentData;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.Optional;

public class EquippableAssetIdModifier<I> implements ItemDataModifier<I> {
    private final Key assetId;

    public EquippableAssetIdModifier(Key assetsId) {
        this.assetId = assetsId;
    }

    @Override
    public String name() {
        return "equippable-asset-id";
    }

    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        Optional<EquipmentData> optionalData = item.equippable();
        optionalData.ifPresent(data -> item.equippable(new EquipmentData(
                data.slot(),
                this.assetId,
                data.dispensable(),
                data.swappable(),
                data.damageOnHurt(),
                data.equipOnInteract(),
                data.cameraOverlay()
        )));
        return item;
    }

    @Override
    public Item<I> prepareNetworkItem(Item<I> item, ItemBuildContext context, CompoundTag networkData) {
        Tag previous = item.getSparrowNBTComponent(ComponentKeys.EQUIPPABLE);
        if (previous != null) {
            networkData.put(ComponentKeys.EQUIPPABLE.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
        } else {
            networkData.put(ComponentKeys.EQUIPPABLE.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
        }
        return item;
    }
}
