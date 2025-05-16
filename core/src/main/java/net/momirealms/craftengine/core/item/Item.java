package net.momirealms.craftengine.core.item;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;
import java.util.Optional;

/**
 * Interface representing an item.
 * This interface provides methods for managing item properties such as custom model data,
 * damage, display name, lore, enchantments, and tags.
 *
 * @param <I> the type of the item implementation
 */
public interface Item<I> {

    Optional<CustomItem<I>> getCustomItem();

    Optional<List<ItemBehavior>> getItemBehavior();

    boolean isCustomItem();

    boolean isBlockItem();

    Key id();

    Key vanillaId();

    Optional<Key> customId();

    Item<I> customId(Key id);

    int count();

    Item<I> count(int amount);

    Item<I> trim(Trim trim);

    Optional<Trim> trim();

    Item<I> customModelData(Integer data);

    Optional<Integer> customModelData();

    Item<I> damage(Integer data);

    Optional<Integer> damage();

    Item<I> repairCost(Integer data);

    Optional<Integer> repairCost();

    Item<I> maxDamage(Integer data);

    Optional<Integer> maxDamage();

    Item<I> dyedColor(Integer data);

    Optional<Integer> dyedColor();

    Item<I> customName(String displayName);

    Optional<String> customName();

    default Optional<String> hoverName() {
        return customName().or(this::itemName);
    }

    Item<I> itemName(String itemName);

    Optional<String> itemName();

    Item<I> itemModel(String itemModel);

    Optional<String> itemModel();

    Item<I> tooltipStyle(String tooltipStyle);

    Optional<String> tooltipStyle();

    Item<I> lore(List<String> lore);

    Optional<JukeboxPlayable> jukeboxSong();

    Item<I> jukeboxSong(JukeboxPlayable song);

    Optional<EquipmentData> equippable();

    Item<I> equippable(EquipmentData equipmentData);

    Optional<List<String>> lore();

    Item<I> unbreakable(boolean unbreakable);

    boolean unbreakable();

    Item<I> skull(String data);

    Optional<Enchantment> getEnchantment(Key enchantmentId);

    Item<I> setEnchantments(List<Enchantment> enchantments);

    Item<I> addEnchantment(Enchantment enchantment);

    Item<I> setStoredEnchantments(List<Enchantment> enchantments);

    Item<I> addStoredEnchantment(Enchantment enchantment);

    Item<I> itemFlags(List<String> flags);

    Object getTag(Object... path);

    Item<I> setTag(Object value, Object... path);

    boolean hasTag(Object... path);

    boolean removeTag(Object... path);

    boolean hasComponent(Object type);

    void removeComponent(Object type);

    Object getComponent(Object type);

    Object getJavaTypeComponent(Object type);

    JsonElement getJsonTypeComponent(Object type);

    void setComponent(Object type, Object value);

    void resetComponent(Object type);

    I getItem();

    I load();

    int maxStackSize();

    Item<I> maxStackSize(int amount);

    Item<I> copyWithCount(int count);

    boolean is(Key itemTag);

    Object getLiteralObject();

    Item<I> mergeCopy(Item<?> another);

    void merge(Item<I> another);

    byte[] toByteArray();
}
