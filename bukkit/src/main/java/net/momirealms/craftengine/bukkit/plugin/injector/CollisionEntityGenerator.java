package net.momirealms.craftengine.bukkit.plugin.injector;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.FieldManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.*;
import net.bytebuddy.matcher.ElementMatcher;
import net.momirealms.craftengine.core.block.setting.PushReaction;
import net.momirealms.craftengine.core.entity.CollisionEntity;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.nbt.CompoundTagProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.damagesource.DamageSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.*;
import net.momirealms.craftengine.proxy.minecraft.world.entity.vehicle.boat.BoatProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.PushReactionProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.storage.ValueOutputProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.constructor.SConstructor4;
import net.momirealms.sparrow.reflection.constructor.matcher.ConstructorMatcher;
import net.momirealms.sparrow.reflection.method.matcher.MethodMatcher;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

public final class CollisionEntityGenerator {
    private static final Class<?>[] CONSTRUCTOR_PARAMETERS = {EntityTypeProxy.CLASS, LevelProxy.CLASS, boolean.class, Supplier.class};
    private static final Supplier<Object> AIR = () -> ItemsProxy.AIR;
    private static final Class<?> SAVE_OUTPUT = VersionHelper.isOrAbove1_21_6 ? ValueOutputProxy.CLASS : CompoundTagProxy.CLASS;
    private static final Class<?> COLLISION_BOAT_CLASS = generate(BoatProxy.CLASS, "CollisionBoat", true);
    private static final Class<?> COLLISION_INTERACTION_CLASS = generate(InteractionProxy.CLASS, "CollisionInteraction", false);
    private static final SConstructor4 COLLISION_BOAT = constructor(COLLISION_BOAT_CLASS);
    private static final SConstructor4 NON_COLLISION_BOAT = constructor(generateNonCollision(BoatProxy.CLASS, COLLISION_BOAT_CLASS, "NonCollisionBoat"));
    private static final SConstructor4 COLLISION_INTERACTION = constructor(COLLISION_INTERACTION_CLASS);
    private static final SConstructor4 NON_COLLISION_INTERACTION = constructor(generateNonCollision(InteractionProxy.CLASS, COLLISION_INTERACTION_CLASS, "NonCollisionInteraction"));

    private CollisionEntityGenerator() {
    }

    public static CollisionEntity createBoat(Object level, Object box, double x, double y, double z, boolean projectileHit, boolean collides, boolean blocksBuilding) {
        CollisionEntity entity = (CollisionEntity) (collides ? COLLISION_BOAT : NON_COLLISION_BOAT).newInstance(EntityTypesProxy.OAK_BOAT, level, projectileHit, AIR);
        initialize(entity, box, x, y, z, blocksBuilding);
        EntityProxy.INSTANCE.setNoPhysics(entity, true);
        return entity;
    }

    public static CollisionEntity createInteraction(Object level, Object box, double x, double y, double z, boolean projectileHit, boolean collides, boolean blocksBuilding) {
        CollisionEntity entity = (CollisionEntity) (collides ? COLLISION_INTERACTION : NON_COLLISION_INTERACTION).newInstance(EntityTypesProxy.INTERACTION, level, projectileHit, AIR);
        initialize(entity, box, x, y, z, blocksBuilding);
        return entity;
    }

    private static void initialize(CollisionEntity entity, Object box, double x, double y, double z, boolean blocksBuilding) {
        EntityProxy.INSTANCE.setInvisible(entity, true);
        EntityProxy.INSTANCE.setPos(entity, x, y, z);
        EntityProxy.INSTANCE.setBoundingBox(entity, box);
        if (VersionHelper.isOrAbove26_3) {
            EntityProxy.INSTANCE.setPermanentlyInvulnerable(entity, true);
        } else {
            EntityProxy.INSTANCE.setInvulnerable(entity, true);
        }
        EntityProxy.INSTANCE.setSilent(entity, true);
        EntityProxy.INSTANCE.setBlocksBuilding(entity, blocksBuilding);
    }

    private static Class<?> generate(Class<?> base, String name, boolean boat) {
        boolean dropItemArgument = boat && VersionHelper.isOrAbove1_21_2;
        Class<?>[] superParameters = dropItemArgument
                ? new Class<?>[] {EntityTypeProxy.CLASS, LevelProxy.CLASS, Supplier.class}
                : new Class<?>[] {EntityTypeProxy.CLASS, LevelProxy.CLASS};
        Constructor<?> superConstructor = requireNonNull(SparrowClass.of(base).getDeclaredConstructor(ConstructorMatcher.takeArguments(superParameters)));
        MethodCall construct = MethodCall.invoke(superConstructor).withArgument(0, 1);
        if (dropItemArgument) {
            construct = construct.withArgument(3);
        }
        Implementation.Composable initialize = construct.andThen(FieldAccessor.ofField("projectileHit").setsArgumentAt(2));
        if (boat) {
            initialize = initialize.andThen(FieldAccessor.ofField("landBoats").setsValue(true));
        }
        DynamicType.Builder<?> builder = subclass(base, name, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .implement(CollisionEntity.class)
                .defineField("projectileHit", boolean.class, Visibility.PRIVATE, FieldManifestation.FINAL)
                .defineConstructor(Visibility.PUBLIC).withParameters(CONSTRUCTOR_PARAMETERS)
                .intercept(initialize)
                .method(named("destroy").and(takesArguments(0)))
                .intercept(MethodCall.invoke(method(base, "remove", EntityProxy.RemovalReasonProxy.CLASS))
                        .withReference(EntityProxy.RemovalReasonProxy.DISCARDED))
                .method(named("getEntityId").and(takesArguments(0)))
                .intercept(MethodCall.invoke(method(base, "getId")))
                .method(virtual(base, "setPos", double.class, double.class, double.class))
                .intercept(setPosition())
                .method(virtual(base, "save", SAVE_OUTPUT))
                .intercept(Advice.to(SaveAdvice.class).wrap(saveWithoutId()))
                .method(virtual(base, "canBeHitByProjectile"))
                .intercept(FieldAccessor.ofField("projectileHit"))
                .method(virtual(base, "tick"))
                .intercept(StubMethod.INSTANCE)
                .method(virtual(base, "shouldBeSaved"))
                .intercept(FixedValue.value(false));
        if (boat) {
            builder = boatMethods(builder, base);
        }
        return InjectorClassLoading.load(collisionMethods(builder, base, true));
    }

    private static Class<?> generateNonCollision(Class<?> base, Class<?> collision, String name) {
        return InjectorClassLoading.load(collisionMethods(subclass(collision, name, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING), base, false));
    }

    private static DynamicType.Builder<?> collisionMethods(DynamicType.Builder<?> builder, Class<?> base, boolean collides) {
        Class<?> entity = EntityProxy.CLASS;
        ElementMatcher.Junction<MethodDescription> methods = virtual(base, "canCollideWith", entity)
                .or(virtual(base, "canCollideWithBukkit", entity))
                .or(VersionHelper.isOrAbove1_21_6 ? virtual(base, "canBeCollidedWith", entity) : virtual(base, "canBeCollidedWith"));
        builder = builder.method(methods).intercept(FixedValue.value(collides));
        if (!VersionHelper.isOrAbove1_21) {
            builder = builder.defineMethod("shouldHardCollide", boolean.class, Visibility.PUBLIC).intercept(FixedValue.value(collides));
        }
        return builder;
    }

    private static DynamicType.Builder<?> boatMethods(DynamicType.Builder<?> builder, Class<?> base) {
        Class<?> entity = EntityProxy.CLASS;
        Class<?> damage = DamageSourceProxy.CLASS;
        ElementMatcher.Junction<MethodDescription> noOp = virtual(base, "push", entity)
                .or(virtual(base, "positionRider", entity, EntityProxy.MoveFunctionProxy.CLASS))
                .or(virtual(base, "animateHurt", float.class))
                .or(virtual(base, "setDeltaMovement", Vec3Proxy.CLASS))
                .or(virtual(base, "move", MoverTypeProxy.CLASS, Vec3Proxy.CLASS))
                .or(virtual(base, "addPassenger", entity))
                .or(VersionHelper.isOrAbove1_21_5
                        ? virtual(base, "onAboveBubbleColumn", boolean.class, BlockPosProxy.CLASS)
                        : virtual(base, "onAboveBubbleCol", boolean.class))
                .or(VersionHelper.isOrAbove1_21_2
                        ? virtual(base, "destroy", ServerLevelProxy.CLASS, damage)
                        : virtual(base, "destroy", damage));
        ElementMatcher.Junction<MethodDescription> alwaysFalse = virtual(base, "canAddPassenger", entity)
                .or(virtual(base, "couldAcceptPassenger"))
                .or(virtual(base, "isUnderWater"))
                .or(virtual(base, "isPickable"))
                .or(VersionHelper.isOrAbove1_21_2
                        ? virtual(base, "hurtServer", ServerLevelProxy.CLASS, damage, float.class)
                        : virtual(base, "hurt", damage, float.class));
        if (VersionHelper.isOrAbove1_21) {
            noOp = noOp.or(virtual(base, "setLeashData", LeashableProxy.LeashDataProxy.CLASS));
            builder = builder.method(virtual(base, "getLeashData")).intercept(FixedValue.nullValue());
        }
        if (VersionHelper.isOrAbove1_21_2) {
            builder = builder.method(virtual(base, "rideHeight", EntityDimensionsProxy.CLASS)).intercept(FixedValue.value(0.0));
        }
        PushReaction reaction = VersionHelper.isOrAbove26_3 ? PushReaction.IMMOVEABLE : PushReaction.IGNORE_ENTITY;
        return builder.method(noOp).intercept(StubMethod.INSTANCE)
                .method(alwaysFalse).intercept(FixedValue.value(false))
                .method(virtual(base, "getMaxPassengers")).intercept(FixedValue.value(0))
                .method(virtual(base, "getPistonPushReaction")).intercept(FixedValue.reference(PushReactionProxy.VALUES[reaction.minecraftIndex(VersionHelper.isOrAbove26_3)]))
                .method(virtual(base, "isIgnoringBlockTriggers")).intercept(FixedValue.value(true));
    }

    private static MethodCall setPosition() {
        Class<?> entity = EntityProxy.CLASS;
        if (VersionHelper.hasPaperPatch) {
            return MethodCall.invoke(method(entity, "setPosRaw", double.class, double.class, double.class, boolean.class))
                    .withAllArguments().with(true);
        }
        return MethodCall.invoke(method(entity, "setPosRaw", double.class, double.class, double.class)).withAllArguments();
    }

    private static Implementation saveWithoutId() {
        Class<?> entity = EntityProxy.CLASS;
        MethodCall save;
        if (VersionHelper.hasPaperPatch && VersionHelper.isOrAbove1_21_4) {
            save = MethodCall.invoke(method(entity, "saveWithoutId", SAVE_OUTPUT, boolean.class, boolean.class, boolean.class))
                    .withArgument(0).with(true, false, true);
        } else if (VersionHelper.hasPaperPatch && VersionHelper.isOrAbove1_20_3) {
            save = MethodCall.invoke(method(entity, "saveWithoutId", SAVE_OUTPUT, boolean.class)).withArgument(0).with(true);
        } else {
            save = MethodCall.invoke(method(entity, "saveWithoutId", SAVE_OUTPUT)).withArgument(0);
        }
        return save.andThen(FixedValue.value(true));
    }

    private static DynamicType.Builder<?> subclass(Class<?> base, String name, ConstructorStrategy constructors) {
        return new ByteBuddy(ClassFileVersion.JAVA_V21)
                .subclass(base, constructors)
                .name(CollisionEntityGenerator.class.getPackageName() + "." + name);
    }

    private static SConstructor4 constructor(Class<?> generated) {
        return SparrowClass.of(generated).getSparrowConstructor(ConstructorMatcher.takeArguments(CONSTRUCTOR_PARAMETERS)).asm$4();
    }

    private static ElementMatcher.Junction<MethodDescription> virtual(Class<?> base, String name, Class<?>... parameters) {
        Method method = method(base, name, parameters);
        return named(method.getName()).and(takesArguments(method.getParameterTypes()));
    }

    private static Method method(Class<?> base, String name, Class<?>... parameters) {
        MethodMatcher matcher = MethodMatcher.named(name).and(MethodMatcher.takeArguments(parameters));
        for (Class<?> type = base; type != null; type = type.getSuperclass()) {
            Method method = SparrowClass.of(type).getDeclaredMethod(matcher);
            if (method != null) {
                return method;
            }
        }
        return requireNonNull(SparrowClass.of(base).getMethod(matcher), () -> base.getName() + "." + name);
    }

    static final class SaveAdvice {
        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        static boolean onEnter(@Advice.This Object entity, @Advice.Argument(0) Object output) {
            String encodeId = EntityProxy.INSTANCE.getEncodeId(entity);
            if (encodeId == null) {
                return true;
            }
            if (VersionHelper.isOrAbove1_21_6) {
                ValueOutputProxy.INSTANCE.putString(output, "id", encodeId);
            } else {
                CompoundTagProxy.INSTANCE.putString(output, "id", encodeId);
            }
            return false;
        }
    }
}
