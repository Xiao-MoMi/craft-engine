package net.momirealms.craftengine.bukkit.plugin.injector;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.modifier.FieldManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.matcher.ElementMatchers;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.WorldHeight;
import net.momirealms.craftengine.core.world.WorldHolder;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.CraftWorldBorderProxy;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.CraftWorldProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.constructor.SConstructor5;
import net.momirealms.sparrow.reflection.constructor.matcher.ConstructorMatcher;
import org.bukkit.World;

import java.lang.reflect.Constructor;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public final class WorldBorderGenerator {
    private static final SConstructor5 CONSTRUCTOR = generate();

    private WorldBorderGenerator() {
    }

    public static BukkitWorld create(World world) {
        Object serverLevel = CraftWorldProxy.INSTANCE.getWorld(world);
        int minHeight = world.getMinHeight();
        WorldHeight worldHeight = WorldHeight.create(minHeight, world.getMaxHeight() - minHeight);
        return (BukkitWorld) CONSTRUCTOR.newInstance(world, serverLevel, worldHeight, world.getUID(), world.getName());
    }

    private static SConstructor5 generate() {
        Class<?> craftWorld = CraftWorldProxy.CLASS;
        Class<?> craftWorldBorder = CraftWorldBorderProxy.CLASS;
        Constructor<?> borderConstructor = requireNonNull(SparrowClass.of(craftWorldBorder)
                .getDeclaredConstructor(ConstructorMatcher.takeArguments(craftWorld)));
        Class<?>[] parameters = {craftWorld, Object.class, WorldHeight.class, UUID.class, String.class};
        Class<?> generated = InjectorClassLoading.load(new ByteBuddy(ClassFileVersion.JAVA_V21)
                .subclass(craftWorldBorder, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .name(WorldBorderGenerator.class.getPackageName() + ".InjectedWorldBorder")
                .implement(BukkitWorld.class, WorldHolder.class)
                .defineField("craftWorld", World.class, Visibility.PRIVATE, FieldManifestation.FINAL)
                .defineField("serverLevel", Object.class, Visibility.PRIVATE, FieldManifestation.FINAL)
                .defineField("worldHeight", WorldHeight.class, Visibility.PRIVATE, FieldManifestation.FINAL)
                .defineField("uuid", UUID.class, Visibility.PRIVATE, FieldManifestation.FINAL)
                .defineField("name", String.class, Visibility.PRIVATE, FieldManifestation.FINAL)
                .defineField("storageWorld", CEWorld.class, Visibility.PRIVATE)
                .defineConstructor(Visibility.PUBLIC)
                .withParameters(parameters)
                .intercept(MethodCall.invoke(borderConstructor).withArgument(0)
                        .andThen(FieldAccessor.ofField("craftWorld").setsArgumentAt(0))
                        .andThen(FieldAccessor.ofField("serverLevel").setsArgumentAt(1))
                        .andThen(FieldAccessor.ofField("worldHeight").setsArgumentAt(2))
                        .andThen(FieldAccessor.ofField("uuid").setsArgumentAt(3))
                        .andThen(FieldAccessor.ofField("name").setsArgumentAt(4)))
                .method(ElementMatchers.named("platformWorld"))
                .intercept(FieldAccessor.ofField("craftWorld"))
                .method(ElementMatchers.named("minecraftWorld"))
                .intercept(FieldAccessor.ofField("serverLevel"))
                .method(ElementMatchers.named("worldHeight"))
                .intercept(FieldAccessor.ofField("worldHeight"))
                .method(ElementMatchers.named("uuid"))
                .intercept(FieldAccessor.ofField("uuid"))
                .method(ElementMatchers.named("name"))
                .intercept(FieldAccessor.ofField("name"))
                .method(ElementMatchers.named("storageWorld").or(ElementMatchers.named("setStorageWorld")))
                .intercept(FieldAccessor.ofField("storageWorld")));
        return SparrowClass.of(generated).getSparrowConstructor(ConstructorMatcher.takeArguments(parameters)).asm$5();
    }
}
