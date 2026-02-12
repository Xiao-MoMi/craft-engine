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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class CustomFurnitureImpl implements CustomFurniture {
    private final Key id;
    private final FurnitureSettings settings;
    private final Map<String, FurnitureVariant> variants;
    private final Map<EventTrigger, List<Function<Context>>> events;
    @Nullable
    private final LootTable<?> lootTable;
    private FurnitureBehavior behavior = EmptyFurnitureBehavior.INSTANCE;

    private CustomFurnitureImpl(@NotNull Key id,
                                @NotNull FurnitureSettings settings,
                                @NotNull Map<String, FurnitureVariant> variants,
                                @NotNull Map<EventTrigger, List<Function<Context>>> events,
                                @Nullable LootTable<?> lootTable) {
        this.id = id;
        this.settings = settings;
        this.variants = ImmutableSortedMap.copyOf(variants);
        this.lootTable = lootTable;
        this.events = events;
    }

    public void setBehavior(FurnitureBehavior behavior) {
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
    public @Nullable LootTable<?> lootTable() {
        return this.lootTable;
    }

    @Override
    public Map<String, FurnitureVariant> variants() {
        return this.variants;
    }

    @Override
    public @NotNull FurnitureBehavior behavior() {
        return this.behavior;
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

        @Override
        public CustomFurniture build() {
            return new CustomFurnitureImpl(this.id, this.settings, this.variants, this.events, this.lootTable);
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
    }
}
