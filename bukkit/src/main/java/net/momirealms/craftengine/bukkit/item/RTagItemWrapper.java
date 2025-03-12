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

package net.momirealms.craftengine.bukkit.item;

import com.saicone.rtag.RtagItem;
import net.momirealms.craftengine.core.item.ItemWrapper;
import org.bukkit.inventory.ItemStack;

@SuppressWarnings("UnstableApiUsage")
public class RTagItemWrapper implements ItemWrapper<ItemStack> {
    private final RtagItem rtagItem;
    private int count;

    public RTagItemWrapper(RtagItem rtagItem, int count) {
        this.rtagItem = rtagItem;
        this.count = count;
    }

    @Override
    public ItemStack getItem() {
        ItemStack itemStack = this.rtagItem.getItem();
        itemStack.setAmount(this.count);
        return itemStack;
    }

    @Override
    public boolean set(Object value, Object... path) {
        return this.rtagItem.set(value, path);
    }

    @Override
    public boolean add(Object value, Object... path) {
        return this.rtagItem.add(value, path);
    }

    @Override
    public <V> V get(Object... path) {
        return this.rtagItem.get(path);
    }

    @Override
    public void setComponent(Object path, Object value) {
        this.rtagItem.setComponent(path, value);
    }

    @Override
    public Object getComponent(Object path) {
        return this.rtagItem.getComponent(path);
    }

    @Override
    public int count() {
        return this.count;
    }

    @Override
    public void count(int amount) {
        if (amount < 0) amount = 0;
        this.count = amount;
    }

    @Override
    public <V> V getExact(Object... path) {
        return this.rtagItem.get(path);
    }

    @Override
    public void removeComponent(Object path) {
        this.rtagItem.removeComponent(path);
    }

    @Override
    public boolean hasComponent(Object path) {
        return this.rtagItem.hasComponent(path);
    }

    @Override
    public void update() {
        this.rtagItem.update();
    }

    @Override
    public boolean remove(Object... path) {
        return this.rtagItem.remove(path);
    }

    @Override
    public boolean hasTag(Object... path) {
        return this.rtagItem.hasTag(path);
    }

    @Override
    public ItemStack load() {
        ItemStack itemStack = this.rtagItem.load();
        itemStack.setAmount(this.count);
        return itemStack;
    }

    @Override
    public ItemStack loadCopy() {
        ItemStack itemStack = this.rtagItem.loadCopy();
        itemStack.setAmount(this.count);
        return itemStack;
    }

    @Override
    public Object getLiteralObject() {
        return this.rtagItem.getLiteralObject();
    }

    @Override
    public ItemWrapper<ItemStack> copyWithCount(int count) {
        return new RTagItemWrapper(new RtagItem(this.rtagItem.loadCopy()), count);
    }
}