package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.DataComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemProcessorFactory;
import net.momirealms.craftengine.core.item.setting.EquipmentData;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class OverwritableEquippableAssetIdProcessor implements SimpleNetworkItemProcessor {
    public static final ItemProcessorFactory<OverwritableEquippableAssetIdProcessor> FACTORY = new Factory();
    private final Key assetId;

    public OverwritableEquippableAssetIdProcessor(Key assetsId) {
        this.assetId = assetsId;
    }

    public Key assetId() {
        return this.assetId;
    }

    @Override
    public <I> Item<I> apply(Item<I> item, ItemBuildContext context) {
        Optional<EquipmentData> optionalData = item.equippable();
        optionalData.ifPresent(data ->
                {
                    Key previousAssetId = data.assetId();
                    boolean canSet = false;
                    if (previousAssetId == null) {
                        canSet = true;
                    } else {
                        Optional<Object> optional = item.type().getJavaComponent(DataComponentKeys.EQUIPPABLE);
                        if (optional.isEmpty()) {
                            canSet = true;
                        } else {
                            Map<String, Object> equippableData = MiscUtils.castToMap(optional.get(), false);
                            Key defaultAssetId = equippableData.containsKey("asset_id") ? Key.of((String) equippableData.get("asset_id")) : null;
                            // 如果默认值和之前值相同，则可以覆写
                            if (Objects.equals(defaultAssetId, previousAssetId)) {
                                canSet = true;
                            }
                        }
                    }
                    if (canSet) {
                        item.equippable(new EquipmentData(
                                data.slot(),
                                this.assetId,
                                data.dispensable(),
                                data.swappable(),
                                data.damageOnHurt(),
                                data.equipOnInteract(),
                                data.cameraOverlay()
                        ));
                    }
                }
        );
        return item;
    }

    @Override
    public <I> Key componentType(Item<I> item, ItemBuildContext context) {
        return DataComponentKeys.EQUIPPABLE;
    }

    private static class Factory implements ItemProcessorFactory<OverwritableEquippableAssetIdProcessor> {

        @Override
        public OverwritableEquippableAssetIdProcessor create(Object arg) {
            String id = arg.toString();
            return new OverwritableEquippableAssetIdProcessor(Key.of(id));
        }
    }
}
