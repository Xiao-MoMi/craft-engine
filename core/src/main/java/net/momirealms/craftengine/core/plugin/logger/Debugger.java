package net.momirealms.craftengine.core.plugin.logger;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;

import java.util.function.Supplier;

public enum Debugger {
    COMMON(Config::debugCommon),
    PACKET(Config::debugPacket),
    FURNITURE(Config::debugFurniture),
    RESOURCE_PACK(Config::debugFurniture),
    ITEM(Config::debugItem);

    private final Supplier<Boolean> condition;

    Debugger(Supplier<Boolean> condition) {
        this.condition = condition;
    }

    public void debug(Supplier<String> message) {
        if (this.condition.get()) {
            CraftEngine.instance().logger().info("[DEBUG] " + message.get());
        }
    }

    public void warn(Supplier<String> message, Throwable e) {
        if (this.condition.get()) {
            CraftEngine.instance().logger().warn("[DEBUG] " + message.get(), e);
        }
    }
}
