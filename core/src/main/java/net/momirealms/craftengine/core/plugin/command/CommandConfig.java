package net.momirealms.craftengine.core.plugin.command;

import java.util.ArrayList;
import java.util.List;

public class CommandConfig<C> {
    private boolean enable = false;
    private List<String> usages = new ArrayList<>();
    private String permission = null;

    private CommandConfig() {
    }

    public CommandConfig(boolean enable, List<String> usages, String permission) {
        this.enable = enable;
        this.usages = usages;
        this.permission = permission;
    }

    public boolean isEnable() {
        return enable;
    }

    public List<String> getUsages() {
        return usages;
    }

    public String getPermission() {
        return permission;
    }

    public static class Builder<C> {
        private final CommandConfig<C> config;

        public Builder() {
            this.config = new CommandConfig<>();
        }

        public Builder<C> usages(List<String> usages) {
            config.usages = usages;
            return this;
        }

        public Builder<C> permission(String permission) {
            config.permission = permission;
            return this;
        }

        public Builder<C> enable(boolean enable) {
            config.enable = enable;
            return this;
        }

        public CommandConfig<C> build() {
            return config;
        }
    }
}