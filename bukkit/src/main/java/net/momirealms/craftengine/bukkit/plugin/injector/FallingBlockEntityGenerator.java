package net.momirealms.craftengine.bukkit.plugin.injector;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatchers;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.block.behavior.LiquidSolidifiableBlock;
import net.momirealms.craftengine.bukkit.block.LiquidSolidification;
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
import net.momirealms.craftengine.proxy.minecraft.world.entity.MoverTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.item.FallingBlockEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.item.ItemEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.ItemLikeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelWriterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockStateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.StateHolderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.properties.BlockStatePropertiesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidStateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.constructor.SConstructor5;
import net.momirealms.sparrow.reflection.constructor.matcher.ConstructorMatcher;
import net.momirealms.sparrow.reflection.method.matcher.MethodMatcher;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.Callable;

import static java.util.Objects.requireNonNull;

public final class FallingBlockEntityGenerator {
    private static SConstructor5 constructor$CraftEngineFallingBlockEntity;
    public static final Method method$Entity$spawnAtLocation = requireNonNull(
            SparrowClass.of(EntityProxy.CLASS).getDeclaredMethod(MethodMatcher.named("spawnAtLocation")
                    .and(VersionHelper.isOrAbove1_21_2
                            ? MethodMatcher.takeArguments(ServerLevelProxy.CLASS, ItemLikeProxy.CLASS)
                            : MethodMatcher.takeArguments(ItemLikeProxy.CLASS))
                    .and(MethodMatcher.returnType(ItemEntityProxy.CLASS)))
    );
    public static final Method method$Entity$move = requireNonNull(
            SparrowClass.of(EntityProxy.CLASS).getDeclaredMethod(MethodMatcher.named("move")
                    .and(MethodMatcher.takeArguments(MoverTypeProxy.CLASS, Vec3Proxy.CLASS))
                    .and(MethodMatcher.returnType(void.class)))
    );
    public static final Method method$FallingBlockEntity$tick = requireNonNull(
            SparrowClass.of(FallingBlockEntityProxy.CLASS).getDeclaredMethod(MethodMatcher.named("tick")
                    .and(MethodMatcher.takeArguments(new Class<?>[0]))
                    .and(MethodMatcher.returnType(void.class)))
    );

    private FallingBlockEntityGenerator() {
    }

    public static void init() {
        String packageWithName = FallingBlockEntityGenerator.class.getName();
        String generatedClassName = packageWithName.substring(0, packageWithName.lastIndexOf('.')) + ".CraftEngineFallingBlockEntity";
        Class<?> clazz$CraftEngineFallingBlockEntity = new ByteBuddy(ClassFileVersion.JAVA_V21)
                .subclass(FallingBlockEntityProxy.CLASS, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                .name(generatedClassName)
                .defineField("liquidSolidifier", LiquidSolidifiableBlock.class, Visibility.PRIVATE)
                .defineField("ticking", boolean.class, Visibility.PRIVATE)
                .implement(InjectedFallingBlockEntity.class)
                .method(ElementMatchers.named("liquidSolidifier"))
                .intercept(FieldAccessor.ofField("liquidSolidifier"))
                .method(ElementMatchers.named("ticking"))
                .intercept(FieldAccessor.ofField("ticking"))
                .method(ElementMatchers.is(method$Entity$spawnAtLocation))
                .intercept(MethodDelegation.to(SpawnAtLocationInterceptor.class))
                .method(ElementMatchers.is(method$Entity$move))
                .intercept(MethodDelegation.to(MoveInterceptor.class))
                .method(ElementMatchers.is(method$FallingBlockEntity$tick))
                .intercept(MethodDelegation.to(TickInterceptor.class))
                .make()
                .load(FallingBlockEntityGenerator.class.getClassLoader())
                .getLoaded();
        constructor$CraftEngineFallingBlockEntity = SparrowClass.of(clazz$CraftEngineFallingBlockEntity)
                .getSparrowConstructor(ConstructorMatcher.takeArguments(
                        LevelProxy.CLASS,
                        double.class,
                        double.class,
                        double.class,
                        BlockStateProxy.CLASS
                ))
                .asm$5();
    }

    public static Object fall(Object level, Object pos, Object blockState) {
        if (constructor$CraftEngineFallingBlockEntity == null) {
            throw new IllegalStateException("FallingBlockEntityGenerator has not been initialized");
        }

        Object finalBlockState = withoutWaterlogged(blockState);
        Object fallingBlockEntity = constructor$CraftEngineFallingBlockEntity.newInstance(
                level,
                Vec3iProxy.INSTANCE.getX(pos) + 0.5,
                (double) Vec3iProxy.INSTANCE.getY(pos),
                Vec3iProxy.INSTANCE.getZ(pos) + 0.5,
                finalBlockState
        );
        ImmutableBlockState customBlockState = BlockStateUtils.getOptionalCustomBlockState(finalBlockState).orElse(null);
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
        ImmutableBlockState customBlockState = BlockStateUtils.getOptionalCustomBlockState(blockState).orElse(null);
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

    public static final class SpawnAtLocationInterceptor {

        private SpawnAtLocationInterceptor() {
        }

        @RuntimeType
        public static Object intercept(@This Object fallingBlockEntity,
                                       @SuperCall Callable<Object> superMethod) throws Exception {
            Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(
                    FallingBlockEntityProxy.INSTANCE.getBlockState(fallingBlockEntity)
            );
            if (optionalCustomState.isEmpty()) {
                return superMethod.call();
            }

            Object level = EntityProxy.INSTANCE.getLevel(fallingBlockEntity);
            World world = BukkitAdaptor.adapt(LevelProxy.INSTANCE.getWorld(level));
            WorldPosition position = new WorldPosition(
                    world,
                    EntityProxy.INSTANCE.getXo(fallingBlockEntity),
                    EntityProxy.INSTANCE.getYo(fallingBlockEntity),
                    EntityProxy.INSTANCE.getZo(fallingBlockEntity)
            );
            ContextHolder.Builder context = ContextHolder.builder()
                    .withParameter(DirectContextParameters.FALLING_BLOCK, true)
                    .withParameter(DirectContextParameters.POSITION, position);
            for (Item item : optionalCustomState.get().getDrops(context, world, null)) {
                world.dropItemNaturally(position, item);
            }
            return null;
        }
    }

    public static final class MoveInterceptor {

        private MoveInterceptor() {
        }

        @RuntimeType
        public static void intercept(@This Object fallingBlockEntity,
                                     @SuperCall Callable<Object> superMethod) throws Exception {
            InjectedFallingBlockEntity injected = (InjectedFallingBlockEntity) fallingBlockEntity;
            LiquidSolidifiableBlock solidifiable = injected.liquidSolidifier();
            if (!injected.ticking() || solidifiable == null) {
                superMethod.call();
                return;
            }

            Object from = EntityProxy.INSTANCE.getPosition(fallingBlockEntity);
            superMethod.call();
            if (LiquidSolidification.solidifyFallingBlock(fallingBlockEntity, from, solidifiable)) {
                throw TickConsumed.INSTANCE;
            }
        }
    }

    public static final class TickInterceptor {

        private TickInterceptor() {
        }

        @RuntimeType
        public static void intercept(@This Object fallingBlockEntity,
                                     @SuperCall Callable<Object> superMethod) throws Exception {
            InjectedFallingBlockEntity injected = (InjectedFallingBlockEntity) fallingBlockEntity;
            injected.ticking(true);
            try {
                superMethod.call();
            } catch (TickConsumed ignored) {
            } finally {
                injected.ticking(false);
            }
        }
    }

    public interface InjectedFallingBlockEntity {
        LiquidSolidifiableBlock liquidSolidifier();

        void liquidSolidifier(LiquidSolidifiableBlock solidifiable);

        boolean ticking();

        void ticking(boolean ticking);
    }

    private static final class TickConsumed extends RuntimeException {
        private static final TickConsumed INSTANCE = new TickConsumed();

        private TickConsumed() {
            super(null, null, false, false);
        }
    }
}
