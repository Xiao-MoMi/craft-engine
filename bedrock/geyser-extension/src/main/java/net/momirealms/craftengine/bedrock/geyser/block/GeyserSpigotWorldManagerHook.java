package net.momirealms.craftengine.bedrock.geyser.block;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.modifier.FieldManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodCall;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.level.WorldManager;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.IntUnaryOperator;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

final class GeyserSpigotWorldManagerHook {
    private static final String SPIGOT_PLUGIN = "org.geysermc.geyser.platform.spigot.GeyserSpigotPlugin";
    private static final String NATIVE_WORLD_MANAGER = "org.geysermc.geyser.platform.spigot.world.manager.GeyserSpigotNativeWorldManager";
    private final GeyserBootstrap bootstrap;
    private final Class<?> pluginType;
    private final Field worldManagerField;
    private final IntUnaryOperator visualState;
    private WorldManager original;
    private WorldManager installed;
    private boolean closed;

    private GeyserSpigotWorldManagerHook(GeyserBootstrap bootstrap, Class<?> pluginType, Field worldManagerField, IntUnaryOperator visualState) {
        this.bootstrap = bootstrap;
        this.pluginType = pluginType;
        this.worldManagerField = worldManagerField;
        this.visualState = visualState;
    }

    @Nullable
    static GeyserSpigotWorldManagerHook install(GeyserBootstrap bootstrap, IntUnaryOperator visualState) {
        ClassLoader loader = bootstrap.getClass().getClassLoader();
        try {
            Class<?> spigotPlugin = Class.forName(SPIGOT_PLUGIN, false, loader);
            if (!spigotPlugin.isInstance(bootstrap)) {
                return null;
            }
            WorldManager current = bootstrap.getWorldManager();
            if (current != null && !isNative(current)) {
                return null;
            }
            Class<?> pluginType = Class.forName("org.bukkit.plugin.Plugin", false, loader);
            Field field = spigotPlugin.getDeclaredField("geyserWorldManager");
            field.setAccessible(true);
            GeyserSpigotWorldManagerHook hook = new GeyserSpigotWorldManagerHook(bootstrap, pluginType, field, visualState);
            if (current == null) {
                // Post-initialize runs inside GeyserImpl.start(), before Spigot assigns its world manager.
                hook.scheduleInstall();
            } else {
                hook.replaceWorldManager();
            }
            return hook;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            bootstrap.getGeyserLogger().error("Could not install CraftEngine's Spigot world manager", exception);
            return null;
        }
    }

    private void scheduleInstall() throws ReflectiveOperationException {
        Object server = this.pluginType.getMethod("getServer").invoke(this.bootstrap);
        Class<?> serverType = Class.forName("org.bukkit.Server", false, this.pluginType.getClassLoader());
        Method scheduler;
        try {
            scheduler = serverType.getMethod("getGlobalRegionScheduler");
        } catch (NoSuchMethodException exception) {
            scheduler = serverType.getMethod("getScheduler");
            scheduler.getReturnType().getMethod("runTask", this.pluginType, Runnable.class).invoke(scheduler.invoke(server), this.bootstrap, (Runnable) this::replaceWorldManager);
            return;
        }
        scheduler.getReturnType().getMethod("execute", this.pluginType, Runnable.class).invoke(scheduler.invoke(server), this.bootstrap, (Runnable) this::replaceWorldManager);
    }

    synchronized void uninstall() {
        this.closed = true;
        if (this.installed == null || this.bootstrap.getWorldManager() != this.installed) {
            return;
        }
        try {
            this.worldManagerField.set(this.bootstrap, this.original);
        } catch (IllegalAccessException exception) {
            this.bootstrap.getGeyserLogger().error("Could not restore Geyser's Spigot world manager", exception);
        }
    }

    private synchronized void replaceWorldManager() {
        if (this.closed) {
            return;
        }
        WorldManager current = this.bootstrap.getWorldManager();
        if (current == null || !isNative(current)) {
            return;
        }
        try {
            Constructor<? extends WorldManager> constructor = wrapperConstructor(this.worldManagerField.getType().asSubclass(WorldManager.class), this.pluginType);
            WorldManager replacement = constructor.newInstance(this.bootstrap, new GeyserWorldManager(current, this.visualState));
            this.worldManagerField.set(this.bootstrap, replacement);
            this.original = current;
            this.installed = replacement;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            this.bootstrap.getGeyserLogger().error("Could not replace Geyser's Spigot world manager", exception);
        }
    }

    private static boolean isNative(WorldManager manager) {
        return manager.getClass().getName().equals(NATIVE_WORLD_MANAGER);
    }

    private static Constructor<? extends WorldManager> wrapperConstructor(Class<? extends WorldManager> superclass, Class<?> pluginType) throws ReflectiveOperationException {
        DynamicType.Builder<? extends WorldManager> builder = new ByteBuddy(ClassFileVersion.JAVA_V21)
                .subclass(superclass, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .name(GeyserSpigotWorldManagerHook.class.getName() + "$Generated")
                .defineField("delegate", GeyserWorldManager.class, Visibility.PUBLIC, FieldManifestation.FINAL)
                .defineConstructor(Visibility.PUBLIC)
                .withParameters(pluginType, GeyserWorldManager.class)
                .intercept(MethodCall.invoke(superclass.getConstructor(pluginType)).withArgument(0).andThen(FieldAccessor.ofField("delegate").setsArgumentAt(1)));
        for (Method method : GeyserWorldManager.class.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                builder = builder
                        .method(named(method.getName()).and(takesArguments(method.getParameterTypes())))
                        .intercept(MethodCall.invoke(method).onField("delegate").withAllArguments());
            }
        }
        return builder.make()
                .load(GeyserSpigotWorldManagerHook.class.getClassLoader())
                .getLoaded()
                .getConstructor(pluginType, GeyserWorldManager.class);
    }
}
