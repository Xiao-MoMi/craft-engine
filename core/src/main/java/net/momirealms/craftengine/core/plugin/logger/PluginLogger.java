package net.momirealms.craftengine.core.plugin.logger;

import java.io.File;
import java.nio.file.Path;

public interface PluginLogger {
    void info(String s);

    void warn(String s);

    default void warn(File file, String s) {
        warn("Error in file: " + file.getAbsolutePath() + " - " + s);
    }

    default void warn(Path file, String s) {
        warn("Error in file: " + file.toAbsolutePath() + " - " + s);
    }

    default void warn(Path file, String s, Throwable t) {
        warn("Error in file: " + file.toAbsolutePath() + " - " + s, t);
    }

    void warn(String s, Throwable t);

    void severe(String s);

    void severe(String s, Throwable t);
}
