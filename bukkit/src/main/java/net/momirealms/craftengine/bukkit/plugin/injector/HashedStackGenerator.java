package net.momirealms.craftengine.bukkit.plugin.injector;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.modifier.FieldManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.matcher.ElementMatchers;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.network.ItemPacketSource;
import net.momirealms.craftengine.proxy.minecraft.network.HashedPatchMapProxy;
import net.momirealms.craftengine.proxy.minecraft.network.HashedStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.constructor.SConstructor2;
import net.momirealms.sparrow.reflection.constructor.matcher.ConstructorMatcher;
import net.momirealms.sparrow.reflection.method.matcher.MethodMatcher;

import java.lang.reflect.Method;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class HashedStackGenerator {
    public static final Method method$HashedStack$matches = requireNonNull(
            SparrowClass.of(HashedStackProxy.CLASS).getDeclaredMethod(MethodMatcher.takeArguments(ItemStackProxy.CLASS, HashedPatchMapProxy.HashGeneratorProxy.CLASS)
                    .and(MethodMatcher.returnType(boolean.class)))
    );
    private static SConstructor2 constructor$CraftEngineHashedStack;

    private HashedStackGenerator() {}

    public static void init() {
        ByteBuddy byteBuddy = new ByteBuddy(ClassFileVersion.JAVA_V17);
        String packageWithName = HashedStackGenerator.class.getName();
        String generatedClassName = packageWithName.substring(0, packageWithName.lastIndexOf('.')) + ".CraftEngineHashedStack";
        Class<?> clazz$CraftEngineHashedStack;
        try {
            clazz$CraftEngineHashedStack = byteBuddy
                    .subclass(Object.class)
                    .name(generatedClassName)
                    .defineField("hashedStack", Object.class, Visibility.PRIVATE, FieldManifestation.FINAL)
                    .defineField("player", Player.class, Visibility.PRIVATE, FieldManifestation.FINAL)
                    .defineConstructor(Visibility.PUBLIC)
                    .withParameters(Object.class, Player.class)
                    .intercept(MethodCall.invoke(Object.class.getDeclaredConstructor()).andThen(FieldAccessor.ofField("hashedStack").setsArgumentAt(0)).andThen(FieldAccessor.ofField("player").setsArgumentAt(1)))
                    .implement(HashedStackProxy.CLASS)
                    .method(ElementMatchers.is(method$HashedStack$matches))
                    .intercept(MethodDelegation.to(MatchesInterceptor.class))
                    .make()
                    .load(HashedStackGenerator.class.getClassLoader())
                    .getLoaded();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Failed to generate " + generatedClassName, e);
        }
        constructor$CraftEngineHashedStack = SparrowClass.of(clazz$CraftEngineHashedStack)
                .getSparrowConstructor(ConstructorMatcher.takeArguments(Object.class, Player.class))
                .asm$2();
    }

    public static Object create(Object hashedStack, Player player) {
        return constructor$CraftEngineHashedStack.newInstance(hashedStack, player);
    }

    public static class MatchesInterceptor {

        private MatchesInterceptor() {}

        public static boolean intercept(@FieldValue("hashedStack") Object hashedStack,
                                        @FieldValue("player") Player player,
                                        @Argument(0) Object stack,
                                        @Argument(1) Object hashGenerator) {
            if (HashedStackProxy.ActualItemProxy.CLASS.isInstance(hashedStack)
                    && HashedStackProxy.ActualItemProxy.INSTANCE.count(hashedStack) != ItemStackProxy.INSTANCE.getCount(stack)) {
                return false;
            }
            if (!ItemStackProxy.INSTANCE.isEmpty(stack)) {
                Optional<Item> optional = BukkitItemManager.instance().s2c(ItemStackUtils.wrap(ItemStackProxy.INSTANCE.copy(stack)), player, ItemPacketSource.GENERIC);
                if (optional.isPresent()) {
                    stack = optional.get().minecraftItem();
                }
            }
            return HashedStackProxy.INSTANCE.matches(hashedStack, stack, hashGenerator);
        }
    }
}
