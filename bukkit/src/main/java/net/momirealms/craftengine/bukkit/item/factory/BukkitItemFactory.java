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

package net.momirealms.craftengine.bukkit.item.factory;

import com.saicone.rtag.RtagItem;
import net.momirealms.craftengine.bukkit.item.RTagItemWrapper;
import net.momirealms.craftengine.bukkit.util.ItemTags;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.core.item.ItemFactory;
import net.momirealms.craftengine.core.item.ItemWrapper;
import net.momirealms.craftengine.core.item.modifier.IdModifier;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.Optional;

public abstract class BukkitItemFactory extends ItemFactory<CraftEngine, RTagItemWrapper, ItemStack> {

    protected BukkitItemFactory(CraftEngine plugin) {
        super(plugin);
    }

    public static BukkitItemFactory create(CraftEngine plugin) {
        Objects.requireNonNull(plugin, "plugin");
        switch (plugin.serverVersion()) {
            case "1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4" -> {
                return new UniversalItemFactory(plugin);
            }
            case "1.20.5", "1.20.6",
                 "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5",
                 "1.22", "1.22.1" -> {
                return new ComponentItemFactory(plugin);
            }
            default -> throw new IllegalStateException("Unsupported server version: " + plugin.serverVersion());
        }
    }

    @Override
    protected Key id(ItemWrapper<ItemStack> item) {
        Object id = item.get(IdModifier.CRAFT_ENGINE_ID);
        if (id == null) return Key.of(item.getItem().getType().getKey().asString());
        return Key.of(id.toString());
    }

    @Override
    protected Optional<Key> customId(ItemWrapper<ItemStack> item) {
        Object id = item.get(IdModifier.CRAFT_ENGINE_ID);
        if (id == null) return Optional.empty();
        return Optional.of(Key.of(id.toString()));
    }

    @Override
    protected boolean isBlockItem(ItemWrapper<ItemStack> item) {
        return item.getItem().getType().isBlock();
    }

    @Override
    protected Key vanillaId(ItemWrapper<ItemStack> item) {
        return Key.of(item.getItem().getType().getKey().asString());
    }

    @Override
    protected ItemWrapper<ItemStack> wrapInternal(ItemStack item) {
        return new RTagItemWrapper(new RtagItem(item), item.getAmount());
    }

    @Override
    protected void setTag(ItemWrapper<ItemStack> item, Object value, Object... path) {
        item.set(value, path);
    }

    @Override
    protected Object getTag(ItemWrapper<ItemStack> item, Object... path) {
        return item.get(path);
    }

    @Override
    protected boolean hasTag(ItemWrapper<ItemStack> item, Object... path) {
        return item.hasTag(path);
    }

    @Override
    protected boolean removeTag(ItemWrapper<ItemStack> item, Object... path) {
        return item.remove(path);
    }

    @Override
    protected void setComponent(ItemWrapper<ItemStack> item, String type, Object value) {
        item.setComponent(type, value);
    }

    @Override
    protected Object getComponent(ItemWrapper<ItemStack> item, String type) {
        return item.getComponent(type);
    }

    @Override
    protected boolean hasComponent(ItemWrapper<ItemStack> item, String type) {
        return item.hasComponent(type);
    }

    @Override
    protected void removeComponent(ItemWrapper<ItemStack> item, String type) {
       item.removeComponent(type);
    }

    @Override
    protected void update(ItemWrapper<ItemStack> item) {
        item.update();
    }

    @Override
    protected ItemStack load(ItemWrapper<ItemStack> item) {
        return item.load();
    }

    @Override
    protected ItemStack getItem(ItemWrapper<ItemStack> item) {
        return item.getItem();
    }

    @Override
    protected ItemStack loadCopy(ItemWrapper<ItemStack> item) {
        return item.loadCopy();
    }

    @Override
    protected boolean is(ItemWrapper<ItemStack> item, Key itemTag) {
        Object literalObject = item.getLiteralObject();
        Object tag = ItemTags.getOrCreate(itemTag);
        try {
            return (boolean) Reflections.method$ItemStack$isTag.invoke(literalObject, tag);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }
}
