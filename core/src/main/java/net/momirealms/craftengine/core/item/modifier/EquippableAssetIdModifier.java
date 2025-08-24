package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.setting.EquipmentData;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class EquippableAssetIdModifier<I> implements SimpleNetworkItemDataModifier<I> {
    private final Key assetId;

    public EquippableAssetIdModifier(Key assetsId) {
        this.assetId = assetsId;
    }

    public Key assetId() {
        return assetId;
    }

    @Override
    public Key type() {
        return ItemDataModifiers.EQUIPPABLE_ASSET_ID;
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
    public @Nullable Key componentType(Item<I> item, ItemBuildContext context) {
        return ComponentKeys.EQUIPPABLE;
    }
}
