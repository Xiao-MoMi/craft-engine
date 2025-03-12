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

package net.momirealms.craftengine.core.loot;

import net.momirealms.craftengine.core.util.context.ContextHolder;
import net.momirealms.craftengine.core.util.context.ContextKey;
import net.momirealms.craftengine.core.world.World;

import java.util.Optional;
import java.util.Random;

public class LootContext {
    private final World world;
    private final ContextHolder contexts;
    private final Random randomSource;
    private final float luck;

    public LootContext(World world, ContextHolder contexts, Random randomSource, float luck) {
        this.randomSource = randomSource;
        this.contexts = contexts;
        this.world = world;
        this.luck = luck;
    }

    public Random randomSource() {
        return randomSource;
    }

    public <T> Optional<T> getOptionalParameter(ContextKey<T> parameter) {
        return this.contexts.getOptional(parameter);
    }

    public boolean hasParameter(ContextKey<?> parameter) {
        return this.contexts.has(parameter);
    }

    public <T> T getParameterOrThrow(ContextKey<T> parameter) {
        return this.contexts.getOrThrow(parameter);
    }

    public float luck() {
        return luck;
    }

    public ContextHolder contexts() {
        return contexts;
    }

    public World world() {
        return world;
    }
}
