package net.momirealms.craftengine.core.plugin.command.sender;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.plugin.Plugin;
import net.momirealms.craftengine.core.util.Tristate;

import java.util.UUID;

/**
 * Simple implementation of {@link Sender} using a {@link SenderFactory}
 *
 * @param <T> the command sender type
 */
public final class AbstractSender<T> implements Sender {
    private final Plugin plugin;
    private final SenderFactory<?, T> factory;
    private final T sender;

    private final UUID uniqueId;
    private final String name;
    private final boolean isConsole;

    AbstractSender(Plugin plugin, SenderFactory<?, T> factory, T sender) {
        this.plugin = plugin;
        this.factory = factory;
        this.sender = sender;
        this.uniqueId = factory.uniqueId(this.sender);
        this.name = factory.name(this.sender);
        this.isConsole = this.factory.isConsole(this.sender);
    }

    @Override
    public Plugin plugin() {
        return this.plugin;
    }

    @Override
    public UUID uniqueId() {
        return this.uniqueId;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public void sendMessage(Component message) {
        this.factory.sendMessage(this.sender, message);
    }

    @Override
    public void sendMessage(Component message, boolean ignoreEmpty) {
        if (ignoreEmpty && message.equals(Component.empty())) {
            return;
        }
        sendMessage(message);
    }

    @Override
    public Tristate permissionState(String permission) {
        return (isConsole() && this.factory.consoleHasAllPermissions()) ? Tristate.TRUE : this.factory.permissionState(this.sender, permission);
    }

    @Override
    public boolean hasPermission(String permission) {
        return (isConsole() && this.factory.consoleHasAllPermissions()) || this.factory.hasPermission(this.sender, permission);
    }

    @Override
    public void performCommand(String commandLine) {
        this.factory.performCommand(this.sender, commandLine);
    }

    @Override
    public boolean isConsole() {
        return this.isConsole;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof AbstractSender<?> that)) return false;
        return this.uniqueId().equals(that.uniqueId());
    }

    @Override
    public int hashCode() {
        return this.uniqueId.hashCode();
    }
}
