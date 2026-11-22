package net.momirealms.craftengine.bukkit.plugin.injector;

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
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.momirealms.craftengine.core.world.SectionPos;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import net.momirealms.craftengine.core.world.chunk.CESection;
import net.momirealms.craftengine.core.world.chunk.InjectedStorage;
import net.momirealms.craftengine.proxy.minecraft.network.FriendlyByteBufProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockStateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.LevelChunkSectionProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.PalettedContainerProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.constructor.SConstructor2;
import net.momirealms.sparrow.reflection.constructor.SConstructor3;
import net.momirealms.sparrow.reflection.constructor.matcher.ConstructorMatcher;
import net.momirealms.sparrow.reflection.field.matcher.FieldMatcher;
import net.momirealms.sparrow.reflection.method.matcher.MethodMatcher;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.namedOneOf;

final class ChunkStorageGenerator {
    private ChunkStorageGenerator() {
    }

    static Class<? extends PaletteBridge> paletteClass() {
        return PaletteClass.TYPE;
    }

    static InjectedStorage.Palette createNativePalette(Object initialState, Object strategy, Object[] presetValues) {
        return (InjectedStorage.Palette) NativePaletteClass.CONSTRUCTOR.newInstance(initialState, strategy, presetValues);
    }

    static InjectedStorage.Section createSection(Object states, Object biomes) {
        return (InjectedStorage.Section) SectionClass.CONSTRUCTOR.newInstance(states, biomes);
    }

    private static SConstructor3 generateNativePalette() {
        Class<?> base = PalettedContainerProxy.CLASS;
        SparrowClass<?> nativeType = SparrowClass.of(base);
        Class<?> strategy = requireNonNull(PalettedContainerProxy.StrategyProxy.CLASS);
        Class<?>[] parameters = {Object.class, strategy, Object[].class};
        Constructor<?> withPresets = nativeType.getDeclaredConstructor(ConstructorMatcher.takeArguments(parameters));
        MethodCall constructor;
        if (withPresets != null) {
            constructor = MethodCall.invoke(withPresets).withAllArguments();
        } else {
            Constructor<?> parent = requireNonNull(nativeType.getDeclaredConstructor(ConstructorMatcher.takeArguments(Object.class, strategy)));
            constructor = MethodCall.invoke(parent).withArgument(0, 1);
        }

        Method exchange = requireNonNull(method(nativeType, "getAndSet", int.class, int.class, int.class, Object.class));
        Method uncheckedExchange = requireNonNull(method(nativeType, "getAndSetUnchecked", int.class, int.class, int.class, Object.class));
        Advice advice = Advice.to(BlockWriteAdvice.class);
        Class<?> generated = load(storageBuilder(base, "NativePalettedContainer", InjectedStorage.Palette.class)
                .defineConstructor(Visibility.PUBLIC).withParameters(parameters).intercept(constructor)
                .method(namedOneOf("delegated")).intercept(FixedValue.self())
                .method(is(exchange)).intercept(advice.wrap(MethodCall.invoke(exchange).onSuper().withAllArguments()))
                .method(is(uncheckedExchange)).intercept(advice.wrap(MethodCall.invoke(uncheckedExchange).onSuper().withAllArguments())));
        return SparrowClass.of(generated).getSparrowConstructor(ConstructorMatcher.takeArguments(parameters)).asm$3();
    }

    private static Class<? extends PaletteBridge> generatePalette() {
        Class<?> base = PalettedContainerProxy.CLASS;
        SparrowClass<?> nativeType = SparrowClass.of(base);
        Method indexedGet = requireNonNull(method(nativeType, "get", int.class));
        Method coordinateGet = requireNonNull(method(nativeType, "get", int.class, int.class, int.class));
        Method indexedVirtualGet = method(nativeType, "getVirtual", int.class);
        Method coordinateVirtualGet = method(nativeType, "getVirtual", int.class, int.class, int.class);
        Method exchange = requireNonNull(method(nativeType, "getAndSet", int.class, int.class, int.class, Object.class));
        Method uncheckedExchange = requireNonNull(method(nativeType, "getAndSetUnchecked", int.class, int.class, int.class, Object.class));
        Field data = requireNonNull(nativeType.getDeclaredField(FieldMatcher.named("data")));
        boolean mirrorData = !Modifier.isPrivate(data.getModifiers());
        Set<Method> mutations = paletteMutators(nativeType);
        Advice exchangeAdvice = Advice.withCustomMapping().bind(MirrorData.class, mirrorData).to(ExchangeAdvice.class);
        Advice syncAdvice = Advice.to(SyncPaletteAdvice.class);

        DynamicType.Builder<?> builder = storageBuilder(base, "InjectedPalettedContainer", PaletteBridge.class)
                .defineField("delegate", base, Visibility.PRIVATE)
                .method(namedOneOf("delegated", "setDelegate"))
                .intercept(FieldAccessor.ofField("delegate").withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC));
        // The palette stores its data in the delegate. Fork-specific virtual methods must use that same receiver.
        for (Method candidate : base.getDeclaredMethods()) {
            int modifiers = candidate.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers) || candidate.isSynthetic() || (!Modifier.isPublic(modifiers) && !candidate.equals(indexedGet))) {
                continue;
            }
            Implementation.Composable forward;
            if (candidate.equals(indexedVirtualGet)) {
                forward = forwardPalette(indexedGet);
            } else if (candidate.equals(coordinateVirtualGet)) {
                forward = forwardPalette(coordinateGet);
            } else {
                forward = forwardPalette(candidate);
            }
            Implementation implementation = forward;
            if (candidate.equals(exchange) || candidate.equals(uncheckedExchange)) {
                implementation = exchangeAdvice.wrap(forward);
            } else if (mirrorData && mutations.contains(candidate)) {
                implementation = syncAdvice.wrap(forward);
            }
            builder = builder.method(is(candidate)).intercept(implementation);
        }
        return load(builder).asSubclass(PaletteBridge.class);
    }

    private static Set<Method> paletteMutators(SparrowClass<?> type) {
        return Stream.of(
                method(type, "set", int.class, int.class, int.class, Object.class),
                method(type, "set", int.class, Object.class),
                method(type, "read", FriendlyByteBufProxy.CLASS),
                method(type, "onResize", int.class, Object.class)
        ).filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
    }

    private static Implementation.Composable forwardPalette(Method method) {
        if (Modifier.isPublic(method.getModifiers())) {
            return MethodCall.invoke(method).onField("delegate").withAllArguments();
        }
        Method getByIndex = requireNonNull(method(SparrowClass.of(ChunkStorageAdapters.class), "getByIndex", Object.class, int.class));
        return MethodCall.invoke(getByIndex).withField("delegate").withAllArguments();
    }

    private static SConstructor2 generateSection() {
        Class<?> base = LevelChunkSectionProxy.CLASS;
        SparrowClass<?> nativeType = SparrowClass.of(base);
        Class<?> palette = PalettedContainerProxy.CLASS;
        Class<?>[] arguments = {palette, palette};
        Constructor<?> parent = requireNonNull(nativeType.getDeclaredConstructor(ConstructorMatcher.takeArguments(arguments)));
        Method recalc = requireNonNull(method(nativeType, "recalcBlockCounts"));
        Method set = requireNonNull(method(nativeType, "setBlockState", int.class, int.class, int.class, BlockStateProxy.CLASS, boolean.class));
        Class<?> generated = load(storageBuilder(base, "InjectedLevelChunkSection", InjectedStorage.Section.class)
                .defineField("initialized", boolean.class, Visibility.PRIVATE, FieldManifestation.FINAL)
                .defineConstructor(Visibility.PUBLIC).withParameters(arguments)
                .intercept(MethodCall.invoke(parent).withAllArguments()
                        .andThen(FieldAccessor.ofField("initialized").setsValue(true)))
                .method(is(recalc)).intercept(Advice.to(RecalcAdvice.class).wrap(MethodCall.invoke(recalc).onSuper()))
                .method(is(set)).intercept(Advice.to(BlockWriteAdvice.class).wrap(MethodCall.invoke(set).onSuper().withAllArguments())));
        return SparrowClass.of(generated).getSparrowConstructor(ConstructorMatcher.takeArguments(arguments)).asm$2();
    }

    private static DynamicType.Builder<?> storageBuilder(Class<?> base, String name, Class<? extends InjectedStorage> marker) {
        return new ByteBuddy(ClassFileVersion.JAVA_V21)
                .subclass(base, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .name(ChunkStorageGenerator.class.getPackageName() + "." + name)
                .implement(marker)
                .defineField("active", boolean.class, Visibility.PRIVATE)
                .defineField("section", CESection.class, Visibility.PRIVATE)
                .defineField("chunk", CEChunk.class, Visibility.PRIVATE)
                .defineField("pos", SectionPos.class, Visibility.PRIVATE)
                .method(namedOneOf("isActive", "setActive")).intercept(FieldAccessor.ofField("active"))
                .method(namedOneOf("section", "setSection")).intercept(FieldAccessor.ofField("section"))
                .method(namedOneOf("chunk", "setChunk")).intercept(FieldAccessor.ofField("chunk"))
                .method(namedOneOf("pos", "setPos")).intercept(FieldAccessor.ofField("pos"));
    }

    private static Method method(SparrowClass<?> owner, String name, Class<?>... arguments) {
        return owner.getDeclaredMethod(MethodMatcher.named(name).and(MethodMatcher.takeArguments(arguments)));
    }

    private static Class<?> load(DynamicType.Builder<?> builder) {
        return builder.make().load(ChunkStorageGenerator.class.getClassLoader(), ClassLoadingStrategy.UsingLookup.of(MethodHandles.lookup())).getLoaded();
    }

    interface PaletteBridge extends InjectedStorage.Palette {
        void setDelegate(Object target);
    }

    private static final class PaletteClass {
        private static final Class<? extends PaletteBridge> TYPE = generatePalette();
    }

    private static final class NativePaletteClass {
        private static final SConstructor3 CONSTRUCTOR = generateNativePalette();
    }

    private static final class SectionClass {
        private static final SConstructor2 CONSTRUCTOR = generateSection();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    private @interface MirrorData {
    }

    private static final class SyncPaletteAdvice {
        @Advice.OnMethodExit
        private static void finish(@Advice.This InjectedStorage.Palette palette) {
            ChunkStorageAdapters.syncPaletteData(palette);
        }
    }

    private static final class ExchangeAdvice {
        @Advice.OnMethodExit
        private static void finish(@Advice.This InjectedStorage.Palette palette, @Advice.FieldValue("active") boolean active,
                                   @Advice.Argument(0) int x,
                                   @Advice.Argument(1) int y,
                                   @Advice.Argument(2) int z,
                                   @Advice.Argument(3) Object state,
                                   @Advice.Return Object previous,
                                   @MirrorData boolean mirrorData) {
            if (mirrorData) {
                ChunkStorageAdapters.syncPaletteData(palette);
            }
            if (active) {
                WorldStorageInjector.compareAndUpdateBlockState(x, y, z, state, previous, palette);
            }
        }
    }

    private static final class RecalcAdvice {
        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        private static boolean skip(@Advice.FieldValue("initialized") boolean initialized) {
            return !initialized;
        }
    }

    private static final class BlockWriteAdvice {
        @Advice.OnMethodExit
        private static void finish(@Advice.This InjectedStorage storage, @Advice.FieldValue("active") boolean active,
                                   @Advice.Argument(0) int x, @Advice.Argument(1) int y, @Advice.Argument(2) int z,
                                   @Advice.Argument(3) Object state, @Advice.Return Object previous) {
            if (active) {
                WorldStorageInjector.compareAndUpdateBlockState(x, y, z, state, previous, storage);
            }
        }
    }
}
