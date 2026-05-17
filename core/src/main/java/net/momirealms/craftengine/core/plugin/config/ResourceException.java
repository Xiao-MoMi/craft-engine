package net.momirealms.craftengine.core.plugin.config;

import java.nio.file.Path;

public abstract class ResourceException extends RuntimeException {

    protected ResourceException() {
        super();
    }

    protected ResourceException(Throwable cause) {
        super(cause);
    }

    public abstract Path filePath();

    public abstract String node();
}
