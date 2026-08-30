package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.setting.value.EquipmentData;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class OverwritableEquippableAssetIdProcessor implements SimpleNetworkItemProcessor {
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
    public void apply(ItemBuildContext context) {
        Item item = context.item();
        Optional<EquipmentData> optionalData = item.equippable();
        optionalData.ifPresentOrElse(data ->
                {
                    Key previousAssetId = data.assetId();
                    boolean canSet = false;
                    boolean removeAssetId = false;
                    if (previousAssetId == null) {
                        canSet = true;
                    } else {
                        Optional<Object> optional = item.type().getJavaComponent(DataComponentKeys.EQUIPPABLE);
                        if (optional.isEmpty()) {
                            canSet = true;
                        } else {
                            Map<String, Object> equippableData = MiscUtils.castToMap(optional.get());
                            Key defaultAssetId = equippableData.containsKey("asset_id") ? Key.of((String) equippableData.get("asset_id")) : null;
                            // 如果默认值和之前值相同，则可以覆写
                            if (Objects.equals(defaultAssetId, previousAssetId)) {
                                canSet = true;
                            }
                            if (previousAssetId.equals(DISABLE_ARMOR_RENDERING)) {
                                canSet = true;
                                removeAssetId = true;
                            }
                        }
                    }
                    if (canSet) {
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
                                data.shearingSound()
                        ));
                    }
                }, () -> {
                    Optional<Object> optional = item.type().getJavaComponent(DataComponentKeys.EQUIPPABLE);
                    if (optional.isEmpty()) {
                        return;
                    }
                    Map<String, Object> equippableData = MiscUtils.castToMap(optional.get());
                    Key defaultAssetId = equippableData.containsKey("asset_id") ? Key.of((String) equippableData.get("asset_id")) : null;
                    if (Objects.equals(defaultAssetId, this.assetId)) {
                        return;
                    }
                    equippableData.put("asset_id", this.assetId.asString());
                    item.setJavaComponent(DataComponentKeys.EQUIPPABLE, equippableData);
                }
        );
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
