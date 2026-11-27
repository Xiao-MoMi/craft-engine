package net.momirealms.craftengine.bukkit.world.gen;

import com.mojang.serialization.MapCodec;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.modifier.FieldManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.util.RandomSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelAccessorProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.WorldGenLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderTypeProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.constructor.SConstructor2;
import net.momirealms.sparrow.reflection.constructor.matcher.ConstructorMatcher;
import net.momirealms.sparrow.reflection.method.matcher.MethodMatcher;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static java.util.Objects.requireNonNull;
import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.named;

public final class BlockStateProviderAdapter {
    private static final SConstructor2 CONSTRUCTOR = generate();

    public static final Object SIMPLE = new Registration<>(CustomSimpleStateProvider.CODEC).type();
    public static final Object WEIGHTED = new Registration<>(CustomWeightedStateProvider.CODEC).type();
    public static final Object ROTATED = new Registration<>(CustomRotatedBlockProvider.CODEC).type();
    public static final Object RANDOMIZED_INT = new Registration<>(CustomRandomizedIntStateProvider.CODEC).type();

    private BlockStateProviderAdapter() {
    }

    private static SConstructor2 generate() {
        Class<?> base = BlockStateProviderProxy.CLASS;
        Class<?> typeClass = VersionHelper.isOrAbove26_3 ? MapCodec.class : BlockStateProviderTypeProxy.CLASS;
        Class<?>[] constructorParameters = {CustomStateProvider.class, typeClass};
        Class<?> parentClass = base.isInterface() ? Object.class : base;
        Constructor<?> parent = requireNonNull(SparrowClass.of(parentClass).getDeclaredConstructor(ConstructorMatcher.takeArguments(new Class<?>[0])));
        Class<?>[] stateParameters;
        if (VersionHelper.isOrAbove26_1) {
            Class<?> levelClass = VersionHelper.isOrAbove26_3 ? LevelAccessorProxy.CLASS : WorldGenLevelProxy.CLASS;
            stateParameters = new Class<?>[]{levelClass, RandomSourceProxy.CLASS, BlockPosProxy.CLASS};
        } else {
            stateParameters = new Class<?>[]{RandomSourceProxy.CLASS, BlockPosProxy.CLASS};
        }
        Method getState = requireNonNull(SparrowClass.of(base).getDeclaredMethod(MethodMatcher.named("getState").and(MethodMatcher.takeArguments(stateParameters))));
        Method type = requireNonNull(SparrowClass.of(base).getDeclaredMethod(MethodMatcher.named(VersionHelper.isOrAbove26_3 ? "codec" : "type").and(MethodMatcher.returnType(typeClass))));
        MethodCall sample = MethodCall.invoke(named("getState")).onField("provider");
        MethodCall delegate = VersionHelper.isOrAbove26_1 ? sample.withAllArguments() : sample.with((Object) null).withAllArguments();

        Class<?> generated = new ByteBuddy(ClassFileVersion.JAVA_V21)
                .subclass(base, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .implement(WrappedProvider.class)
                .name(BlockStateProviderAdapter.class.getPackageName() + ".NativeBlockStateProvider")
                .defineField("provider", CustomStateProvider.class, Visibility.PRIVATE, FieldManifestation.FINAL)
                .defineField("type", typeClass, Visibility.PRIVATE, FieldManifestation.FINAL)
                .defineConstructor(Visibility.PUBLIC).withParameters(constructorParameters)
                .intercept(MethodCall.invoke(parent)
                        .andThen(FieldAccessor.ofField("provider").setsArgumentAt(0))
                        .andThen(FieldAccessor.ofField("type").setsArgumentAt(1)))
                .method(named("provider")).intercept(FieldAccessor.ofField("provider"))
                .method(is(type)).intercept(FieldAccessor.ofField("type"))
                .method(is(getState)).intercept(delegate.withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
                .make()
                .load(BlockStateProviderAdapter.class.getClassLoader(), ClassLoadingStrategy.UsingLookup.of(MethodHandles.lookup()))
                .getLoaded();
        return SparrowClass.of(generated).getSparrowConstructor(ConstructorMatcher.takeArguments(constructorParameters)).asm$2();
    }

    interface WrappedProvider {
        CustomStateProvider provider();
    }

    private static final class Registration<P extends CustomStateProvider> {
        private final Object type;

        private Registration(MapCodec<P> logicCodec) {
            MapCodec<Object> codec = logicCodec.xmap(provider -> CONSTRUCTOR.newInstance(provider, this.type()), Registration::unwrap);
            if (VersionHelper.isOrAbove26_3) {
                this.type = codec;
            } else if (VersionHelper.isOrAbove1_20_5) {
                this.type = BlockStateProviderTypeProxy.INSTANCE.newInstance(codec);
            } else {
                this.type = BlockStateProviderTypeProxy.INSTANCE.newInstance(codec.codec());
            }
        }

        private Object type() {
            return this.type;
        }

        @SuppressWarnings("unchecked")
        private static <P extends CustomStateProvider> P unwrap(Object nativeProvider) {
            return (P) ((WrappedProvider) nativeProvider).provider();
        }
    }
}
