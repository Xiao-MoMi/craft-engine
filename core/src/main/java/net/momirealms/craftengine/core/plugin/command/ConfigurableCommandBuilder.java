package net.momirealms.craftengine.core.plugin.command;

import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

public interface ConfigurableCommandBuilder<C> {

    ConfigurableCommandBuilder<C> permission(String permission);

    ConfigurableCommandBuilder<C> nodes(String... subNodes);

    Command.Builder<C> build();

    class BasicConfigurableCommandBuilder<C> implements ConfigurableCommandBuilder<C> {
        private Command.Builder<C> commandBuilder;

        public BasicConfigurableCommandBuilder(CommandManager<C> commandManager, String rootNode) {
            this.commandBuilder = commandManager.commandBuilder(rootNode);
        }

        @Override
        public ConfigurableCommandBuilder<C> permission(String permission) {
            this.commandBuilder = this.commandBuilder.permission(permission);
            return this;
        }

        @Override
        public ConfigurableCommandBuilder<C> nodes(String... subNodes) {
            for (String sub : subNodes) {
                this.commandBuilder = this.commandBuilder.literal(sub);
            }
            return this;
        }

        @Override
        public Command.Builder<C> build() {
            return commandBuilder;
        }
    }
}
