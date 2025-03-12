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

package net.momirealms.craftengine.core.entity.player;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.entity.Entity;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.BlockPos;
import org.jetbrains.annotations.Nullable;

public abstract class Player extends Entity implements NetWorkUser {

    public abstract boolean isSecondaryUseActive();

    @Nullable
    public abstract Item<?> getItemInHand(InteractionHand hand);

    @Override
    public abstract Object platformPlayer();

    @Override
    public abstract Object serverPlayer();

    public abstract float getDestroyProgress(Object blockState, BlockPos pos);

    public abstract void stopMiningBlock();

    public abstract void abortMiningBlock();

    public abstract double getInteractionRange();

    public abstract void abortDestroyProgress();

    public abstract void onSwingHand();

    public abstract boolean isMiningBlock();

    public abstract boolean shouldSyncAttribute();

    public abstract boolean isSneaking();

    public abstract boolean isCreativeMode();

    public abstract boolean isSpectatorMode();

    public abstract boolean isAdventureMode();

    public abstract void sendActionBar(Component text);

    public abstract boolean updateLastSuccessfulInteractionTick(int tick);

    public abstract int gameTicks();

    public abstract void swingHand(InteractionHand hand);

    public abstract boolean hasPermission(String permission);

    public abstract boolean canInstabuild();

    public abstract String name();

    public void playSound(Key sound) {
        playSound(sound, 1, 1);
    }

    public abstract void playSound(Key sound, float volume, float pitch);

    public abstract void giveItem(Item<?> item);
}
