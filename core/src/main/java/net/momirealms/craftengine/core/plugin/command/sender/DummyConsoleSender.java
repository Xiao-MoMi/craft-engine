package net.momirealms.craftengine.core.plugin.command.sender;

import net.momirealms.craftengine.core.plugin.Plugin;

import java.util.UUID;

public abstract class DummyConsoleSender implements Sender {
    private final Plugin platform;

    public DummyConsoleSender(Plugin plugin) {
        this.platform = plugin;
    }

    @Override
    public void performCommand(String commandLine) {
    }

    @Override
    public boolean isConsole() {
        return true;
    }

    @Override
    public Plugin plugin() {
        return this.platform;
    }

    @Override
    public UUID uniqueId() {
        return Sender.CONSOLE_UUID;
    }

    @Override
    public String name() {
        return Sender.CONSOLE_NAME;
    }
}
