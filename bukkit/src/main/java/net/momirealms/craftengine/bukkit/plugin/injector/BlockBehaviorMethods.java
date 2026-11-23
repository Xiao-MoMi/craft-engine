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
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.ObjectHolder;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerChunkCacheProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerLevelProxy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

public final class BlockBehaviorMethods {
    private static final int UPDATE_LEVEL = VersionHelper.isOrAbove1_21_2 ? 1 : 3;
    private static final int UPDATE_DIRECTION = VersionHelper.isOrAbove1_21_2 ? 4 : 1;
    private static final int UPDATE_POS = VersionHelper.isOrAbove1_21_2 ? 3 : 4;

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

    @RuntimeType
    public static Object updateShape(@This DelegatingBlock thisBlock,
                                     @FieldValue("behaviorHolder") ObjectHolder<BlockBehavior> holder,
                                     @AllArguments Object[] args) {
        if (thisBlock.isNoteBlock() && ServerLevelProxy.CLASS.isInstance(args[UPDATE_LEVEL])) {
            startNoteBlockChain(args);
        }
        try {
            return holder.value().updateShape(thisBlock, args);
        } catch (Exception e) {
            CraftEngine.instance().logger().error("Failed to run updateShape", e);
            return args[0];
        }
    }

    private static void startNoteBlockChain(Object[] args) {
        Object direction = args[UPDATE_DIRECTION];
        Object serverLevel = args[UPDATE_LEVEL];
        Object blockPos = args[UPDATE_POS];
        if (direction != DirectionProxy.DOWN && direction != DirectionProxy.UP) {
            return;
        }
        Object chunkSource = ServerLevelProxy.INSTANCE.getChunkSource(serverLevel);
        ServerChunkCacheProxy.INSTANCE.blockChanged(chunkSource, blockPos);
        Object opposite = direction == DirectionProxy.DOWN ? DirectionProxy.UP : DirectionProxy.DOWN;
        NoteBlockChainUpdateUtils.noteBlockChainUpdate(serverLevel, chunkSource, opposite, blockPos, Config.maxNoteBlockChainUpdate());
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
