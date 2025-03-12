package net.momirealms.craftengine.core.plugin.classpath;

import java.nio.file.Path;

public interface ClassPathAppender extends AutoCloseable {

    void addJarToClasspath(Path file);

    @Override
    default void close() {
    }
}
