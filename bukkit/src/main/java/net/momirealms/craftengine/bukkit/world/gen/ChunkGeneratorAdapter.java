package net.momirealms.craftengine.bukkit.world.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.modifier.FieldManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.chunk.ChunkGenerationStage;
import net.momirealms.craftengine.proxy.minecraft.world.level.biome.BiomeSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.ChunkAccessProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.ChunkGeneratorProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.constructor.SConstructor3;
import net.momirealms.sparrow.reflection.constructor.matcher.ConstructorMatcher;
import net.momirealms.sparrow.reflection.method.matcher.MethodMatcher;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;
import static net.bytebuddy.matcher.ElementMatchers.is;

public final class ChunkGeneratorAdapter {
    private static final SparrowClass<?> BASE = SparrowClass.of(ChunkGeneratorProxy.CLASS);
    private static final SConstructor3 CONSTRUCTOR = generate();

    private ChunkGeneratorAdapter() {
    }

    public static InjectedChunkGenerator create(BukkitWorld world, Object target) {
        Object biomeSource = ChunkGeneratorProxy.INSTANCE.biomeSource(target);
        return (InjectedChunkGenerator) CONSTRUCTOR.newInstance(biomeSource, target, new InjectedCustomChunkGenerator(world, biomeSource));
    }

    private static SConstructor3 generate() {
        Class<?>[] parameters = {BiomeSourceProxy.CLASS, ChunkGeneratorProxy.CLASS, InjectedCustomChunkGenerator.class};
        Constructor<?> parent = requireNonNull(BASE.getDeclaredConstructor(ConstructorMatcher.takeArguments(BiomeSourceProxy.CLASS)));
        Codec<Object> codec = ChunkGeneratorProxy.INSTANCE.codec();
        Object codecValue = VersionHelper.isOrAbove1_20_5 ? MapCodec.assumeMapUnsafe(codec) : codec;
        DynamicType.Builder<?> builder = new ByteBuddy(ClassFileVersion.JAVA_V21)
                .subclass(ChunkGeneratorProxy.CLASS, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .name(ChunkGeneratorAdapter.class.getPackageName() + ".NativeCustomChunkGenerator")
                .implement(InjectedChunkGenerator.class)
                .defineField("target", ChunkGeneratorProxy.CLASS, Visibility.PRIVATE, FieldManifestation.FINAL)
                .defineField("logic", InjectedCustomChunkGenerator.class, Visibility.PRIVATE, FieldManifestation.FINAL)
                .defineConstructor(Visibility.PUBLIC).withParameters(parameters)
                .intercept(MethodCall.invoke(parent).withArgument(0)
                        .andThen(FieldAccessor.ofField("target").setsArgumentAt(1))
                        .andThen(FieldAccessor.ofField("logic").setsArgumentAt(2)))
                .method(is(method("codec", 0))).intercept(FixedValue.reference(codecValue));
        builder = forwardVanillaMethods(builder);
        builder = adaptStages(builder);
        Class<?> generated = builder.make()
                .load(ChunkGeneratorAdapter.class.getClassLoader(), ClassLoadingStrategy.UsingLookup.of(MethodHandles.lookup()))
                .getLoaded();
        return SparrowClass.of(generated).getSparrowConstructor(ConstructorMatcher.takeArguments(parameters)).asm$3();
    }

    private static DynamicType.Builder<?> forwardVanillaMethods(DynamicType.Builder<?> builder) {
        builder = forward(builder, "spawnOriginalMobs", 1);
        builder = forward(builder, "getGenDepth", 0);
        builder = forward(builder, "getSeaLevel", 0);
        builder = forward(builder, "getMinY", 0);
        builder = forward(builder, "getBaseHeight", 5);
        builder = forward(builder, "getBaseColumn", 4);
        builder = forward(builder, "addDebugScreenInfo", VersionHelper.isOrAbove26_3 ? 4 : 3);
        builder = forward(builder, "createState", 4);
        builder = forward(builder, "getTypeNameForDataFixer", 0);
        builder = forward(builder, "createBiomes", VersionHelper.isOrAbove1_21 ? 4 : 5);
        builder = forward(builder, "findNearestMapStructure", 5);
        builder = forward(builder, "addVanillaDecorations", 3);
        builder = forward(builder, "applyBiomeDecoration", 4);
        builder = forward(builder, "getSpawnHeight", 1);
        builder = forward(builder, "getBiomeSource", 0);
        builder = forward(builder, "getMobsAt", 4);
        builder = forward(builder, "createReferences", 3);
        builder = forward(builder, "getFirstFreeHeight", 5);
        builder = forward(builder, "getFirstOccupiedHeight", 5);
        if (VersionHelper.isOrAbove1_20_5) {
            builder = forward(builder, "validate", 0);
        }
        if (VersionHelper.isOrAbove26_3) {
            builder = forward(builder, "getOrigin", 1);
        }
        return builder;
    }

    private static DynamicType.Builder<?> adaptStages(DynamicType.Builder<?> builder) {
        Method decoration = method("applyBiomeDecoration", 3);
        builder = builder.method(is(decoration)).intercept(Advice.to(DecorationAdvice.class).wrap(forward(decoration)));
        builder = stage(builder, method("createStructures", VersionHelper.isOrAbove1_21_4 ? 6 : 5), 3, ChunkGenerationStage.STRUCTURE);
        if (VersionHelper.isOrAbove26_3) {
            return terrain(builder, method("buildTerrain", 7), 0);
        }
        builder = stage(builder, method("applyCarvers", VersionHelper.isOrAbove1_21_2 ? 6 : 7), 5, ChunkGenerationStage.CARVER);
        builder = stage(builder, method("buildSurface", 4), 3, ChunkGenerationStage.SURFACE);
        int noiseArguments = VersionHelper.isOrAbove1_21 ? 4 : 5;
        return terrain(builder, method("fillFromNoise", noiseArguments), noiseArguments - 1);
    }

    private static DynamicType.Builder<?> stage(DynamicType.Builder<?> builder, Method method, int chunkIndex, ChunkGenerationStage stage) {
        Advice advice = chunkArgument(method, chunkIndex).bind(Stage.class, stage).to(StageAdvice.class);
        return builder.method(is(method)).intercept(advice.wrap(forward(method)));
    }

    private static DynamicType.Builder<?> terrain(DynamicType.Builder<?> builder, Method method, int chunkIndex) {
        Advice advice = chunkArgument(method, chunkIndex).to(TerrainAdvice.class);
        return builder.method(is(method)).intercept(advice.wrap(forward(method)));
    }

    private static Advice.WithCustomMapping chunkArgument(Method method, int index) {
        if (method.getParameterTypes()[index] != ChunkAccessProxy.CLASS) {
            throw new IllegalStateException("Expected ChunkAccess in " + method);
        }
        return Advice.withCustomMapping().bind(ChunkArgument.class, method, index);
    }

    private static DynamicType.Builder<?> forward(DynamicType.Builder<?> builder, String name, int arguments) {
        Method method = method(name, arguments);
        return builder.method(is(method)).intercept(forward(method));
    }

    private static Implementation.Composable forward(Method method) {
        return MethodCall.invoke(method).onField("target").withAllArguments();
    }

    private static Method method(String name, int arguments) {
        return requireNonNull(BASE.getDeclaredMethod(MethodMatcher.named(name).and(method -> !method.isBridge() && method.getParameterCount() == arguments)),
                () -> "Missing ChunkGenerator." + name + " (" + arguments + " arguments)");
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    private @interface ChunkArgument {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    private @interface Stage {
    }

    private static final class StageAdvice {
        @Advice.OnMethodEnter
        private static Runnable begin(@Advice.FieldValue("logic") InjectedCustomChunkGenerator logic,
                                      @ChunkArgument Object chunk,
                                      @Stage ChunkGenerationStage stage) {
            return logic.beginStage(chunk, stage);
        }

        @Advice.OnMethodExit
        private static void finish(@Advice.Enter Runnable save) {
            InjectedCustomChunkGenerator.finishStage(save);
        }
    }

    private static final class TerrainAdvice {
        @Advice.OnMethodEnter
        private static Runnable begin(@Advice.FieldValue("logic") InjectedCustomChunkGenerator logic,
                                      @ChunkArgument Object chunk) {
            return logic.beginTerrain(chunk);
        }

        @Advice.OnMethodExit
        private static void finish(@Advice.Enter Runnable save, @Advice.Return(readOnly = false) CompletableFuture<?> terrain) {
            terrain = InjectedCustomChunkGenerator.finishTerrain(terrain, save);
        }
    }

    private static final class DecorationAdvice {
        @Advice.OnMethodEnter
        private static Runnable begin(@Advice.FieldValue("logic") InjectedCustomChunkGenerator logic,
                                      @Advice.Argument(1) Object chunk) {
            return logic.beginStage(chunk, ChunkGenerationStage.FEATURE);
        }

        @Advice.OnMethodExit
        private static void finish(@Advice.FieldValue("logic") InjectedCustomChunkGenerator logic,
                                   @Advice.This Object generator, @Advice.Argument(0) Object level,
                                   @Advice.Argument(1) Object chunk, @Advice.Enter Runnable save) {
            logic.finishDecoration(generator, level, chunk, save);
        }
    }
}