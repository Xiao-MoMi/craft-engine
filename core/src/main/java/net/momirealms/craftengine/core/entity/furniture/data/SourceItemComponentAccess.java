package net.momirealms.craftengine.core.entity.furniture.data;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class SourceItemComponentAccess {
    private static final Map<Key, LegacyComponent> LEGACY_COMPONENTS = Map.ofEntries(
            legacy(DataComponentKeys.ATTRIBUTE_MODIFIERS, path("AttributeModifiers")),
            legacy(DataComponentKeys.BANNER_PATTERN, path("BlockEntityTag", "Patterns")),
            legacy(DataComponentKeys.BASE_COLOR, path("BlockEntityTag", "Base")),
            legacy(DataComponentKeys.BEES, path("BlockEntityTag", "Bees")),
            legacy(DataComponentKeys.BLOCK_ENTITY_DATA, path("BlockEntityTag")),
            legacy(DataComponentKeys.BLOCK_STATE, path("BlockStateTag")),
            legacy(DataComponentKeys.BUCKET_ENTITY_DATA,
                    path("BucketVariantTag"),
                    path("NoAI"),
                    path("Silent"),
                    path("NoGravity"),
                    path("Glowing"),
                    path("Invulnerable"),
                    path("Health"),
                    path("Age"),
                    path("Variant"),
                    path("HuntingCooldown")),
            legacy(DataComponentKeys.BUNDLE_CONTENTS, path("Items")),
            legacy(DataComponentKeys.CAN_BREAK, path("CanDestroy")),
            legacy(DataComponentKeys.CAN_PLACE_ON, path("CanPlaceOn")),
            legacy(DataComponentKeys.CHARGED_PROJECTILES,
                    path("ChargedProjectiles"),
                    path("Charged")),
            legacy(DataComponentKeys.CONTAINER,
                    path("BlockEntityTag", "Items"),
                    path("BlockEntityTag", "item")),
            legacy(DataComponentKeys.CONTAINER_LOOT,
                    path("BlockEntityTag", "LootTable"),
                    path("BlockEntityTag", "LootTableSeed")),
            legacy(DataComponentKeys.CUSTOM_MODEL_DATA, path("CustomModelData")),
            legacy(DataComponentKeys.CUSTOM_NAME, path("display", "Name")),
            legacy(DataComponentKeys.DAMAGE, path("Damage")),
            legacy(DataComponentKeys.DEBUG_STICK_STATE, path("DebugProperty")),
            legacy(DataComponentKeys.DYED_COLOR, path("display", "color")),
            legacy(DataComponentKeys.ENCHANTMENTS, path("Enchantments")),
            legacy(DataComponentKeys.ENTITY_DATA, path("EntityTag")),
            legacy(DataComponentKeys.FIREWORK_EXPLOSION, path("Explosion")),
            legacy(DataComponentKeys.FIREWORKS, path("Fireworks")),
            legacy(DataComponentKeys.INSTRUMENT, path("instrument")),
            legacy(DataComponentKeys.LOCK, path("BlockEntityTag", "Lock")),
            legacy(DataComponentKeys.LODESTONE_TRACKER,
                    path("LodestonePos"),
                    path("LodestoneDimension"),
                    path("LodestoneTracked")),
            legacy(DataComponentKeys.LORE, path("display", "Lore")),
            legacy(DataComponentKeys.MAP_COLOR, path("display", "MapColor")),
            legacy(DataComponentKeys.MAP_DECORATIONS, path("Decorations")),
            legacy(DataComponentKeys.MAP_ID, path("map")),
            legacy(DataComponentKeys.NOTE_BLOCK_SOUND, path("BlockEntityTag", "note_block_sound")),
            legacy(DataComponentKeys.POT_DECORATIONS, path("BlockEntityTag", "sherds")),
            legacy(DataComponentKeys.POTION_CONTENTS,
                    path("CustomPotionColor"),
                    path("Potion"),
                    path("custom_potion_effects")),
            legacy(DataComponentKeys.PROFILE, path("SkullOwner")),
            legacy(DataComponentKeys.RECIPES, path("Recipes")),
            legacy(DataComponentKeys.REPAIR_COST, path("RepairCost")),
            legacy(DataComponentKeys.STORED_ENCHANTMENTS, path("StoredEnchantments")),
            legacy(DataComponentKeys.SUSPICIOUS_STEW_EFFECTS, path("effects")),
            legacy(DataComponentKeys.TOOLTIP_DISPLAY, path("HideFlags")),
            legacy(DataComponentKeys.TRIM, path("Trim")),
            legacy(DataComponentKeys.UNBREAKABLE, path("Unbreakable")),
            legacy(DataComponentKeys.WRITABLE_BOOK_CONTENT,
                    path("pages"),
                    path("filtered_pages")),
            legacy(DataComponentKeys.WRITTEN_BOOK_CONTENT,
                    path("pages"),
                    path("filtered_pages"),
                    path("title"),
                    path("filtered_title"),
                    path("author"),
                    path("generation"),
                    path("resolved"))
    );

    private SourceItemComponentAccess() {
    }

    static @Nullable Tag read(@NotNull Item item, @NotNull Key component) {
        if (VersionHelper.COMPONENT_RELEASE) {
            return item.getComponentAsSparrowTag(component);
        }
        LegacyComponent legacy = LEGACY_COMPONENTS.get(component);
        return legacy == null ? null : item.getSparrowTag(legacy.primaryPath());
    }

    static void copy(@NotNull Item source, @NotNull Item target, @NotNull Key component) {
        if (VersionHelper.COMPONENT_RELEASE) {
            Tag value = source.getComponentAsSparrowTag(component);
            if (value != null) {
                target.setSparrowTagComponent(component, value);
            }
            return;
        }
        LegacyComponent legacy = LEGACY_COMPONENTS.get(component);
        if (legacy == null) {
            return;
        }
        for (Object[] path : legacy.paths()) {
            Tag value = source.getSparrowTag(path);
            if (value != null) {
                target.setTag(value, path);
            }
        }
    }

    private static Map.Entry<Key, LegacyComponent> legacy(Key component, Object[] primaryPath, Object[]... additionalPaths) {
        List<Object[]> paths = new ArrayList<>(additionalPaths.length + 1);
        paths.add(primaryPath);
        paths.addAll(List.of(additionalPaths));
        return Map.entry(component, new LegacyComponent(primaryPath, List.copyOf(paths)));
    }

    private static Object[] path(Object... path) {
        return path;
    }

    private record LegacyComponent(Object[] primaryPath, List<Object[]> paths) {
    }
}
