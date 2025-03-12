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
import java.util.Map;

public class InvertedPathMatcher implements PathMatcher {
    public static final Factory FACTORY = new Factory();
    private final PathMatcher matcher;

    public InvertedPathMatcher(PathMatcher matcher) {
        this.matcher = matcher;
    }

    @Override
    public Key type() {
        return PathMatchers.INVERTED;
    }

    @Override
    public boolean test(Path path) {
        return !matcher.test(path);
    }

    public static class Factory implements PathMatcherFactory {

        @SuppressWarnings("unchecked")
        @Override
        public PathMatcher create(Map<String, Object> arguments) {
            Map<String, Object> term = (Map<String, Object>) arguments.get("term");
            return new InvertedPathMatcher(PathMatchers.fromMap(term));
        }
    }
}
