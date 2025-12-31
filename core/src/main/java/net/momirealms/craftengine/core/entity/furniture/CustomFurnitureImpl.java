package net.momirealms.craftengine.core.entity.furniture;

import com.google.common.collect.ImmutableSortedMap;
import net.momirealms.craftengine.core.entity.furniture.behavior.EmptyFurnitureBehavior;
import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureBehavior;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.EventTrigger;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

class CustomFurnitureImpl implements CustomFurniture {
    private final Key id;
    private final FurnitureSettings settings;
    private final Map<String, FurnitureVariant> variants;
    private final Map<EventTrigger, List<Function<Context>>> events;
    @Nullable
    private final LootTable<?> lootTable;
    private FurnitureBehavior behavior;

    private CustomFurnitureImpl(@NotNull Key id,
                                @NotNull FurnitureSettings settings,
                                @NotNull Map<String, FurnitureVariant> variants,
                                @NotNull Map<EventTrigger, List<Function<Context>>> events,
                                @NotNull FurnitureBehavior behavior,
                                @Nullable LootTable<?> lootTable) {
        this.id = id;
        this.settings = settings;
        this.variants = ImmutableSortedMap.copyOf(variants);
        this.lootTable = lootTable;
        this.events = events;
        this.behavior = behavior;
    }

    @Override
    public void execute(Context context, EventTrigger trigger) {
        for (Function<Context> function : Optional.ofNullable(this.events.get(trigger)).orElse(Collections.emptyList())) {
            function.run(context);
        }
    }

    @Override
    public Key id() {
        return this.id;
    }

    @Override
    public FurnitureSettings settings() {
        return this.settings;
    }

    @Override
    public @Nullable LootTable<?> lootTable(@Nullable Furniture furniture) {
        if (furniture == null) return this.lootTable;
        return behavior.getLootTable(furniture, this.lootTable);
    }

    @Override
    public Map<String, FurnitureVariant> variants() {
        return this.variants;
    }

    @Override
    public @NotNull FurnitureBehavior behavior() {
        return this.behavior;
    }

    @Override
    public void setBehavior(@NotNull FurnitureBehavior behavior) {
        this.behavior = Objects.requireNonNull(behavior, "behavior cannot be null");
    }

    @Nullable
    @Override
    public FurnitureVariant getVariant(String variantName) {
        return this.variants.get(variantName);
    }

    public static class BuilderImpl implements Builder {
        private Key id;
        private Map<String, FurnitureVariant> variants;
        private FurnitureSettings settings;
        private Map<EventTrigger, List<Function<Context>>> events;
        private LootTable<?> lootTable;
        private FurnitureBehavior behavior = EmptyFurnitureBehavior.INSTANCE;

        @Override
        public CustomFurniture build() {
            return new CustomFurnitureImpl(this.id, this.settings, this.variants, this.events, this.behavior, this.lootTable);
        }

        @Override
        public Builder id(Key id) {
            this.id = id;
            return this;
        }

        @Override
        public Builder variants(Map<String, FurnitureVariant> variants) {
            this.variants = variants;
            return this;
        }

        @Override
        public Builder settings(FurnitureSettings settings) {
            this.settings = settings;
            return this;
        }

        @Override
        public Builder lootTable(LootTable<?> lootTable) {
            this.lootTable = lootTable;
            return this;
        }

        @Override
        public Builder events(Map<EventTrigger, List<Function<Context>>> events) {
            this.events = events;
            return this;
        }

        @Override
        public Builder behavior(FurnitureBehavior behavior) {
            this.behavior = Objects.requireNonNull(behavior, "behavior cannot be null");
            return this;
        }
    }
}
