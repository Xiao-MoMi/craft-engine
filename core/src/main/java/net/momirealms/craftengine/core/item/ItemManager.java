package net.momirealms.craftengine.core.item;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.equipment.Equipment;
import net.momirealms.craftengine.core.item.modifier.ItemDataModifier;
import net.momirealms.craftengine.core.pack.model.LegacyOverridesModel;
import net.momirealms.craftengine.core.pack.model.ModernItemModel;
import net.momirealms.craftengine.core.pack.model.generation.ModelGenerator;
import net.momirealms.craftengine.core.plugin.Manageable;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.util.Key;
import org.incendo.cloud.suggestion.Suggestion;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

public interface ItemManager<T> extends Manageable, ModelGenerator {

    void registerDataType(Function<Object, ItemDataModifier<T>> factory, String... alias);

    Map<Key, Equipment> equipments();

    ConfigParser[] parsers();

    Map<Key, TreeSet<LegacyOverridesModel>> legacyItemOverrides();

    Map<Key, TreeMap<Integer, ModernItemModel>> modernItemOverrides();

    Map<Key, ModernItemModel> modernItemModels1_21_4();

    Map<Key, TreeSet<LegacyOverridesModel>> modernItemModels1_21_2();

    Collection<Key> vanillaItems();

    T buildCustomItemStack(Key id, @Nullable Player player);

    T buildItemStack(Key id, @Nullable Player player);

    Item<T> createCustomWrappedItem(Key id, @Nullable Player player);

    Item<T> createWrappedItem(Key id, @Nullable Player player);

    Item<T> wrap(T itemStack);

    Item<T> fromByteArray(byte[] bytes);

    Collection<Key> items();

    Key itemId(T itemStack);

    Key customItemId(T itemStack);

    ExternalItemProvider<T> getExternalItemProvider(String name);

    boolean registerExternalItemProvider(ExternalItemProvider<T> externalItemProvider);

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

    boolean addCustomItem(CustomItem<T> customItem);

    List<Holder<Key>> tagToItems(Key tag);

    List<Holder<Key>> tagToVanillaItems(Key tag);

    List<Holder<Key>> tagToCustomItems(Key tag);

    int fuelTime(T itemStack);

    int fuelTime(Key id);

    Collection<Suggestion> cachedSuggestions();

    Collection<Suggestion> cachedTotemSuggestions();

    boolean isVanillaItem(Key item);
}