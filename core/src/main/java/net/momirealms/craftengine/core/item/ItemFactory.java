package net.momirealms.craftengine.core.item;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class ItemFactory<W extends ItemWrapper<I>, I> {
    protected final CraftEngine plugin;

    protected ItemFactory(CraftEngine plugin) {
        this.plugin = plugin;
    }

    public Item<I> wrap(I item) {
        Objects.requireNonNull(item, "item");
        return new AbstractItem<>(this, wrapInternal(item));
    }

    protected abstract W mergeCopy(W item1, W item2);

    protected abstract void merge(W item1, W item2);

    protected abstract Object encodeJava(Object type, @Nullable Object component);

    protected abstract JsonElement encodeJson(Object type, Object component);

    protected abstract W wrapInternal(I item);

    protected abstract Object getTag(W item, Object... path);

    protected abstract void setTag(W item, Object value, Object... path);

    protected abstract boolean hasTag(W item, Object... path);

    protected abstract boolean removeTag(W item, Object... path);

    protected abstract void setComponent(W item, Object type, Object value);

    protected abstract Object getComponent(W item, Object type);

    protected abstract boolean hasComponent(W item, Object type);

    protected abstract void removeComponent(W item, Object type);

    protected abstract void resetComponent(W item, Object type);

    protected abstract I load(W item);

    protected abstract I getItem(W item);

    protected abstract void customModelData(W item, Integer data);

    protected abstract Optional<Integer> customModelData(W item);

    protected abstract void customName(W item, String json);

    protected abstract Optional<String> customName(W item);

    protected abstract void itemName(W item, String json);

    protected abstract Optional<String> itemName(W item);

    protected abstract void skull(W item, String skullData);

    protected abstract Optional<List<String>> lore(W item);

    protected abstract void lore(W item, List<String> lore);

    protected abstract boolean unbreakable(W item);

    protected abstract void unbreakable(W item, boolean unbreakable);

    protected abstract Optional<Boolean> glint(W item);

    protected abstract void glint(W item, Boolean glint);

    protected abstract Optional<Integer> damage(W item);

    protected abstract void damage(W item, Integer damage);

    protected abstract Optional<Integer> dyedColor(W item);

    protected abstract void dyedColor(W item, Integer color);

    protected abstract Optional<Integer> maxDamage(W item);

    protected abstract void maxDamage(W item, Integer damage);

    protected abstract void enchantments(W item, List<Enchantment> enchantments);

    protected abstract void storedEnchantments(W item, List<Enchantment> enchantments);

    protected abstract void addEnchantment(W item, Enchantment enchantment);

    protected abstract void addStoredEnchantment(W item, Enchantment enchantment);

    protected abstract Optional<Enchantment> getEnchantment(W item, Key key);

    protected abstract void itemFlags(W item, List<String> flags);

    protected abstract Key id(W item);

    protected abstract Optional<Key> customId(W item);

    protected abstract void customId(W item, Key id);

    protected abstract Key vanillaId(W item);

    protected abstract int maxStackSize(W item);

    protected abstract void maxStackSize(W item, Integer maxStackSize);

    protected abstract boolean is(W item, Key itemTag);

    protected abstract boolean isBlockItem(W item);

    protected abstract void repairCost(W item, Integer data);

    protected abstract Optional<Integer> repairCost(W item);

    protected abstract void trim(W item, Trim trim);

    protected abstract Optional<Trim> trim(W item);

    protected abstract void tooltipStyle(W item, String data);

    protected abstract Optional<String> tooltipStyle(W item);

    protected abstract void jukeboxSong(W item, JukeboxPlayable data);

    protected abstract Optional<JukeboxPlayable> jukeboxSong(W item);

    protected abstract void itemModel(W item, String data);

    protected abstract Optional<String> itemModel(W item);

    protected abstract void equippable(W item, EquipmentData data);

    protected abstract Optional<EquipmentData> equippable(W item);

    protected abstract byte[] toByteArray(W item);

}
