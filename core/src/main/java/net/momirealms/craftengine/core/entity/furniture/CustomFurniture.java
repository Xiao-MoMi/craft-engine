package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.plugin.event.EventTrigger;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

public class CustomFurniture {
    private final Key id;
    private final FurnitureSettings settings;
    private final EnumMap<AnchorType, Placement> placements;
    private final AnchorType anyType;
    private final EnumMap<EventTrigger, List<Function<PlayerOptionalContext>>> events;
    @Nullable
    private final LootTable<?> lootTable;

    public CustomFurniture(@NotNull Key id,
                           @NotNull FurnitureSettings settings,
                           @NotNull EnumMap<AnchorType, Placement> placements,
                           @NotNull EnumMap<EventTrigger, List<Function<PlayerOptionalContext>>> events,
                           @Nullable LootTable<?> lootTable) {
        this.id = id;
        this.settings = settings;
        this.placements = placements;
        this.lootTable = lootTable;
        this.events = events;
        this.anyType = placements.keySet().stream().findFirst().orElse(null);
    }

    public void execute(PlayerOptionalContext context, EventTrigger trigger) {
        for (Function<PlayerOptionalContext> function : Optional.ofNullable(this.events.get(trigger)).orElse(Collections.emptyList())) {
            function.run(context);
        }
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
        return this.anyType;
    }

    public boolean isAllowedPlacement(AnchorType anchorType) {
        return placements.containsKey(anchorType);
    }

    public Placement getPlacement(AnchorType anchorType) {
        return placements.get(anchorType);
    }

    public record Placement(FurnitureElement[] elements,
                            HitBox[] hitBoxes,
                            RotationRule rotationRule,
                            AlignmentRule alignmentRule,
                            Optional<ExternalModel> externalModel,
                            Optional<Vector3f> dropOffset) {
    }
}
