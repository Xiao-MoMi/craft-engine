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

package net.momirealms.craftengine.core.plugin.config.template;

import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

public class PlainStringTemplateArgument implements TemplateArgument {
    public static final Factory FACTORY = new Factory();
    private final String value;

    public PlainStringTemplateArgument(String value) {
        this.value = value;
    }

    @Override
    public String get() {
        return value;
    }

    @Override
    public Key type() {
        return TemplateArguments.PLAIN;
    }

    public static class Factory implements TemplateArgumentFactory {
        @Override
        public TemplateArgument create(Map<String, Object> arguments) {
            return new PlainStringTemplateArgument(arguments.getOrDefault("value", "").toString());
        }
    }
}
