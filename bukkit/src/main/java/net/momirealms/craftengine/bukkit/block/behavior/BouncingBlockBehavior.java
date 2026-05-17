package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.PrioritizedFallOnHandler;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.proxy.minecraft.world.damagesource.DamageSourcesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.LivingEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.player.PlayerProxy;

public final class BouncingBlockBehavior extends BukkitBlockBehavior implements PrioritizedFallOnHandler {
    public static final BlockBehaviorFactory<BouncingBlockBehavior> FACTORY = new Factory();
    public final double bounceHeight;
    public final boolean syncPlayerPosition;
    public final double fallDamageMultiplier;

    private BouncingBlockBehavior(BlockDefinition blockDefinition,
                                  double bounceHeight,
                                  boolean syncPlayerPosition,
                                  double fallDamageMultiplier) {
        super(blockDefinition);
        this.bounceHeight = bounceHeight;
        this.syncPlayerPosition = syncPlayerPosition;
        this.fallDamageMultiplier = fallDamageMultiplier;
    }

    @Override
    public void fallOn(Object thisBlock, Object[] args) {
        if (this.fallDamageMultiplier <= 0.0) return;
        Object entity = args[3];
        Number fallDistance = (Number) args[4];
        if (VersionHelper.isOrAbove1_21_5) {
            EntityProxy.INSTANCE.causeFallDamage(
                    entity, fallDistance.doubleValue() * this.fallDamageMultiplier, 1.0F,
                    DamageSourcesProxy.INSTANCE.fall(EntityProxy.INSTANCE.damageSources(entity))
            );
        } else {
            EntityProxy.INSTANCE.causeFallDamage(
                    entity, fallDistance.floatValue() * (float) this.fallDamageMultiplier, 1.0F,
                    DamageSourcesProxy.INSTANCE.fall(EntityProxy.INSTANCE.damageSources(entity))
            );
        }
    }

    @Override
    public void updateEntityMovementAfterFallOn(Object thisBlock, Object[] args) {
        Object entity = args[1];
        if (EntityProxy.INSTANCE.getSharedFlag(entity, 1)) {
            super.updateEntityMovementAfterFallOn(thisBlock, args);
        } else {
            bounceUp(entity);
        }
    }

    private void bounceUp(Object entity) {
        Vec3d deltaMovement = LocationUtils.fromVec(EntityProxy.INSTANCE.getDeltaMovement(entity));
        if (deltaMovement.y < 0.0) {
            double d = LivingEntityProxy.CLASS.isInstance(entity) ? 1.0 : 0.8;
            double y = -deltaMovement.y * this.bounceHeight * d;
            EntityProxy.INSTANCE.setDeltaMovement(entity, deltaMovement.x, y, deltaMovement.z);
            if (PlayerProxy.CLASS.isInstance(entity) && this.syncPlayerPosition
                    && /* 防抖 -> */ y > 0.035 /* <- 防抖 */
            ) {
                // 这里一定要延迟 1t 不然就会出问题
                BukkitCraftEngine.instance().scheduler().platform().runLater(() -> {
                    EntityProxy.INSTANCE.setHurtMarked(entity, true);
                }, null, 1, EntityProxy.INSTANCE.getBukkitEntity(entity));
            }
        }
    }

    private static class Factory implements BlockBehaviorFactory<BouncingBlockBehavior> {
        private static final String[] BOUNCE_HEIGHT = new String[] {"bounce_height", "bounce-height"};
        private static final String[] SYNC_PLAYER_POSITION = new String[] {"sync_player_position", "sync-player-position"};
        private static final String[] FALL_DAMAGE_MULTIPLIER = new String[] {"fall_damage_multiplier", "fall-damage-multiplier"};

        @Override
        public BouncingBlockBehavior create(BlockDefinition block, ConfigSection section) {
            return new BouncingBlockBehavior(
                    block,
                    section.getDouble(BOUNCE_HEIGHT, 0.66),
                    section.getBoolean(SYNC_PLAYER_POSITION, true),
                    section.getDouble(FALL_DAMAGE_MULTIPLIER, 0.5)
            );
        }
    }
}
