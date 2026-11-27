package net.momirealms.craftengine.bukkit.plugin.injector;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.matcher.ElementMatchers;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.block.LiquidSolidification;
import net.momirealms.craftengine.bukkit.block.behavior.LiquidSolidifiableBlock;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.property.BooleanProperty;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.event.CraftEventFactoryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.Vec3iProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityTypesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.MoverTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.item.FallingBlockEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.item.ItemEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.ItemLikeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelWriterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.StateHolderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.properties.BlockStatePropertiesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidStateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.constructor.SConstructor2;
import net.momirealms.sparrow.reflection.constructor.matcher.ConstructorMatcher;
import net.momirealms.sparrow.reflection.method.matcher.MethodMatcher;

import java.lang.reflect.Method;

import static java.util.Objects.requireNonNull;

public final class FallingBlockEntityGenerator {
    private static SConstructor2 constructor;
    private static final Method SPAWN_AT_LOCATION = requireNonNull(
            SparrowClass.of(EntityProxy.CLASS).getDeclaredMethod(MethodMatcher.named("spawnAtLocation")
                    .and(VersionHelper.isOrAbove1_21_2
                            ? MethodMatcher.takeArguments(ServerLevelProxy.CLASS, ItemLikeProxy.CLASS)
                            : MethodMatcher.takeArguments(ItemLikeProxy.CLASS))
                    .and(MethodMatcher.returnType(ItemEntityProxy.CLASS)))
    );
    private static final Method MOVE = requireNonNull(
            SparrowClass.of(EntityProxy.CLASS).getDeclaredMethod(MethodMatcher.named("move")
                    .and(MethodMatcher.takeArguments(MoverTypeProxy.CLASS, Vec3Proxy.CLASS))
                    .and(MethodMatcher.returnType(void.class)))
    );
    private static final Method TICK = requireNonNull(
            SparrowClass.of(FallingBlockEntityProxy.CLASS).getDeclaredMethod(MethodMatcher.named("tick")
                    .and(MethodMatcher.takeArguments(new Class<?>[0]))
                    .and(MethodMatcher.returnType(void.class)))
    );

    private FallingBlockEntityGenerator() {
    }

    public static void init() {
        Class<?> generated = InjectorClassLoading.load(new ByteBuddy(ClassFileVersion.JAVA_V21)
                .subclass(FallingBlockEntityProxy.CLASS, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                .name(FallingBlockEntityGenerator.class.getPackageName() + ".CraftEngineFallingBlockEntity")
                .defineField("liquidSolidifier", LiquidSolidifiableBlock.class, Visibility.PRIVATE)
                .defineField("ticking", boolean.class, Visibility.PRIVATE)
                .implement(InjectedFallingBlockEntity.class)
                .method(ElementMatchers.named("liquidSolidifier"))
                .intercept(FieldAccessor.ofField("liquidSolidifier"))
                .method(ElementMatchers.named("ticking"))
                .intercept(FieldAccessor.ofField("ticking"))
                .method(ElementMatchers.is(SPAWN_AT_LOCATION))
                .intercept(Advice.to(SpawnAtLocationAdvice.class).wrap(SuperMethodCall.INSTANCE))
                .method(ElementMatchers.is(MOVE))
                .intercept(Advice.to(MoveAdvice.class).wrap(SuperMethodCall.INSTANCE))
                .method(ElementMatchers.is(TICK))
                .intercept(Advice.to(TickAdvice.class).wrap(SuperMethodCall.INSTANCE)));
        // The position constructor is private on vanilla/Spigot and only exposed by Paper.
        constructor = SparrowClass.of(generated)
                .getSparrowConstructor(ConstructorMatcher.takeArguments(
                        EntityTypeProxy.CLASS,
                        LevelProxy.CLASS
                ))
                .asm$2();
    }

    public static Object fall(Object level, Object pos, Object blockState) {
        if (constructor == null) {
            throw new IllegalStateException("FallingBlockEntityGenerator has not been initialized");
        }

        Object finalBlockState = withoutWaterlogged(blockState);
        Object fallingBlockEntity = constructor.newInstance(
                EntityTypesProxy.FALLING_BLOCK,
                level
        );
        // Mirror the initialization performed by FallingBlockEntity's private position constructor.
        double x = Vec3iProxy.INSTANCE.getX(pos) + 0.5;
        double y = Vec3iProxy.INSTANCE.getY(pos);
        double z = Vec3iProxy.INSTANCE.getZ(pos) + 0.5;
        FallingBlockEntityProxy.INSTANCE.setBlockState(fallingBlockEntity, finalBlockState);
        EntityProxy.INSTANCE.setBlocksBuilding(fallingBlockEntity, true);
        EntityProxy.INSTANCE.setPos(fallingBlockEntity, x, y, z);
        EntityProxy.INSTANCE.setDeltaMovement(fallingBlockEntity, Vec3Proxy.ZERO);
        EntityProxy.INSTANCE.setXo(fallingBlockEntity, x);
        EntityProxy.INSTANCE.setYo(fallingBlockEntity, y);
        EntityProxy.INSTANCE.setZo(fallingBlockEntity, z);
        FallingBlockEntityProxy.INSTANCE.setStartPos(
                fallingBlockEntity,
                EntityProxy.INSTANCE.getBlockPosition(fallingBlockEntity)
        );
        ImmutableBlockState customBlockState = BlockStateUtils.getNullableCustomBlockState(finalBlockState);
        if (customBlockState != null) {
            ((InjectedFallingBlockEntity) fallingBlockEntity).liquidSolidifier(customBlockState.behavior().getFirst(LiquidSolidifiableBlock.class));
        }

        Object fluidState = BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getFluidState(blockState);
        Object legacyFluidState = FluidStateProxy.INSTANCE.createLegacyBlock(fluidState);
        if (!CraftEventFactoryProxy.INSTANCE.callEntityChangeBlockEvent(fallingBlockEntity, pos, legacyFluidState)) {
            return fallingBlockEntity;
        }
        LevelWriterProxy.INSTANCE.setBlock(level, pos, legacyFluidState, 3);
        LevelWriterProxy.INSTANCE.addFreshEntity(level, fallingBlockEntity, null);
        return fallingBlockEntity;
    }

    private static Object withoutWaterlogged(Object blockState) {
        ImmutableBlockState customBlockState = BlockStateUtils.getNullableCustomBlockState(blockState);
        if (customBlockState != null) {
            for (Property<?> property : customBlockState.getProperties()) {
                if (!property.name().equals("waterlogged") || property.valueClass() != Boolean.class) {
                    continue;
                }
                BooleanProperty waterlogged = (BooleanProperty) property;
                return customBlockState.get(waterlogged)
                        ? customBlockState.with(waterlogged, false).customBlockState().minecraftState()
                        : blockState;
            }
            return blockState;
        }
        return StateHolderProxy.INSTANCE.hasProperty(blockState, BlockStatePropertiesProxy.WATERLOGGED)
                ? StateHolderProxy.INSTANCE.setValue(blockState, BlockStatePropertiesProxy.WATERLOGGED, false)
                : blockState;
    }

    static final class SpawnAtLocationAdvice {
        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        static boolean dropCustomItems(@Advice.This Object entity) {
            ImmutableBlockState state = BlockStateUtils.getNullableCustomBlockState(FallingBlockEntityProxy.INSTANCE.getBlockState(entity));
            if (state == null) {
                return false;
            }
            FallingBlockEntityGenerator.dropItems(entity, state);
            return true;
        }
    }

    static void dropItems(Object fallingBlockEntity, ImmutableBlockState state) {
        Object level = EntityProxy.INSTANCE.getLevel(fallingBlockEntity);
        World world = BukkitAdaptor.adapt(LevelProxy.INSTANCE.getWorld(level));
        WorldPosition position = new WorldPosition(
                world,
                EntityProxy.INSTANCE.getXo(fallingBlockEntity),
                EntityProxy.INSTANCE.getYo(fallingBlockEntity),
                EntityProxy.INSTANCE.getZo(fallingBlockEntity)
        );
        for (Item item : state.getDrops(ContextHolder.builder(
                DirectContextParameters.CUSTOM_BLOCK_STATE, state,
                DirectContextParameters.FALLING_BLOCK, true,
                DirectContextParameters.POSITION, position
        ).build(), world, null)) {
            world.dropItemNaturally(position, item);
        }
    }

    static final class MoveAdvice {
        @Advice.OnMethodEnter
        static Object beforeMove(@Advice.This Object entity, @Advice.FieldValue("ticking") boolean ticking,
                                 @Advice.FieldValue("liquidSolidifier") LiquidSolidifiableBlock solidifier,
                                 @Advice.Local("movingSolidifier") LiquidSolidifiableBlock movingSolidifier) {
            if (ticking && solidifier != null) {
                movingSolidifier = solidifier;
                return EntityProxy.INSTANCE.getPosition(entity);
            }
            return null;
        }

        @Advice.OnMethodExit
        static void afterMove(@Advice.This Object entity, @Advice.Enter Object from,
                              @Advice.Local("movingSolidifier") LiquidSolidifiableBlock solidifier) {
            if (solidifier != null && LiquidSolidification.solidifyFallingBlock(entity, from, solidifier)) {
                throw TickConsumed.INSTANCE;
            }
        }
    }

    static final class TickAdvice {
        @Advice.OnMethodEnter
        static void begin(@Advice.FieldValue(value = "ticking", readOnly = false) boolean ticking) {
            ticking = true;
        }

        @Advice.OnMethodExit(onThrowable = Throwable.class)
        static void finish(@Advice.FieldValue(value = "ticking", readOnly = false) boolean ticking,
                           @Advice.Thrown(readOnly = false) Throwable failure) {
            ticking = false;
            if (failure == TickConsumed.INSTANCE) {
                failure = null;
            }
        }
    }

    public interface InjectedFallingBlockEntity {
        LiquidSolidifiableBlock liquidSolidifier();

        void liquidSolidifier(LiquidSolidifiableBlock solidifiable);

        boolean ticking();

        void ticking(boolean ticking);
    }

    static final class TickConsumed extends RuntimeException {
        static final TickConsumed INSTANCE = new TickConsumed();

        private TickConsumed() {
            super(null, null, false, false);
        }
    }
}
