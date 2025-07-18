package net.momirealms.craftengine.core.plugin.compatibility;

public final class PluginTask {
    private final Runnable task;
    private final Priority priority;
    private final String plugin;

    private PluginTask(Runnable task, String plugin, Priority priority) {
        this.task = task;
        this.priority = priority;
        this.plugin = plugin;
    }

    public static PluginTask create(Runnable task, String plugin, Priority priority) {
        return new PluginTask(task, plugin, priority);
    }

    public static PluginTask create(Runnable task, String plugin) {
        return new PluginTask(task, plugin, Priority.tail());
    }

    public String plugin() {
        return plugin;
    }

    public Priority priority() {
        return priority;
    }

    public Runnable task() {
        return task;
    }

    public static class Priority {
        public enum Position {
            BEFORE_PLUGIN,
            AFTER_PLUGIN,
            HEAD,
            TAIL
        }

        private final Position position;
        private final String relativePlugin;

        private Priority(Position position, String relativePlugin) {
            this.position = position;
            this.relativePlugin = relativePlugin;
        }

        public static Priority before(String pluginName) {
            return new Priority(Position.BEFORE_PLUGIN, pluginName);
        }

        public static Priority after(String pluginName) {
            return new Priority(Position.AFTER_PLUGIN, pluginName);
        }

        public static Priority head() {
            return new Priority(Position.HEAD, null);
        }

        public static Priority tail() {
            return new Priority(Position.TAIL, null);
        }

        Position position() {
            return position;
        }

        String relativePlugin() {
            return relativePlugin;
        }
    }
}