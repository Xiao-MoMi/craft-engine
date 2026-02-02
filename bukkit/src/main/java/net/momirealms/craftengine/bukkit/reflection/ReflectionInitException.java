package net.momirealms.craftengine.bukkit.reflection;

public class ReflectionInitException extends RuntimeException {

    public ReflectionInitException(String message) {
        super(message);
    }

    public ReflectionInitException(Throwable cause) {
        super(cause);
    }

    public ReflectionInitException(String message, Throwable cause) {
        super(message, cause);
    }
}
