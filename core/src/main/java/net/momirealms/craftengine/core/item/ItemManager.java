package net.momirealms.craftengine.core.item;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.equipment.Equipment;
import net.momirealms.craftengine.core.item.recipe.DatapackRecipeResult;
import net.momirealms.craftengine.core.item.recipe.UniqueIdItem;
import net.momirealms.craftengine.core.item.updater.ItemUpdateResult;
import net.momirealms.craftengine.core.pack.model.LegacyOverridesModel;
import net.momirealms.craftengine.core.pack.model.ModernItemModel;
import net.momirealms.craftengine.core.pack.model.generation.ModelGenerator;
import net.momirealms.craftengine.core.plugin.Manageable;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.UniqueKey;
import org.incendo.cloud.suggestion.Suggestion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public interface ItemManager<T> extends Manageable, ModelGenerator {

    Map<Key, Equipment> equipments();

    ConfigParser[] parsers();

    Map<Key, TreeSet<LegacyOverridesModel>> legacyItemOverrides();

    Map<Key, TreeMap<Integer, ModernItemModel>> modernItemOverrides();

    Map<Key, ModernItemModel> modernItemModels1_21_4();

    Map<Key, TreeSet<LegacyOverridesModel>> modernItemModels1_21_2();

    Collection<Key> vanillaItems();

    @Nullable
    T buildCustomItemStack(Key id, @Nullable Player player);

    @Nullable
    T buildItemStack(Key id, @Nullable Player player);

    @Nullable
    Item<T> createCustomWrappedItem(Key id, @Nullable Player player);

    @Nullable
    Item<T> createWrappedItem(Key id, @Nullable Player player);

    @NotNull
    Item<T> wrap(T itemStack);

    Item<T> fromByteArray(byte[] bytes);

    Collection<Key> items();

    ExternalItemSource<T> getExternalItemSource(String name);

    boolean registerExternalItemSource(ExternalItemSource<T> externalItemSource);

    Optional<Equipment> getEquipment(Key key);

    Optional<CustomItem<T>> getCustomItem(Key key);

    Optional<List<ItemBehavior>> getItemBehavior(Key key);

    Optional<? extends BuildableItem<T>> getVanillaItem(Key key);

    NetworkItemHandler<T> networkItemHandler();

    default Optional<? extends BuildableItem<T>> getBuildableItem(Key key) {
        Optional<CustomItem<T>> item = getCustomItem(key);
        if (item.isPresent()) {
            return item;
        }
        return getVanillaItem(key);
    }

    Optional<CustomItem<T>> getCustomItemByPathOnly(String path);

    boolean addCustomItem(CustomItem<T> customItem);

    default List<UniqueKey> itemIdsByTag(Key tag) {
        List<UniqueKey> items = new ArrayList<>();
        items.addAll(vanillaItemIdsByTag(tag));
        items.addAll(customItemIdsByTag(tag));
        return items;
    }

    List<UniqueKey> vanillaItemIdsByTag(Key tag);

    List<UniqueKey> customItemIdsByTag(Key tag);

    int fuelTime(T itemStack);

    int fuelTime(Key id);

    Collection<Suggestion> cachedSuggestions();

    Collection<Suggestion> cachedTotemSuggestions();

    boolean isVanillaItem(Key item);

    Item<T> decode(FriendlyByteBuf byteBuf);

    void encode(FriendlyByteBuf byteBuf, Item<T> item);

    Item<T> s2c(Item<T> item, Player player);

    Item<T> c2s(Item<T> item);

    UniqueIdItem<T> uniqueEmptyItem();

    Item<T> applyTrim(Item<T> base, Item<T> addition, Item<T> template, Key pattern);

    Item<T> build(DatapackRecipeResult result);

    ItemUpdateResult updateItem(Item<T> item, Supplier<ItemBuildContext> contextSupplier);
}