package net.momirealms.craftengine.core.plugin.logger.filter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.Message;

public interface Log4JFilter {
    Filter.Result filter(LogEvent event);

    Filter.Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t);

    Filter.Result filter(Logger logger, Level level, Marker marker, String msg, Object... params);

    Filter.Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t);
}