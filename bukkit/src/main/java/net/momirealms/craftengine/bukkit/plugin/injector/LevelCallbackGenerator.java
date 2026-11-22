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
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.matcher.ElementMatchers;
import net.momirealms.craftengine.bukkit.api.CraftEngineFurniture;
import net.momirealms.craftengine.bukkit.util.LevelUtils;
import net.momirealms.craftengine.core.entity.NonTickingEntity;
import net.momirealms.craftengine.core.entity.UntrackedEntity;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.InjectedWorldCallback;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerChunkCacheProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.entity.LevelCallbackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.entity.LevelEntityGetterProxy;
import net.momirealms.craftengine.proxy.paper.chunk.system.entity.EntityLookupProxy;
import net.momirealms.craftengine.proxy.paper.chunk.system.entity.ServerEntityLookupProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.constructor.SConstructor2;
import net.momirealms.sparrow.reflection.constructor.matcher.ConstructorMatcher;
import net.momirealms.sparrow.reflection.method.matcher.MethodMatcher;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static java.util.Objects.requireNonNull;

public final class LevelCallbackGenerator {
    static final Class<?> ITEM_DISPLAY_CLASS = SparrowClass.find("net.minecraft.world.entity.Display$ItemDisplay");
    static final boolean IS_AT_MOST_26_1_2 = VersionHelper.MINECRAFT_VERSION.compareTo(MinecraftVersion.V26_1_2) <= 0;
    static final boolean HAS_TRACKER_ENTITIES = hasTrackerEntities();
    private static final SConstructor2 CONSTRUCTOR = generate();

    private LevelCallbackGenerator() {
    }

    public static void install(Object serverLevel) {
        Object entityLookup = LevelUtils.getEntityLookup(serverLevel);
        Object callback = EntityLookupProxy.INSTANCE.getWorldCallback(entityLookup);
        if (!(callback instanceof InjectedWorldCallback)) {
            EntityLookupProxy.INSTANCE.setWorldCallback(entityLookup, create(callback, entityLookup));
        }
    }

    public static InjectedWorldCallback create(Object callback, Object entityLookup) {
        InjectedWorldCallback injected = (InjectedWorldCallback) CONSTRUCTOR.newInstance(callback, entityLookup);
        if (VersionHelper.hasFoliaPatch) {
            return injected;
        }
        for (Object entity : LevelEntityGetterProxy.INSTANCE.getAll(entityLookup)) {
            if (entity instanceof NonTickingEntity || (ITEM_DISPLAY_CLASS.isInstance(entity) && CraftEngineFurniture.isFurniture(EntityProxy.INSTANCE.getBukkitEntity(entity)))) {
                LevelCallbackProxy.INSTANCE.onTickingEnd(callback, entity);
            }
            if (entity instanceof UntrackedEntity) {
                Object level = EntityProxy.INSTANCE.getLevel(entity);
                Object chunkSource = ServerLevelProxy.INSTANCE.getChunkSource(level);
                ServerChunkCacheProxy.INSTANCE.removeEntity(chunkSource, entity);
                if (HAS_TRACKER_ENTITIES) {
                    Object trackerEntities = ServerEntityLookupProxy.INSTANCE.getTrackerEntities(entityLookup);
                    ServerEntityLookupProxy.ReferenceListProxy.INSTANCE.remove(trackerEntities, entity);
                }
            }
        }
        return injected;
    }

    private static SConstructor2 generate() {
        Class<?> callbackClass = LevelCallbackProxy.CLASS;
        SparrowClass<?> callbackType = SparrowClass.of(callbackClass);
        Method tickingStart = requireNonNull(callbackType.getDeclaredMethod(MethodMatcher.named("onTickingStart").and(MethodMatcher.takeArguments(Object.class))));
        Method trackingStart = requireNonNull(callbackType.getDeclaredMethod(MethodMatcher.named("onTrackingStart").and(MethodMatcher.takeArguments(Object.class))));
        Constructor<Object> objectConstructor = requireNonNull(SparrowClass.of(Object.class).getDeclaredConstructor(ConstructorMatcher.takeArguments(new Class<?>[0])));
        DynamicType.Builder<?> builder = new ByteBuddy(ClassFileVersion.JAVA_V21)
                .subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .name(LevelCallbackGenerator.class.getPackageName() + ".InjectedLevelCallback")
                .implement(callbackClass, InjectedWorldCallback.class)
                .defineField("callback", callbackClass, Visibility.PRIVATE, FieldManifestation.FINAL)
                .defineField("entityLookup", Object.class, Visibility.PRIVATE, FieldManifestation.FINAL)
                .defineConstructor(Visibility.PUBLIC)
                .withParameters(callbackClass, Object.class)
                .intercept(MethodCall.invoke(objectConstructor)
                        .andThen(FieldAccessor.ofField("callback").setsArgumentAt(0))
                        .andThen(FieldAccessor.ofField("entityLookup").setsArgumentAt(1)));
        for (Method method : callbackClass.getDeclaredMethods()) {
            if (!Modifier.isAbstract(method.getModifiers())) {
                continue;
            }
            Implementation.Composable forward = MethodCall.invoke(method).onField("callback").withAllArguments();
            Implementation implementation = forward;
            if (method.equals(tickingStart)) {
                implementation = Advice.to(TickingStartAdvice.class).wrap(forward);
            } else if (method.equals(trackingStart)) {
                implementation = Advice.to(TrackingStartAdvice.class).wrap(forward);
            }
            builder = builder.method(ElementMatchers.is(method)).intercept(implementation);
        }
        Class<?> generated = builder.make()
                .load(LevelCallbackGenerator.class.getClassLoader(), ClassLoadingStrategy.UsingLookup.of(MethodHandles.lookup()))
                .getLoaded();
        return SparrowClass.of(generated).getSparrowConstructor(ConstructorMatcher.takeArguments(callbackClass, Object.class)).asm$2();
    }

    private static boolean hasTrackerEntities() {
        if (VersionHelper.hasFoliaPatch || !VersionHelper.isOrAbove1_21 || !IS_AT_MOST_26_1_2) {
            return false;
        }
        try {
            ServerEntityLookupProxy.CLASS.getField("trackerEntities");
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    static final class TickingStartAdvice {
        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        static boolean onEnter(@Advice.Argument(0) Object entity) {
            return entity instanceof NonTickingEntity || (ITEM_DISPLAY_CLASS.isInstance(entity) && CraftEngineFurniture.isFurniture(EntityProxy.INSTANCE.getBukkitEntity(entity)));
        }
    }

    static final class TrackingStartAdvice {
        @Advice.OnMethodExit
        static void onExit(@Advice.Argument(0) Object entity, @Advice.FieldValue("entityLookup") Object entityLookup) {
            if (entity instanceof UntrackedEntity) {
                Object level = EntityProxy.INSTANCE.getLevel(entity);
                Object chunkSource = ServerLevelProxy.INSTANCE.getChunkSource(level);
                ServerChunkCacheProxy.INSTANCE.removeEntity(chunkSource, entity);
                if (HAS_TRACKER_ENTITIES) {
                    Object trackerEntities = ServerEntityLookupProxy.INSTANCE.getTrackerEntities(entityLookup);
                    ServerEntityLookupProxy.ReferenceListProxy.INSTANCE.remove(trackerEntities, entity);
                } else if (VersionHelper.hasFoliaPatch && VersionHelper.isOrAbove1_21_4 && IS_AT_MOST_26_1_2) {
                    ServerEntityLookupProxy.INSTANCE.removeRegionTracker(entityLookup, entity);
                }
            }
        }
    }
}
