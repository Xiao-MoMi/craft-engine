package net.momirealms.craftengine.bukkit.util;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.matcher.ElementMatchers;
import net.momirealms.craftengine.proxy.leaf.config.SleepingBlockEntityProxy;
import net.momirealms.craftengine.proxy.lithium.inventory.*;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.constructor.SConstructor3;
import net.momirealms.sparrow.reflection.constructor.matcher.ConstructorMatcher;
import org.jetbrains.annotations.Nullable;

public final class LithiumInventoryUtils {
    private static final boolean AVAILABLE = SleepingBlockEntityProxy.CLASS != null
            && LithiumInventoryProxy.CLASS != null
            && LithiumStackListProxy.CLASS != null
            && InventoryHelperProxy.CLASS != null
            && HopperHelperProxy.CLASS != null
            && InventoryChangeTrackerProxy.CLASS != null
            && InventoryChangeListenerProxy.CLASS != null;

    private LithiumInventoryUtils() {
    }

    public static boolean isAvailable() {
        return AVAILABLE && SleepingBlockEntityProxy.INSTANCE.isEnabled();
    }

    @Nullable
    public static Subscription watch(Object container, Listener listener) {
        if (!isAvailable()) {
            return null;
        }
        Object inventory = HopperHelperProxy.INSTANCE.replaceDoubleInventory(container);
        if (!LithiumInventoryProxy.CLASS.isInstance(inventory) || !InventoryChangeTrackerProxy.CLASS.isInstance(inventory)) {
            return null;
        }
        Object stackList = InventoryHelperProxy.INSTANCE.getLithiumStackList(inventory);
        Subscription subscription = (Subscription) SubscriptionFactory.CONSTRUCTOR.newInstance(inventory, stackList, listener);
        InventoryChangeTrackerProxy.INSTANCE.listenForMajorInventoryChanges(inventory, subscription);
        return subscription;
    }

    public interface Listener {
        void onContentChanged();

        void onInvalidated();
    }

    private static final class SubscriptionFactory {
        private static final SConstructor3 CONSTRUCTOR = createConstructor();

        private static SConstructor3 createConstructor() {
            try {
                Class<?> subscriptionClass = new ByteBuddy(ClassFileVersion.JAVA_V17)
                        .subclass(Subscription.class, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                        .name(LithiumInventoryUtils.class.getName() + "$LithiumSubscription")
                        .implement(InventoryChangeListenerProxy.CLASS)
                        .method(ElementMatchers.named("lithium$handleInventoryContentModified"))
                        .intercept(MethodCall.invoke(Subscription.class.getDeclaredMethod("contentChanged")))
                        .method(ElementMatchers.named("lithium$handleInventoryRemoved").or(ElementMatchers.named("handleStackListReplaced")))
                        .intercept(MethodCall.invoke(Subscription.class.getDeclaredMethod("invalidated")))
                        .method(ElementMatchers.named("lithium$handleComparatorAdded"))
                        .intercept(MethodCall.invoke(Subscription.class.getDeclaredMethod("comparatorAdded")))
                        .make()
                        .load(LithiumInventoryUtils.class.getClassLoader())
                        .getLoaded();
                return SparrowClass.of(subscriptionClass)
                        .getSparrowConstructor(ConstructorMatcher.takeArguments(Object.class, Object.class, Listener.class))
                        .asm$3();
            } catch (NoSuchMethodException exception) {
                throw new ExceptionInInitializerError(exception);
            }
        }
    }

    public abstract static class Subscription implements AutoCloseable {
        private Object inventory;
        private Object stackList;
        private Listener listener;
        private boolean listeningForContent;

        protected Subscription(Object inventory, Object stackList, Listener listener) {
            this.inventory = inventory;
            this.stackList = stackList;
            this.listener = listener;
        }

        public final boolean isValid() {
            return this.inventory != null;
        }

        @Nullable
        public final Object inventory() {
            return this.inventory;
        }

        public final boolean listenForContentChangesOnce() {
            if (!this.isValid() || !isAvailable()) {
                return false;
            }
            if (!this.listeningForContent) {
                // Registration can invalidate a replaced callback; only sleep if the subscription survives.
                this.listeningForContent = true;
                InventoryChangeTrackerProxy.INSTANCE.listenForContentChangesOnce(this.inventory, this.stackList, this);
            }
            return this.isValid() && this.listeningForContent;
        }

        @Override
        public final void close() {
            Object inventory = this.inventory;
            // Lithium has no per-listener content cancellation. Detach all owner references before unregistering.
            this.inventory = null;
            this.stackList = null;
            this.listener = null;
            this.listeningForContent = false;
            if (inventory != null) {
                InventoryChangeTrackerProxy.INSTANCE.stopListenForMajorInventoryChanges(inventory, this);
            }
        }

        protected final void contentChanged() {
            this.listeningForContent = false;
            Listener listener = this.listener;
            if (listener != null) {
                // Notifications may precede the actual item mutation; callers must defer work and rearming.
                listener.onContentChanged();
            }
        }

        protected final boolean comparatorAdded() {
            this.contentChanged();
            return true;
        }

        protected final void invalidated() {
            Listener listener = this.listener;
            this.close();
            if (listener != null) {
                listener.onInvalidated();
            }
        }
    }
}
