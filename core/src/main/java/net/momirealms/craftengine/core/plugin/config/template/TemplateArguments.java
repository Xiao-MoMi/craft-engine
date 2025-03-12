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

import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.Map;

public class TemplateArguments {
    public static final Key PLAIN = Key.of("craftengine:plain");
    public static final Key SELF_INCREASE_INT = Key.of("craftengine:self_increase_int");
    public static final Key MAP = Key.of("craftengine:map");
    public static final Key LIST = Key.of("craftengine:list");

    public static void register(Key key, TemplateArgumentFactory factory) {
        Holder.Reference<TemplateArgumentFactory> holder = ((WritableRegistry<TemplateArgumentFactory>) BuiltInRegistries.TEMPLATE_ARGUMENT_FACTORY)
                .registerForHolder(new ResourceKey<>(Registries.TEMPLATE_ARGUMENT_FACTORY.location(), key));
        holder.bindValue(factory);
    }

    static {
        register(PLAIN, PlainStringTemplateArgument.FACTORY);
        register(SELF_INCREASE_INT, SelfIncreaseIntTemplateArgument.FACTORY);
        register(MAP, MapTemplateArgument.FACTORY);
        register(LIST, ListTemplateArgument.FACTORY);
    }

    public static TemplateArgument fromMap(Map<String, Object> map) {
        String type = (String) map.get("type");
        if (type == null) {
            return MapTemplateArgument.FACTORY.create(map);
        } else {
            Key key = Key.withDefaultNamespace(type, "craftengine");
            TemplateArgumentFactory factory = BuiltInRegistries.TEMPLATE_ARGUMENT_FACTORY.getValue(key);
            if (factory == null) {
                throw new IllegalArgumentException("Unknown argument type: " + type);
            }
            return factory.create(map);
        }
    }
}
