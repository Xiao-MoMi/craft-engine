package net.momirealms.craftengine.core.plugin.command.sender;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.plugin.Plugin;
import net.momirealms.craftengine.core.util.Tristate;

import java.util.Objects;
import java.util.UUID;

public abstract class SenderFactory<P extends Plugin, T> {
    private final P plugin;

    public SenderFactory(P plugin) {
        this.plugin = plugin;
    }

    protected P plugin() {
        return this.plugin;
    }

    protected abstract UUID uniqueId(T sender);

    protected abstract String name(T sender);

    protected abstract Audience audience(T sender);

    protected abstract void sendMessage(T sender, Component message);

    protected abstract Tristate permissionState(T sender, String node);

    protected abstract boolean hasPermission(T sender, String node);

    protected abstract void performCommand(T sender, String command);

    protected abstract boolean isConsole(T sender);

    protected boolean consoleHasAllPermissions() {
        return true;
    }

    public <C extends T> Sender wrap(C sender) {
        Objects.requireNonNull(sender, "sender");
        return new AbstractSender<>(this.plugin, this, sender);
    }

    public void close() {
    }
}
