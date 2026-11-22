package net.momirealms.craftengine.bukkit.item.recipe;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.recipe.CustomBrewingRecipe;
import net.momirealms.craftengine.core.item.recipe.CustomShapedRecipe;
import net.momirealms.craftengine.core.item.recipe.CustomShapelessRecipe;
import net.momirealms.craftengine.core.item.recipe.Recipe;
import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.world.ContainerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.inventory.CraftingContainerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.crafting.CraftingInputProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.crafting.RecipeInputProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.method.matcher.MethodMatcher;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.named;

final class RecipeGenerator {
    private static final Map<Class<?>, Constructor<?>> GENERATED = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Constructor<?>> VANILLA = new ConcurrentHashMap<>();

    private RecipeGenerator() {
    }

    static Object create(Class<?> base, Kind kind, Recipe recipe, BiPredicate<Object, Object> roughMatch, Object... arguments) {
        Constructor<?> constructor = GENERATED.computeIfAbsent(base, type -> generate(type, kind, arguments));
        Adapter result = (Adapter) instantiate(constructor, arguments);
        result.setRecipeBehavior(new Behavior(recipe, kind.input, roughMatch));
        return result;
    }

    static Object vanilla(Class<?> base, Object... arguments) {
        return instantiate(VANILLA.computeIfAbsent(base, type -> findConstructor(type, arguments)), arguments);
    }

    private static Constructor<?> generate(Class<?> base, Kind kind, Object[] arguments) {
        Constructor<?> parent = findConstructor(base, arguments);
        Class<?> input = kind.inputClass();
        Class<?>[] matchParameters = kind == Kind.BREWING ? new Class<?>[] {input} : new Class<?>[] {input, LevelProxy.CLASS};
        Method matches = requireNonNull(findMethod(base, "matches", matchParameters), base.getName() + ".matches");
        DynamicType.Builder<?> builder = new ByteBuddy(ClassFileVersion.JAVA_V21)
                .subclass(base, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .name(RecipeGenerator.class.getPackageName() + ".Injected" + base.getSimpleName())
                .implement(Adapter.class)
                .defineField("recipeBehavior", Behavior.class, Visibility.PRIVATE)
                .method(named("setRecipeBehavior")).intercept(FieldAccessor.ofField("recipeBehavior"))
                .defineConstructor(Visibility.PUBLIC).withParameters(parent.getParameterTypes())
                .intercept(MethodCall.invoke(parent).withAllArguments());
        if (kind.nativeMatch()) {
            builder = builder.method(is(matches)).intercept(Advice.to(NativeMatch.class).wrap(MethodCall.invoke(matches).onSuper().withAllArguments()));
        } else {
            builder = builder.method(is(matches)).intercept(MethodDelegation.to(Match.class));
        }
        if (kind.crafting()) {
            Method remaining = findMethod(base, "getRemainingItems", input);
            if (remaining == null) {
                Class<?> genericInput = VersionHelper.isOrAbove1_21 ? RecipeInputProxy.CLASS : ContainerProxy.CLASS;
                remaining = findMethod(base, "getRemainingItems", genericInput);
            }
            builder = builder.method(is(requireNonNull(remaining, base.getName() + ".getRemainingItems")))
                    .intercept(MethodDelegation.to(RemainingItems.class));
        }
        if (kind == Kind.DYE || kind == Kind.BREWING) {
            Method assemble = requireNonNull(findMethod(base, "assemble", input), base.getName() + ".assemble");
            builder = builder.method(is(assemble)).intercept(MethodDelegation.to(Assemble.class));
        }
        if (kind == Kind.COOKING || kind == Kind.STONECUTTING || !VersionHelper.isOrAbove26_1) {
            Method notification = findMethod(base, "showNotification");
            if (notification != null) {
                builder = builder.method(is(notification)).intercept(MethodDelegation.to(ShowNotification.class));
            }
        }
        Class<?> generated = builder.make()
                .load(RecipeGenerator.class.getClassLoader(), ClassLoadingStrategy.UsingLookup.of(MethodHandles.lookup())).getLoaded();
        return findConstructor(generated, arguments);
    }

    static Method findMethod(Class<?> base, String name, Class<?>... parameters) {
        MethodMatcher matcher = MethodMatcher.named(name).and(MethodMatcher.takeArguments(parameters));
        for (Class<?> type = base; type != null; type = type.getSuperclass()) {
            Method method = SparrowClass.of(type).getDeclaredMethod(matcher);
            if (method != null) {
                return method;
            }
            for (Class<?> iface : type.getInterfaces()) {
                method = findMethod(iface, name, parameters);
                if (method != null) {
                    return method;
                }
            }
        }
        return null;
    }

    private static Constructor<?> findConstructor(Class<?> type, Object[] arguments) {
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            Class<?>[] parameters = constructor.getParameterTypes();
            if (parameters.length == arguments.length && accepts(parameters, arguments)) {
                constructor.setAccessible(true);
                return constructor;
            }
        }
        throw new IllegalArgumentException("No matching constructor for " + type.getName() + " with " + Arrays.toString(arguments));
    }

    private static boolean accepts(Class<?>[] parameters, Object[] arguments) {
        for (int index = 0; index < parameters.length; index++) {
            Class<?> type = parameters[index];
            Object value = arguments[index];
            if (value == null) {
                if (type.isPrimitive()) {
                    return false;
                }
            } else if (!(type.isPrimitive() ? MethodType.methodType(type).wrap().returnType() : type).isInstance(value)) {
                return false;
            }
        }
        return true;
    }

    private static Object instantiate(Constructor<?> constructor, Object[] arguments) {
        try {
            return constructor.newInstance(arguments);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot construct " + constructor.getDeclaringClass().getName(), exception);
        }
    }

    enum Kind {
        SHAPED(RecipeConversions::craftingInput),
        SHAPELESS(RecipeConversions::craftingInput),
        DYE(RecipeConversions::craftingInput),
        COOKING(RecipeConversions::singleInput),
        STONECUTTING(RecipeConversions::singleInput),
        SMITHING(RecipeConversions::smithingInput),
        BREWING(RecipeConversions::brewingInput);

        private final Function<Object, RecipeInput> input;

        Kind(Function<Object, RecipeInput> input) {
            this.input = input;
        }

        Class<?> inputClass() {
            if (this.crafting()) {
                return VersionHelper.isOrAbove1_21 ? CraftingInputProxy.CLASS : CraftingContainerProxy.CLASS;
            }
            if (!VersionHelper.isOrAbove1_21) {
                return ContainerProxy.CLASS;
            }
            return switch (this) {
                case SMITHING -> RecipeInputProxy.Smithing.CLASS;
                case BREWING -> RecipeInputProxy.Brewing.CLASS;
                default -> RecipeInputProxy.Single.CLASS;
            };
        }

        boolean crafting() {
            return this == SHAPED || this == SHAPELESS || this == DYE;
        }

        boolean nativeMatch() {
            return this == STONECUTTING || this == SMITHING || this == BREWING;
        }
    }

    interface Adapter {
        void setRecipeBehavior(Behavior behavior);
    }

    record Behavior(Recipe recipe, Function<Object, RecipeInput> converter, BiPredicate<Object, Object> roughMatch) {
        boolean matches(Object input) {
            return this.recipe.matches(this.converter.apply(input));
        }

        Object remainingItems(Object input) {
            if ((this.recipe instanceof CustomShapedRecipe shaped && shaped.ingredientCountSupport())
                    || (this.recipe instanceof CustomShapelessRecipe shapeless && shapeless.ingredientCountSupport())) {
                this.recipe.takeInput(this.converter.apply(input), 1);
            }
            return RecipeConversions.remainingItems(this.recipe.id(), input);
        }

        Object assemble(Object input) {
            if (this.recipe instanceof CustomBrewingRecipe brewing) {
                return brewing.result(ItemBuildContext.empty()).minecraftItem();
            }
            return this.recipe.assemble(this.converter.apply(input), ItemBuildContext.empty()).minecraftItem();
        }
    }

    public static final class Match {
        public static boolean matches(@FieldValue("recipeBehavior") Behavior behavior, @Argument(0) Object input, @Argument(1) Object level) {
            return behavior.roughMatch().test(input, level) && behavior.matches(input);
        }
    }

    private static final class NativeMatch {
        @Advice.OnMethodExit
        private static void matches(@Advice.FieldValue("recipeBehavior") Behavior behavior, @Advice.Argument(0) Object input,
                                    @Advice.Return(readOnly = false) boolean matches) {
            if (matches) {
                matches = behavior.matches(input);
            }
        }
    }

    public static final class RemainingItems {
        @RuntimeType
        public static Object remainingItems(@FieldValue("recipeBehavior") Behavior behavior, @Argument(0) Object input) {
            return behavior.remainingItems(input);
        }
    }

    public static final class Assemble {
        @RuntimeType
        public static Object assemble(@FieldValue("recipeBehavior") Behavior behavior, @Argument(0) Object input) {
            return behavior.assemble(input);
        }
    }

    public static final class ShowNotification {
        public static boolean showNotification(@FieldValue("recipeBehavior") Behavior behavior) {
            return behavior.recipe().showNotification();
        }
    }
}
