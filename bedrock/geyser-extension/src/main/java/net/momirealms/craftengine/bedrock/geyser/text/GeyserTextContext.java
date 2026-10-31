package net.momirealms.craftengine.bedrock.geyser.text;

import io.netty.channel.EventLoop;
import net.momirealms.craftengine.bedrock.geyser.proxy.session.GeyserSessionProxy;
import org.geysermc.geyser.registry.PacketTranslatorRegistry;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;

import java.util.Map;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.concurrent.Callable;

public final class GeyserTextContext {
    private static final ThreadLocal<GeyserSession> SESSION = new ThreadLocal<>();

    private GeyserTextContext() {
    }

    public static GeyserSession session() {
        return SESSION.get();
    }

    public static void install(GeyserSession session) {
        GeyserSessionProxy.INSTANCE.tickEventLoop(session, eventLoop(session, session.getTickEventLoop()));
    }

    static EventLoop eventLoop(GeyserSession session, EventLoop delegate) {
        return (EventLoop) Proxy.newProxyInstance(EventLoop.class.getClassLoader(), new Class<?>[]{EventLoop.class}, (proxy, method, arguments) -> {
            if (method.getName().equals("inEventLoop")) {
                return (boolean) method.invoke(delegate, arguments) && SESSION.get() == session;
            }
            // Inventory refreshes may be scheduled after packet translation returns; keep their session context too.
            if (arguments != null && arguments.length > 0) {
                if (arguments[0] instanceof Runnable task) {
                    arguments[0] = (Runnable) () -> run(session, task);
                } else if (arguments[0] instanceof Callable<?> task) {
                    arguments[0] = (Callable<?>) () -> {
                        GeyserSession previous = SESSION.get();
                        SESSION.set(session);
                        try {
                            return task.call();
                        } finally {
                            restore(previous);
                        }
                    };
                }
            }
            try {
                return method.invoke(delegate, arguments);
            } catch (InvocationTargetException exception) {
                throw exception.getCause();
            }
        });
    }

    private static void run(GeyserSession session, Runnable task) {
        GeyserSession previous = SESSION.get();
        SESSION.set(session);
        try {
            task.run();
        } finally {
            restore(previous);
        }
    }

    private static void restore(GeyserSession previous) {
        if (previous == null) {
            SESSION.remove();
        } else {
            SESSION.set(previous);
        }
    }

    public static void install() {
        install(Registries.JAVA_PACKET_TRANSLATORS);
        install(Registries.BEDROCK_PACKET_TRANSLATORS);
    }

    @SuppressWarnings("unchecked")
    private static <T> void install(PacketTranslatorRegistry<T> registry) {
        for (Map.Entry<Class<? extends T>, PacketTranslator<? extends T>> entry : registry.get().entrySet()) {
            PacketTranslator<T> original = (PacketTranslator<T>) entry.getValue();
            if (!(original instanceof ContextTranslator<?>)) {
                entry.setValue(new ContextTranslator<>(original));
            }
        }
    }

    public static void uninstall() {
        uninstall(Registries.JAVA_PACKET_TRANSLATORS);
        uninstall(Registries.BEDROCK_PACKET_TRANSLATORS);
    }

    private static <T> void uninstall(PacketTranslatorRegistry<T> registry) {
        for (Map.Entry<Class<? extends T>, PacketTranslator<? extends T>> entry : registry.get().entrySet()) {
            if (entry.getValue() instanceof ContextTranslator<? extends T> translator) {
                entry.setValue(translator.delegate);
            }
        }
    }

    private static final class ContextTranslator<T> extends PacketTranslator<T> {
        private final PacketTranslator<T> delegate;

        private ContextTranslator(PacketTranslator<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean shouldExecuteInEventLoop() {
            return this.delegate.shouldExecuteInEventLoop();
        }

        @Override
        public void translate(GeyserSession session, T packet) {
            GeyserSession previous = SESSION.get();
            SESSION.set(session);
            try {
                this.delegate.translate(session, packet);
            } finally {
                // Geyser's serializer is global; never leave one player's mappings on a shared translator thread.
                restore(previous);
            }
        }
    }
}
