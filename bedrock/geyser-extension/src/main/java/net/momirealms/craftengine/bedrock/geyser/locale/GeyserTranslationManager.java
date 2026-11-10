package net.momirealms.craftengine.bedrock.geyser.locale;

import org.geysermc.geyser.api.extension.ExtensionLogger;

import java.io.PrintWriter;
import java.io.StringWriter;

public interface GeyserTranslationManager {

    static GeyserTranslationManager instance() {
        return GeyserTranslationManagerImpl.instance;
    }

    static String message(String key, Object... arguments) {
        return instance().plainTranslation(key, arguments);
    }

    static void info(String key, Object... arguments) {
        instance().logger().info(message(key, arguments));
    }

    static void warn(String key, Object... arguments) {
        instance().logger().warning(message(key, arguments));
    }

    static void warn(String key, Throwable exception, Object... arguments) {
        StringWriter trace = new StringWriter();
        exception.printStackTrace(new PrintWriter(trace));
        instance().logger().warning(message(key, arguments) + System.lineSeparator() + trace);
    }

    static void error(String key, Object... arguments) {
        instance().logger().error(message(key, arguments));
    }

    static void error(String key, Throwable exception, Object... arguments) {
        instance().logger().error(message(key, arguments), exception);
    }

    void load();

    String plainTranslation(String key, Object... arguments);

    ExtensionLogger logger();
}
