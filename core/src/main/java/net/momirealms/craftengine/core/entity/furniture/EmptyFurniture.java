package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.entity.furniture.behavior.EmptyFurnitureBehavior;
import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureBehavior;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.EventTrigger;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class EmptyFurniture implements CustomFurniture {
    public static final EmptyFurniture INSTANCE = new EmptyFurniture();
    public static final Key ID = Key.ce("empty");

    private EmptyFurniture() {}

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
    public LootTable<?> lootTable() {
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
    public @NotNull FurnitureBehavior behavior() {
        return EmptyFurnitureBehavior.INSTANCE;
    }
}
