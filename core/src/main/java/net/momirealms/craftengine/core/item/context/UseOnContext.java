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

package net.momirealms.craftengine.core.item.context;

import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.world.BlockHitResult;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.World;

public class UseOnContext {
    private final Player player;
    private final InteractionHand hand;
    private final BlockHitResult hitResult;
    private final World level;
    private final Item<?> itemStack;

    public UseOnContext(Player player, InteractionHand hand, BlockHitResult hit) {
        this(player.level(), player, hand, player.getItemInHand(hand), hit);
    }

    public UseOnContext(World world, Player player, InteractionHand hand, Item<?> stack, BlockHitResult hit) {
        this.player = player;
        this.hand = hand;
        this.hitResult = hit;
        this.itemStack = stack;
        this.level = world;
    }

    public BlockHitResult getHitResult() {
        return this.hitResult;
    }

    public BlockPos getClickedPos() {
        return this.hitResult.getBlockPos();
    }

    public Direction getClickedFace() {
        return this.hitResult.getDirection();
    }

    public Vec3d getClickLocation() {
        return this.hitResult.getLocation();
    }

    public boolean isInside() {
        return this.hitResult.isInside();
    }

    public Item<?> getItem() {
        return this.itemStack;
    }

    public Player getPlayer() {
        return this.player;
    }

    public InteractionHand getHand() {
        return this.hand;
    }

    public World getLevel() {
        return this.level;
    }

    public Direction getHorizontalDirection() {
        return this.player.getDirection();
    }

    public boolean isSecondaryUseActive() {
        return this.player.isSecondaryUseActive();
    }

    public float getRotation() {
        return this.player == null ? 0.0F : this.player.getYRot();
    }
}
