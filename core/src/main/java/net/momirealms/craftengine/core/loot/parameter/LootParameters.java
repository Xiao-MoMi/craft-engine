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

package net.momirealms.craftengine.core.loot.parameter;

import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.entity.Entity;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.context.ContextKey;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.World;

public class LootParameters {
    public static final ContextKey<Vec3d> LOCATION = new ContextKey<>(Key.of("craftengine:location"));
    public static final ContextKey<World> WORLD = new ContextKey<>(Key.of("craftengine:world"));
    public static final ContextKey<Entity> ENTITY = new ContextKey<>(Key.of("craftengine:entity"));
    public static final ContextKey<Boolean> FALLING_BLOCK = new ContextKey<>(Key.of("craftengine:falling_block"));
    public static final ContextKey<Float> EXPLOSION_RADIUS = new ContextKey<>(Key.of("craftengine:explosion_radius"));
    public static final ContextKey<Player> PLAYER = new ContextKey<>(Key.of("craftengine:player"));
    public static final ContextKey<Item<?>> TOOL = new ContextKey<>(Key.of("craftengine:tool"));
    public static final ContextKey<ImmutableBlockState> BLOCK_STATE = new ContextKey<>(Key.of("craftengine:block_state"));
}
