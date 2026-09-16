package net.momirealms.craftengine.bukkit.plugin.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public final class EntityWorldAgent {

    private EntityWorldAgent() {}

    public static boolean install(Instrumentation instrumentation, Class<?> targetClass, Class<?> entityClass,
                                  Field worldField, String addMethod, String removeMethod) {
        AtomicBoolean transformed = new AtomicBoolean();
        AtomicBoolean failed = new AtomicBoolean();
        ClassFileTransformer transformer = new AgentBuilder.Default()
                .disableClassFormatChanges()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(new AgentBuilder.Listener.Adapter() {
                    @Override
                    public void onError(@NotNull String typeName, ClassLoader classLoader, JavaModule module,
                                        boolean loaded, @NotNull Throwable throwable) {
                        failed.set(true);
                    }
                })
                .type(ElementMatchers.named(targetClass.getName()))
                .transform((builder, type, classLoader, module, protectionDomain) -> {
                    DynamicType.Builder<?> result = weave(builder, entityClass, worldField, addMethod, removeMethod);
                    transformed.set(true);
                    return result;
                })
                .installOn(instrumentation);
        instrumentation.removeTransformer(transformer);
        return transformed.get() && !failed.get();
    }

    static DynamicType.Builder<?> weave(DynamicType.Builder<?> builder, Class<?> entityClass,
                                        Field worldField, String addMethod, String removeMethod) {
        ElementMatcher.Junction<MethodDescription> add = callbackMethod(addMethod, entityClass);
        ElementMatcher.Junction<MethodDescription> remove = callbackMethod(removeMethod, entityClass);
        if (addMethod.equals(removeMethod)
                || builder.toTypeDescription().getDeclaredMethods().filter(add).size() != 1
                || builder.toTypeDescription().getDeclaredMethods().filter(remove).size() != 1) {
            throw new IllegalStateException("Could not identify both entity tracking callbacks");
        }
        return builder
                .visit(Advice.withCustomMapping().bind(CallbackWorld.class, worldField).to(AddAdvice.class).on(add))
                .visit(Advice.withCustomMapping().bind(CallbackWorld.class, worldField).to(RemoveAdvice.class).on(remove));
    }

    private static ElementMatcher.Junction<MethodDescription> callbackMethod(String name, Class<?> entityClass) {
        // Ignore the compiler-generated EntityAccess bridges, which delegate to these methods.
        return ElementMatchers.named(name).and(ElementMatchers.takesArguments(entityClass))
                .and(ElementMatchers.returns(void.class)).and(ElementMatchers.isPublic())
                .and(ElementMatchers.not(ElementMatchers.isBridge()));
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface CallbackWorld {}

    public static final class AddAdvice {
        @Advice.OnMethodExit
        public static void onExit(@CallbackWorld Object world, @Advice.Argument(0) Object entity) {
            BiConsumer<Object, Object> callback = AgentBridge.ENTITY_ADDED_TO_WORLD;
            if (callback != null) callback.accept(world, entity);
        }
    }

    public static final class RemoveAdvice {
        @Advice.OnMethodExit
        public static void onExit(@CallbackWorld Object world, @Advice.Argument(0) Object entity) {
            BiConsumer<Object, Object> callback = AgentBridge.ENTITY_REMOVED_FROM_WORLD;
            if (callback != null) callback.accept(world, entity);
        }
    }
}
