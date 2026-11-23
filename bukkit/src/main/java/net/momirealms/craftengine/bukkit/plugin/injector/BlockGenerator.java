package net.momirealms.craftengine.bukkit.plugin.injector;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.momirealms.craftengine.bukkit.block.BukkitBlockShape;
import net.momirealms.craftengine.bukkit.block.behavior.EmptyBlockBehavior;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.block.BlockShape;
import net.momirealms.craftengine.core.block.DelegatingBlock;
import net.momirealms.craftengine.core.block.behavior.BlockBehavior;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ObjectHolder;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.registries.RegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.ResourceKeyProxy;
import net.momirealms.craftengine.proxy.minecraft.world.WorldlyContainerHolderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BonemealableBlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.FallableProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.SimpleWaterloggedBlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.StateDefinitionProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.constructor.SConstructor3;
import net.momirealms.sparrow.reflection.constructor.matcher.ConstructorMatcher;
import net.momirealms.sparrow.reflection.field.SBooleanField;
import net.momirealms.sparrow.reflection.field.matcher.FieldMatcher;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static java.util.Objects.requireNonNull;
import static net.momirealms.craftengine.bukkit.plugin.injector.BlockBehaviorMethods.forward;
import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.named;

public final class BlockGenerator {
    private static SConstructor3 constructor;
    private static SBooleanField noteBlockField;
    private static SBooleanField tripwireField;

    private BlockGenerator() {
    }

    public static void init() {
        Class<?> properties = BlockBehaviourProxy.PropertiesProxy.CLASS;
        Constructor<?> parent = requireNonNull(SparrowClass.of(BlockProxy.CLASS).getDeclaredConstructor(ConstructorMatcher.takeArguments(properties)));
        Class<?>[] parameters = {properties, ObjectHolder.class, ObjectHolder.class};
        DynamicType.Builder<?> builder = new ByteBuddy(ClassFileVersion.JAVA_V21)
                .subclass(BlockProxy.CLASS, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                .name(BlockGenerator.class.getPackageName() + ".CraftEngineBlock")
                .defineField("behaviorHolder", ObjectHolder.class, Visibility.PUBLIC)
                .defineField("shapeHolder", ObjectHolder.class, Visibility.PUBLIC)
                .defineField("isClientSideNoteBlock", boolean.class, Visibility.PUBLIC)
                .defineField("isClientSideTripwire", boolean.class, Visibility.PUBLIC)
                .defineConstructor(Visibility.PUBLIC).withParameters(parameters)
                .intercept(MethodCall.invoke(parent).withArgument(0)
                        .andThen(FieldAccessor.ofField("behaviorHolder").setsArgumentAt(1))
                        .andThen(FieldAccessor.ofField("shapeHolder").setsArgumentAt(2)))
                .implement(DelegatingBlock.class, BlockSuperCalls.class)
                .implement(FallableProxy.CLASS)
                .implement(BonemealableBlockProxy.CLASS)
                .implement(SimpleWaterloggedBlockProxy.CLASS)
                .implement(WorldlyContainerHolderProxy.CLASS)
                .method(named("behaviorDelegate"))
                .intercept(FieldAccessor.ofField("behaviorHolder"))
                .method(named("shapeDelegate"))
                .intercept(FieldAccessor.ofField("shapeHolder"))
                .method(named("isNoteBlock"))
                .intercept(FieldAccessor.ofField("isClientSideNoteBlock"))
                .method(named("isTripwire"))
                .intercept(FieldAccessor.ofField("isClientSideTripwire"));
        builder = bindBehavior(builder);
        builder = bindCapabilities(builder);
        builder = bindSuperCalls(builder);
        SparrowClass<?> generated = SparrowClass.of(builder.make().load(BlockGenerator.class.getClassLoader()).getLoaded());
        constructor = generated.getSparrowConstructor(ConstructorMatcher.takeArguments(parameters)).asm$3();
        noteBlockField = generated.getSparrowField(FieldMatcher.named("isClientSideNoteBlock")).asm$boolean();
        tripwireField = generated.getSparrowField(FieldMatcher.named("isClientSideTripwire")).asm$boolean();
    }

    private static DynamicType.Builder<?> bindBehavior(DynamicType.Builder<?> builder) {
        builder = builder
                .method(is(BlockReflections.IS_PATHFINDABLE)).intercept(forward("isPathFindable"))
                .method(is(BlockReflections.MIRROR)).intercept(customBehavior("mirror"))
                .method(is(BlockReflections.ROTATE)).intercept(customBehavior("rotate"))
                .method(is(BlockReflections.HAS_ANALOG_OUTPUT_SIGNAL)).intercept(forward("hasAnalogOutputSignal"))
                .method(is(BlockReflections.GET_ANALOG_OUTPUT_SIGNAL)).intercept(forward("getAnalogOutputSignal"))
                .method(is(BlockReflections.TICK)).intercept(forward("tick"))
                .method(is(BlockReflections.RANDOM_TICK)).intercept(forward("randomTick"))
                .method(is(BlockReflections.ON_PLACE)).intercept(forward("onPlace"))
                .method(is(BlockReflections.CAN_SURVIVE)).intercept(customBehavior("canSurvive"))
                .method(is(BlockReflections.UPDATE_SHAPE)).intercept(capability(BlockBehaviorMethods.ShapeUpdate.class))
                .method(is(BlockReflections.NEIGHBOR_CHANGED)).intercept(forward("neighborChanged"))
                .method(is(BlockReflections.ENTITY_INSIDE)).intercept(forward("entityInside"))
                .method(is(BlockReflections.GET_SIGNAL)).intercept(forward("getSignal"))
                .method(is(BlockReflections.GET_DIRECT_SIGNAL)).intercept(forward("getDirectSignal"))
                .method(is(BlockReflections.IS_SIGNAL_SOURCE)).intercept(forward("isSignalSource"))
                .method(is(BlockReflections.SPAWN_AFTER_BREAK)).intercept(forward("spawnAfterBreak"))
                .method(is(BlockReflections.FALL_ON)).intercept(forward("fallOn"))
                .method(is(BlockReflections.STEP_ON)).intercept(forward("stepOn"))
                .method(is(BlockReflections.ON_PROJECTILE_HIT)).intercept(forward("onProjectileHit"))
                .method(is(BlockReflections.SET_PLACED_BY)).intercept(customBehavior("setPlacedBy"))
                .method(is(BlockReflections.AFFECT_NEIGHBORS_AFTER_REMOVAL)).intercept(forward("affectNeighborsAfterRemoval"))
                .method(is(BlockReflections.ATTACK)).intercept(forward("attack"))
                .method(is(BlockReflections.TRIGGER_EVENT)).intercept(forward("triggerEvent"))
                .method(is(BlockReflections.HANDLE_PRECIPITATION)).intercept(forward("handlePrecipitation"));
        if (BlockReflections.UPDATE_ENTITY_MOVEMENT_AFTER_FALL_ON != null) {
            builder = builder.method(is(BlockReflections.UPDATE_ENTITY_MOVEMENT_AFTER_FALL_ON)).intercept(forward("updateEntityMovementAfterFallOn"));
        }
        return builder;
    }

    private static DynamicType.Builder<?> bindCapabilities(DynamicType.Builder<?> builder) {
        return builder
                .method(is(BlockReflections.IS_VALID_BONEMEAL_TARGET)).intercept(capability(BlockCapabilityAdvice.IsValidBoneMealTarget.class))
                .method(is(BlockReflections.GET_CONTAINER)).intercept(capability(BlockCapabilityAdvice.GetContainer.class))
                .method(is(BlockReflections.IS_BONEMEAL_SUCCESS)).intercept(capability(BlockCapabilityAdvice.IsBoneMealSuccess.class))
                .method(is(BlockReflections.PERFORM_BONEMEAL)).intercept(capability(BlockCapabilityAdvice.PerformBoneMeal.class))
                .method(is(BlockReflections.ON_BROKEN_AFTER_FALL)).intercept(capability(BlockCapabilityAdvice.OnBrokenAfterFall.class))
                .method(is(BlockReflections.ON_LAND)).intercept(capability(BlockCapabilityAdvice.OnLand.class))
                .method(is(BlockReflections.PICKUP_BLOCK)).intercept(capability(BlockCapabilityAdvice.PickUpBlock.class))
                .method(is(BlockReflections.GET_PICKUP_SOUND)).intercept(capability(BlockCapabilityAdvice.GetPickUpSound.class))
                .method(is(BlockReflections.PLACE_LIQUID)).intercept(capability(BlockCapabilityAdvice.PlaceLiquid.class))
                .method(is(BlockReflections.CAN_PLACE_LIQUID)).intercept(capability(BlockCapabilityAdvice.CanPlaceLiquid.class));
    }

    private static DynamicType.Builder<?> bindSuperCalls(DynamicType.Builder<?> builder) {
        builder = bindSuperCall(builder, BlockReflections.GET_SHAPE, "getShape", "superGetShape");
        builder = bindSuperCall(builder, BlockReflections.GET_COLLISION_SHAPE, "getCollisionShape", "superGetCollisionShape");
        builder = bindSuperCall(builder, BlockReflections.GET_BLOCK_SUPPORT_SHAPE, "getSupportShape", "superGetSupportShape");
        builder = bindSuperCall(builder, BlockReflections.PLAYER_WILL_DESTROY, "playerWillDestroy", "superPlayerWillDestroy");
        if (BlockReflections.ON_EXPLOSION_HIT != null) {
            builder = bindSuperCall(builder.implement(BlockSuperCalls.Explosion.class), BlockReflections.ON_EXPLOSION_HIT, "onExplosionHit", "superOnExplosionHit");
        }
        return builder;
    }

    private static Implementation capability(Class<?> advice) {
        return Advice.to(advice).wrap(StubMethod.INSTANCE);
    }

    private static MethodDelegation customBehavior(String method) {
        return MethodDelegation.withDefaultConfiguration().filter(named(method)).to(BlockBehaviorMethods.class);
    }

    private static DynamicType.Builder<?> bindSuperCall(DynamicType.Builder<?> builder, Method nativeMethod, String callback, String bridge) {
        return builder.method(named(bridge))
                .intercept(MethodCall.invoke(nativeMethod).onSuper().withAllArguments().withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
                .method(is(nativeMethod))
                .intercept(MethodDelegation.withDefaultConfiguration().filter(named(callback)).to(BlockSuperMethods.class));
    }

    public static SBooleanField field$CraftEngineBlock$isNoteBlock() {
        return noteBlockField;
    }

    public static SBooleanField field$CraftEngineBlock$isTripwire() {
        return tripwireField;
    }

    public static DelegatingBlock generateBlock(Key blockId) {
        ObjectHolder<BlockBehavior> behaviorHolder = new ObjectHolder<>(EmptyBlockBehavior.INSTANCE);
        ObjectHolder<BlockShape> shapeHolder = new ObjectHolder<>(BukkitBlockShape.STONE);
        Object newBlockInstance = constructor.newInstance(createEmptyBlockProperties(blockId), behaviorHolder, shapeHolder);
        Object stateDefinitionBuilder = StateDefinitionProxy.BuilderProxy.INSTANCE.newInstance(newBlockInstance);
        Object stateDefinition = StateDefinitionProxy.BuilderProxy.INSTANCE.create(stateDefinitionBuilder,
                BlockProxy.INSTANCE::getDefaultBlockState, BlockStateGenerator.instance$StateDefinition$Factory);
        BlockProxy.INSTANCE.setStateDefinition(newBlockInstance, stateDefinition);
        BlockProxy.INSTANCE.setDefaultBlockState(newBlockInstance, StateDefinitionProxy.INSTANCE.getStates(stateDefinition).getFirst());
        return (DelegatingBlock) newBlockInstance;
    }

    private static Object createEmptyBlockProperties(Key id) {
        Object blockProperties = BlockBehaviourProxy.PropertiesProxy.INSTANCE.of();
        if (VersionHelper.isOrAbove1_21_2) {
            Object resourceKey = ResourceKeyProxy.INSTANCE.create(RegistriesProxy.BLOCK, KeyUtils.toIdentifier(id));
            BlockBehaviourProxy.PropertiesProxy.INSTANCE.setId(blockProperties, resourceKey);
        }
        return blockProperties;
    }
}
