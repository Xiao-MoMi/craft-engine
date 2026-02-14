package net.momirealms.craftengine.bukkit.plugin.injector;

import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.util.RandomSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.WorldlyContainerHolderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.WorldlyContainerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.InsideBlockEffectApplierProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.LivingEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.item.FallingBlockEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.player.PlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.projectile.ProjectileProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.*;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.*;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockStateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.StateHolderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.properties.PropertyProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidStateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.pathfinder.PathComputationTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.redstone.OrientationProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.LootParamsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.BlockHitResultProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.shapes.CollisionContextProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.shapes.VoxelShapeProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.method.matcher.MethodMatcher;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.BiConsumer;

import static java.util.Objects.requireNonNull;

final class BlockReflections {
    private BlockReflections() {}

    public static final Method method$BlockBehaviour$isPathfindable = requireNonNull(
            SparrowClass.of(BlockBehaviourProxy.CLASS).getDeclaredMethod(MethodMatcher.named("isPathfindable")
                    .and(VersionHelper.isOrAbove1_20_5()
                            ? MethodMatcher.takeArguments(BlockStateProxy.CLASS, PathComputationTypeProxy.CLASS)
                            : MethodMatcher.takeArguments(BlockStateProxy.CLASS, BlockGetterProxy.CLASS, BlockPosProxy.CLASS, PathComputationTypeProxy.CLASS))
                    .and(MethodMatcher.returnType(boolean.class)))
    );

    public static final Method method$BlockBehaviour$getShape = requireNonNull(
            SparrowClass.of(BlockBehaviourProxy.CLASS).getDeclaredMethod(MethodMatcher.named("getShape")
                    .and(MethodMatcher.takeArguments(BlockStateProxy.CLASS, BlockGetterProxy.CLASS, BlockPosProxy.CLASS, CollisionContextProxy.CLASS))
                    .and(MethodMatcher.returnType(VoxelShapeProxy.CLASS)))
    );

    public static final Method method$BlockBehaviour$getCollisionShape = requireNonNull(
            SparrowClass.of(BlockBehaviourProxy.CLASS).getDeclaredMethod(MethodMatcher.named("getCollisionShape")
                    .and(MethodMatcher.takeArguments(BlockStateProxy.CLASS, BlockGetterProxy.CLASS, BlockPosProxy.CLASS, CollisionContextProxy.CLASS))
                    .and(MethodMatcher.returnType(VoxelShapeProxy.CLASS)))
    );

    public static final Method method$BlockBehaviour$getBlockSupportShape = requireNonNull(
            SparrowClass.of(BlockBehaviourProxy.CLASS).getDeclaredMethod(MethodMatcher.named("getBlockSupportShape")
                    .and(MethodMatcher.takeArguments(BlockStateProxy.CLASS, BlockGetterProxy.CLASS, BlockPosProxy.CLASS))
                    .and(MethodMatcher.returnType(VoxelShapeProxy.CLASS)))
    );

    public static final Method method$BlockBehaviour$hasAnalogOutputSignal = requireNonNull(
            SparrowClass.of(BlockBehaviourProxy.CLASS).getDeclaredMethod(MethodMatcher.named("hasAnalogOutputSignal")
                    .and(MethodMatcher.takeArguments(BlockStateProxy.CLASS))
                    .and(MethodMatcher.returnType(boolean.class)))
    );

    public static final Method method$BlockBehaviour$getAnalogOutputSignal = requireNonNull(
            SparrowClass.of(BlockBehaviourProxy.CLASS).getDeclaredMethod(MethodMatcher.named("getAnalogOutputSignal")
                    .and(VersionHelper.isOrAbove1_21_9()
                            ? MethodMatcher.takeArguments(BlockStateProxy.CLASS, LevelProxy.CLASS, BlockPosProxy.CLASS, DirectionProxy.CLASS)
                            : MethodMatcher.takeArguments(BlockStateProxy.CLASS, LevelProxy.CLASS, BlockPosProxy.CLASS))
                    .and(MethodMatcher.returnType(int.class)))
    );

    public static final Method method$BlockBehaviour$updateShape = requireNonNull(
            SparrowClass.of(BlockBehaviourProxy.CLASS).getDeclaredMethod(MethodMatcher.named("updateShape")
                    .and(VersionHelper.isOrAbove1_21_2()
                            ? MethodMatcher.takeArguments(BlockStateProxy.CLASS, LevelReaderProxy.CLASS, ScheduledTickAccessProxy.CLASS, BlockPosProxy.CLASS, DirectionProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, RandomSourceProxy.CLASS)
                            : MethodMatcher.takeArguments(BlockStateProxy.CLASS, DirectionProxy.CLASS, BlockStateProxy.CLASS, LevelAccessorProxy.CLASS, BlockPosProxy.CLASS, BlockPosProxy.CLASS))
                    .and(MethodMatcher.returnType(BlockStateProxy.CLASS)))
    );

    public static final Method method$BlockBehaviour$canSurvive = requireNonNull(
            SparrowClass.of(BlockBehaviourProxy.CLASS).getDeclaredMethod(MethodMatcher.named("canSurvive")
                    .and(MethodMatcher.takeArguments(BlockStateProxy.CLASS, LevelReaderProxy.CLASS, BlockPosProxy.CLASS))
                    .and(MethodMatcher.returnType(boolean.class)))
    );

    public static final Method method$Block$stepOn = requireNonNull(
            SparrowClass.of(BlockProxy.CLASS).getDeclaredMethod(MethodMatcher.named("stepOn")
                    .and(MethodMatcher.takeArguments(LevelProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, EntityProxy.CLASS))
                    .and(MethodMatcher.returnType(void.class)))
    );

    public static final Method method$Fallable$onLand = requireNonNull(
            SparrowClass.of(FallableProxy.CLASS).getDeclaredMethod(MethodMatcher.named("onLand")
                    .and(MethodMatcher.takeArguments(LevelProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, BlockStateProxy.CLASS, FallingBlockEntityProxy.CLASS))
                    .and(MethodMatcher.returnType(void.class)))
    );

    public static final Method method$Fallable$onBrokenAfterFall = requireNonNull(
            SparrowClass.of(FallableProxy.CLASS).getDeclaredMethod(MethodMatcher.named("onBrokenAfterFall")
                    .and(MethodMatcher.takeArguments(LevelProxy.CLASS, BlockPosProxy.CLASS, FallingBlockEntityProxy.CLASS))
                    .and(MethodMatcher.returnType(void.class)))
    );

    public static final Method method$StateHolder$hasProperty = requireNonNull(
            SparrowClass.of(StateHolderProxy.CLASS).getDeclaredMethod(MethodMatcher.named("hasProperty")
                    .and(MethodMatcher.takeArguments(PropertyProxy.CLASS))
                    .and(MethodMatcher.returnType(boolean.class)))
    );

    public static final Method method$StateHolder$getValue = requireNonNull(
            SparrowClass.of(StateHolderProxy.CLASS).getDeclaredMethod(MethodMatcher.named("getValue")
                    .and(MethodMatcher.takeArguments(PropertyProxy.CLASS))
                    .and(MethodMatcher.returnType(Comparable.class)))
    );

    public static final Method method$StateHolder$setValue = requireNonNull(
            SparrowClass.of(StateHolderProxy.CLASS).getDeclaredMethod(MethodMatcher.named("setValue")
                    .and(MethodMatcher.takeArguments(PropertyProxy.CLASS, Comparable.class))
                    .and(MethodMatcher.returnType(Object.class)))
    );

    public static final Method method$BonemealableBlock$isValidBonemealTarget = requireNonNull(
            SparrowClass.of(BonemealableBlockProxy.CLASS).getDeclaredMethod(MethodMatcher.named("isValidBonemealTarget")
                    .and(VersionHelper.isOrAbove1_20_2()
                            ? MethodMatcher.takeArguments(LevelReaderProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS)
                            : MethodMatcher.takeArguments(LevelReaderProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, boolean.class))
                    .and(MethodMatcher.returnType(boolean.class)))
    );

    public static final Method method$WorldlyContainerHolder$getContainer = requireNonNull(
            SparrowClass.of(WorldlyContainerHolderProxy.CLASS).getDeclaredMethod(MethodMatcher.named("getContainer")
                    .and(MethodMatcher.takeArguments(BlockStateProxy.CLASS, LevelAccessorProxy.CLASS, BlockPosProxy.CLASS))
                    .and(MethodMatcher.returnType(WorldlyContainerProxy.CLASS)))
    );

    public static final Method method$BonemealableBlock$isBonemealSuccess = requireNonNull(
            SparrowClass.of(BonemealableBlockProxy.CLASS).getDeclaredMethod(MethodMatcher.named("isBonemealSuccess")
                    .and(MethodMatcher.takeArguments(LevelProxy.CLASS, RandomSourceProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS))
                    .and(MethodMatcher.returnType(boolean.class)))
    );

    public static final Method method$SimpleWaterloggedBlock$canPlaceLiquid = requireNonNull(
            SparrowClass.of(SimpleWaterloggedBlockProxy.CLASS).getDeclaredMethod(MethodMatcher.named("canPlaceLiquid")
                    .and(VersionHelper.isOrAbove1_21_5()
                            ? MethodMatcher.takeArguments(LivingEntityProxy.CLASS, BlockGetterProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, FluidProxy.CLASS)
                            : VersionHelper.isOrAbove1_20_2()
                                ? MethodMatcher.takeArguments(PlayerProxy.CLASS, BlockGetterProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, FluidProxy.CLASS)
                                : MethodMatcher.takeArguments(BlockGetterProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, FluidProxy.CLASS))
                    .and(MethodMatcher.returnType(boolean.class)))
    );

    public static final Method method$SimpleWaterloggedBlock$placeLiquid = requireNonNull(
            SparrowClass.of(SimpleWaterloggedBlockProxy.CLASS).getDeclaredMethod(MethodMatcher.named("placeLiquid")
                    .and(MethodMatcher.takeArguments(LevelAccessorProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, FluidStateProxy.CLASS))
                    .and(MethodMatcher.returnType(boolean.class)))
    );

    public static final Method method$SimpleWaterloggedBlock$pickupBlock = requireNonNull(
            SparrowClass.of(SimpleWaterloggedBlockProxy.CLASS).getDeclaredMethod(MethodMatcher.named("pickupBlock")
                    .and(VersionHelper.isOrAbove1_21_5()
                            ? MethodMatcher.takeArguments(LivingEntityProxy.CLASS, LevelAccessorProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS)
                            : VersionHelper.isOrAbove1_20_2()
                                ? MethodMatcher.takeArguments(PlayerProxy.CLASS, LevelAccessorProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS)
                                : MethodMatcher.takeArguments(LevelAccessorProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS))
                    .and(MethodMatcher.returnType(ItemStackProxy.CLASS)))
    );

    public static final Method method$BlockBehaviour$rotate = requireNonNull(
            SparrowClass.of(BlockBehaviourProxy.CLASS).getDeclaredMethod(MethodMatcher.named("rotate")
                    .and(MethodMatcher.takeArguments(BlockStateProxy.CLASS, RotationProxy.CLASS))
                    .and(MethodMatcher.returnType(BlockStateProxy.CLASS)))
    );

    public static final Method method$BlockBehaviour$mirror = requireNonNull(
            SparrowClass.of(BlockBehaviourProxy.CLASS).getDeclaredMethod(MethodMatcher.named("mirror")
                    .and(MethodMatcher.takeArguments(BlockStateProxy.CLASS, MirrorProxy.CLASS))
                    .and(MethodMatcher.returnType(BlockStateProxy.CLASS)))
    );

    public static final Method method$BlockBehaviour$neighborChanged = requireNonNull(
            SparrowClass.of(BlockBehaviourProxy.CLASS).getDeclaredMethod(MethodMatcher.named("neighborChanged")
                    .and(VersionHelper.isOrAbove1_21_2()
                            ? MethodMatcher.takeArguments(BlockStateProxy.CLASS, LevelProxy.CLASS, BlockPosProxy.CLASS, BlockProxy.CLASS, OrientationProxy.CLASS, boolean.class)
                            : MethodMatcher.takeArguments(BlockStateProxy.CLASS, LevelProxy.CLASS, BlockPosProxy.CLASS, BlockProxy.CLASS, BlockPosProxy.CLASS, boolean.class))
                    .and(MethodMatcher.returnType(void.class)))
    );

    public static final Method method$BonemealableBlock$performBonemeal = requireNonNull(
            SparrowClass.of(BonemealableBlockProxy.CLASS).getDeclaredMethod(MethodMatcher.named("performBonemeal")
                    .and(MethodMatcher.takeArguments(ServerLevelProxy.CLASS, RandomSourceProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS))
                    .and(MethodMatcher.returnType(void.class)))
    );

    public static final Method method$BlockBehaviour$tick = requireNonNull(
            SparrowClass.of(BlockBehaviourProxy.CLASS).getDeclaredMethod(MethodMatcher.named("tick")
                    .and(MethodMatcher.takeArguments(BlockStateProxy.CLASS, ServerLevelProxy.CLASS, BlockPosProxy.CLASS, RandomSourceProxy.CLASS))
                    .and(MethodMatcher.returnType(void.class)))
    );

    public static final Method method$BlockBehaviour$randomTick = requireNonNull(
            SparrowClass.of(BlockBehaviourProxy.CLASS).getDeclaredMethod(MethodMatcher.named("randomTick")
                    .and(MethodMatcher.takeArguments(BlockStateProxy.CLASS, ServerLevelProxy.CLASS, BlockPosProxy.CLASS, RandomSourceProxy.CLASS))
                    .and(MethodMatcher.returnType(void.class)))
    );

    public static final Method method$BlockBehaviour$onPlace = requireNonNull(
            SparrowClass.of(BlockBehaviourProxy.CLASS).getDeclaredMethod(MethodMatcher.named("onPlace")
                    .and(MethodMatcher.takeArguments(BlockStateProxy.CLASS, LevelProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, boolean.class))
                    .and(MethodMatcher.returnType(void.class)))
    );

    public static final Method method$BlockBehaviour$entityInside = requireNonNull(
            SparrowClass.of(BlockBehaviourProxy.CLASS).getDeclaredMethod(MethodMatcher.named("entityInside")
                    .and(VersionHelper.isOrAbove1_21_10()
                            ? MethodMatcher.takeArguments(BlockStateProxy.CLASS, LevelProxy.CLASS, BlockPosProxy.CLASS, EntityProxy.CLASS, InsideBlockEffectApplierProxy.CLASS, boolean.class)
                            : VersionHelper.isOrAbove1_21_5()
                                ? MethodMatcher.takeArguments(BlockStateProxy.CLASS, LevelProxy.CLASS, BlockPosProxy.CLASS, EntityProxy.CLASS, InsideBlockEffectApplierProxy.CLASS)
                                : MethodMatcher.takeArguments(BlockStateProxy.CLASS, LevelProxy.CLASS, BlockPosProxy.CLASS, EntityProxy.CLASS))
                    .and(MethodMatcher.returnType(void.class)))
    );

    public static final Method method$BlockBehaviour$affectNeighborsAfterRemoval = requireNonNull(
            SparrowClass.of(BlockBehaviourProxy.CLASS).getDeclaredMethod(MethodMatcher.named("affectNeighborsAfterRemoval", "onRemove")
                    .and(VersionHelper.isOrAbove1_21_5()
                            ? MethodMatcher.takeArguments(BlockStateProxy.CLASS, ServerLevelProxy.CLASS, BlockPosProxy.CLASS, boolean.class)
                            : MethodMatcher.takeArguments(BlockStateProxy.CLASS, LevelProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, boolean.class))
                    .and(MethodMatcher.returnType(void.class)))
    );

    public static final Method method$BlockBehaviour$getSignal = requireNonNull(
            SparrowClass.of(BlockBehaviourProxy.CLASS).getDeclaredMethod(MethodMatcher.named("getSignal")
                    .and(MethodMatcher.takeArguments(BlockStateProxy.CLASS, BlockGetterProxy.CLASS, BlockPosProxy.CLASS, DirectionProxy.CLASS))
                    .and(MethodMatcher.returnType(int.class)))
    );

    public static final Method method$BlockBehaviour$getDirectSignal = requireNonNull(
            SparrowClass.of(BlockBehaviourProxy.CLASS).getDeclaredMethod(MethodMatcher.named("getDirectSignal")
                    .and(MethodMatcher.takeArguments(BlockStateProxy.CLASS, BlockGetterProxy.CLASS, BlockPosProxy.CLASS, DirectionProxy.CLASS))
                    .and(MethodMatcher.returnType(int.class)))
    );

    public static final Method method$BlockBehaviour$isSignalSource = requireNonNull(
            SparrowClass.of(BlockBehaviourProxy.CLASS).getDeclaredMethod(MethodMatcher.named("isSignalSource")
                    .and(MethodMatcher.takeArguments(BlockStateProxy.CLASS))
                    .and(MethodMatcher.returnType(boolean.class)))
    );

    public static final Method method$BlockStateBase$getDrops = requireNonNull(
            SparrowClass.of(BlockBehaviourProxy.BlockStateBaseProxy.CLASS).getDeclaredMethod(MethodMatcher.named("getDrops")
                    .and(MethodMatcher.takeArguments(LootParamsProxy.BuilderProxy.CLASS))
                    .and(MethodMatcher.returnType(List.class)))
    );

    public static final Method method$Block$playerWillDestroy = requireNonNull(
            SparrowClass.of(BlockProxy.CLASS).getDeclaredMethod(MethodMatcher.named("playerWillDestroy")
                    .and(MethodMatcher.takeArguments(LevelProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, PlayerProxy.CLASS))
                    .and(MethodMatcher.returnType(VersionHelper.isOrAbove1_20_3() ? BlockStateProxy.CLASS : void.class)))
    );

    public static final Method method$BlockBehaviour$spawnAfterBreak = requireNonNull(
            SparrowClass.of(BlockBehaviourProxy.CLASS).getDeclaredMethod(MethodMatcher.named("spawnAfterBreak")
                    .and(MethodMatcher.takeArguments(BlockStateProxy.CLASS, ServerLevelProxy.CLASS, BlockPosProxy.CLASS, ItemStackProxy.CLASS, boolean.class))
                    .and(MethodMatcher.returnType(void.class)))
    );

    public static final Method method$Block$fallOn = requireNonNull(
            SparrowClass.of(BlockProxy.CLASS).getDeclaredMethod(MethodMatcher.named("fallOn")
                    .and(MethodMatcher.takeArguments(LevelProxy.CLASS, BlockStateProxy.CLASS, BlockPosProxy.CLASS, EntityProxy.CLASS, VersionHelper.isOrAbove1_21_5() ? double.class : float.class))
                    .and(MethodMatcher.returnType(void.class)))
    );

    public static final Method method$Block$updateEntityMovementAfterFallOn = requireNonNull(
            SparrowClass.of(BlockProxy.CLASS).getDeclaredMethod(MethodMatcher.named("updateEntityMovementAfterFallOn", "updateEntityAfterFallOn")
                    .and(MethodMatcher.takeArguments(BlockGetterProxy.CLASS, EntityProxy.CLASS))
                    .and(MethodMatcher.returnType(void.class)))
    );

    public static final Method method$BlockStateBase$is = requireNonNull(
            SparrowClass.of(BlockBehaviourProxy.BlockStateBaseProxy.CLASS).getDeclaredMethod(MethodMatcher.named("is")
                    .and(MethodMatcher.takeArguments(BlockProxy.CLASS))
                    .and(MethodMatcher.returnType(boolean.class)))
    );

    public static final Method method$BlockBehaviour$onProjectileHit = requireNonNull(
            SparrowClass.of(BlockBehaviourProxy.CLASS).getDeclaredMethod(MethodMatcher.named("onProjectileHit")
                    .and(MethodMatcher.takeArguments(LevelProxy.CLASS, BlockStateProxy.CLASS, BlockHitResultProxy.CLASS, ProjectileProxy.CLASS))
                    .and(MethodMatcher.returnType(void.class)))
    );

    public static final Method method$Block$setPlacedBy = requireNonNull(
            SparrowClass.of(BlockProxy.CLASS).getDeclaredMethod(MethodMatcher.named("setPlacedBy")
                    .and(MethodMatcher.takeArguments(LevelProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, LivingEntityProxy.CLASS, ItemStackProxy.CLASS))
                    .and(MethodMatcher.returnType(void.class)))
    );

    // 1.21+
    public static final Method method$BlockBehaviour$onExplosionHit = MiscUtils.requireNonNullIf(
            SparrowClass.of(BlockBehaviourProxy.CLASS).getDeclaredMethod(MethodMatcher.named("onExplosionHit")
                    .and(MethodMatcher.takeArguments(BlockStateProxy.CLASS, VersionHelper.isOrAbove1_21_2() ? ServerLevelProxy.CLASS : LevelProxy.CLASS, BlockPosProxy.CLASS, ExplosionProxy.CLASS, BiConsumer.class))
                    .and(MethodMatcher.returnType(void.class))),
            VersionHelper.isOrAbove1_21()
    );
}
