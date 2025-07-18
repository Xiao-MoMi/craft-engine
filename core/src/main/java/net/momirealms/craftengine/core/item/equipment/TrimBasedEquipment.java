package net.momirealms.craftengine.core.item.equipment;

import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.modifier.HideTooltipModifier;
import net.momirealms.craftengine.core.item.modifier.ItemDataModifier;
import net.momirealms.craftengine.core.item.modifier.TrimModifier;
import net.momirealms.craftengine.core.pack.AbstractPackManager;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TrimBasedEquipment extends AbstractEquipment {
    public static final Factory FACTORY = new Factory();
    private final Key humanoid;
    private final Key humanoidLeggings;

    public TrimBasedEquipment(Key assetId, @Nullable Key humanoid, @Nullable Key humanoidLeggings) {
        super(assetId);
        this.humanoid = humanoid;
        this.humanoidLeggings = humanoidLeggings;
    }

    @Override
    public Key type() {
        return Equipments.TRIM;
    }

    @Nullable
    public Key humanoid() {
        return humanoid;
    }

    @Nullable
    public Key humanoidLeggings() {
        return humanoidLeggings;
    }

    @Override
    public <I> List<ItemDataModifier<I>> modifiers() {
        return List.of(
                new TrimModifier<>(Key.of(AbstractPackManager.NEW_TRIM_MATERIAL), this.assetId),
                new HideTooltipModifier<>(List.of(ComponentKeys.TRIM))
        );
    }

    public static class Factory implements EquipmentFactory {

        @Override
        public Equipment create(Key id, Map<String, Object> args) {
            Key humanoidId = Optional.ofNullable((String) args.get("humanoid")).map(Key::of).orElse(null);
            Key humanoidLeggingsId = Optional.ofNullable((String) args.get("humanoid-leggings")).map(Key::of).orElse(null);
            return new TrimBasedEquipment(id, humanoidId, humanoidLeggingsId);
        }
    }
}
