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

package net.momirealms.craftengine.core.registry;

import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface Registry<T> extends Holder.Owner<T> {

    ResourceKey<? extends Registry<T>> key();

    @Nullable
    T getValue(@Nullable ResourceKey<T> key);

    @Nullable
    T getValue(@Nullable Key id);

    Set<Key> keySet();

    Set<Map.Entry<ResourceKey<T>, T>> entrySet();

    Set<ResourceKey<T>> registryKeySet();

    boolean containsKey(Key id);

    boolean containsKey(ResourceKey<T> key);

    Optional<Holder.Reference<T>> get(Key id);

    Optional<Holder.Reference<T>> get(ResourceKey<T> key);
}
