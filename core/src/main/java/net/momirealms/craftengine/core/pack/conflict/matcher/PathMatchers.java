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

package net.momirealms.craftengine.core.pack.conflict.matcher;

import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PathMatchers {
    public static final Key EXACT = Key.of("craftengine:exact");
    public static final Key CONTAINS = Key.of("craftengine:contains");
    public static final Key FILENAME = Key.of("craftengine:filename");
    public static final Key PARENT_PATH_SUFFIX = Key.of("craftengine:parent_path_suffix");
    public static final Key PARENT_PATH_PREFIX = Key.of("craftengine:parent_path_prefix");
    public static final Key PATTERN = Key.of("craftengine:pattern");
    public static final Key ANY_OF = Key.of("craftengine:any_of");
    public static final Key ALL_OF = Key.of("craftengine:all_of");
    public static final Key INVERTED = Key.of("craftengine:inverted");

    static {
        register(PARENT_PATH_SUFFIX, ParentPathSuffixMatcher.FACTORY);
        register(PARENT_PATH_PREFIX, ParentPathPrefixMatcher.FACTORY);
        register(PATTERN, PathPatternMatcher.FACTORY);
        register(EXACT, ExactPathMatcher.FACTORY);
        register(FILENAME, FilenameMatcher.FACTORY);
        register(ANY_OF, AnyOfPathMatcher.FACTORY);
        register(ALL_OF, AllOfPathMatcher.FACTORY);
        register(INVERTED, InvertedPathMatcher.FACTORY);
        register(CONTAINS, PathContainsMatcher.FACTORY);
    }

    public static void register(Key key, PathMatcherFactory factory) {
        Holder.Reference<PathMatcherFactory> holder = ((WritableRegistry<PathMatcherFactory>) BuiltInRegistries.PATH_MATCHER_FACTORY).registerForHolder(new ResourceKey<>(Registries.PATH_MATCHER_FACTORY.location(), key));
        holder.bindValue(factory);
    }

    public static List<PathMatcher> fromMapList(List<Map<String, Object>> arguments) {
        List<PathMatcher> matchers = new ArrayList<>();
        for (Map<String, Object> term : arguments) {
            matchers.add(PathMatchers.fromMap(term));
        }
        return matchers;
    }

    public static PathMatcher fromMap(Map<String, Object> map) {
        String type = (String) map.getOrDefault("type", "empty");
        if (type == null) {
            throw new NullPointerException("path matcher type cannot be null");
        }
        Key key = Key.withDefaultNamespace(type, "craftengine");
        PathMatcherFactory factory = BuiltInRegistries.PATH_MATCHER_FACTORY.getValue(key);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown matcher type: " + type);
        }
        return factory.create(map);
    }
}
