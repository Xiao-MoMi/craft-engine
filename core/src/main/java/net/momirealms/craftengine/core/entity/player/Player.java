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
