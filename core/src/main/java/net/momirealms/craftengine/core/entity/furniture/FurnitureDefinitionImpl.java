package net.momirealms.craftengine.core.entity.furniture;

import com.google.common.collect.ImmutableSortedMap;
import net.momirealms.craftengine.core.entity.furniture.behavior.EmptyFurnitureBehaviorTemplate;
import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureBehaviorTemplate;
import net.momirealms.craftengine.core.entity.furniture.setting.FurnitureSettings;
import net.momirealms.craftengine.core.loot.Loot;
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

class FurnitureDefinitionImpl implements FurnitureDefinition {
    private final Key id;
    private final FurnitureSettings settings;
    private final Map<String, FurnitureVariant> variants;
    private final Map<EventTrigger, List<Function<Context>>> events;
    @Nullable
    private final Loot loot;
    private List<FurnitureBehaviorTemplate> behaviors = List.of(EmptyFurnitureBehaviorTemplate.INSTANCE);

    private FurnitureDefinitionImpl(@NotNull Key id,
                                    @NotNull FurnitureSettings settings,
                                    @NotNull Map<String, FurnitureVariant> variants,
                                    @NotNull Map<EventTrigger, List<Function<Context>>> events,
                                    @Nullable Loot loot) {
        this.id = id;
        this.settings = settings;
        this.variants = ImmutableSortedMap.copyOf(variants);
        this.loot = loot;
        this.events = events;
    }

    public void setBehaviors(List<FurnitureBehaviorTemplate> behaviors) {
        this.behaviors = behaviors;
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
    public @Nullable Loot loot() {
        return this.loot;
    }

    @Override
    public Map<String, FurnitureVariant> variants() {
        return this.variants;
    }

    @Override
    public @NotNull List<FurnitureBehaviorTemplate> behaviors() {
        return this.behaviors;
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
        private Loot loot;

        @Override
        public FurnitureDefinition build() {
            return new FurnitureDefinitionImpl(this.id, this.settings, this.variants, this.events, this.loot);
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
        public Builder loot(Loot loot) {
            this.loot = loot;
            return this;
        }

        @Override
        public Builder events(Map<EventTrigger, List<Function<Context>>> events) {
            this.events = events;
            return this;
        }
    }
}
