package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.network.NetworkItemBuildContext;
import net.momirealms.craftengine.core.item.network.NetworkItemData;
import net.momirealms.craftengine.core.item.setting.value.EquipmentData;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class OverwritableEquippableAssetIdProcessor implements NetworkItemProcessor {
    public static final ItemProcessorFactory<OverwritableEquippableAssetIdProcessor> FACTORY = new Factory();
    public static final Key DISABLE_ARMOR_RENDERING = Key.ce("disable_armor_rendering");
    private final Key assetId;

    public OverwritableEquippableAssetIdProcessor(Key assetsId) {
        this.assetId = assetsId;
    }

    public Key assetId() {
        return this.assetId;
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public void apply(ItemBuildContext context) {
        this.apply(context, null);
    }

    @Override
    public boolean applyNetworkItem(NetworkItemBuildContext context, @Nullable NetworkItemData networkData) {
        return this.apply(context, networkData);
    }

    private boolean apply(ItemBuildContext context, @Nullable NetworkItemData networkData) {
        Item item = context.item();
        Optional<EquipmentData> optionalData = item.equippable();
        if (optionalData.isPresent()) {
            EquipmentData data = optionalData.get();
            Key previousAssetId = data.assetId();
            boolean canSet = previousAssetId == null;
            boolean removeAssetId = false;
            if (previousAssetId != null) {
                Optional<Object> optional = item.type().getJavaComponent(DataComponentKeys.EQUIPPABLE);
                if (optional.isEmpty()) {
                    canSet = true;
                } else {
                    Map<String, Object> equippableData = MiscUtils.castToMap(optional.get());
                    Key defaultAssetId = equippableData.containsKey("asset_id") ? Key.of((String) equippableData.get("asset_id")) : null;
                    canSet = Objects.equals(defaultAssetId, previousAssetId);
                    if (previousAssetId.equals(DISABLE_ARMOR_RENDERING)) {
                        canSet = true;
                        removeAssetId = true;
                    }
                }
            }
            if (!canSet) {
                return false;
            }
            if (networkData != null) {
                networkData.saveComponent(DataComponentKeys.EQUIPPABLE);
            }
            item.equippable(new EquipmentData(
                    data.slot(),
                    removeAssetId ? null : this.assetId,
                    data.dispensable(),
                    data.swappable(),
                    data.damageOnHurt(),
                    data.equipOnInteract(),
                    data.canBeSheared(),
                    data.cameraOverlay(),
                    data.equipSound(),
                    data.shearingSound(),
                    data.allowedEntities()
            ));
            return true;
        }
        Optional<Object> optional = item.type().getJavaComponent(DataComponentKeys.EQUIPPABLE);
        if (optional.isEmpty()) {
            return false;
        }
        Map<String, Object> equippableData = MiscUtils.castToMap(optional.get());
        Key defaultAssetId = equippableData.containsKey("asset_id") ? Key.of((String) equippableData.get("asset_id")) : null;
        if (Objects.equals(defaultAssetId, this.assetId)) {
            return false;
        }
        if (networkData != null) {
            networkData.saveComponent(DataComponentKeys.EQUIPPABLE);
        }
        equippableData.put("asset_id", this.assetId.asString());
        item.setJavaComponent(DataComponentKeys.EQUIPPABLE, equippableData);
        return true;
    }

    @Override
    public Key componentType(Item item, ItemBuildContext context) {
        return DataComponentKeys.EQUIPPABLE;
    }

    private static class Factory implements ItemProcessorFactory<OverwritableEquippableAssetIdProcessor> {

        @Override
        public OverwritableEquippableAssetIdProcessor create(ConfigValue value) {
            return new OverwritableEquippableAssetIdProcessor(value.getAsIdentifier());
        }
    }
}
