/*
 * Copyright (C) <2025> <XiaoMoMi>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.momirealms.craftengine.core.item;

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

    int count();

    Item<I> count(int amount);

    Item<I> customModelData(Integer data);

    Optional<Integer> customModelData();

    Item<I> damage(Integer data);

    Optional<Integer> damage();

    Item<I> maxDamage(Integer data);

    Optional<Integer> maxDamage();

    Item<I> displayName(String displayName);

    Optional<String> displayName();

    Item<I> itemName(String itemName);

    Optional<String> itemName();

    Item<I> lore(List<String> lore);

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

    boolean hasComponent(String type);

    void removeComponent(String type);

    Object getComponent(String type);

    void setComponent(String type, Object value);

    I getItem();

    I load();

    I loadCopy();

    void update();

    int maxStackSize();

    Item<I> maxStackSize(int amount);

    Item<I> copyWithCount(int count);

    boolean is(Key itemTag);

    Object getLiteralObject();
}
