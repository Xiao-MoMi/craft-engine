/*
 * Copyright (C) <2025> <XiaoMoMi>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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