package net.momirealms.craftengine.core.plugin.logger.filter;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.impl.MutableLogEvent;

@Plugin(name = "DisconnectLogFilter", category = Node.CATEGORY, elementType = Filter.ELEMENT_TYPE)
public final class DisconnectLogFilter extends AbstractFilter {
    private static final String TARGET_LOGGER = "net.minecraft.server.network.ServerConfigurationPacketListenerImpl";
    private static final String TARGET_MESSAGE_PATTERN = "{} lost connection: {}";
    private static DisconnectLogFilter instance;
    private boolean enable = false;

    public DisconnectLogFilter() {
        instance = this;
    }

    public static DisconnectLogFilter instance() {
        return instance;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    @Override
    public Result filter(LogEvent event) {
        if (!enable) {
            return Result.NEUTRAL;
        }

        if (!event.getLoggerName().equals(TARGET_LOGGER)) {
            return Result.NEUTRAL;
        }

        if (event.getMessage() instanceof MutableLogEvent msg) {
            String format = msg.getFormat();

            if (TARGET_MESSAGE_PATTERN.equals(format)) {
                return Result.DENY;
            }
        }
        return Result.NEUTRAL;
    }
}
