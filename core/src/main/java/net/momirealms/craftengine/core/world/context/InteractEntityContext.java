package net.momirealms.craftengine.core.world.context;

import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.EntityHitResult;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.World;

public class InteractEntityContext {
    private final Player player;
    private final InteractionHand hand;
    private final EntityHitResult hitResult;
    private final World level;
    private final Item<?> itemStack;

    public InteractEntityContext(Player player, InteractionHand hand, EntityHitResult hitResult) {
        this.player = player;
        this.hand = hand;
        this.hitResult = hitResult;
        this.level = player.world();
        this.itemStack = player.getItemInHand(hand);
    }

    public Player getPlayer() {
        return this.player;
    }

    public InteractionHand getHand() {
        return this.hand;
    }

    public EntityHitResult getHitResult() {
        return this.hitResult;
    }

    public World getLevel() {
        return this.level;
    }

    public Item<?> getItem() {
        return this.itemStack;
    }

    public BlockPos getClickedPos() {
        return this.hitResult.blockPos();
    }

    public Direction getClickedFace() {
        return this.hitResult.direction();
    }

    public Vec3d getClickLocation() {
        return this.hitResult.hitLocation();
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
