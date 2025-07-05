package net.momirealms.craftengine.core.entity.furniture;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.event.EventTrigger;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.util.Key;

// TODO 家具的设计存在问题。家具也应该存在不同的状态，而不是根据放置规则直接决定状态类型
public interface CustomFurniture {

    void execute(PlayerOptionalContext context, EventTrigger trigger);

    Key id();

    Map<AnchorType, Placement> placements();

    FurnitureSettings settings();

    @Nullable
    LootTable<?> lootTable();

    AnchorType getAnyAnchorType();

    boolean isAllowedPlacement(AnchorType anchorType);

    Placement getPlacement(AnchorType anchorType);

    Placement getValidPlacement(AnchorType anchorType);

    interface Builder {

        Builder id(Key id);

        Builder placement(Map<AnchorType, Placement> placements);

        Builder settings(FurnitureSettings settings);

        Builder lootTable(LootTable<?> lootTable);

        Builder events(Map<EventTrigger, List<Function<PlayerOptionalContext>>> events);

        CustomFurniture build();
    }

    record Placement(AnchorType anchorType,
                     FurnitureElement[] elements,
                     HitBox[] hitBoxes,
                     FurnitureEmitter[] emitters,
                     RotationRule rotationRule,
                     AlignmentRule alignmentRule,
                     Optional<ExternalModel> externalModel,
                     Optional<Vector3f> dropOffset) {
    }
}
