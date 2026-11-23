package net.momirealms.craftengine.bukkit.plugin.injector;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.momirealms.craftengine.bukkit.util.NoteBlockChainUpdateUtils;
import net.momirealms.craftengine.core.block.DelegatingBlock;
import net.momirealms.craftengine.core.block.behavior.BlockBehavior;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.ObjectHolder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import static net.momirealms.craftengine.bukkit.block.behavior.BukkitBlockBehavior.updateShape$blockPos;
import static net.momirealms.craftengine.bukkit.block.behavior.BukkitBlockBehavior.updateShape$direction;
import static net.momirealms.craftengine.bukkit.block.behavior.BukkitBlockBehavior.updateShape$level;
import static net.momirealms.craftengine.bukkit.block.behavior.BukkitBlockBehavior.updateShape$neighborState;

public final class BlockBehaviorMethods {
    private BlockBehaviorMethods() {
    }

    static Implementation forward(String methodName) {
        try {
            Method callback = BlockBehavior.class.getMethod(methodName, Object.class, Object[].class);
            MethodCall behavior = MethodCall.invoke(ObjectHolder.class.getMethod("value")).onField("behaviorHolder");
            Implementation forward = MethodCall.invoke(callback).onMethodCall(behavior).withThis().withArgumentArray()
                    .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC);
            return Advice.withCustomMapping().bind(FailureMessage.class, "Failed to run " + methodName)
                    .to(DefaultOnFailure.class).wrap(forward);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Cannot bind block behavior " + methodName, e);
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    private @interface FailureMessage {
    }

    private static final class DefaultOnFailure {
        @Advice.OnMethodExit(onThrowable = Throwable.class, backupArguments = false)
        static void finish(@Advice.Thrown(readOnly = false) Throwable failure, @FailureMessage String message) {
            if (failure != null) {
                CraftEngine.instance().logger().error(message, failure);
                // Clearing the exception returns the JVM default for this method's return type.
                failure = null;
            }
        }
    }

    @RuntimeType
    public static Object mirror(@This Object thisBlock,
                                @FieldValue("behaviorHolder") ObjectHolder<BlockBehavior> holder,
                                @AllArguments Object[] args) {
        try {
            return holder.value().mirror(thisBlock, args);
        } catch (Throwable t) {
            CraftEngine.instance().logger().error("Failed to run mirror", t);
            return args[0];
        }
    }

    @RuntimeType
    public static Object rotate(@This Object thisBlock,
                                @FieldValue("behaviorHolder") ObjectHolder<BlockBehavior> holder,
                                @AllArguments Object[] args) {
        try {
            return holder.value().rotate(thisBlock, args);
        } catch (Throwable t) {
            CraftEngine.instance().logger().error("Failed to run rotate", t);
            return args[0];
        }
    }

    public static boolean canSurvive(@This Object thisBlock,
                                     @FieldValue("behaviorHolder") ObjectHolder<BlockBehavior> holder,
                                     @AllArguments Object[] args) {
        try {
            return holder.value().canSurvive(thisBlock, args);
        } catch (Throwable t) {
            CraftEngine.instance().logger().error("Failed to run canSurvive", t);
        }
        return true;
    }

    static final class ShapeUpdate {
        @Advice.OnMethodExit(backupArguments = false)
        static void apply(@Advice.This DelegatingBlock thisBlock,
                          @Advice.FieldValue("behaviorHolder") ObjectHolder<BlockBehavior> holder,
                          @Advice.FieldValue("isClientSideNoteBlock") boolean note,
                          @Advice.FieldValue("isClientSideTripwire") boolean wire,
                          @Advice.AllArguments Object[] args,
                          @Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object result) {
            // Read @AllArguments once so the callback and its failure path see the same array.
            Object[] behaviorArgs = args;
            if (note || wire) {
                try {
                    NoteBlockChainUpdateUtils.onUpdateShape(behaviorArgs[0], behaviorArgs[updateShape$level], behaviorArgs[updateShape$blockPos], behaviorArgs[updateShape$direction], behaviorArgs[updateShape$neighborState]);
                } catch (Exception e) {
                    CraftEngine.instance().logger().error("Failed to correct neighbor shapes during block animation", e);
                }
            }

            Object updated;
            try {
                updated = holder.value().updateShape(thisBlock, behaviorArgs);
            } catch (Exception e) {
                CraftEngine.instance().logger().error("Failed to run updateShape", e);
                updated = behaviorArgs[0];
            }
            result = updated;
        }
    }

    public static void setPlacedBy(@This Object thisBlock,
                                   @FieldValue("behaviorHolder") ObjectHolder<BlockBehavior> holder,
                                   @AllArguments Object[] args) {
        try {
            holder.value().placeMultiState(thisBlock, args);
        } catch (Throwable t) {
            CraftEngine.instance().logger().error("Failed to run setPlaceBy", t);
        }
    }
}
