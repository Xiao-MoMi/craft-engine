package net.momirealms.craftengine.core.item.equipment;

import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.processor.HideTooltipProcessor;
import net.momirealms.craftengine.core.item.processor.ItemProcessor;
import net.momirealms.craftengine.core.item.processor.TrimProcessor;
import net.momirealms.craftengine.core.pack.AbstractPackManager;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class TrimBasedEquipment extends AbstractEquipment {
    public static final EquipmentFactory<TrimBasedEquipment> FACTORY = new Factory();
    private final Key humanoid;
    private final Key humanoidLeggings;

    public TrimBasedEquipment(Key assetId, @Nullable Key humanoid, @Nullable Key humanoidLeggings) {
        super(assetId);
        this.humanoid = humanoid;
        this.humanoidLeggings = humanoidLeggings;
    }

    @Nullable
    public Key humanoid() {
        return this.humanoid;
    }

    @Nullable
    public Key humanoidLeggings() {
        return this.humanoidLeggings;
    }

    @Override
    public List<ItemProcessor> modifiers() {
        return List.of(
                new TrimProcessor(Key.of(AbstractPackManager.NEW_TRIM_MATERIAL), this.assetId),
                new HideTooltipProcessor(List.of(DataComponentKeys.TRIM))
        );
    }

    private static class Factory implements EquipmentFactory<TrimBasedEquipment> {
        private static final String[] HUMANOID = new String[] {"humanoid", "layer0"};
        private static final String[] HUMANOID_LEGGINGS = new String[] {"humanoid_leggings", "humanoid-leggings", "layer1"};

        @Override
        public TrimBasedEquipment create(Key id, ConfigSection section) {
            Key humanoidId = section.getIdentifier(HUMANOID);
            Key humanoidLeggingsId = section.getIdentifier(HUMANOID_LEGGINGS);
            return new TrimBasedEquipment(id, humanoidId, humanoidLeggingsId);
        }
    }
}
