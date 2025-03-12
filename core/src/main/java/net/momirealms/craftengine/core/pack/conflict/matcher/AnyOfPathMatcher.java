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

import net.momirealms.craftengine.core.util.Key;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class AnyOfPathMatcher implements PathMatcher {
    public static final Factory FACTORY = new Factory();
    private final List<PathMatcher> matchers;

    public AnyOfPathMatcher(List<PathMatcher> matchers) {
        this.matchers = matchers;
    }

    @Override
    public Key type() {
        return PathMatchers.ANY_OF;
    }

    @Override
    public boolean test(Path path) {
        for (PathMatcher matcher : matchers) {
            if (matcher.test(path)) {
                return true;
            }
        }
        return false;
    }

    public static class Factory implements PathMatcherFactory {

        @SuppressWarnings("unchecked")
        @Override
        public PathMatcher create(Map<String, Object> arguments) {
            List<Map<String, Object>> terms = (List<Map<String, Object>>) arguments.get("terms");
            return new AnyOfPathMatcher(PathMatchers.fromMapList(terms));
        }
    }
}
