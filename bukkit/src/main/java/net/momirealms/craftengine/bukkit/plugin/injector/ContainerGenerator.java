package net.momirealms.craftengine.bukkit.plugin.injector;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.modifier.FieldManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.world.BukkitContainer;
import net.momirealms.craftengine.bukkit.world.DelegatingContainer;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.WorldlyContainer;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.entity.CraftHumanEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.world.ContainerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.WorldlyContainerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.ContainerUserProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.player.PlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.constructor.SConstructor1;
import net.momirealms.sparrow.reflection.constructor.matcher.ConstructorMatcher;
import net.momirealms.sparrow.reflection.method.matcher.MethodMatcher;
import org.bukkit.Location;
import org.bukkit.entity.HumanEntity;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;
import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

public final class ContainerGenerator {
    private static final Class<?> CUSTOM_CONTAINER = generateContainer();
    private static final SConstructor1 CONTAINER = constructor(CUSTOM_CONTAINER);
    private static final SConstructor1 WORLDLY_CONTAINER = constructor(generateWorldlyContainer());

    private ContainerGenerator() {
    }

    public static DelegatingContainer create(BukkitContainer container) {
        return (DelegatingContainer) (container instanceof WorldlyContainer ? WORLDLY_CONTAINER : CONTAINER).newInstance(container);
    }

    private static Class<?> generateContainer() {
        Class<?> type = ContainerProxy.CLASS;
        Class<?> stack = ItemStackProxy.CLASS;
        Class<?> human = CraftHumanEntityProxy.CLASS;
        Class<?> user = VersionHelper.isOrAbove1_21_9 ? ContainerUserProxy.CLASS : PlayerProxy.CLASS;
        DynamicType.Builder<?> builder = new ByteBuddy(ClassFileVersion.JAVA_V21)
                .subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .name(ContainerGenerator.class.getPackageName() + ".CustomContainer")
                .implement(type, DelegatingContainer.class)
                .defineField("container", BukkitContainer.class, Visibility.PROTECTED, FieldManifestation.FINAL)
                .defineConstructor(Visibility.PUBLIC).withParameters(BukkitContainer.class)
                .intercept(MethodCall.invoke(requireNonNull(SparrowClass.of(Object.class).getDeclaredConstructor(ConstructorMatcher.takeArguments(new Class<?>[0]))))
                        .andThen(FieldAccessor.ofField("container").setsArgumentAt(0)))
                .method(named("getContainer").and(takesArguments(0)))
                .intercept(FieldAccessor.ofField("container"))
                .defineMethod("stillValid", boolean.class, Visibility.PUBLIC)
                .intercept(FixedValue.value(true));

        builder = forward(builder, method(type, "getContainerSize"), "containerSize");
        builder = forward(builder, method(type, "isEmpty"), "isEmpty");
        builder = forward(builder, method(type, "getMaxStackSize"), "maxStackSize");
        builder = forward(builder, method(type, "setChanged"), "setChanged");
        builder = forward(builder, method(type, "onOpen", human), "onOpen", HumanEntity.class);
        builder = forward(builder, method(type, "onClose", human), "onClose", HumanEntity.class);
        builder = forward(builder, method(type, "getViewers"), "getViewers");
        builder = forward(builder, method(type, "getOwner"), "getOwner");
        builder = forward(builder, method(type, "setMaxStackSize", int.class), "setMaxStackSize", int.class);
        builder = forward(builder, method(type, "clearContent"), "clearContent");

        if (VersionHelper.isOrAbove1_20_5) {
            builder = adapt(builder, type, "getMaxStackSize", stack);
        }
        builder = adapt(builder, type, "getItem", int.class);
        builder = adapt(builder, type, "removeItem", int.class, int.class);
        builder = adapt(builder, type, "removeItemNoUpdate", int.class);
        builder = adapt(builder, type, "setItem", int.class, stack);
        builder = adapt(builder, type, "stillValid", PlayerProxy.CLASS);
        builder = adapt(builder, type, "getContents");
        builder = adapt(builder, type, "getLocation");
        builder = adapt(builder, type, "startOpen", user);
        builder = adapt(builder, type, "stopOpen", user);
        builder = adapt(builder, type, "canPlaceItem", int.class, stack);
        builder = adapt(builder, type, "canTakeItem", type, int.class, stack);
        builder = adapt(builder, type, "hasAnyMatching", Predicate.class);
        return load(builder);
    }

    private static Class<?> generateWorldlyContainer() {
        Class<?> type = WorldlyContainerProxy.CLASS;
        Class<?> side = DirectionProxy.CLASS;
        Class<?> stack = ItemStackProxy.CLASS;
        DynamicType.Builder<?> builder = new ByteBuddy(ClassFileVersion.JAVA_V21)
                .subclass(CUSTOM_CONTAINER, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                .name(ContainerGenerator.class.getPackageName() + ".CustomWorldlyContainer")
                .implement(type);
        builder = adapt(builder, type, "getSlotsForFace", side);
        builder = adapt(builder, type, "canPlaceItemThroughFace", int.class, stack, side);
        builder = adapt(builder, type, "canTakeItemThroughFace", int.class, stack, side);
        return load(builder);
    }

    private static DynamicType.Builder<?> forward(DynamicType.Builder<?> builder, Method source, String target, Class<?>... parameters) {
        Method method = method(BukkitContainer.class, target, parameters);
        return builder.method(is(source)).intercept(MethodCall.invoke(method).onField("container").withAllArguments());
    }

    private static DynamicType.Builder<?> adapt(DynamicType.Builder<?> builder, Class<?> type, String name, Class<?>... parameters) {
        return builder.method(is(method(type, name, parameters)))
                .intercept(MethodDelegation.withDefaultConfiguration().filter(named(name)).to(ContainerMethods.class));
    }

    private static Method method(Class<?> type, String name, Class<?>... parameters) {
        return requireNonNull(SparrowClass.of(type).getMethod(MethodMatcher.named(name).and(MethodMatcher.takeArguments(parameters))),
                () -> type.getName() + "." + name);
    }

    private static Class<?> load(DynamicType.Builder<?> builder) {
        return builder.make()
                .load(ContainerGenerator.class.getClassLoader(), ClassLoadingStrategy.UsingLookup.of(MethodHandles.lookup()))
                .getLoaded();
    }

    private static SConstructor1 constructor(Class<?> generated) {
        return SparrowClass.of(generated).getSparrowConstructor(ConstructorMatcher.takeArguments(BukkitContainer.class)).asm$1();
    }

    public static final class ContainerMethods {
        private ContainerMethods() {
        }

        @RuntimeType
        public static Object getItem(@FieldValue("container") BukkitContainer container, @Argument(0) int slot) {
            return container.getItem(slot).minecraftItem();
        }

        @RuntimeType
        public static Object removeItem(@FieldValue("container") BukkitContainer container, @Argument(0) int slot, @Argument(1) int count) {
            return container.removeItem(slot, count).minecraftItem();
        }

        @RuntimeType
        public static Object removeItemNoUpdate(@FieldValue("container") BukkitContainer container, @Argument(0) int slot) {
            return container.removeItemNoUpdate(slot).minecraftItem();
        }

        public static void setItem(@FieldValue("container") BukkitContainer container, @Argument(0) int slot, @Argument(1) Object stack) {
            container.setItem(slot, ItemStackUtils.wrap(stack));
        }

        public static int getMaxStackSize(@FieldValue("container") BukkitContainer container, @Argument(0) Object stack) {
            return container.getMaxStackSize(ItemStackUtils.wrap(stack));
        }

        public static boolean stillValid(@FieldValue("container") BukkitContainer container, @Argument(0) Object player) {
            return container.stillValid(adaptPlayer(player));
        }

        public static List<Object> getContents(@FieldValue("container") BukkitContainer container) {
            List<Object> contents = new ArrayList<>(container.containerSize());
            for (Item item : container) {
                contents.add(item.minecraftItem());
            }
            return contents;
        }

        public static Location getLocation(@FieldValue("container") BukkitContainer container) {
            WorldPosition position = container.position();
            return position == null ? null : LocationUtils.toLocation(position);
        }

        public static void startOpen(@FieldValue("container") BukkitContainer container, @Argument(0) Object user) {
            if (!VersionHelper.isOrAbove1_21_9 || PlayerProxy.CLASS.isInstance(user)) {
                container.startOpen(adaptPlayer(user));
            }
        }

        public static void stopOpen(@FieldValue("container") BukkitContainer container, @Argument(0) Object user) {
            if (!VersionHelper.isOrAbove1_21_9 || PlayerProxy.CLASS.isInstance(user)) {
                container.stopOpen(adaptPlayer(user));
            }
        }

        public static boolean canPlaceItem(@FieldValue("container") BukkitContainer container, @Argument(0) int slot, @Argument(1) Object stack) {
            return container.canPlaceItem(slot, ItemStackUtils.wrap(stack));
        }

        public static boolean canTakeItem(@FieldValue("container") BukkitContainer container, @Argument(0) Object into, @Argument(1) int slot, @Argument(2) Object stack) {
            return container.canTakeItem(into, slot, ItemStackUtils.wrap(stack));
        }

        public static boolean hasAnyMatching(@FieldValue("container") BukkitContainer container, @Argument(0) Predicate<Object> predicate) {
            return container.hasAnyMatching(item -> predicate.test(item.minecraftItem()));
        }

        public static int[] getSlotsForFace(@FieldValue("container") BukkitContainer container, @Argument(0) Object direction) {
            return ((WorldlyContainer) container).getSlotsForFace(DirectionUtils.fromNMSDirection(direction));
        }

        public static boolean canPlaceItemThroughFace(@FieldValue("container") BukkitContainer container, @Argument(0) int slot, @Argument(1) Object stack, @Argument(2) Object direction) {
            return ((WorldlyContainer) container).canPlaceItemThroughFace(slot, ItemStackUtils.wrap(stack), direction == null ? null : DirectionUtils.fromNMSDirection(direction));
        }

        public static boolean canTakeItemThroughFace(@FieldValue("container") BukkitContainer container, @Argument(0) int slot, @Argument(1) Object stack, @Argument(2) Object direction) {
            return ((WorldlyContainer) container).canTakeItemThroughFace(slot, ItemStackUtils.wrap(stack), DirectionUtils.fromNMSDirection(direction));
        }

        private static Player adaptPlayer(Object player) {
            return BukkitAdaptor.adapt((org.bukkit.entity.Player) EntityProxy.INSTANCE.getBukkitEntity(player));
        }
    }
}
