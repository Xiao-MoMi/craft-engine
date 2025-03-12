package net.momirealms.craftengine.core.item;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.pack.LegacyOverridesModel;
import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.pack.model.ItemModel;
import net.momirealms.craftengine.core.pack.model.generator.ModelGenerator;
import net.momirealms.craftengine.core.plugin.Reloadable;
import net.momirealms.craftengine.core.plugin.config.ConfigSectionParser;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.util.Key;
import org.incendo.cloud.suggestion.Suggestion;

import javax.annotation.Nullable;
import java.util.*;

public interface ItemManager<T> extends Reloadable, ModelGenerator, ConfigSectionParser {
    String CONFIG_SECTION_NAME = "items";

    default String sectionId() {
        return CONFIG_SECTION_NAME;
    }

    Map<Key, TreeSet<LegacyOverridesModel>> legacyItemOverrides();

    Map<Key, TreeMap<Integer, ItemModel>> modernItemOverrides();

    Map<Key, ItemModel> modernItemModels1_21_4();

    Map<Key, List<LegacyOverridesModel>> modernItemModels1_21_2();

    Collection<Key> vanillaItems();

    T buildCustomItemStack(Key id, @Nullable Player player);

    T buildItemStack(Key id, @Nullable Player player);

    Item<T> createCustomWrappedItem(Key id, @Nullable Player player);

    Item<T> createWrappedItem(Key id, @Nullable Player player);

    Item<T> wrap(T itemStack);

    Collection<Key> items();

    Key itemId(T itemStack);

    Key customItemId(T itemStack);

    Optional<CustomItem<T>> getCustomItem(Key key);

    Optional<List<ItemBehavior>> getItemBehavior(Key key);

    Optional<? extends BuildableItem<T>> getVanillaItem(Key key);

    default Optional<? extends BuildableItem<T>> getBuildableItem(Key key) {
        Optional<CustomItem<T>> item = getCustomItem(key);
        if (item.isPresent()) {
            return item;
        }
        return getVanillaItem(key);
    }

    List<Holder<Key>> tagToItems(Key tag);

    List<Holder<Key>> tagToVanillaItems(Key tag);

    List<Holder<Key>> tagToCustomItems(Key tag);

    int fuelTime(T itemStack);

    int fuelTime(Key id);

    default int loadingSequence() {
        return LoadingSequence.ITEM;
    }

    Collection<Suggestion> cachedSuggestions();

    void delayedInit();
}