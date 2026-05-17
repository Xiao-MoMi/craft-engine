package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureBehaviorTemplate;
import net.momirealms.craftengine.core.entity.furniture.setting.FurnitureSettings;
import net.momirealms.craftengine.core.loot.Loot;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.EventTrigger;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public interface FurnitureDefinition {

    void execute(Context context, EventTrigger trigger);

    Key id();

    FurnitureSettings settings();

    default String translationKey() {
        Key id = this.id();
        return "furniture." + id.namespace() + "." + id.value();
    }

    @Nullable
    Loot loot();

    Map<String, FurnitureVariant> variants();

    default FurnitureVariant anyVariant() {
        return variants().values().iterator().next();
    }

    default String anyVariantName() {
        return variants().keySet().iterator().next();
    }

    @Nullable
    FurnitureVariant getVariant(String variantName);

    @NotNull
    List<FurnitureBehaviorTemplate> behaviors();

    @NotNull
    default FurnitureVariant getVariant(FurniturePersistentData accessor) {
        Optional<String> optionalVariant = accessor.variant();
        String variantName = null;
        if (optionalVariant.isPresent()) {
            variantName = optionalVariant.get();
        } else {
            Optional<AnchorType> optionalAnchorType = accessor.anchorType();
            if (optionalAnchorType.isPresent()) {
                variantName = optionalAnchorType.get().name().toLowerCase(Locale.ROOT);
                accessor.setVariant(variantName);
                accessor.removeTag(FurniturePersistentData.ANCHOR_TYPE);
            }
        }
        if (variantName == null) {
            return anyVariant();
        }
        FurnitureVariant variant = getVariant(variantName);
        if (variant == null) {
            return anyVariant();
        }
        return variant;

    }

    static Builder builder() {
        return new FurnitureDefinitionImpl.BuilderImpl();
    }

    interface Builder {

        Builder id(Key id);

        Builder variants(Map<String, FurnitureVariant> variants);

        Builder settings(FurnitureSettings settings);

        Builder loot(Loot loot);

        Builder events(Map<EventTrigger, List<Function<Context>>> events);

        FurnitureDefinition build();
    }
}
