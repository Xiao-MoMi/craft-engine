package net.momirealms.craftengine.core.plugin.config;

import java.nio.file.Path;

public final class UnknownResourceException extends ResourceException {
    private final Path path;
    private final String node;

    public UnknownResourceException(Path file, String node, Throwable cause) {
        super(cause);
        this.path = file;
        this.node = node;
    }

    @Override
    public Path filePath() {
        return this.path;
    }

    @Override
    public String node() {
        return this.node;
    }
}
