package net.momirealms.craftengine.core.item;

import com.google.gson.JsonElement;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.attribute.AttributeModifier;
import net.momirealms.craftengine.core.item.data.Enchantment;
import net.momirealms.craftengine.core.item.data.FireworkExplosion;
import net.momirealms.craftengine.core.item.data.JukeboxPlayable;
import net.momirealms.craftengine.core.item.data.Trim;
import net.momirealms.craftengine.core.item.setting.EquipmentData;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Color;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.UniqueKey;
import net.momirealms.sparrow.nbt.Tag;

import java.util.List;
import java.util.Map;
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

    protected abstract ItemType type(W item);

    protected abstract W mergeCopy(W item1, W item2);

    protected abstract void merge(W item1, W item2);

    protected abstract W wrapInternal(I item);

    protected abstract Object getJavaTag(W item, Object... path);

    protected abstract Tag getTag(W item, Object... path);

    protected abstract void setTag(W item, Object value, Object... path);

    protected abstract boolean hasTag(W item, Object... path);

    protected abstract boolean removeTag(W item, Object... path);

    protected abstract void setComponent(W item, Object type, Object value);

    protected abstract Object getExactComponent(W item, Object type);

    protected abstract Object getExactTag(W item, Object... path);

    protected abstract void setExactComponent(W item, Object type, Object value);

    protected abstract Object getJavaComponent(W item, Object type);

    protected abstract JsonElement getJsonComponent(W item, Object type);

    protected abstract Tag getSparrowNBTComponent(W item, Object type);

    protected abstract Object getNBTComponent(W item, Object type);

    protected abstract boolean hasComponent(W item, Object type);

    protected abstract void removeComponent(W item, Object type);

    protected abstract void resetComponent(W item, Object type);

    protected abstract boolean hasNonDefaultComponent(W item, Object type);

    protected abstract I getItem(W item);

    protected abstract void customModelData(W item, Integer data);

    protected abstract Optional<Integer> customModelData(W item);

    protected abstract void customNameJson(W item, String json);

    protected abstract Optional<String> customNameJson(W item);

    protected void customNameComponent(W item, Component component) {
        if (component != null) {
            customNameJson(item, AdventureHelper.componentToJson(component));
        } else {
            customNameJson(item, null);
        }
    }

    protected Optional<Component> customNameComponent(W item) {
        return customNameJson(item).map(AdventureHelper::jsonToComponent);
    }

    protected abstract void itemNameJson(W item, String json);

    protected abstract Optional<String> itemNameJson(W item);

    protected void itemNameComponent(W item, Component component) {
        if (component != null) {
            itemNameJson(item, AdventureHelper.componentToJson(component));
        } else {
            itemNameJson(item, null);
        }
    }

    protected Optional<Component> itemNameComponent(W item) {
        return itemNameJson(item).map(AdventureHelper::jsonToComponent);
    }

    protected abstract Optional<List<String>> loreJson(W item);

    protected abstract void loreJson(W item, List<String> lore);

    protected void loreComponent(W item, List<Component> component) {
        if (component != null && !component.isEmpty()) {
            loreJson(item, component.stream().map(AdventureHelper::componentToJson).toList());
        } else {
            loreJson(item, null);
        }
    }

    protected Optional<List<Component>> loreComponent(W item) {
        return loreJson(item).map(list -> list.stream().map(AdventureHelper::jsonToComponent).toList());
    }

    protected abstract void skull(W item, String skullData);

    protected abstract boolean unbreakable(W item);

    protected abstract void unbreakable(W item, boolean unbreakable);

    protected abstract Optional<Boolean> glint(W item);

    protected abstract void glint(W item, Boolean glint);

    protected abstract Optional<Integer> damage(W item);

    protected abstract void damage(W item, Integer damage);

    protected abstract Optional<Color> dyedColor(W item);

    protected abstract void dyedColor(W item, Color color);

    protected abstract int maxDamage(W item);

    protected abstract void maxDamage(W item, Integer damage);

    protected abstract Optional<List<Enchantment>> enchantments(W item);

    protected abstract Optional<List<Enchantment>> storedEnchantments(W item);

    protected abstract void enchantments(W item, List<Enchantment> enchantments);

    protected abstract void storedEnchantments(W item, List<Enchantment> enchantments);

    protected abstract Optional<Enchantment> getEnchantment(W item, Key key);

    protected abstract void itemFlags(W item, List<String> flags);

    protected abstract Key id(W item);

    protected abstract Optional<Key> customId(W item);

    protected abstract void customId(W item, Key id);

    protected abstract Key vanillaId(W item);

    protected abstract int maxStackSize(W item);

    protected abstract void maxStackSize(W item, Integer maxStackSize);

    protected abstract boolean hasItemTag(W item, Key itemTag);

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

    protected abstract void fireworkExplosion(W item, FireworkExplosion explosion);

    protected abstract Optional<FireworkExplosion> fireworkExplosion(W item);

    protected abstract byte[] toByteArray(W item);

    protected abstract void setJavaComponent(W item, Object type, Object value);

    protected abstract void setJsonComponent(W item, Object type, JsonElement value);

    protected abstract void setNBTComponent(W item, Object type, Tag value);

    protected abstract W transmuteCopy(W item, Key newItem, int amount);

    protected abstract W unsafeTransmuteCopy(W item, Object newItem, int count);

    protected abstract boolean isEmpty(W item);

    protected abstract UniqueKey recipeIngredientID(W item);

    protected abstract void attributeModifiers(W item, List<AttributeModifier> modifiers);

    protected abstract Optional<Map<String, String>> blockState(W item);

    protected abstract void blockState(W item, Map<String, String> state);
}
