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

package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractRecipe<T> implements Recipe<T> {
    protected final String group;
    protected final Key id;
    protected final CustomRecipeResult<T> result;

    protected AbstractRecipe(Key id, String group, CustomRecipeResult<T> result) {
        this.group = group;
        this.id = id;
        this.result = result;
    }

    @Override
    @Nullable
    public String group() {
        return group;
    }

    @Override
    public Key id() {
        return id;
    }

    @Override
    public T result(ItemBuildContext context) {
        return result.buildItemStack(context);
    }

    @Override
    public CustomRecipeResult<T> result() {
        return this.result;
    }
}
