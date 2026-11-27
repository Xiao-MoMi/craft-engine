package net.momirealms.craftengine.bukkit.world.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.modifier.FieldManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.util.RandomSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.WorldGenLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.ChunkGeneratorProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.feature.FeaturePlaceContextProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.feature.FeatureProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.feature.SimpleBlockFeatureProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.constructor.SConstructor1;
import net.momirealms.sparrow.reflection.constructor.matcher.ConstructorMatcher;
import net.momirealms.sparrow.reflection.method.matcher.MethodMatcher;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static java.util.Objects.requireNonNull;
import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.named;

public final class SimpleBlockFeatureAdapter {
    private static final Object TYPE = VersionHelper.isOrAbove26_3 ? createCodec() : createLegacy();

    private SimpleBlockFeatureAdapter() {
    }

    public static Object type() {
        return TYPE;
    }

    private static Object createLegacy() {
        Constructor<?> parent = requireNonNull(SparrowClass.of(FeatureProxy.CLASS).getDeclaredConstructor(ConstructorMatcher.takeArguments(Codec.class)));
        Method place = method(FeatureProxy.CLASS, "place", FeaturePlaceContextProxy.CLASS);
        Method delegate = method(CustomSimpleBlockFeature.class, "placeLegacy", Object.class);
        Class<?> generated = load(subclass()
                .defineConstructor(Visibility.PUBLIC).withParameters(Codec.class)
                .intercept(MethodCall.invoke(parent).withAllArguments())
                .method(is(place)).intercept(MethodCall.invoke(delegate).withAllArguments()));
        return SparrowClass.of(generated).getSparrowConstructor(ConstructorMatcher.takeArguments(Codec.class))
                .asm$1().newInstance(SimpleBlockFeatureProxy.ConfigurationProxy.INSTANCE.codec());
    }

    private static MapCodec<Object> createCodec() {
        Constructor<?> parent = requireNonNull(SparrowClass.of(Object.class).getDeclaredConstructor(ConstructorMatcher.takeArguments(new Class<?>[0])));
        Method place = method(FeatureProxy.CLASS, "place", WorldGenLevelProxy.CLASS, ChunkGeneratorProxy.CLASS, RandomSourceProxy.CLASS, BlockPosProxy.CLASS);
        Method delegate = method(CustomSimpleBlockFeature.class, "placeModern", Object.class, Object.class, Object.class, Object.class);
        Method type = method(SimpleBlockFeatureAdapter.class, "type");
        Class<?> generated = load(subclass()
                .implement(ConfiguredBlockFeature.class)
                .defineField("configuration", Object.class, Visibility.PRIVATE, FieldManifestation.FINAL)
                .defineConstructor(Visibility.PUBLIC).withParameters(Object.class)
                .intercept(MethodCall.invoke(parent).andThen(FieldAccessor.ofField("configuration").setsArgumentAt(0)))
                .method(named("configuration")).intercept(FieldAccessor.ofField("configuration"))
                .method(is(method(FeatureProxy.CLASS, "codec")))
                .intercept(MethodCall.invoke(type).withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
                .method(is(place)).intercept(MethodCall.invoke(delegate).withField("configuration").withArgument(0, 2, 3)));
        SConstructor1 constructor = SparrowClass.of(generated).getSparrowConstructor(ConstructorMatcher.takeArguments(Object.class)).asm$1();
        return SimpleBlockFeatureProxy.INSTANCE.codec().xmap(constructor::newInstance, feature -> ((ConfiguredBlockFeature) feature).configuration());
    }

    private static DynamicType.Builder<?> subclass() {
        return new ByteBuddy(ClassFileVersion.JAVA_V21)
                .subclass(FeatureProxy.CLASS, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .name(SimpleBlockFeatureAdapter.class.getPackageName() + ".NativeSimpleBlockFeature");
    }

    private static Class<?> load(DynamicType.Builder<?> builder) {
        return builder.make().load(SimpleBlockFeatureAdapter.class.getClassLoader(), ClassLoadingStrategy.UsingLookup.of(MethodHandles.lookup())).getLoaded();
    }

    private static Method method(Class<?> owner, String name, Class<?>... parameters) {
        return requireNonNull(SparrowClass.of(owner).getDeclaredMethod(MethodMatcher.named(name).and(MethodMatcher.takeArguments(parameters))),
                () -> owner.getName() + "." + name);
    }

    interface ConfiguredBlockFeature {
        Object configuration();
    }
}
