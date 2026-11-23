package net.momirealms.craftengine.bukkit.plugin.injector;

import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.core.TypedInstanceProxy;
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
import net.momirealms.craftengine.proxy.minecraft.world.level.biome.BiomeProxy;
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
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import static net.momirealms.sparrow.reflection.method.matcher.MethodMatcher.takeArguments;

final class BlockReflections {
    static final Method GET_SHAPE = method(BlockBehaviourProxy.CLASS, "getShape", VoxelShapeProxy.CLASS,
            takeArguments(BlockStateProxy.CLASS, BlockGetterProxy.CLASS, BlockPosProxy.CLASS, CollisionContextProxy.CLASS));
    static final Method GET_COLLISION_SHAPE = method(BlockBehaviourProxy.CLASS, "getCollisionShape", VoxelShapeProxy.CLASS,
            takeArguments(BlockStateProxy.CLASS, BlockGetterProxy.CLASS, BlockPosProxy.CLASS, CollisionContextProxy.CLASS));
    static final Method GET_BLOCK_SUPPORT_SHAPE = method(BlockBehaviourProxy.CLASS, "getBlockSupportShape", VoxelShapeProxy.CLASS,
            takeArguments(BlockStateProxy.CLASS, BlockGetterProxy.CLASS, BlockPosProxy.CLASS));
    static final Method IS_PATHFINDABLE = method(BlockBehaviourProxy.CLASS, "isPathfindable", boolean.class,
            VersionHelper.isOrAbove1_20_5
                    ? takeArguments(BlockStateProxy.CLASS, PathComputationTypeProxy.CLASS)
                    : takeArguments(BlockStateProxy.CLASS, BlockGetterProxy.CLASS, BlockPosProxy.CLASS, PathComputationTypeProxy.CLASS));
    static final Method MIRROR = method(BlockBehaviourProxy.CLASS, "mirror", BlockStateProxy.CLASS,
            takeArguments(BlockStateProxy.CLASS, MirrorProxy.CLASS));
    static final Method ROTATE = method(BlockBehaviourProxy.CLASS, "rotate", BlockStateProxy.CLASS,
            takeArguments(BlockStateProxy.CLASS, RotationProxy.CLASS));
    static final Method CAN_SURVIVE = method(BlockBehaviourProxy.CLASS, "canSurvive", boolean.class,
            takeArguments(BlockStateProxy.CLASS, LevelReaderProxy.CLASS, BlockPosProxy.CLASS));
    static final Method UPDATE_SHAPE = method(BlockBehaviourProxy.CLASS, "updateShape", BlockStateProxy.CLASS,
            VersionHelper.isOrAbove1_21_2
                    ? takeArguments(BlockStateProxy.CLASS, LevelReaderProxy.CLASS, ScheduledTickAccessProxy.CLASS, BlockPosProxy.CLASS,
                            DirectionProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, RandomSourceProxy.CLASS)
                    : takeArguments(BlockStateProxy.CLASS, DirectionProxy.CLASS, BlockStateProxy.CLASS,
                            LevelAccessorProxy.CLASS, BlockPosProxy.CLASS, BlockPosProxy.CLASS));
    static final Method NEIGHBOR_CHANGED = method(BlockBehaviourProxy.CLASS, "neighborChanged", void.class,
            takeArguments(BlockStateProxy.CLASS, LevelProxy.CLASS, BlockPosProxy.CLASS, BlockProxy.CLASS,
                    VersionHelper.isOrAbove1_21_2 ? OrientationProxy.CLASS : BlockPosProxy.CLASS, boolean.class));

    static final Method IS_SIGNAL_SOURCE = method(BlockBehaviourProxy.CLASS, "isSignalSource", boolean.class,
            takeArguments(BlockStateProxy.CLASS));
    static final Method GET_SIGNAL = method(BlockBehaviourProxy.CLASS, "getSignal", int.class,
            takeArguments(BlockStateProxy.CLASS, BlockGetterProxy.CLASS, BlockPosProxy.CLASS, DirectionProxy.CLASS));
    static final Method GET_DIRECT_SIGNAL = method(BlockBehaviourProxy.CLASS, "getDirectSignal", int.class,
            takeArguments(BlockStateProxy.CLASS, BlockGetterProxy.CLASS, BlockPosProxy.CLASS, DirectionProxy.CLASS));
    static final Method HAS_ANALOG_OUTPUT_SIGNAL = method(BlockBehaviourProxy.CLASS, "hasAnalogOutputSignal", boolean.class,
            takeArguments(BlockStateProxy.CLASS));
    static final Method GET_ANALOG_OUTPUT_SIGNAL = method(BlockBehaviourProxy.CLASS, "getAnalogOutputSignal", int.class,
            VersionHelper.isOrAbove1_21_9
                    ? takeArguments(BlockStateProxy.CLASS, LevelProxy.CLASS, BlockPosProxy.CLASS, DirectionProxy.CLASS)
                    : takeArguments(BlockStateProxy.CLASS, LevelProxy.CLASS, BlockPosProxy.CLASS));

    static final Method TICK = method(BlockBehaviourProxy.CLASS, "tick", void.class,
            takeArguments(BlockStateProxy.CLASS, ServerLevelProxy.CLASS, BlockPosProxy.CLASS, RandomSourceProxy.CLASS));
    static final Method RANDOM_TICK = method(BlockBehaviourProxy.CLASS, "randomTick", void.class,
            takeArguments(BlockStateProxy.CLASS, ServerLevelProxy.CLASS, BlockPosProxy.CLASS, RandomSourceProxy.CLASS));
    static final Method ON_PLACE = method(BlockBehaviourProxy.CLASS, "onPlace", void.class,
            takeArguments(BlockStateProxy.CLASS, LevelProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, boolean.class));
    static final Method AFFECT_NEIGHBORS_AFTER_REMOVAL = lookup(BlockBehaviourProxy.CLASS, void.class,
            VersionHelper.isOrAbove1_21_5
                    ? takeArguments(BlockStateProxy.CLASS, ServerLevelProxy.CLASS, BlockPosProxy.CLASS, boolean.class)
                    : takeArguments(BlockStateProxy.CLASS, LevelProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, boolean.class),
            true, "affectNeighborsAfterRemoval", "onRemove");
    static final Method ATTACK = method(BlockBehaviourProxy.CLASS, "attack", void.class,
            takeArguments(BlockStateProxy.CLASS, LevelProxy.CLASS, BlockPosProxy.CLASS, PlayerProxy.CLASS));
    static final Method TRIGGER_EVENT = method(BlockBehaviourProxy.CLASS, "triggerEvent", boolean.class,
            takeArguments(BlockStateProxy.CLASS, LevelProxy.CLASS, BlockPosProxy.CLASS, int.class, int.class));
    static final Method ENTITY_INSIDE = method(BlockBehaviourProxy.CLASS, "entityInside", void.class,
            entityInsideArguments());
    static final Method ON_PROJECTILE_HIT = method(BlockBehaviourProxy.CLASS, "onProjectileHit", void.class,
            takeArguments(LevelProxy.CLASS, BlockStateProxy.CLASS, BlockHitResultProxy.CLASS, ProjectileProxy.CLASS));
    static final Method SPAWN_AFTER_BREAK = method(BlockBehaviourProxy.CLASS, "spawnAfterBreak", void.class,
            takeArguments(BlockStateProxy.CLASS, ServerLevelProxy.CLASS, BlockPosProxy.CLASS, ItemStackProxy.CLASS, boolean.class));
    @Nullable
    static final Method ON_EXPLOSION_HIT = lookup(BlockBehaviourProxy.CLASS, void.class,
            takeArguments(BlockStateProxy.CLASS, VersionHelper.isOrAbove1_21_2 ? ServerLevelProxy.CLASS : LevelProxy.CLASS,
                    BlockPosProxy.CLASS, ExplosionProxy.CLASS, BiConsumer.class),
            VersionHelper.isOrAbove1_21, "onExplosionHit");

    static final Method SET_PLACED_BY = method(BlockProxy.CLASS, "setPlacedBy", void.class,
            takeArguments(LevelProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, LivingEntityProxy.CLASS, ItemStackProxy.CLASS));
    static final Method PLAYER_WILL_DESTROY = method(BlockProxy.CLASS, "playerWillDestroy",
            VersionHelper.isOrAbove1_20_3 ? BlockStateProxy.CLASS : void.class,
            takeArguments(LevelProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, PlayerProxy.CLASS));
    static final Method STEP_ON = method(BlockProxy.CLASS, "stepOn", void.class,
            takeArguments(LevelProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, EntityProxy.CLASS));
    static final Method FALL_ON = method(BlockProxy.CLASS, "fallOn", void.class,
            takeArguments(LevelProxy.CLASS, BlockStateProxy.CLASS, BlockPosProxy.CLASS, EntityProxy.CLASS,
                    VersionHelper.isOrAbove1_21_5 ? double.class : float.class));
    @Nullable
    static final Method UPDATE_ENTITY_MOVEMENT_AFTER_FALL_ON = lookup(BlockProxy.CLASS, void.class,
            takeArguments(BlockGetterProxy.CLASS, EntityProxy.CLASS),
            !VersionHelper.isOrAbove26_2, "updateEntityMovementAfterFallOn", "updateEntityAfterFallOn");
    static final Method HANDLE_PRECIPITATION = method(BlockProxy.CLASS, "handlePrecipitation", void.class,
            takeArguments(BlockStateProxy.CLASS, LevelProxy.CLASS, BlockPosProxy.CLASS, BiomeProxy.PrecipitationProxy.CLASS));

    static final Method ON_LAND = method(FallableProxy.CLASS, "onLand", void.class,
            takeArguments(LevelProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, BlockStateProxy.CLASS, FallingBlockEntityProxy.CLASS));
    static final Method ON_BROKEN_AFTER_FALL = method(FallableProxy.CLASS, "onBrokenAfterFall", void.class,
            takeArguments(LevelProxy.CLASS, BlockPosProxy.CLASS, FallingBlockEntityProxy.CLASS));

    static final Method IS_VALID_BONEMEAL_TARGET = method(BonemealableBlockProxy.CLASS, "isValidBonemealTarget", boolean.class,
            bonemealTargetArguments());
    static final Method IS_BONEMEAL_SUCCESS = method(BonemealableBlockProxy.CLASS, "isBonemealSuccess", boolean.class,
            VersionHelper.isOrAbove26_3
                    ? takeArguments(LevelProxy.CLASS, RandomSourceProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, BonemealSourceProxy.CLASS)
                    : takeArguments(LevelProxy.CLASS, RandomSourceProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS));
    static final Method PERFORM_BONEMEAL = method(BonemealableBlockProxy.CLASS, "performBonemeal", void.class,
            VersionHelper.isOrAbove26_3
                    ? takeArguments(ServerLevelProxy.CLASS, RandomSourceProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, BonemealSourceProxy.CLASS)
                    : takeArguments(ServerLevelProxy.CLASS, RandomSourceProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS));

    static final Method CAN_PLACE_LIQUID = method(SimpleWaterloggedBlockProxy.CLASS, "canPlaceLiquid", boolean.class,
            canPlaceLiquidArguments());
    static final Method PLACE_LIQUID = method(SimpleWaterloggedBlockProxy.CLASS, "placeLiquid", boolean.class,
            takeArguments(LevelAccessorProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, FluidStateProxy.CLASS));
    static final Method PICKUP_BLOCK = method(SimpleWaterloggedBlockProxy.CLASS, "pickupBlock", ItemStackProxy.CLASS,
            pickupBlockArguments());
    static final Method GET_PICKUP_SOUND = method(SimpleWaterloggedBlockProxy.CLASS, "getPickupSound", Optional.class,
            MethodMatcher.any());

    static final Method GET_CONTAINER = method(WorldlyContainerHolderProxy.CLASS, "getContainer", WorldlyContainerProxy.CLASS,
            takeArguments(BlockStateProxy.CLASS, LevelAccessorProxy.CLASS, BlockPosProxy.CLASS));

    static final Method HAS_PROPERTY = method(StateHolderProxy.CLASS, "hasProperty", boolean.class,
            takeArguments(PropertyProxy.CLASS));
    static final Method GET_VALUE = method(StateHolderProxy.CLASS, "getValue", Comparable.class,
            takeArguments(PropertyProxy.CLASS));
    static final Method SET_VALUE = method(StateHolderProxy.CLASS, "setValue", Object.class,
            takeArguments(PropertyProxy.CLASS, Comparable.class));
    static final Method GET_DROPS = method(BlockBehaviourProxy.BlockStateBaseProxy.CLASS, "getDrops", List.class,
            takeArguments(LootParamsProxy.BuilderProxy.CLASS));
    static final Method IS_BLOCK = VersionHelper.isOrAbove26_1
            ? method(TypedInstanceProxy.CLASS, "is", boolean.class, takeArguments(Object.class))
            : method(BlockBehaviourProxy.BlockStateBaseProxy.CLASS, "is", boolean.class, takeArguments(BlockProxy.CLASS));

    private BlockReflections() {
    }

    private static MethodMatcher bonemealTargetArguments() {
        if (VersionHelper.isOrAbove26_3) {
            return takeArguments(LevelReaderProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, BonemealSourceProxy.CLASS);
        }
        if (VersionHelper.isOrAbove1_20_2) {
            return takeArguments(LevelReaderProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS);
        }
        return takeArguments(LevelReaderProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, boolean.class);
    }

    private static MethodMatcher entityInsideArguments() {
        if (VersionHelper.isOrAbove1_21_10) {
            return takeArguments(BlockStateProxy.CLASS, LevelProxy.CLASS, BlockPosProxy.CLASS, EntityProxy.CLASS,
                    InsideBlockEffectApplierProxy.CLASS, boolean.class);
        }
        if (VersionHelper.isOrAbove1_21_5) {
            return takeArguments(BlockStateProxy.CLASS, LevelProxy.CLASS, BlockPosProxy.CLASS, EntityProxy.CLASS,
                    InsideBlockEffectApplierProxy.CLASS);
        }
        return takeArguments(BlockStateProxy.CLASS, LevelProxy.CLASS, BlockPosProxy.CLASS, EntityProxy.CLASS);
    }

    private static MethodMatcher canPlaceLiquidArguments() {
        if (VersionHelper.isOrAbove1_20_2) {
            Class<?> actor = VersionHelper.isOrAbove1_21_5 ? LivingEntityProxy.CLASS : PlayerProxy.CLASS;
            return takeArguments(actor, BlockGetterProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, FluidProxy.CLASS);
        }
        return takeArguments(BlockGetterProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS, FluidProxy.CLASS);
    }

    private static MethodMatcher pickupBlockArguments() {
        if (VersionHelper.isOrAbove1_20_2) {
            Class<?> actor = VersionHelper.isOrAbove1_21_5 ? LivingEntityProxy.CLASS : PlayerProxy.CLASS;
            return takeArguments(actor, LevelAccessorProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS);
        }
        return takeArguments(LevelAccessorProxy.CLASS, BlockPosProxy.CLASS, BlockStateProxy.CLASS);
    }

    private static Method method(Class<?> owner, String name, Class<?> returnType, MethodMatcher parameters) {
        return lookup(owner, returnType, parameters, true, name);
    }

    private static Method lookup(Class<?> owner, Class<?> returnType, MethodMatcher parameters, boolean required, String... names) {
        Method method = SparrowClass.of(owner).getDeclaredMethod(MethodMatcher.returnType(returnType)
                .and(MethodMatcher.named(names)).and(parameters));
        if (method == null && required) {
            throw new IllegalStateException("Missing method " + owner.getName() + "." + String.join(" / ", names) + " returning " + returnType.getTypeName());
        }
        return method;
    }
}
