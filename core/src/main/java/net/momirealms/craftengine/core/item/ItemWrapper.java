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

public interface ItemWrapper<I> {

    I getItem();

    void update();

    I load();

    I loadCopy();

    Object getLiteralObject();

    boolean set(Object value, Object... path);

    boolean add(Object value, Object... path);

    <V> V get(Object... path);

    <V> V getExact(Object... path);

    boolean remove(Object... path);

    boolean hasTag(Object... path);

    void removeComponent(Object type);

    boolean hasComponent(Object type);

    void setComponent(Object type, Object value);

    Object getComponent(Object type);

    int count();

    void count(int amount);

    ItemWrapper<I> copyWithCount(int count);
}
