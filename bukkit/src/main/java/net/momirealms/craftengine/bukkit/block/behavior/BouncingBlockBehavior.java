package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.FallOnBlockBehavior;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.proxy.minecraft.world.damagesource.DamageSourcesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import org.bukkit.entity.Entity;

import java.util.Map;
import java.util.concurrent.Callable;

public class BouncingBlockBehavior extends BukkitBlockBehavior implements FallOnBlockBehavior {
    public static final BlockBehaviorFactory<BouncingBlockBehavior> FACTORY = new Factory();
    private final double bounceHeight;
    private final boolean syncPlayerPosition;
    private final double fallDamageMultiplier;

    public BouncingBlockBehavior(CustomBlock customBlock, double bounceHeight, boolean syncPlayerPosition, double fallDamageMultiplier) {
        super(customBlock);
        this.bounceHeight = bounceHeight;
        this.syncPlayerPosition = syncPlayerPosition;
        this.fallDamageMultiplier = fallDamageMultiplier;
    }

    @Override
    public void fallOn(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        if (this.fallDamageMultiplier <= 0.0) return;
        Object entity = args[3];
        Number fallDistance = (Number) args[4];
        if (VersionHelper.isOrAbove1_21_5()) {
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
    public void updateEntityMovementAfterFallOn(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object entity = args[1];
        if (EntityProxy.INSTANCE.getSharedFlag(entity, 1)) {
            superMethod.call();
        } else {
            bounceUp(entity);
        }
    }

    private void bounceUp(Object entity) {
        Vec3d deltaMovement = LocationUtils.fromVec(EntityProxy.INSTANCE.getDeltaMovement(entity));
        if (deltaMovement.y < 0.0) {
            double d = CoreReflections.clazz$LivingEntity.isInstance(entity) ? 1.0 : 0.8;
            double y = -deltaMovement.y * this.bounceHeight * d;
            EntityProxy.INSTANCE.setDeltaMovement(entity, deltaMovement.x, y, deltaMovement.z);
            if (CoreReflections.clazz$Player.isInstance(entity) && this.syncPlayerPosition
                    && /* 防抖 -> */ y > 0.035 /* <- 防抖 */
            ) {
                // 这里一定要延迟 1t 不然就会出问题
                if (VersionHelper.isFolia()) {
                    Entity bukkitEntity = FastNMS.INSTANCE.method$Entity$getBukkitEntity(entity);
                    bukkitEntity.getScheduler().runDelayed(BukkitCraftEngine.instance().javaPlugin(),
                            r -> EntityProxy.INSTANCE.setHurtMarked(entity, true),
                            null, 1L
                    );
                } else {
                    CraftEngine.instance().scheduler().sync().runLater(
                            () -> EntityProxy.INSTANCE.setHurtMarked(entity, true),
                            1L
                    );
                }
            }
        }
    }

    private static class Factory implements BlockBehaviorFactory<BouncingBlockBehavior> {

        @Override
        public BouncingBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            double bounceHeight = ResourceConfigUtils.getAsDouble(arguments.getOrDefault("bounce-height", 0.66), "bounce-height");
            boolean syncPlayerPosition = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("sync-player-position", true), "sync-player-position");
            double fallDamageMultiplier = ResourceConfigUtils.getAsDouble(arguments.getOrDefault("fall-damage-multiplier", 0.5), "fall-damage-multiplier");
            return new BouncingBlockBehavior(block, bounceHeight, syncPlayerPosition, fallDamageMultiplier);
        }
    }
}
