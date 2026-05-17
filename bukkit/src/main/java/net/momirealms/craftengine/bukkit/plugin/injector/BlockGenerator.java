package net.momirealms.craftengine.bukkit.plugin.injector;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatchers;
import net.momirealms.craftengine.bukkit.block.BukkitBlockShape;
import net.momirealms.craftengine.bukkit.block.behavior.EmptyBlockBehavior;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.NoteBlockChainUpdateUtils;
import net.momirealms.craftengine.core.block.BlockShape;
import net.momirealms.craftengine.core.block.DelegatingBlock;
import net.momirealms.craftengine.core.block.behavior.*;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ObjectHolder;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.RegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.ResourceKeyProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerChunkCacheProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.WorldlyContainerHolderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.*;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.StateDefinitionProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.constructor.SConstructor1;
import net.momirealms.sparrow.reflection.constructor.matcher.ConstructorMatcher;
import net.momirealms.sparrow.reflection.field.SBooleanField;
import net.momirealms.sparrow.reflection.field.SField;
import net.momirealms.sparrow.reflection.field.matcher.FieldMatcher;

import java.util.Optional;
import java.util.concurrent.Callable;

public final class BlockGenerator {
    private static final BukkitBlockShape STONE_SHAPE =
            new BukkitBlockShape(BlocksProxy.STONE$defaultState, BlocksProxy.STONE$defaultState);
    private static SConstructor1 constructor$CraftEngineBlock;
    private static SField field$CraftEngineBlock$behavior;
    private static SField field$CraftEngineBlock$shape;
    private static SBooleanField field$CraftEngineBlock$isNoteBlock;
    private static SBooleanField field$CraftEngineBlock$isTripwire;

    public static void init() {
        ByteBuddy byteBuddy = new ByteBuddy(ClassFileVersion.JAVA_V17);
        // CraftEngine Blocks
        String packageWithName = BlockGenerator.class.getName();
        String generatedClassName = packageWithName.substring(0, packageWithName.lastIndexOf('.')) + ".CraftEngineBlock";
        DynamicType.Builder<?> builder = byteBuddy
                .subclass(BlockProxy.CLASS, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                .name(generatedClassName)
                .defineField("behaviorHolder", ObjectHolder.class, Visibility.PUBLIC)
                .defineField("shapeHolder", ObjectHolder.class, Visibility.PUBLIC)
                .defineField("isClientSideNoteBlock", boolean.class, Visibility.PUBLIC)
                .defineField("isClientSideTripwire", boolean.class, Visibility.PUBLIC)
                // should always implement this interface
                .implement(DelegatingBlock.class)
                .implement(FallableProxy.CLASS)
                .implement(BonemealableBlockProxy.CLASS)
                .implement(SimpleWaterloggedBlockProxy.CLASS)
                .implement(WorldlyContainerHolderProxy.CLASS)
                // internal interfaces
                .method(ElementMatchers.named("behaviorDelegate"))
                .intercept(FieldAccessor.ofField("behaviorHolder"))
                .method(ElementMatchers.named("shapeDelegate"))
                .intercept(FieldAccessor.ofField("shapeHolder"))
                .method(ElementMatchers.named("isNoteBlock"))
                .intercept(FieldAccessor.ofField("isClientSideNoteBlock"))
                .method(ElementMatchers.named("isTripwire"))
                .intercept(FieldAccessor.ofField("isClientSideTripwire"))
                // getShape
                .method(ElementMatchers.is(BlockReflections.method$BlockBehaviour$getShape))
                .intercept(MethodDelegation.to(GetShapeInterceptor.INSTANCE))
                // getCollisionShape
                .method(ElementMatchers.is(BlockReflections.method$BlockBehaviour$getCollisionShape))
                .intercept(MethodDelegation.to(GetCollisionShapeInterceptor.INSTANCE))
                // getSupportShape
                .method(ElementMatchers.is(BlockReflections.method$BlockBehaviour$getBlockSupportShape))
                .intercept(MethodDelegation.to(GetSupportShapeInterceptor.INSTANCE))
                // isPathFindable
                .method(ElementMatchers.is(BlockReflections.method$BlockBehaviour$isPathfindable))
                .intercept(MethodDelegation.to(IsPathFindableInterceptor.INSTANCE))
                // mirror
                .method(ElementMatchers.is(BlockReflections.method$BlockBehaviour$mirror))
                .intercept(MethodDelegation.to(MirrorInterceptor.INSTANCE))
                // rotate
                .method(ElementMatchers.is(BlockReflections.method$BlockBehaviour$rotate))
                .intercept(MethodDelegation.to(RotateInterceptor.INSTANCE))
                // hasAnalogOutputSignal
                .method(ElementMatchers.is(BlockReflections.method$BlockBehaviour$hasAnalogOutputSignal))
                .intercept(MethodDelegation.to(HasAnalogOutputSignalInterceptor.INSTANCE))
                // getAnalogOutputSignal
                .method(ElementMatchers.is(BlockReflections.method$BlockBehaviour$getAnalogOutputSignal))
                .intercept(MethodDelegation.to(GetAnalogOutputSignalInterceptor.INSTANCE))
                // tick
                .method(ElementMatchers.is(BlockReflections.method$BlockBehaviour$tick))
                .intercept(MethodDelegation.to(TickInterceptor.INSTANCE))
                // isValidBoneMealTarget
                .method(ElementMatchers.is(BlockReflections.method$BonemealableBlock$isValidBonemealTarget))
                .intercept(MethodDelegation.to(IsValidBoneMealTargetInterceptor.INSTANCE))
                // getContainer
                .method(ElementMatchers.is(BlockReflections.method$WorldlyContainerHolder$getContainer))
                .intercept(MethodDelegation.to(GetContainerInterceptor.INSTANCE))
                // isBoneMealSuccess
                .method(ElementMatchers.is(BlockReflections.method$BonemealableBlock$isBonemealSuccess))
                .intercept(MethodDelegation.to(IsBoneMealSuccessInterceptor.INSTANCE))
                // performBoneMeal
                .method(ElementMatchers.is(BlockReflections.method$BonemealableBlock$performBonemeal))
                .intercept(MethodDelegation.to(PerformBoneMealInterceptor.INSTANCE))
                // random tick
                .method(ElementMatchers.is(BlockReflections.method$BlockBehaviour$randomTick))
                .intercept(MethodDelegation.to(RandomTickInterceptor.INSTANCE))
                // onPlace
                .method(ElementMatchers.is(BlockReflections.method$BlockBehaviour$onPlace))
                .intercept(MethodDelegation.to(OnPlaceInterceptor.INSTANCE))
                // onBrokenAfterFall
                .method(ElementMatchers.is(BlockReflections.method$Fallable$onBrokenAfterFall))
                .intercept(MethodDelegation.to(OnBrokenAfterFallInterceptor.INSTANCE))
                // onLand
                .method(ElementMatchers.is(BlockReflections.method$Fallable$onLand))
                .intercept(MethodDelegation.to(OnLandInterceptor.INSTANCE))
                // canSurvive
                .method(ElementMatchers.is(BlockReflections.method$BlockBehaviour$canSurvive))
                .intercept(MethodDelegation.to(CanSurviveInterceptor.INSTANCE))
                // updateShape
                .method(ElementMatchers.is(BlockReflections.method$BlockBehaviour$updateShape))
                .intercept(MethodDelegation.to(UpdateShapeInterceptor.INSTANCE))
                // neighborChanged
                .method(ElementMatchers.is(BlockReflections.method$BlockBehaviour$neighborChanged))
                .intercept(MethodDelegation.to(NeighborChangedInterceptor.INSTANCE))
                // pickupBlock
                .method(ElementMatchers.is(BlockReflections.method$SimpleWaterloggedBlock$pickupBlock))
                .intercept(MethodDelegation.to(PickUpBlockInterceptor.INSTANCE))
                // getPickupSound
                .method(ElementMatchers.is(BlockReflections.method$SimpleWaterloggedBlock$getPickupSound))
                .intercept(MethodDelegation.to(GetPickUpSoundInterceptor.INSTANCE))
                // placeLiquid
                .method(ElementMatchers.is(BlockReflections.method$SimpleWaterloggedBlock$placeLiquid))
                .intercept(MethodDelegation.to(PlaceLiquidInterceptor.INSTANCE))
                // canPlaceLiquid
                .method(ElementMatchers.is(BlockReflections.method$SimpleWaterloggedBlock$canPlaceLiquid))
                .intercept(MethodDelegation.to(CanPlaceLiquidInterceptor.INSTANCE))
                // entityInside
                .method(ElementMatchers.is(BlockReflections.method$BlockBehaviour$entityInside))
                .intercept(MethodDelegation.to(EntityInsideInterceptor.INSTANCE))
                // getSignal
                .method(ElementMatchers.is(BlockReflections.method$BlockBehaviour$getSignal))
                .intercept(MethodDelegation.to(GetSignalInterceptor.INSTANCE))
                // getDirectSignal
                .method(ElementMatchers.is(BlockReflections.method$BlockBehaviour$getDirectSignal))
                .intercept(MethodDelegation.to(GetDirectSignalInterceptor.INSTANCE))
                // isSignalSource
                .method(ElementMatchers.is(BlockReflections.method$BlockBehaviour$isSignalSource))
                .intercept(MethodDelegation.to(IsSignalSourceInterceptor.INSTANCE))
                // playerWillDestroy
                .method(ElementMatchers.is(BlockReflections.method$Block$playerWillDestroy))
                .intercept(MethodDelegation.to(PlayerWillDestroyInterceptor.INSTANCE))
                // spawnAfterBreak
                .method(ElementMatchers.is(BlockReflections.method$BlockBehaviour$spawnAfterBreak))
                .intercept(MethodDelegation.to(SpawnAfterBreakInterceptor.INSTANCE))
                // fallOn
                .method(ElementMatchers.is(BlockReflections.method$Block$fallOn))
                .intercept(MethodDelegation.to(FallOnInterceptor.INSTANCE))
                // updateEntityMovementAfterFallOn
                .method(ElementMatchers.is(BlockReflections.method$Block$updateEntityMovementAfterFallOn))
                .intercept(MethodDelegation.to(UpdateEntityMovementAfterFallOnInterceptor.INSTANCE))
                // stepOn
                .method(ElementMatchers.is(BlockReflections.method$Block$stepOn))
                .intercept(MethodDelegation.to(StepOnInterceptor.INSTANCE))
                // onProjectileHit
                .method(ElementMatchers.is(BlockReflections.method$BlockBehaviour$onProjectileHit))
                .intercept(MethodDelegation.to(OnProjectileHitInterceptor.INSTANCE))
                // setPlaceBy
                .method(ElementMatchers.is(BlockReflections.method$Block$setPlacedBy))
                .intercept(MethodDelegation.to(SetPlaceByInterceptor.INSTANCE))
                // affectNeighborsAfterRemoval
                .method(ElementMatchers.is(BlockReflections.method$BlockBehaviour$affectNeighborsAfterRemoval))
                .intercept(MethodDelegation.to(AffectNeighborsAfterRemovalInterceptor.INSTANCE))
                // attack
                .method(ElementMatchers.is(BlockReflections.method$BlockBehaviour$attack))
                .intercept(MethodDelegation.to(AttackInterceptor.INSTANCE))
                // triggerEvent
                .method(ElementMatchers.is(BlockReflections.method$BlockBehaviour$triggerEvent))
                .intercept(MethodDelegation.to(TriggerEventInterceptor.INSTANCE))
                // handlePrecipitation
                .method(ElementMatchers.is(BlockReflections.method$Block$handlePrecipitation))
                .intercept(MethodDelegation.to(HandlePrecipitationInterceptor.INSTANCE))
                ;
        // 1.21+
        if (BlockReflections.method$BlockBehaviour$onExplosionHit != null) {
            builder = builder.method(ElementMatchers.is(BlockReflections.method$BlockBehaviour$onExplosionHit))
                    .intercept(MethodDelegation.to(OnExplosionHitInterceptor.INSTANCE));
        }
        SparrowClass<?> clazz$CraftEngineBlock = SparrowClass.of(builder.make().load(BlockGenerator.class.getClassLoader()).getLoaded());
        constructor$CraftEngineBlock = clazz$CraftEngineBlock.getSparrowConstructor(ConstructorMatcher.takeArguments(BlockBehaviourProxy.PropertiesProxy.CLASS)).asm$1();
        field$CraftEngineBlock$behavior = clazz$CraftEngineBlock.getSparrowField(FieldMatcher.named("behaviorHolder")).asm();
        field$CraftEngineBlock$shape = clazz$CraftEngineBlock.getSparrowField(FieldMatcher.named("shapeHolder")).asm();
        field$CraftEngineBlock$isNoteBlock = clazz$CraftEngineBlock.getSparrowField(FieldMatcher.named("isClientSideNoteBlock")).asm$boolean();
        field$CraftEngineBlock$isTripwire = clazz$CraftEngineBlock.getSparrowField(FieldMatcher.named("isClientSideTripwire")).asm$boolean();
    }

    public static SBooleanField field$CraftEngineBlock$isNoteBlock() {
        return field$CraftEngineBlock$isNoteBlock;
    }

    public static SBooleanField field$CraftEngineBlock$isTripwire() {
        return field$CraftEngineBlock$isTripwire;
    }

    public static DelegatingBlock generateBlock(Key blockId) {
        ObjectHolder<BlockBehavior> behaviorHolder = new ObjectHolder<>(EmptyBlockBehavior.INSTANCE);
        ObjectHolder<BlockShape> shapeHolder = new ObjectHolder<>(STONE_SHAPE);
        Object newBlockInstance = constructor$CraftEngineBlock.newInstance(createEmptyBlockProperties(blockId));
        field$CraftEngineBlock$behavior.set(newBlockInstance, behaviorHolder);
        field$CraftEngineBlock$shape.set(newBlockInstance, shapeHolder);
        Object stateDefinitionBuilder = StateDefinitionProxy.BuilderProxy.INSTANCE.newInstance(newBlockInstance);
        Object stateDefinition = StateDefinitionProxy.BuilderProxy.INSTANCE.create(stateDefinitionBuilder,
                BlockProxy.INSTANCE::getDefaultBlockState, BlockStateGenerator.instance$StateDefinition$Factory);
        BlockProxy.INSTANCE.setStateDefinition(newBlockInstance, stateDefinition);
        BlockProxy.INSTANCE.setDefaultBlockState(newBlockInstance, StateDefinitionProxy.INSTANCE.getStates(stateDefinition).getFirst());
        return (DelegatingBlock) newBlockInstance;
    }

    private static Object createEmptyBlockProperties(Key id) {
        Object blockProperties = BlockBehaviourProxy.PropertiesProxy.INSTANCE.of();
        Object identifier = KeyUtils.toIdentifier(id);
        Object resourceKey = ResourceKeyProxy.INSTANCE.create(RegistriesProxy.BLOCK, identifier);
        if (VersionHelper.isOrAbove1_21_2) {
            BlockBehaviourProxy.PropertiesProxy.INSTANCE.setId(blockProperties, resourceKey);
        }
        return blockProperties;
    }

    public static class UpdateShapeInterceptor {
        public static final UpdateShapeInterceptor INSTANCE = new UpdateShapeInterceptor();
        public static final int levelIndex = VersionHelper.isOrAbove1_21_2 ? 1 : 3;
        public static final int directionIndex = VersionHelper.isOrAbove1_21_2 ? 4 : 1;
        public static final int posIndex = VersionHelper.isOrAbove1_21_2 ? 3 : 4;

        @SuppressWarnings("deprecation")
        @RuntimeType
        public Object intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            DelegatingBlock indicator = (DelegatingBlock) thisBlock;
            // todo better chain updater
            if (indicator.isNoteBlock() && ServerLevelProxy.CLASS.isInstance(args[levelIndex])) {
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
            Object direction = args[directionIndex];
            Object serverLevel = args[levelIndex];
            Object blockPos = args[posIndex];
            // Y axis
            if (direction == DirectionProxy.DOWN) {
                Object chunkSource = ServerLevelProxy.INSTANCE.getChunkSource(serverLevel);
                ServerChunkCacheProxy.INSTANCE.blockChanged(chunkSource, blockPos);
                NoteBlockChainUpdateUtils.noteBlockChainUpdate(serverLevel, chunkSource, DirectionProxy.UP, blockPos, Config.maxNoteBlockChainUpdate());
            } else if (direction == DirectionProxy.UP) {
                Object chunkSource = ServerLevelProxy.INSTANCE.getChunkSource(serverLevel);
                ServerChunkCacheProxy.INSTANCE.blockChanged(chunkSource, blockPos);
                NoteBlockChainUpdateUtils.noteBlockChainUpdate(serverLevel, chunkSource, DirectionProxy.DOWN, blockPos, Config.maxNoteBlockChainUpdate());
            }
        }
    }

    public static class GetShapeInterceptor {
        public static final GetShapeInterceptor INSTANCE = new GetShapeInterceptor();

        @RuntimeType
        public Object intercept(@This Object thisBlock, @AllArguments Object[] args, @SuperCall Callable<Object> superMethod) throws Exception {
            ObjectHolder<BlockShape> holder = ((DelegatingBlock) thisBlock).shapeDelegate();
            try {
                return holder.value().getShape(thisBlock, args);
            } catch (Exception e) {
                CraftEngine.instance().logger().error("Failed to run getShape", e);
                return superMethod.call();
            }
        }
    }

    public static class GetCollisionShapeInterceptor {
        public static final GetCollisionShapeInterceptor INSTANCE = new GetCollisionShapeInterceptor();

        @RuntimeType
        public Object intercept(@This Object thisBlock, @AllArguments Object[] args, @SuperCall Callable<Object> superMethod) throws Exception {
            ObjectHolder<BlockShape> holder = ((DelegatingBlock) thisBlock).shapeDelegate();
            try {
                return holder.value().getCollisionShape(thisBlock, args);
            } catch (Throwable t) {
                CraftEngine.instance().logger().error("Failed to run getCollisionShape", t);
                return superMethod.call();
            }
        }
    }

    public static class GetSupportShapeInterceptor {
        public static final GetSupportShapeInterceptor INSTANCE = new GetSupportShapeInterceptor();

        @RuntimeType
        public Object intercept(@This Object thisBlock, @AllArguments Object[] args, @SuperCall Callable<Object> superMethod) throws Exception {
            ObjectHolder<BlockShape> holder = ((DelegatingBlock) thisBlock).shapeDelegate();
            try {
                return holder.value().getSupportShape(thisBlock, args);
            } catch (Throwable t) {
                CraftEngine.instance().logger().error("Failed to run getSupportShape", t);
                return superMethod.call();
            }
        }
    }

    public static class IsPathFindableInterceptor {
        public static final IsPathFindableInterceptor INSTANCE = new IsPathFindableInterceptor();

        @RuntimeType
        public Object intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            try {
                return holder.value().isPathFindable(thisBlock, args);
            } catch (Throwable t) {
                CraftEngine.instance().logger().error("Failed to run isPathFindable", t);
                return false;
            }
        }
    }

    public static class MirrorInterceptor {
        public static final MirrorInterceptor INSTANCE = new MirrorInterceptor();

        @RuntimeType
        public Object intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            try {
                return holder.value().mirror(thisBlock, args);
            } catch (Throwable t) {
                CraftEngine.instance().logger().error("Failed to run mirror", t);
                return args[0];
            }
        }
    }

    public static class RotateInterceptor {
        public static final RotateInterceptor INSTANCE = new RotateInterceptor();

        @RuntimeType
        public Object intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            try {
                return holder.value().rotate(thisBlock, args);
            } catch (Throwable t) {
                CraftEngine.instance().logger().error("Failed to run rotate", t);
                return args[0];
            }
        }
    }

    public static class RandomTickInterceptor {
        public static final RandomTickInterceptor INSTANCE = new RandomTickInterceptor();

        @RuntimeType
        public void intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            try {
                holder.value().randomTick(thisBlock, args);
            } catch (Throwable t) {
                CraftEngine.instance().logger().error("Failed to run randomTick", t);
            }
        }
    }

    public static class TickInterceptor {
        public static final TickInterceptor INSTANCE = new TickInterceptor();

        @RuntimeType
        public void intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            try {
                holder.value().tick(thisBlock, args);
            } catch (Throwable t) {
                CraftEngine.instance().logger().error("Failed to run tick", t);
            }
        }
    }

    public static class OnPlaceInterceptor {
        public static final OnPlaceInterceptor INSTANCE = new OnPlaceInterceptor();

        @RuntimeType
        public void intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            try {
                holder.value().onPlace(thisBlock, args);
            } catch (Throwable t) {
                CraftEngine.instance().logger().error("Failed to run onPlace", t);
            }
        }
    }

    public static class OnLandInterceptor {
        public static final OnLandInterceptor INSTANCE = new OnLandInterceptor();

        @RuntimeType
        public void intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            if (holder.value() instanceof FallableBlock fallable) {
                try {
                    fallable.onLand(thisBlock, args);
                } catch (Throwable t) {
                    CraftEngine.instance().logger().error("Failed to run onLand", t);
                }
            }
        }
    }

    public static class OnBrokenAfterFallInterceptor {
        public static final OnBrokenAfterFallInterceptor INSTANCE = new OnBrokenAfterFallInterceptor();

        @RuntimeType
        public void intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            if (holder.value() instanceof FallableBlock fallable) {
                try {
                    fallable.onBrokenAfterFall(thisBlock, args);
                } catch (Throwable t) {
                    CraftEngine.instance().logger().error("Failed to run onBrokenAfterFall", t);
                }
            }
        }
    }

    public static class CanSurviveInterceptor {
        public static final CanSurviveInterceptor INSTANCE = new CanSurviveInterceptor();

        @RuntimeType
        public boolean intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            try {
                return holder.value().canSurvive(thisBlock, args);
            } catch (Throwable t) {
                CraftEngine.instance().logger().error("Failed to run canSurvive", t);
            }
            return true;
        }
    }

    public static class IsBoneMealSuccessInterceptor {
        public static final IsBoneMealSuccessInterceptor INSTANCE = new IsBoneMealSuccessInterceptor();

        @RuntimeType
        public boolean intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            if (holder.value() instanceof BonemealableBlock bonemealableBlock) {
                try {
                    return bonemealableBlock.isBonemealSuccess(thisBlock, args);
                } catch (Throwable t) {
                    CraftEngine.instance().logger().error("Failed to run isBoneMealSuccess", t);
                }
            }
            return false;
        }
    }

    public static class IsValidBoneMealTargetInterceptor {
        public static final IsValidBoneMealTargetInterceptor INSTANCE = new IsValidBoneMealTargetInterceptor();

        @RuntimeType
        public boolean intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            if (holder.value() instanceof BonemealableBlock bonemealableBlock) {
                try {
                    return bonemealableBlock.isValidBonemealTarget(thisBlock, args);
                } catch (Throwable t) {
                    CraftEngine.instance().logger().error("Failed to run isValidBoneMealTarget", t);
                }
            }
            return false;
        }
    }

    public static class GetContainerInterceptor {
        public static final GetContainerInterceptor INSTANCE = new GetContainerInterceptor();

        @RuntimeType
        public Object intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            if (holder.value() instanceof WorldlyContainerHolder containerHolder) {
                try {
                    return containerHolder.getContainer(thisBlock, args);
                } catch (Throwable t) {
                    CraftEngine.instance().logger().error("Failed to run getContainer", t);
                }
            }
            return null;
        }
    }

    public static class HasAnalogOutputSignalInterceptor {
        public static final HasAnalogOutputSignalInterceptor INSTANCE = new HasAnalogOutputSignalInterceptor();

        @RuntimeType
        public boolean intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            try {
                return holder.value().hasAnalogOutputSignal(thisBlock, args);
            } catch (Throwable e) {
                CraftEngine.instance().logger().error("Failed to run hasAnalogOutputSignal", e);
            }
            return false;
        }
    }

    public static class GetAnalogOutputSignalInterceptor {
        public static final GetAnalogOutputSignalInterceptor INSTANCE = new GetAnalogOutputSignalInterceptor();

        @RuntimeType
        public int intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            try {
                return holder.value().getAnalogOutputSignal(thisBlock, args);
            } catch (Throwable t) {
                CraftEngine.instance().logger().error("Failed to run getAnalogOutputSignal", t);
            }
            return 0;
        }
    }

    public static class PerformBoneMealInterceptor {
        public static final PerformBoneMealInterceptor INSTANCE = new PerformBoneMealInterceptor();

        @RuntimeType
        public void intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            if (holder.value() instanceof BonemealableBlock bonemealableBlock) {
                try {
                    bonemealableBlock.performBonemeal(thisBlock, args);
                } catch (Throwable t) {
                    CraftEngine.instance().logger().error("Failed to run performBoneMeal", t);
                }
            }
        }
    }

    public static class NeighborChangedInterceptor {
        public static final NeighborChangedInterceptor INSTANCE = new NeighborChangedInterceptor();

        @RuntimeType
        public void intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            try {
                holder.value().neighborChanged(thisBlock, args);
            } catch (Throwable t) {
                CraftEngine.instance().logger().error("Failed to run neighborChanged", t);
            }
        }
    }

    public static class OnExplosionHitInterceptor {
        public static final OnExplosionHitInterceptor INSTANCE = new OnExplosionHitInterceptor();

        @RuntimeType
        public void intercept(@This Object thisBlock, @AllArguments Object[] args, @SuperCall Callable<Object> superMethod) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            try {
                BlockBehavior value = holder.value();
                value.preExplosionHit(thisBlock, args);
                superMethod.call();
                value.postExplosionHit(thisBlock, args);
            } catch (Throwable t) {
                CraftEngine.instance().logger().error("Failed to run onExplosionHit", t);
            }
        }
    }

    public static class PickUpBlockInterceptor {
        public static final PickUpBlockInterceptor INSTANCE = new PickUpBlockInterceptor();

        @RuntimeType
        public Object intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            try {
                if (holder.value() instanceof BucketPickup bucketPickup) {
                    Object item = bucketPickup.pickupBlock(thisBlock, args);
                    if (item != null) return item;
                }
            } catch (Throwable t) {
                CraftEngine.instance().logger().error("Failed to run pickupBlock", t);
            }
            return ItemStackProxy.EMPTY;
        }
    }

    public static class GetPickUpSoundInterceptor {
        public static final GetPickUpSoundInterceptor INSTANCE = new GetPickUpSoundInterceptor();

        @RuntimeType
        public Object intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            try {
                if (holder.value() instanceof BucketPickup bucketPickup) {
                    Object sound = bucketPickup.getPickupSound(thisBlock, args);
                    if (sound != null) return sound;
                }
            } catch (Throwable t) {
                CraftEngine.instance().logger().error("Failed to run getPickupSound", t);
            }
            return Optional.empty();
        }
    }

    public static class PlaceLiquidInterceptor {
        public static final PlaceLiquidInterceptor INSTANCE = new PlaceLiquidInterceptor();

        @RuntimeType
        public boolean intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            if (holder.value() instanceof LiquidBlockContainer liquidBlockContainer) {
                try {
                    return liquidBlockContainer.placeLiquid(thisBlock, args);
                } catch (Throwable t) {
                    CraftEngine.instance().logger().error("Failed to run placeLiquid", t);
                }
            }
            return false;
        }
    }

    public static class CanPlaceLiquidInterceptor {
        public static final CanPlaceLiquidInterceptor INSTANCE = new CanPlaceLiquidInterceptor();

        @RuntimeType
        public boolean intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            if (holder.value() instanceof LiquidBlockContainer liquidBlockContainer) {
                try {
                    return liquidBlockContainer.canPlaceLiquid(thisBlock, args);
                } catch (Throwable t) {
                    CraftEngine.instance().logger().error("Failed to run canPlaceLiquid", t);
                }
            }
            return false;
        }
    }

    public static class GetDirectSignalInterceptor {
        public static final GetDirectSignalInterceptor INSTANCE = new GetDirectSignalInterceptor();

        @RuntimeType
        public int intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            try {
                return holder.value().getDirectSignal(thisBlock, args);
            } catch (Throwable t) {
                CraftEngine.instance().logger().error("Failed to run getDirectSignal", t);
            }
            return 0;
        }
    }

    public static class GetSignalInterceptor {
        public static final GetSignalInterceptor INSTANCE = new GetSignalInterceptor();

        @RuntimeType
        public int intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            try {
                return holder.value().getSignal(thisBlock, args);
            } catch (Throwable t) {
                CraftEngine.instance().logger().error("Failed to run getSignal", t);
            }
            return 0;
        }
    }

    public static class IsSignalSourceInterceptor {
        public static final IsSignalSourceInterceptor INSTANCE = new IsSignalSourceInterceptor();

        @RuntimeType
        public boolean intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            try {
                return holder.value().isSignalSource(thisBlock, args);
            } catch (Throwable t) {
                CraftEngine.instance().logger().error("Failed to run isSignalSource", t);
            }
            return false;
        }
    }

    public static class AffectNeighborsAfterRemovalInterceptor {
        public static final AffectNeighborsAfterRemovalInterceptor INSTANCE = new AffectNeighborsAfterRemovalInterceptor();

        @RuntimeType
        public void intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            try {
                holder.value().affectNeighborsAfterRemoval(thisBlock, args);
            } catch (Throwable t) {
                CraftEngine.instance().logger().error("Failed to run affectNeighborsAfterRemoval", t);
            }
        }
    }

    public static class EntityInsideInterceptor {
        public static final EntityInsideInterceptor INSTANCE = new EntityInsideInterceptor();

        @RuntimeType
        public void intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            try {
                holder.value().entityInside(thisBlock, args);
            } catch (Throwable t) {
                CraftEngine.instance().logger().error("Failed to run entityInside", t);
            }
        }
    }

    public static class PlayerWillDestroyInterceptor {
        public static final PlayerWillDestroyInterceptor INSTANCE = new PlayerWillDestroyInterceptor();

        @RuntimeType
        public Object intercept(@This Object thisBlock, @AllArguments Object[] args, @SuperCall Callable<Object> superMethod) throws Exception {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            try {
                Object afterState = holder.value().playerWillDestroy(thisBlock, args);
                superMethod.call();
                return afterState;
            } catch (Throwable t) {
                CraftEngine.instance().logger().error("Failed to run playerWillDestroy", t);
            }
            return superMethod.call();
        }
    }

    public static class SpawnAfterBreakInterceptor {
        public static final SpawnAfterBreakInterceptor INSTANCE = new SpawnAfterBreakInterceptor();

        @RuntimeType
        public void intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            try {
                holder.value().spawnAfterBreak(thisBlock, args);
            } catch (Throwable t) {
                CraftEngine.instance().logger().error("Failed to run spawnAfterBreak", t);
            }
        }
    }

    public static class StepOnInterceptor {
        public static final StepOnInterceptor INSTANCE = new StepOnInterceptor();

        @RuntimeType
        public void intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            try {
                holder.value().stepOn(thisBlock, args);
            } catch (Throwable t) {
                CraftEngine.instance().logger().error("Failed to run stepOn", t);
            }
        }
    }

    public static class FallOnInterceptor {
        public static final FallOnInterceptor INSTANCE = new FallOnInterceptor();

        @RuntimeType
        public void intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            try {
                holder.value().fallOn(thisBlock, args);
            } catch (Throwable t) {
                CraftEngine.instance().logger().error("Failed to run fallOn", t);
            }
        }
    }

    public static class UpdateEntityMovementAfterFallOnInterceptor {
        public static final UpdateEntityMovementAfterFallOnInterceptor INSTANCE = new UpdateEntityMovementAfterFallOnInterceptor();

        @RuntimeType
        public void intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            try {
                holder.value().updateEntityMovementAfterFallOn(thisBlock, args);
            } catch (Throwable t) {
                CraftEngine.instance().logger().error("Failed to run updateEntityMovementAfterFallOn", t);
            }
        }
    }

    public static class OnProjectileHitInterceptor {
        public static final OnProjectileHitInterceptor INSTANCE = new OnProjectileHitInterceptor();

        @RuntimeType
        public void intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            try {
                holder.value().onProjectileHit(thisBlock, args);
            } catch (Throwable t) {
                CraftEngine.instance().logger().error("Failed to run onProjectileHit", t);
            }
        }
    }

    public static class SetPlaceByInterceptor {
        public static final SetPlaceByInterceptor INSTANCE = new SetPlaceByInterceptor();

        @RuntimeType
        public void intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            try {
                holder.value().placeMultiState(thisBlock, args);
            } catch (Throwable t) {
                CraftEngine.instance().logger().error("Failed to run setPlaceBy", t);
            }
        }
    }

    public static class AttackInterceptor {
        public static final AttackInterceptor INSTANCE = new AttackInterceptor();

        @RuntimeType
        public void intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            try {
                holder.value().attack(thisBlock, args);
            } catch (Throwable t) {
                CraftEngine.instance().logger().error("Failed to run attack", t);
            }
        }
    }

    public static class TriggerEventInterceptor {
        public static final TriggerEventInterceptor INSTANCE = new TriggerEventInterceptor();

        @RuntimeType
        public boolean intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            try {
                return holder.value().triggerEvent(thisBlock, args);
            } catch (Throwable t) {
                CraftEngine.instance().logger().error("Failed to run triggerEvent", t);
            }
            return false;
        }
    }

    public static class HandlePrecipitationInterceptor {
        public static final HandlePrecipitationInterceptor INSTANCE = new HandlePrecipitationInterceptor();

        @RuntimeType
        public void intercept(@This Object thisBlock, @AllArguments Object[] args) {
            ObjectHolder<BlockBehavior> holder = ((DelegatingBlock) thisBlock).behaviorDelegate();
            try {
                holder.value().handlePrecipitation(thisBlock, args);
            } catch (Throwable t) {
                CraftEngine.instance().logger().error("Failed to run handlePrecipitation", t);
            }
        }
    }
}