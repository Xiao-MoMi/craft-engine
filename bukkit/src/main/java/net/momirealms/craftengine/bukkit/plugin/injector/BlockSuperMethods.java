package net.momirealms.craftengine.bukkit.plugin.injector;

import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import net.momirealms.craftengine.core.block.BlockShape;
import net.momirealms.craftengine.core.block.behavior.BlockBehavior;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.ObjectHolder;

public final class BlockSuperMethods {
    private BlockSuperMethods() {
    }

    @RuntimeType
    public static Object getShape(@This BlockSuperCalls block,
                                  @FieldValue("shapeHolder") ObjectHolder<BlockShape> holder,
                                  @Argument(0) Object state,
                                  @Argument(1) Object level,
                                  @Argument(2) Object pos,
                                  @Argument(3) Object context) {
        try {
            return holder.value().getShape(block, state, level, pos, context);
        } catch (Exception e) {
            CraftEngine.instance().logger().error("Failed to run getShape", e);
            return block.superGetShape(state, level, pos, context);
        }
    }

    @RuntimeType
    public static Object getCollisionShape(@This BlockSuperCalls block,
                                           @FieldValue("shapeHolder") ObjectHolder<BlockShape> holder,
                                           @Argument(0) Object state,
                                           @Argument(1) Object level,
                                           @Argument(2) Object pos,
                                           @Argument(3) Object context) {
        try {
            return holder.value().getCollisionShape(block, state, level, pos, context);
        } catch (Throwable e) {
            CraftEngine.instance().logger().error("Failed to run getCollisionShape", e);
            return block.superGetCollisionShape(state, level, pos, context);
        }
    }

    @RuntimeType
    public static Object getSupportShape(@This BlockSuperCalls block,
                                         @FieldValue("shapeHolder") ObjectHolder<BlockShape> holder,
                                         @Argument(0) Object state,
                                         @Argument(1) Object level,
                                         @Argument(2) Object pos) {
        try {
            return holder.value().getSupportShape(block, state, level, pos);
        } catch (Throwable e) {
            CraftEngine.instance().logger().error("Failed to run getSupportShape", e);
            return block.superGetSupportShape(state, level, pos);
        }
    }

    @RuntimeType
    public static Object playerWillDestroy(@This BlockSuperCalls block,
                                           @FieldValue("behaviorHolder") ObjectHolder<BlockBehavior> holder,
                                           @Argument(0) Object level,
                                           @Argument(1) Object pos,
                                           @Argument(2) Object state,
                                           @Argument(3) Object player) {
        try {
            Object result = holder.value().playerWillDestroy(block, new Object[]{level, pos, state, player});
            block.superPlayerWillDestroy(level, pos, state, player);
            return result;
        } catch (Throwable e) {
            CraftEngine.instance().logger().error("Failed to run playerWillDestroy", e);
        }
        return block.superPlayerWillDestroy(level, pos, state, player);
    }

    public static void onExplosionHit(@This BlockSuperCalls.Explosion block,
                                      @FieldValue("behaviorHolder") ObjectHolder<BlockBehavior> holder,
                                      @Argument(0) Object state,
                                      @Argument(1) Object level,
                                      @Argument(2) Object pos,
                                      @Argument(3) Object explosion,
                                      @Argument(4) Object drops) {
        Object[] args = {state, level, pos, explosion, drops};
        try {
            BlockBehavior behavior = holder.value();
            behavior.preExplosionHit(block, args);
            block.superOnExplosionHit(state, level, pos, explosion, drops);
            behavior.postExplosionHit(block, args);
        } catch (Throwable e) {
            CraftEngine.instance().logger().error("Failed to run onExplosionHit", e);
        }
    }
}
