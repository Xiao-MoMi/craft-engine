package net.momirealms.craftengine.bukkit.plugin.injector;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.momirealms.craftengine.bukkit.block.LiquidSolidification;
import net.momirealms.craftengine.bukkit.block.behavior.LiquidSolidifiableBlock;
import net.momirealms.craftengine.core.block.behavior.*;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.ObjectHolder;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;

import java.util.Optional;

// Inlined advice materializes @AllArguments only where the selected behavior consumes it.
final class BlockCapabilityAdvice {
    private BlockCapabilityAdvice() {
    }

    static final class IsValidBoneMealTarget {
        @Advice.OnMethodExit(backupArguments = false)
        static void apply(@Advice.This Object thisBlock,
                          @Advice.FieldValue("behaviorHolder") ObjectHolder<BlockBehavior> holder,
                          @Advice.AllArguments Object[] args,
                          @Advice.Return(readOnly = false) boolean result) {
            if (holder.value() instanceof BonemealableBlock bonemealableBlock) {
                try {
                    result = bonemealableBlock.isValidBonemealTarget(thisBlock, args);
                } catch (Throwable t) {
                    CraftEngine.instance().logger().error("Failed to run isValidBoneMealTarget", t);
                }
            }
        }
    }

    static final class GetContainer {
        @Advice.OnMethodExit(backupArguments = false)
        static void apply(@Advice.This Object thisBlock,
                          @Advice.FieldValue("behaviorHolder") ObjectHolder<BlockBehavior> holder,
                          @Advice.AllArguments Object[] args,
                          @Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object result) {
            Object container = null;
            if (holder.value() instanceof WorldlyContainerHolder containerHolder) {
                try {
                    container = containerHolder.getContainer(thisBlock, args);
                } catch (Throwable t) {
                    CraftEngine.instance().logger().error("Failed to run getContainer", t);
                }
            }
            result = container;
        }
    }

    static final class IsBoneMealSuccess {
        @Advice.OnMethodExit(backupArguments = false)
        static void apply(@Advice.This Object thisBlock,
                          @Advice.FieldValue("behaviorHolder") ObjectHolder<BlockBehavior> holder,
                          @Advice.AllArguments Object[] args,
                          @Advice.Return(readOnly = false) boolean result) {
            if (holder.value() instanceof BonemealableBlock bonemealableBlock) {
                try {
                    result = bonemealableBlock.isBonemealSuccess(thisBlock, args);
                } catch (Throwable t) {
                    CraftEngine.instance().logger().error("Failed to run isBoneMealSuccess", t);
                }
            }
        }
    }

    static final class PerformBoneMeal {
        @Advice.OnMethodExit(backupArguments = false)
        static void apply(@Advice.This Object thisBlock,
                          @Advice.FieldValue("behaviorHolder") ObjectHolder<BlockBehavior> holder,
                          @Advice.AllArguments Object[] args) {
            if (holder.value() instanceof BonemealableBlock bonemealableBlock) {
                try {
                    bonemealableBlock.performBonemeal(thisBlock, args);
                } catch (Throwable t) {
                    CraftEngine.instance().logger().error("Failed to run performBoneMeal", t);
                }
            }
        }
    }

    static final class OnBrokenAfterFall {
        @Advice.OnMethodExit(backupArguments = false)
        static void apply(@Advice.This Object thisBlock,
                          @Advice.FieldValue("behaviorHolder") ObjectHolder<BlockBehavior> holder,
                          @Advice.AllArguments Object[] args) {
            if (holder.value() instanceof FallableBlock fallable) {
                try {
                    fallable.onBrokenAfterFall(thisBlock, args);
                } catch (Throwable t) {
                    CraftEngine.instance().logger().error("Failed to run onBrokenAfterFall", t);
                }
            }
        }
    }

    static final class OnLand {
        @Advice.OnMethodExit(backupArguments = false)
        static void apply(@Advice.This Object thisBlock,
                          @Advice.FieldValue("behaviorHolder") ObjectHolder<BlockBehavior> holder,
                          @Advice.AllArguments Object[] args) {
            BlockBehavior behavior = holder.value();
            if (!(behavior instanceof LiquidSolidifiableBlock) && !(behavior instanceof FallableBlock)) {
                return;
            }
            Object[] arguments = args;
            try {
                if (behavior instanceof LiquidSolidifiableBlock solidifiable) {
                    LiquidSolidification.solidifyOnLand(solidifiable, arguments);
                }
                if (behavior instanceof FallableBlock fallable) {
                    fallable.onLand(thisBlock, arguments);
                }
            } catch (Throwable t) {
                CraftEngine.instance().logger().error("Failed to run onLand", t);
            }
        }
    }

    static final class PickUpBlock {
        @Advice.OnMethodExit(backupArguments = false)
        static void apply(@Advice.This Object thisBlock,
                          @Advice.FieldValue("behaviorHolder") ObjectHolder<BlockBehavior> holder,
                          @Advice.AllArguments Object[] args,
                          @Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object result) {
            Object item = null;
            try {
                if (holder.value() instanceof BucketPickup bucketPickup) {
                    item = bucketPickup.pickupBlock(thisBlock, args);
                }
            } catch (Throwable t) {
                CraftEngine.instance().logger().error("Failed to run pickupBlock", t);
            }
            result = item != null ? item : ItemStackProxy.EMPTY;
        }
    }

    static final class GetPickUpSound {
        @Advice.OnMethodExit(backupArguments = false)
        static void apply(@Advice.This Object thisBlock,
                          @Advice.FieldValue("behaviorHolder") ObjectHolder<BlockBehavior> holder,
                          @Advice.AllArguments Object[] args,
                          @Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object result) {
            Object sound = null;
            try {
                if (holder.value() instanceof BucketPickup bucketPickup) {
                    sound = bucketPickup.getPickupSound(thisBlock, args);
                }
            } catch (Throwable t) {
                CraftEngine.instance().logger().error("Failed to run getPickupSound", t);
            }
            result = sound != null ? sound : Optional.empty();
        }
    }

    static final class PlaceLiquid {
        @Advice.OnMethodExit(backupArguments = false)
        static void apply(@Advice.This Object thisBlock,
                          @Advice.FieldValue("behaviorHolder") ObjectHolder<BlockBehavior> holder,
                          @Advice.AllArguments Object[] args,
                          @Advice.Return(readOnly = false) boolean result) {
            if (holder.value() instanceof LiquidBlockContainer liquidBlockContainer) {
                try {
                    result = liquidBlockContainer.placeLiquid(thisBlock, args);
                } catch (Throwable t) {
                    CraftEngine.instance().logger().error("Failed to run placeLiquid", t);
                }
            }
        }
    }

    static final class CanPlaceLiquid {
        @Advice.OnMethodExit(backupArguments = false)
        static void apply(@Advice.This Object thisBlock, @Advice.FieldValue("behaviorHolder") ObjectHolder<BlockBehavior> holder,
                          @Advice.AllArguments Object[] args,
                          @Advice.Return(readOnly = false) boolean result) {
            if (holder.value() instanceof LiquidBlockContainer liquidBlockContainer) {
                try {
                    result = liquidBlockContainer.canPlaceLiquid(thisBlock, args);
                } catch (Throwable t) {
                    CraftEngine.instance().logger().error("Failed to run canPlaceLiquid", t);
                }
            }
        }
    }
}
