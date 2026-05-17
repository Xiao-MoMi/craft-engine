package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.entity.furniture.behavior.EmptyFurnitureBehaviorTemplate;
import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureBehaviorTemplate;
import net.momirealms.craftengine.core.entity.furniture.setting.FurnitureSettings;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.EventTrigger;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public final class EmptyFurnitureDefinition implements FurnitureDefinition {
    public static final EmptyFurnitureDefinition INSTANCE = new EmptyFurnitureDefinition();
    public static final Key ID = Key.ce("empty");

    private EmptyFurnitureDefinition() {}

    @Override
    public void execute(Context context, EventTrigger trigger) {
    }

    @Override
    public Key id() {
        return ID;
    }

    @Override
    public FurnitureSettings settings() {
        return FurnitureSettings.of();
    }

    @Nullable
    @Override
    public LootTable loot() {
        return null;
    }

    @Override
    public Map<String, FurnitureVariant> variants() {
        return Map.of();
    }

    @Override
    public @Nullable FurnitureVariant getVariant(String variantName) {
        return null;
    }

    @Override
    public @NotNull List<FurnitureBehaviorTemplate> behaviors() {
        return List.of(EmptyFurnitureBehaviorTemplate.INSTANCE);
    }
}
