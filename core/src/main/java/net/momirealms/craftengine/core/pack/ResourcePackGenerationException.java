package net.momirealms.craftengine.core.pack;

public class ResourcePackGenerationException extends RuntimeException {

    public ResourcePackGenerationException(String message) {
        super(message);
    }

    public ResourcePackGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}