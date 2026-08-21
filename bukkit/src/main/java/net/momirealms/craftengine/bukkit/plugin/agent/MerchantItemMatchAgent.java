package net.momirealms.craftengine.bukkit.plugin.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import org.jetbrains.annotations.NotNull;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public final class MerchantItemMatchAgent {

    private MerchantItemMatchAgent() {
    }

    public static boolean install(Instrumentation instrumentation, Class<?> targetClass, Class<?> itemStackClass, String legacyMethodName) {
        AtomicBoolean transformed = new AtomicBoolean();
        AtomicBoolean ambiguous = new AtomicBoolean();
        AtomicBoolean failed = new AtomicBoolean();
        ClassFileTransformer transformer = new AgentBuilder.Default()
                .disableClassFormatChanges()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(new AgentBuilder.Listener.Adapter() {
                    @Override
                    public void onError(@NotNull String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, @NotNull Throwable throwable) {
                        failed.set(true);
                    }
                })
                .type(ElementMatchers.named(targetClass.getName()))
                .transform((builder, type, classLoader, module, protectionDomain) -> {
                    ElementMatcher.Junction<MethodDescription> matcher = legacyMethodName == null
                            ? ElementMatchers.returns(boolean.class).and(ElementMatchers.takesArguments(itemStackClass))
                            : ElementMatchers.named(legacyMethodName)
                              .and(ElementMatchers.isPublic())
                              .and(ElementMatchers.returns(boolean.class))
                              .and(ElementMatchers.takesArguments(itemStackClass, itemStackClass));
                    int candidates = type.getDeclaredMethods().filter(matcher).size();
                    if (candidates != 1) {
                        ambiguous.set(true);
                        return builder;
                    }
                    transformed.set(true);
                    Class<?> advice = legacyMethodName == null ? ModernMatchAdvice.class : LegacyMatchAdvice.class;
                    return builder.visit(Advice.to(advice).on(matcher));
                })
                .installOn(instrumentation);
        try {
            instrumentation.retransformClasses(targetClass);
        } catch (Throwable ignored) {
            failed.set(true);
        } finally {
            instrumentation.removeTransformer(transformer);
        }
        return transformed.get() && !ambiguous.get() && !failed.get();
    }

    public static final class ModernMatchAdvice {

        private ModernMatchAdvice() {
        }

        @Advice.OnMethodExit
        public static void onExit(
                @Advice.This Object requirement,
                @Advice.Argument(0) Object offeredStack,
                @Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) boolean result
        ) {
            if (!result) return;
            BiPredicate<Object, Object> callback = AgentBridge.MERCHANT_ITEM_MATCH;
            if (callback == null) return;
            try {
                result = callback.test(requirement, offeredStack);
            } catch (Throwable ignored) {
                // Fail open when the plugin callback is unavailable or incompatible.
            }
        }
    }

    public static final class LegacyMatchAdvice {

        private LegacyMatchAdvice() {
        }

        @Advice.OnMethodExit
        public static void onExit(
                @Advice.This Object offer,
                @Advice.Argument(0) Object firstStack,
                @Advice.Argument(1) Object secondStack,
                @Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) boolean result
        ) {
            if (!result) return;
            Predicate<Object[]> callback = AgentBridge.MERCHANT_OFFER_MATCH;
            if (callback == null) return;
            try {
                result = callback.test(new Object[]{offer, firstStack, secondStack});
            } catch (Throwable ignored) {
                // Fail open when the plugin callback is unavailable or incompatible.
            }
        }
    }
}
