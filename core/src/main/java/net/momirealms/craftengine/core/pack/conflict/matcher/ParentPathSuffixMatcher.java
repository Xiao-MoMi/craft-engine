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

public class ParentPathSuffixMatcher implements PathMatcher {
    public static final Factory FACTORY = new Factory();
    private final String suffix;

    public ParentPathSuffixMatcher(String suffix) {
        this.suffix = suffix;
    }

    @Override
    public boolean test(Path path) {
        Path parent = path.getParent();
        if (parent == null) return false;
        String pathStr = parent.toString().replace("\\", "/");
        return pathStr.endsWith(suffix);
    }

    @Override
    public Key type() {
        return PathMatchers.PARENT_PATH_SUFFIX;
    }

    public static class Factory implements PathMatcherFactory {

        @Override
        public PathMatcher create(Map<String, Object> arguments) {
            String suffix = (String) arguments.get("suffix");
            if (suffix == null) {
                throw new IllegalArgumentException("The suffix argument must not be null");
            }
            return new ParentPathSuffixMatcher(suffix);
        }
    }
}
