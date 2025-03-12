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

public class FilenameMatcher implements PathMatcher {
    public static final Factory FACTORY = new Factory();
    private final String name;

    public FilenameMatcher(String name) {
        this.name = name;
    }

    @Override
    public boolean test(Path path) {
        String fileName = String.valueOf(path.getFileName());
        return fileName.equals(name);
    }

    @Override
    public Key type() {
        return PathMatchers.FILENAME;
    }

    public static class Factory implements PathMatcherFactory {

        @Override
        public PathMatcher create(Map<String, Object> arguments) {
            String name = (String) arguments.get("name");
            if (name == null) {
                throw new IllegalArgumentException("The name argument must not be null");
            }
            return new FilenameMatcher(name);
        }
    }
}
