package net.momirealms.craftengine.core.plugin.command.sender;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.plugin.Plugin;
import net.momirealms.craftengine.core.util.Tristate;

import java.util.UUID;

public interface Sender {
    UUID CONSOLE_UUID = new UUID(0, 0); // 00000000-0000-0000-0000-000000000000
    String CONSOLE_NAME = "Console";

    Plugin plugin();

    String name();

    UUID uniqueId();

    void sendMessage(Component message);

    void sendMessage(Component message, boolean ignoreEmpty);

    Tristate permissionState(String permission);

    boolean hasPermission(String permission);

    void performCommand(String commandLine);

    boolean isConsole();

    default boolean isValid() {
        return true;
    }

}
