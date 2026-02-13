package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import net.momirealms.craftengine.bukkit.util.BukkitReflectionUtils;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ReflectionUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.util.RandomSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.WorldlyContainerHolderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.WorldlyContainerProxy;
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
import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.LevelChunkSectionProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.PalettedContainerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidStateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.pathfinder.PathComputationTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.redstone.OrientationProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.LootParamsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.BlockHitResultProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.shapes.CollisionContextProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.shapes.VoxelShapeProxy;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

import static java.util.Objects.requireNonNull;

public final class CoreReflections {
    private CoreReflections() {}

    public static final Method method$LevelChunkSection$setBlockState = requireNonNull(
            ReflectionUtils.getMethod(LevelChunkSectionProxy.CLASS, BlockStateProxy.CLASS, int.class, int.class, int.class, BlockStateProxy.CLASS, boolean.class)
    );

    public static final Method method$BlockBehaviour$isPathFindable = requireNonNull(
            VersionHelper.isOrAbove1_20_5() ?
            ReflectionUtils.getDeclaredMethod(BlockBehaviourProxy.CLASS, boolean.class, BlockStateProxy.CLASS, PathComputationTypeProxy.CLASS) :
            ReflectionUtils.getMethod(BlockBehaviourProxy.CLASS, boolean.class, BlockStateProxy.CLASS, BlockGetterProxy.CLASS, BlockPosProxy.CLASS, PathComputationTypeProxy.CLASS)
    );

    public static final Method method$BlockBehaviour$getShape = requireNonNull(
            ReflectionUtils.getDeclaredMethod(BlockBehaviourProxy.CLASS, VoxelShapeProxy.CLASS, new String[]{"getShape", "a"}, BlockStateProxy.CLASS, BlockGetterProxy.CLASS, BlockPosProxy.CLASS, CollisionContextProxy.CLASS)
    );

    public static final Method method$BlockBehaviour$getCollisionShape = requireNonNull(
            ReflectionUtils.getDeclaredMethod(BlockBehaviourProxy.CLASS, VoxelShapeProxy.CLASS, new String[]{"getCollisionShape", VersionHelper.isOrAbove1_20_3() ? "b" : "c"}, BlockStateProxy.CLASS, BlockGetterProxy.CLASS, BlockPosProxy.CLASS, CollisionContextProxy.CLASS)
    );

    public static final Method method$BlockBehaviour$getBlockSupportShape = requireNonNull(
            ReflectionUtils.getDeclaredMethod(BlockBehaviourProxy.CLASS, VoxelShapeProxy.CLASS, new String[]{"getBlockSupportShape", "b_"}, BlockStateProxy.CLASS, BlockGetterProxy.CLASS, BlockPosProxy.CLASS)
    );

    public static final Class<?> clazz$Entity = requireNonNull(
            ReflectionUtils.getClazz(BukkitReflectionUtils.assembleMCClass("world.entity.Entity"))
    );
    
    public static final Method method$BlockBehaviour$hasAnalogOutputSignal = requireNonNull(
            ReflectionUtils.getDeclaredMethod(BlockBehaviourProxy.CLASS, boolean.class, new String[]{"hasAnalogOutputSignal",
            VersionHelper.isOrAbove1_20_5() ? "c_" : "d_"}, BlockStateProxy.CLASS)
    );

    public static final Method method$BlockBehaviour$getAnalogOutputSignal = requireNonNull(
            VersionHelper.isOrAbove1_21_9()
                    ? ReflectionUtils.getDeclaredMethod(BlockBehaviourProxy.CLASS, int.class, new String[]{"getAnalogOutputSignal", "a"}, BlockStateProxy.CLASS, LevelProxy.CLASS, BlockPosProxy.CLASS, DirectionProxy.CLASS)
                    : ReflectionUtils.getDeclaredMethod(BlockBehaviourProxy.CLASS, int.class, new String[]{"getAnalogOutputSignal", "a"}, BlockStateProxy.CLASS, LevelProxy.CLASS, BlockPosProxy.CLASS)
    );

    public static final Method method$BlockBehaviour$updateShape = requireNonNull(
            VersionHelper.isOrAbove1_21_2() ?
                    ReflectionUtils.getDeclaredMethod(BlockBehaviourProxy.CLASS, BlockStateProxy.CLASS, BlockStateProxy.CLASS, LevelReaderProxy.CLASS, ScheduledTickAccessProxy.CLASS, BlockPosProxy.CLASS, DirectionProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, RandomSourceProxy.CLASS) :
                    ReflectionUtils.getDeclaredMethod(BlockBehaviourProxy.CLASS, BlockStateProxy.CLASS, BlockStateProxy.CLASS, DirectionProxy.CLASS, BlockStateProxy.CLASS, LevelAccessorProxy.CLASS, BlockPosProxy.CLASS, BlockPosProxy.CLASS)
    );

    public static final Method method$BlockBehaviour$canSurvive = requireNonNull(
            ReflectionUtils.getDeclaredMethod(BlockBehaviourProxy.CLASS, boolean.class, BlockStateProxy.CLASS, LevelReaderProxy.CLASS, BlockPosProxy.CLASS)
    );

    public static final Method method$Block$stepOn = requireNonNull(
            ReflectionUtils.getMethod(BlockProxy.CLASS, void.class, new String[] {"stepOn", "a"}, LevelProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, clazz$Entity)
    );

    public static final Method method$BlockBehaviour$onExplosionHit = MiscUtils.requireNonNullIf(
            ReflectionUtils.getDeclaredMethod(BlockBehaviourProxy.CLASS, void.class, BlockStateProxy.CLASS, VersionHelper.isOrAbove1_21_2() ? ServerLevelProxy.CLASS : LevelProxy.CLASS, BlockPosProxy.CLASS, ExplosionProxy.CLASS, BiConsumer.class),
            VersionHelper.isOrAbove1_21()
    );

    public static final Method method$Fallable$onLand = requireNonNull(
            ReflectionUtils.getMethod(FallableProxy.CLASS, void.class, LevelProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, BlockStateProxy.CLASS, FallingBlockEntityProxy.CLASS)
    );

    public static final Method method$Fallable$onBrokenAfterFall = requireNonNull(
            ReflectionUtils.getMethod(FallableProxy.CLASS, void.class, LevelProxy.CLASS, BlockPosProxy.CLASS, FallingBlockEntityProxy.CLASS)
    );

    public static final Method method$StateHolder$hasProperty = requireNonNull(
            ReflectionUtils.getMethod(StateHolderProxy.CLASS, boolean.class, PropertyProxy.CLASS)
    );

    public static final Method method$StateHolder$getValue = requireNonNull(
            ReflectionUtils.getMethod(StateHolderProxy.CLASS, Object.class, new String[] {"getValue", "c"}, PropertyProxy.CLASS)
    );

    public static final Method method$StateHolder$setValue = requireNonNull(
            ReflectionUtils.getMethod(StateHolderProxy.CLASS, Object.class, new String[] {"setValue", VersionHelper.isOrAbove1_21_2() ? "b" : "a"}, PropertyProxy.CLASS, Comparable.class)
    );

    public static final Method method$BlockStateBase$updateNeighbourShapes = requireNonNull(
            ReflectionUtils.getMethod(BlockBehaviourProxy.BlockStateBaseProxy.CLASS, void.class, LevelAccessorProxy.CLASS, BlockPosProxy.CLASS, int.class, int.class)
    );

    public static final Method method$BonemealableBlock$isValidBonemealTarget = requireNonNull(
            VersionHelper.isOrAbove1_20_2() ?
                    ReflectionUtils.getInstanceMethod(BonemealableBlockProxy.CLASS, boolean.class, LevelReaderProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS) :
                    ReflectionUtils.getInstanceMethod(BonemealableBlockProxy.CLASS, boolean.class, LevelReaderProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, boolean.class)
    );

    public static final Method method$WorldlyContainerHolder$getContainer = requireNonNull(
            ReflectionUtils.getMethod(WorldlyContainerHolderProxy.CLASS, WorldlyContainerProxy.CLASS, BlockStateProxy.CLASS, LevelAccessorProxy.CLASS, BlockPosProxy.CLASS)
    );

    public static final Method method$BonemealableBlock$isBonemealSuccess = requireNonNull(
            ReflectionUtils.getMethod(BonemealableBlockProxy.CLASS, boolean.class, LevelProxy.CLASS, RandomSourceProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS)
    );
    
    public static final Method method$PalettedContainer$getAndSet = Objects.requireNonNull(
            ReflectionUtils.getMethod(PalettedContainerProxy.CLASS, Object.class, new String[] {"a", "getAndSet"}, int.class, int.class, int.class, Object.class)
    );

    public static final Method method$PalettedContainer$getAndSetUnchecked = Objects.requireNonNull(
            ReflectionUtils.getMethod(PalettedContainerProxy.CLASS, Object.class, new String[] {"b", "getAndSetUnchecked"}, int.class, int.class, int.class, Object.class)
    );

    public static final Method method$SimpleWaterloggedBlock$canPlaceLiquid = requireNonNull(
            VersionHelper.isOrAbove1_21_5()
                    ? ReflectionUtils.getMethod(SimpleWaterloggedBlockProxy.CLASS, boolean.class, LivingEntityProxy.CLASS, BlockGetterProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, FluidProxy.CLASS)
                    : VersionHelper.isOrAbove1_20_2()
                    ? ReflectionUtils.getMethod(SimpleWaterloggedBlockProxy.CLASS, boolean.class, PlayerProxy.CLASS, BlockGetterProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, FluidProxy.CLASS)
                    : ReflectionUtils.getMethod(SimpleWaterloggedBlockProxy.CLASS, boolean.class, BlockGetterProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, FluidProxy.CLASS)
    );

    public static final Method method$SimpleWaterloggedBlock$placeLiquid = requireNonNull(
            ReflectionUtils.getMethod(SimpleWaterloggedBlockProxy.CLASS, boolean.class, LevelAccessorProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, FluidStateProxy.CLASS)
    );

    public static final Method method$SimpleWaterloggedBlock$pickupBlock = requireNonNull(
            VersionHelper.isOrAbove1_21_5()
                    ? ReflectionUtils.getMethod(SimpleWaterloggedBlockProxy.CLASS, ItemStackProxy.CLASS, LivingEntityProxy.CLASS, LevelAccessorProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS)
                    : VersionHelper.isOrAbove1_20_2()
                    ? ReflectionUtils.getMethod(SimpleWaterloggedBlockProxy.CLASS, ItemStackProxy.CLASS, PlayerProxy.CLASS, LevelAccessorProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS)
                    : ReflectionUtils.getMethod(SimpleWaterloggedBlockProxy.CLASS, ItemStackProxy.CLASS, LevelAccessorProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS)
    );

    public static final Method method$BlockStateBase$isFaceSturdy = requireNonNull(
            ReflectionUtils.getMethod(BlockBehaviourProxy.BlockStateBaseProxy.CLASS, boolean.class, BlockGetterProxy.CLASS, BlockPosProxy.CLASS, DirectionProxy.CLASS, SupportTypeProxy.CLASS)
    );

    public static final Method method$BlockBehaviour$rotate = requireNonNull(
            ReflectionUtils.getDeclaredMethod(BlockBehaviourProxy.CLASS, BlockStateProxy.CLASS, BlockStateProxy.CLASS, RotationProxy.CLASS)
    );

    public static final Method method$BlockBehaviour$mirror = requireNonNull(
            ReflectionUtils.getDeclaredMethod(BlockBehaviourProxy.CLASS, BlockStateProxy.CLASS, BlockStateProxy.CLASS, MirrorProxy.CLASS)
    );

    public static final Method method$BlockBehaviour$neighborChanged = requireNonNull(
            VersionHelper.isOrAbove1_21_2() ?
                    ReflectionUtils.getDeclaredMethod(BlockBehaviourProxy.CLASS, void.class, BlockStateProxy.CLASS, LevelProxy.CLASS, BlockPosProxy.CLASS, BlockProxy.CLASS, OrientationProxy.CLASS, boolean.class) :
                    Optional.ofNullable(ReflectionUtils.getDeclaredMethod(BlockBehaviourProxy.CLASS, void.class, BlockStateProxy.CLASS, LevelProxy.CLASS, BlockPosProxy.CLASS, BlockProxy.CLASS, BlockPosProxy.CLASS, boolean.class))
                            .orElse(ReflectionUtils.getMethod(BlockBehaviourProxy.CLASS, void.class, BlockStateProxy.CLASS, LevelProxy.CLASS, BlockPosProxy.CLASS, BlockProxy.CLASS, BlockPosProxy.CLASS, boolean.class))
    );

    public static final Method method$BonemealableBlock$performBonemeal = requireNonNull(
            ReflectionUtils.getMethod(BonemealableBlockProxy.CLASS, void.class, ServerLevelProxy.CLASS, RandomSourceProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS)
    );
    
    public static final Method method$BlockBehaviour$tick = requireNonNull(
            ReflectionUtils.getDeclaredMethod(BlockBehaviourProxy.CLASS, void.class, new String[]{"tick", "a"}, BlockStateProxy.CLASS, ServerLevelProxy.CLASS, BlockPosProxy.CLASS, RandomSourceProxy.CLASS)
    );

    public static final Method method$BlockBehaviour$randomTick = requireNonNull(
            ReflectionUtils.getDeclaredMethod(BlockBehaviourProxy.CLASS, void.class, new String[]{"randomTick", "b"}, BlockStateProxy.CLASS, ServerLevelProxy.CLASS, BlockPosProxy.CLASS, RandomSourceProxy.CLASS)
    );

    public static final Method method$BlockBehaviour$onPlace = requireNonNull(
            ReflectionUtils.getDeclaredMethod(BlockBehaviourProxy.CLASS, void.class, new String[]{"onPlace", VersionHelper.isOrAbove1_21_5() ? "a" : "b"},
                    BlockStateProxy.CLASS, LevelProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, boolean.class)
    );

    public static final Method method$BlockBehaviour$entityInside = requireNonNull(
            VersionHelper.isOrAbove1_21_10()
                    ? ReflectionUtils.getDeclaredMethod(BlockBehaviourProxy.CLASS, void.class, new String[]{"entityInside", "a"}, BlockStateProxy.CLASS, LevelProxy.CLASS, BlockPosProxy.CLASS, clazz$Entity, InsideBlockEffectApplierProxy.CLASS, boolean.class)
                    : VersionHelper.isOrAbove1_21_5()
                        ? ReflectionUtils.getDeclaredMethod(BlockBehaviourProxy.CLASS, void.class, new String[]{"entityInside", "a"}, BlockStateProxy.CLASS, LevelProxy.CLASS, BlockPosProxy.CLASS, clazz$Entity, InsideBlockEffectApplierProxy.CLASS)
                        : ReflectionUtils.getDeclaredMethod(BlockBehaviourProxy.CLASS, void.class, new String[]{"entityInside", "a"}, BlockStateProxy.CLASS, LevelProxy.CLASS, BlockPosProxy.CLASS, clazz$Entity)
    );

    // 1.21.5+
    public static final Method method$BlockBehaviour$affectNeighborsAfterRemoval = ReflectionUtils.getDeclaredMethod(BlockBehaviourProxy.CLASS, void.class, new String[]{"affectNeighborsAfterRemoval", "a"}, BlockStateProxy.CLASS, ServerLevelProxy.CLASS, BlockPosProxy.CLASS, boolean.class);

    public static final Method method$BlockBehaviour$getSignal = requireNonNull(
            ReflectionUtils.getDeclaredMethod(BlockBehaviourProxy.CLASS, int.class, new String[]{"getSignal", "a"}, BlockStateProxy.CLASS, BlockGetterProxy.CLASS, BlockPosProxy.CLASS, DirectionProxy.CLASS)
    );

    public static final Method method$BlockBehaviour$getDirectSignal = requireNonNull(
            ReflectionUtils.getDeclaredMethod(BlockBehaviourProxy.CLASS, int.class, new String[]{"getDirectSignal", "b"}, BlockStateProxy.CLASS, BlockGetterProxy.CLASS, BlockPosProxy.CLASS, DirectionProxy.CLASS)
    );

    public static final Method method$BlockBehaviour$isSignalSource = requireNonNull(
            ReflectionUtils.getDeclaredMethod(BlockBehaviourProxy.CLASS, boolean.class, new String[]{
                    "isSignalSource",
                    !VersionHelper.isOrAbove1_20_5() ? "f_" : // 1.20.1-1.20.4
                    !VersionHelper.isOrAbove1_21_2() ? "e_" /* 1.20.5-1.21.1 */ : "f_" // 1.21.2+
            }, BlockStateProxy.CLASS)
    );

    public static final Method method$BlockStateBase$getDrops = requireNonNull(
            ReflectionUtils.getMethod(BlockBehaviourProxy.BlockStateBaseProxy.CLASS, List.class, LootParamsProxy.BuilderProxy.CLASS)
    );

    public static final Method method$Block$playerWillDestroy = requireNonNull(
            ReflectionUtils.getDeclaredMethod(
                    BlockProxy.CLASS,
                    VersionHelper.isOrAbove1_20_3() ? BlockStateProxy.CLASS : void.class,
                    LevelProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, PlayerProxy.CLASS
            )
    );

    public static final Method method$BlockBehaviour$spawnAfterBreak = requireNonNull(
            ReflectionUtils.getDeclaredMethod(BlockBehaviourProxy.CLASS, void.class, BlockStateProxy.CLASS, ServerLevelProxy.CLASS, BlockPosProxy.CLASS, ItemStackProxy.CLASS, boolean.class)
    );

    // 1.20~1.21.4
    public static final Method method$BlockBehaviour$onRemove = MiscUtils.requireNonNullIf(
            ReflectionUtils.getDeclaredMethod(BlockBehaviourProxy.CLASS, void.class, new String[]{"a", "onRemove"}, BlockStateProxy.CLASS, LevelProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, boolean.class),
            !VersionHelper.isOrAbove1_21_5()
    );

    public static final Method method$Block$fallOn = requireNonNull(
            ReflectionUtils.getDeclaredMethod(BlockProxy.CLASS, void.class, LevelProxy.CLASS, BlockStateProxy.CLASS, BlockPosProxy.CLASS, clazz$Entity, VersionHelper.isOrAbove1_21_5() ? double.class : float.class)
    );

    public static final Method method$Block$updateEntityMovementAfterFallOn = requireNonNull(
            ReflectionUtils.getDeclaredMethod(BlockProxy.CLASS, void.class, BlockGetterProxy.CLASS, clazz$Entity)
    );

    public static final Method method$BlockStateBase$isBlock = requireNonNull(
            ReflectionUtils.getDeclaredMethod(BlockBehaviourProxy.BlockStateBaseProxy.CLASS, boolean.class, new String[]{"is", "a"}, BlockProxy.CLASS)
    );

    public static final Method method$BlockBehaviour$onProjectileHit = requireNonNull(
            ReflectionUtils.getDeclaredMethod(BlockBehaviourProxy.CLASS, void.class, new String[]{"onProjectileHit", "a"}, LevelProxy.CLASS, BlockStateProxy.CLASS, BlockHitResultProxy.CLASS, ProjectileProxy.CLASS)
    );

    public static final Method method$Block$setPlacedBy = requireNonNull(
            ReflectionUtils.getMethod(BlockProxy.CLASS, void.class, LevelProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, LivingEntityProxy.CLASS, ItemStackProxy.CLASS)
    );
}
