package net.momirealms.craftengine.bukkit.plugin.injector;

import com.mojang.serialization.MapCodec;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.modifier.FieldManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.matcher.ElementMatchers;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.ResourceKeyProxy;
import net.momirealms.craftengine.proxy.minecraft.util.RandomSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelReaderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.placement.PlacementContextProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.placement.PlacementFilterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.placement.PlacementModifierProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.placement.PlacementModifierTypeProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.constructor.SConstructor1;
import net.momirealms.sparrow.reflection.constructor.matcher.ConstructorMatcher;
import net.momirealms.sparrow.reflection.method.matcher.MethodMatcher;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

public final class BiomeFilterGenerator {
    public static final Constructor<?> constructor$PlacementFilter = requireNonNull(
            (VersionHelper.isOrAbove26_3 ? SparrowClass.of(Object.class) : SparrowClass.of(PlacementFilterProxy.CLASS)).getDeclaredConstructor(ConstructorMatcher.takeArguments(new Class<?>[0]))
    );
    public static final Method method$PlacementFilter$shouldPlace = requireNonNull(
            SparrowClass.of(PlacementFilterProxy.CLASS).getDeclaredMethod(MethodMatcher.takeArguments(PlacementContextProxy.CLASS, RandomSourceProxy.CLASS, BlockPosProxy.CLASS).and(MethodMatcher.returnType(boolean.class)))
    );
    public static final Method method$PlacementModifier$type = requireNonNull(
            SparrowClass.of(PlacementModifierProxy.CLASS).getDeclaredMethod(MethodMatcher.takeArguments(new Class<?>[0]).and(MethodMatcher.returnType(VersionHelper.isOrAbove26_3 ? MapCodec.class : PlacementModifierTypeProxy.CLASS)))
    );
    private static SConstructor1 constructor$CraftEngineBiomeFilter;
    private static Object placementModifierType$BIOME_FILTER;

    private BiomeFilterGenerator() {}

    public static void init() {
        ByteBuddy byteBuddy = new ByteBuddy(ClassFileVersion.JAVA_V21);
        placementModifierType$BIOME_FILTER = PlacementModifierTypeProxy.INSTANCE.getBiomeFilter();
        String packageWithName = BiomeFilterGenerator.class.getName();
        String generatedClassName = packageWithName.substring(0, packageWithName.lastIndexOf('.')) + ".CraftEngineBiomeFilter";
        Class<?> clazz$CraftEngineBiomeFilter = byteBuddy
                .subclass(PlacementFilterProxy.CLASS)
                .name(generatedClassName)
                .defineField("filter", Predicate.class, Visibility.PRIVATE, FieldManifestation.FINAL)
                .defineConstructor(Visibility.PUBLIC)
                .withParameters(Predicate.class)
                .intercept(MethodCall.invoke(constructor$PlacementFilter).andThen(FieldAccessor.ofField("filter").setsArgumentAt(0)))
                .method(ElementMatchers.is(method$PlacementFilter$shouldPlace))
                .intercept(MethodDelegation.to(ShouldPlaceInterceptor.class))
                .method(ElementMatchers.is(method$PlacementModifier$type))
                .intercept(MethodDelegation.to(TypeInterceptor.class))
                .make()
                .load(BiomeFilterGenerator.class.getClassLoader())
                .getLoaded();
        constructor$CraftEngineBiomeFilter = SparrowClass.of(clazz$CraftEngineBiomeFilter)
                .getSparrowConstructor(ConstructorMatcher.takeArguments(Predicate.class))
                .asm$1();
    }

    public static Object createBiomePlacementFilter(Predicate<Key> filter) {
        return constructor$CraftEngineBiomeFilter.newInstance(filter);
    }

    public static class TypeInterceptor {

        private TypeInterceptor() {}

        @RuntimeType
        public static Object intercept() {
            return placementModifierType$BIOME_FILTER;
        }
    }

    public static class ShouldPlaceInterceptor {

        private ShouldPlaceInterceptor() {}

        public static boolean intercept(@FieldValue("filter") Predicate<Key> filter,
                                        @Argument(0) Object context,
                                        @Argument(2) Object pos) {
            Object level = PlacementContextProxy.INSTANCE.getLevel(context);
            Object biomeHolder = LevelReaderProxy.INSTANCE.getBiome(level, pos);
            Object resourceKey = HolderProxy.ReferenceProxy.INSTANCE.getKey(biomeHolder);
            Object identifier = ResourceKeyProxy.INSTANCE.getIdentifier(resourceKey);
            Key biomeId = Key.of(IdentifierProxy.INSTANCE.getNamespace(identifier), IdentifierProxy.INSTANCE.getPath(identifier));
            return filter.test(biomeId);
        }
    }
}
