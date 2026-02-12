package net.momirealms.craftengine.core.world.context;

import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.world.BlockHitResult;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UseOnContext {
    @Nullable
    private final Player player;
    private final InteractionHand hand;
    private final BlockHitResult hitResult;
    private final World level;
    private final Item<?> itemStack;

    public UseOnContext(Player player, InteractionHand hand, BlockHitResult hit) {
        this(player.world(), player, hand, player.getItemInHand(hand), hit);
    }

    public UseOnContext(Player player, InteractionHand hand, Item<?> stack, BlockHitResult hit) {
        this(player.world(), player, hand, stack, hit);
    }

    public UseOnContext(@NotNull World world, @Nullable Player player, InteractionHand hand, @NotNull Item<?> stack, BlockHitResult hit) {
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
        return this.hitResult.blockPos();
    }

    public Direction getClickedFace() {
        return this.hitResult.direction();
    }

    @Deprecated
    public Vec3d getClickLocation() {
        return this.hitResult.location();
    }

    public Vec3d getClickedLocation() {
        return this.hitResult.location();
    }

    public boolean isInside() {
        return this.hitResult.isInside();
    }

    @NotNull
    public Item<?> getItem() {
        return this.itemStack;
    }

    @Nullable
    public Player getPlayer() {
        return this.player;
    }

    public InteractionHand getHand() {
        return this.hand;
    }

    public World getLevel() {
        return this.level;
    }

    public World getWorld() {
        return this.level;
    }

    public Direction getHorizontalDirection() {
        return this.player == null ? Direction.NORTH : this.player.getDirection();
    }

    public boolean isSecondaryUseActive() {
        return this.player != null && this.player.isSecondaryUseActive();
    }

    public float getRotation() {
        return this.player == null ? 0.0F : this.player.yRot();
    }
}
