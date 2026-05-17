package net.momirealms.craftengine.core.item;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.equipment.Equipment;
import net.momirealms.craftengine.core.item.recipe.DatapackRecipeResult;
import net.momirealms.craftengine.core.item.updater.ItemUpdateResult;
import net.momirealms.craftengine.core.pack.model.definition.ModernItemModel;
import net.momirealms.craftengine.core.pack.model.generation.ModelGenerator;
import net.momirealms.craftengine.core.pack.model.legacy.LegacyOverridesModel;
import net.momirealms.craftengine.core.plugin.Manageable;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.UniqueKey;
import net.momirealms.sparrow.nbt.CompoundTag;
import org.incendo.cloud.suggestion.Suggestion;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public interface ItemManager extends Manageable, ModelGenerator {

    ConfigParser[] parsers();

    Map<Key, Equipment> equipments();

    Collection<Key> vanillaItems();

    @Nullable
    Item createCustomWrappedItem(Key id, @Nullable Player player);

    @Nullable
    Item createWrappedItem(Key id, @Nullable Player player);

    @NotNull
    Item wrap(Object itemStack);

    Item fromBytes(byte[] bytes);

    Item fromNBT(CompoundTag tag);

    Map<Key, ItemDefinition> loadedItems();

    Optional<Equipment> getEquipment(Key key);

    Optional<ItemDefinition> getItemDefinition(Key key);

    Optional<ItemBehavior> getItemBehavior(Key key);

    Optional<? extends BuildableItem> getVanillaItem(Key key);

    UniqueKey getIngredientKey(Item item);

    default Optional<? extends BuildableItem> getBuildableItem(Key key) {
        Optional<ItemDefinition> item = getItemDefinition(key);
        if (item.isPresent()) {
            return item;
        }
        return getVanillaItem(key);
    }

    Optional<ItemDefinition> getItemDefinitionByPath(String path);

    default List<UniqueKey> itemIdsByTag(Key tag) {
        List<UniqueKey> items = new ArrayList<>();
        items.addAll(vanillaItemIdsByTag(tag));
        items.addAll(customItemIdsByTag(tag));
        return items;
    }

    List<UniqueKey> vanillaItemIdsByTag(Key tag);

    List<UniqueKey> customItemIdsByTag(Key tag);

    boolean isVanillaItem(Key item);

    Optional<Item> c2s(Item item);

    Optional<Item> s2c(Item item, @Nullable Player player);

    Item applyTrim(Item base, Item addition, Item template, Key pattern);

    Item build(DatapackRecipeResult result);

    List<UniqueKey> getIngredientSubstitutes(Key item);

    ItemUpdateResult updateItem(Item item, Supplier<ItemBuildContext> contextSupplier);

    Item emptyItem();

    @ApiStatus.Internal
    Collection<Suggestion> cachedCustomItemSuggestions();

    @ApiStatus.Internal
    Collection<Suggestion> cachedTotemSuggestions();

    @ApiStatus.Internal
    Map<Key, TreeSet<LegacyOverridesModel>> legacyItemOverrides();

    @ApiStatus.Internal
    Map<Key, TreeMap<Integer, ModernItemModel>> modernItemOverrides();

    @ApiStatus.Internal
    Map<Key, ModernItemModel> modernItemModels1_21_4();

    @ApiStatus.Internal
    Map<Key, TreeSet<LegacyOverridesModel>> modernItemModels1_21_2();
}