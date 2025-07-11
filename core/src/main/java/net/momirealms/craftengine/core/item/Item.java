package net.momirealms.craftengine.core.item;

import com.google.gson.JsonElement;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.data.Enchantment;
import net.momirealms.craftengine.core.item.data.FireworkExplosion;
import net.momirealms.craftengine.core.item.data.JukeboxPlayable;
import net.momirealms.craftengine.core.item.data.Trim;
import net.momirealms.craftengine.core.item.modifier.ItemDataModifier;
import net.momirealms.craftengine.core.item.setting.EquipmentData;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.UniqueKey;
import net.momirealms.sparrow.nbt.Tag;

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

    boolean isEmpty();

    Optional<CustomItem<I>> getCustomItem();

    Optional<List<ItemBehavior>> getItemBehavior();

    boolean isCustomItem();

    boolean isBlockItem();

    Key id();

    Key vanillaId();

    UniqueKey recipeIngredientId();

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

    int maxDamage();

    Item<I> dyedColor(Integer data);

    Optional<Integer> dyedColor();

    Item<I> fireworkExplosion(FireworkExplosion explosion);

    Optional<FireworkExplosion> fireworkExplosion();

    Item<I> customNameJson(String displayName);

    Item<I> customNameComponent(Component displayName);

    Optional<String> customNameJson();

    Optional<Component> customNameComponent();

    default Optional<String> hoverNameJson() {
        return customNameJson().or(this::itemNameJson);
    }

    default Optional<Component> hoverNameComponent() {
        return customNameComponent().or(this::itemNameComponent);
    }

    Item<I> itemNameJson(String itemName);

    Item<I> itemNameComponent(Component itemName);

    Optional<String> itemNameJson();

    Optional<Component> itemNameComponent();

    Item<I> itemModel(String itemModel);

    Optional<String> itemModel();

    Item<I> tooltipStyle(String tooltipStyle);

    Optional<String> tooltipStyle();

    Item<I> loreJson(List<String> lore);

    Item<I> loreComponent(List<Component> lore);

    Optional<List<String>> loreJson();

    Optional<List<Component>> loreComponent();

    Optional<JukeboxPlayable> jukeboxSong();

    Item<I> jukeboxSong(JukeboxPlayable song);

    Optional<EquipmentData> equippable();

    Item<I> equippable(EquipmentData equipmentData);

    Item<I> unbreakable(boolean unbreakable);

    boolean unbreakable();

    Item<I> skull(String data);

    Optional<Enchantment> getEnchantment(Key enchantmentId);

    Item<I> setEnchantments(List<Enchantment> enchantments);

    Item<I> setStoredEnchantments(List<Enchantment> enchantments);

    Item<I> itemFlags(List<String> flags);

    Object getJavaTag(Object... path);

    Tag getNBTTag(Object... path);

    Item<I> setTag(Object value, Object... path);

    boolean hasTag(Object... path);

    boolean removeTag(Object... path);

    boolean hasComponent(Object type);

    void removeComponent(Object type);

    Object getExactComponent(Object type);

    Object getJavaComponent(Object type);

    JsonElement getJsonComponent(Object type);

    Tag getSparrowNBTComponent(Object type);

    Object getNBTComponent(Object type);

    void setComponent(Object type, Object value);

    void setJavaComponent(Object type, Object value);

    void setJsonComponent(Object type, JsonElement value);

    void setNBTComponent(Object type, Tag value);

    void resetComponent(Object type);

    I getItem();

    int maxStackSize();

    Item<I> maxStackSize(int amount);

    Item<I> copyWithCount(int count);

    boolean is(Key itemTag);

    Object getLiteralObject();

    Item<I> mergeCopy(Item<?> another);

    Item<I> transmuteCopy(Key another, int count);

    Item<I> unsafeTransmuteCopy(Object another, int count);

    void shrink(int amount);

    default Item<I> transmuteCopy(Key another) {
        return transmuteCopy(another, this.count());
    }

    void merge(Item<I> another);

    default Item<I> apply(ItemDataModifier<I> modifier, ItemBuildContext context) {
        return modifier.apply(this, context);
    }

    byte[] toByteArray();
}
