package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;

public class CustomFurniture {
    private final Key id;
    private final FurnitureSettings settings;
    private final EnumMap<AnchorType, Placement> placements;
    @Nullable
    private final LootTable<?> lootTable;

    public CustomFurniture(@NotNull Key id,
                           @NotNull FurnitureSettings settings,
                           @NotNull EnumMap<AnchorType, Placement> placements,
                           @Nullable LootTable<?> lootTable) {
        this.id = id;
        this.settings = settings;
        this.placements = placements;
        this.lootTable = lootTable;
    }

    public Key id() {
        return id;
    }

    public EnumMap<AnchorType, Placement> placements() {
        return placements;
    }

    public FurnitureSettings settings() {
        return settings;
    }

    @Nullable
    public LootTable<?> lootTable() {
        return lootTable;
    }

    public AnchorType getAnyPlacement() {
        return placements.keySet().stream().findFirst().orElse(null);
    }

    public boolean isAllowedPlacement(AnchorType anchorType) {
        return placements.containsKey(anchorType);
    }

    public Placement getPlacement(AnchorType anchorType) {
        return placements.get(anchorType);
    }

    public static class Placement {
        private final FurnitureElement[] elements;
        private final HitBox[] hitbox;
        private final RotationRule rotationRule;
        private final AlignmentRule alignmentRule;

        public Placement(FurnitureElement[] elements, HitBox[] hitbox, RotationRule rotationRule, AlignmentRule alignmentRule) {
            this.elements = elements;
            this.hitbox = hitbox;
            this.rotationRule = rotationRule;
            this.alignmentRule = alignmentRule;
        }

        public HitBox[] hitbox() {
            return hitbox;
        }

        public FurnitureElement[] elements() {
            return elements;
        }

        public RotationRule rotationRule() {
            return rotationRule;
        }

        public AlignmentRule alignmentRule() {
            return alignmentRule;
        }
    }
}
